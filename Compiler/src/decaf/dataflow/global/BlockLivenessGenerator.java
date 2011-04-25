package decaf.dataflow.global;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.PopStmt;
import decaf.codegen.flatir.PushStmt;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.VarName;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;

public class BlockLivenessGenerator {
	private HashMap<String, List<CFGBlock>> cfgMap;
	private HashMap<CFGBlock, BlockDataFlowState> blockLiveVars;
	private HashSet<CFGBlock> cfgBlocksToProcess;
	// One Variable per Name
	private HashMap<Name, Variable> nameToVar;
	// List of Variable IDs which correspond to global names
	private List<Integer> globalVarIDs;
	private int totalVars;
	
	public BlockLivenessGenerator(HashMap<String, List<CFGBlock>> cMap) {
		cfgMap = cMap;
		blockLiveVars = new HashMap<CFGBlock, BlockDataFlowState>();
		cfgBlocksToProcess = new HashSet<CFGBlock>();
		globalVarIDs = new ArrayList<Integer>();
		nameToVar = new HashMap<Name, Variable>();
		totalVars = 0;
	}
	
	public void generate() {
		initializeLiveVars();
		initializeOutSets();
		
		while (cfgBlocksToProcess.size() != 0) {
			CFGBlock block = (CFGBlock)(cfgBlocksToProcess.toArray())[0]; 
			BlockDataFlowState bFlow = generateForBlock(block);
			blockLiveVars.put(block, bFlow);
		}
		
		printNameToVar();
		printBlockLiveMap();
	}
	
	// Initialize the out BitSet for each CFG block that has no successors to the BitSet which
	// has 1s in the locations which correspond to global names, 0s everywhere else
	private void initializeOutSets() {
		for (String s: this.cfgMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			for (CFGBlock block: this.cfgMap.get(s)) {
				if (!block.getSuccessors().isEmpty())
					continue;
				BlockDataFlowState bFlow = new BlockDataFlowState(totalVars);
				BitSet out = bFlow.getOut();
				for (Integer globalId : globalVarIDs) {
					out.set(globalId);
				}
				// Kill = Defs
				// Gen = Use
				calculateUseDefSets(block, bFlow);
				BitSet in = bFlow.getIn();
				in.or(out);
				// Kill is not a subset of out, so iterate manually
				for (int i = 0; i < bFlow.getKill().size(); i++) {
					if (bFlow.getKill().get(i)) {
						if (bFlow.getIn().get(i)) {
							bFlow.getIn().set(i, false);
						}
					}
				}			
				in.or(bFlow.getGen());
				cfgBlocksToProcess.remove(block);
				blockLiveVars.put(block, bFlow);
			}
		}
	}
	
	private void initializeLiveVars() {
		// LiveVar IDs that we assign should start from 0, so they can
		// correspond to the appropriate index in the BitSet
		Variable.setID(0);
		for (String s: this.cfgMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			for (CFGBlock block: this.cfgMap.get(s)) {
				List<LIRStatement> blockStmts = block.getStatements();
				for (int i = 0; i < blockStmts.size(); i++) {
					LIRStatement stmt = blockStmts.get(i);
					if (stmt.getClass().equals(QuadrupletStmt.class)) {
						QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
						Name dest = qStmt.getDestination();
						if (!nameToVar.containsKey(dest)) {
							nameToVar.put(dest, new Variable(dest));
							updateGlobalVarIDs(dest);
						}
						Name arg1 = qStmt.getArg1();
						if (!nameToVar.containsKey(arg1)) {
							nameToVar.put(dest, new Variable(arg1));
							updateGlobalVarIDs(arg1);
						}
						Name arg2 = qStmt.getArg2();
						if (!nameToVar.containsKey(arg2)) {
							nameToVar.put(dest, new Variable(arg2));
							updateGlobalVarIDs(arg2);
						}
					} 
						
				}
				cfgBlocksToProcess.add(block);
			}
		}
		totalVars = nameToVar.size(); // The number of all variables
	}

	private void updateGlobalVarIDs(Name arg) {
		if (arg != null) {
			if (arg.getClass().equals(VarName.class)) {
				VarName var = (VarName) arg;
				if (var.getBlockId() == -1) { // Global
					Variable v = nameToVar.get(arg);
					if (v != null)
						globalVarIDs.add(v.getMyId());
				}
			}
		}
	}
	
	private BlockDataFlowState generateForBlock(CFGBlock block){
		BitSet origIn;
		if (blockLiveVars.containsKey(block)) {
			origIn = blockLiveVars.get(block).getIn();
		} else {
			origIn = new BitSet(totalVars);
		}
		BlockDataFlowState bFlow = new BlockDataFlowState(totalVars);
		BitSet out = bFlow.getOut();
		for (CFGBlock succ : block.getSuccessors()) {
			if (blockLiveVars.containsKey(succ)) {
				out.or(blockLiveVars.get(succ).getIn());
			}
		} 
		calculateUseDefSets(block, bFlow);
		// Calculate In
		BitSet in = bFlow.getIn();
		in.or(out);
		// Kill (def) is not always a subset of out, so iterate manually
		for (int i = 0; i < bFlow.getKill().size(); i++) {
			if (bFlow.getKill().get(i)) {
				if (bFlow.getIn().get(i)) {
					bFlow.getIn().set(i, false);
				}
			}
		}
		in.or(bFlow.getGen());
		if (!in.equals(origIn)) {
			// Add predecessors to cfgBlocks list
			for (CFGBlock pred : block.getPredecessors()) {
				if (!cfgBlocksToProcess.contains(pred)) {
					cfgBlocksToProcess.add(pred);
				}
			}
		}
		// Remove this block, since it has been processed
		cfgBlocksToProcess.remove(block);
		return bFlow;
	}
	
	private void calculateUseDefSets(CFGBlock block, BlockDataFlowState bFlow){
		List<LIRStatement> blockStmts = block.getStatements();
		PopStmt popStmt;
		PushStmt pushStmt;
		CmpStmt cStmt;
		QuadrupletStmt qStmt;
		Name arg1 = null, arg2 = null, dest = null, arrIndex = null;
		Variable destVar, arg1Var, arg2Var;
		
		// Traverse statements in reverse order
		for (int i = blockStmts.size()-1; i >= 0; i--) {
			LIRStatement stmt = blockStmts.get(i);
			
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				qStmt = (QuadrupletStmt)stmt;
				dest = qStmt.getDestination();
				arg1 = qStmt.getArg1();
				arg2 = qStmt.getArg2();
			} else if (stmt.getClass().equals(PopStmt.class)) {
				popStmt = (PopStmt)stmt;
				arg1 = popStmt.getName();
				
			} else if (stmt.getClass().equals(PushStmt.class)) {
				pushStmt = (PushStmt)stmt;
				arg1 = pushStmt.getName();
				
			} else if (stmt.getClass().equals(CmpStmt.class)) {
				cStmt = (CmpStmt)stmt;
				arg1 = cStmt.getArg1();
				arg2 = cStmt.getArg2();
			}
			if (dest != null) {
				// Set dest -> id to true in current def set
				destVar = nameToVar.get(dest);
				if (destVar != null) {
					bFlow.getKill().set(destVar.getMyId());
					// Set dest -> id to false in current use set
					bFlow.getGen().set(destVar.getMyId(), false);
				}
			}
			if (arg1 != null) {
				// Set arg1 -> id to true in current use set
				arg1Var = nameToVar.get(arg1);
				if (arg1Var != null) {	
					bFlow.getGen().set(arg1Var.getMyId());
				}
				// If arg1 is a ArrayName, process the index Name
				if (arg1.getClass().equals(ArrayName.class)) {
					arrIndex = ((ArrayName)arg1).getIndex();
					arg1Var = nameToVar.get(arrIndex);
					if (arg1Var != null) {	
						bFlow.getGen().set(arg1Var.getMyId());
					}
				}
			}
			if (arg2 != null) {
				// Set arg2 -> id to true in current use set
				arg2Var = nameToVar.get(arg2);
				if (arg2Var != null) {
					bFlow.getGen().set(arg2Var.getMyId());
				}
				// If arg2 is a ArrayName, process the index Name
				if (arg2.getClass().equals(ArrayName.class)) {
					arrIndex = ((ArrayName)arg2).getIndex();
					arg2Var = nameToVar.get(arrIndex);
					if (arg2Var != null) {	
						bFlow.getGen().set(arg2Var.getMyId());
					}
				}
			}
		}
	}

	public void printNameToVar() {
		for (Name n : nameToVar.keySet()) {
			System.out.println("NAME: " + n + " --> " + nameToVar.get(n).getMyId());
		}
		System.out.println("----");
	}
	
	public void printBlockLiveMap() {
		for (CFGBlock block : blockLiveVars.keySet()) {
			System.out.println("BLOCK # " + block.getIndex());
			System.out.println(blockLiveVars.get(block));
			System.out.println("----");
		}
	}
	
	public void setBlockLiveVars(HashMap<CFGBlock, BlockDataFlowState> blockLiveVars) {
		this.blockLiveVars = blockLiveVars;
	}

	public HashMap<CFGBlock, BlockDataFlowState> getBlockLiveVars() {
		return blockLiveVars;
	}
	
	public HashMap<Name, Variable> getNameToVar() {
		return nameToVar;
	}

	public void setNameToVar(HashMap<Name, Variable> nameToVar) {
		this.nameToVar = nameToVar;
	}
}
