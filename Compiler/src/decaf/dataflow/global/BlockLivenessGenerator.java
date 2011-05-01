package decaf.dataflow.global;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.PopStmt;
import decaf.codegen.flatir.PushStmt;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.VarName;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

public class BlockLivenessGenerator {
	private HashMap<String, MethodIR> mMap;
	private HashMap<CFGBlock, BlockDataFlowState> blockLiveVars;
	private HashSet<CFGBlock> cfgBlocksToProcess;
	// One Variable per Name
	private HashMap<Name, Variable> nameToVar;
	// index to List<ArrayName> map where index is used
	private HashMap<Name, List<ArrayName>> nameToArrNames;

	// List of Variable IDs which correspond to global names
	private List<Integer> globalVarIDs;
	private int totalVars;
	
	public BlockLivenessGenerator(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		blockLiveVars = new HashMap<CFGBlock, BlockDataFlowState>();
		cfgBlocksToProcess = new HashSet<CFGBlock>();
		globalVarIDs = new ArrayList<Integer>();
		nameToVar = new HashMap<Name, Variable>();
		nameToArrNames = new HashMap<Name, List<ArrayName>>();
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
		
		System.out.println("AFTER LIVENESS");
		printGlobalVarIds();
		printNameToVar();
		printBlockLiveMap();
	}
	
	// Initialize the out BitSet for each CFG block that has no successors to the BitSet which
	// has 1s in the locations which correspond to global names, 0s everywhere else
	private void initializeOutSets() {
		for (String s: this.mMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			for (CFGBlock block: this.mMap.get(s).getCfgBlocks()) {
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
		Name dest = null, arg1 = null, arg2 = null;
		QuadrupletStmt qStmt;
		PopStmt popStmt;
		PushStmt pushStmt;
		CmpStmt cStmt;
		for (String s: this.mMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			for (CFGBlock block: this.mMap.get(s).getCfgBlocks()) {
				List<LIRStatement> blockStmts = block.getStatements();
				for (int i = 0; i < blockStmts.size(); i++) {
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
						// Update the Name to Variable map
						if (!nameToVar.containsKey(dest)) {
							nameToVar.put(dest, new Variable(dest));
							updateGlobalVarIDs(dest);
							if (dest.getClass().equals(ArrayName.class)) {
								// Update the index to ArrayName map
								Name arrIndex = ((ArrayName)dest).getIndex();
								if (!(nameToArrNames.containsKey(arrIndex))) {
									nameToArrNames.put(arrIndex, new ArrayList<ArrayName>());
								}
								if (!(nameToArrNames.get(arrIndex).contains((ArrayName)dest)))
									nameToArrNames.get(arrIndex).add((ArrayName)dest);
							}
						}
					}
					if (arg1 != null) {
						if (!nameToVar.containsKey(arg1)) {
							nameToVar.put(arg1, new Variable(arg1));
							updateGlobalVarIDs(arg1);
						}
					}
					if (arg2 != null) {
						if (!nameToVar.containsKey(arg2)) {
							nameToVar.put(arg2, new Variable(arg2));
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
					if (v != null) {
						globalVarIDs.add(v.getMyId());
					}
				}
			// ArrayName is by default global so check for that as well
			} else if (arg.getClass().equals(ArrayName.class)) {
				Variable v = nameToVar.get(arg);
				if (v != null) {
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
		Variable destVar, arg1Var;
		
		// Traverse statements in reverse order
		for (int i = blockStmts.size()-1; i >= 0; i--) {
			LIRStatement stmt = blockStmts.get(i);
			if (stmt.getClass().equals(CallStmt.class)) {
				CallStmt callStmt = (CallStmt) stmt;
				if (callStmt.getMethodLabel().equals(ProgramFlattener.exceptionHandlerLabel)) continue;
				
				// Set all global variable IDs to true in the gen set
				for (int globalVarId : globalVarIDs) {
					bFlow.getGen().set(globalVarId);
				}
				continue;
			}
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
					// Any ArrayName that have dest as an id should update Gen bit to true
					List<ArrayName> arrNamesWithDestIndex = nameToArrNames.get(dest);
					if (arrNamesWithDestIndex != null) {
						for (ArrayName aName : arrNamesWithDestIndex) {
							Variable aNameVar = nameToVar.get(aName);
							if (aNameVar != null) {
								bFlow.getGen().set(aNameVar.getMyId());
							}
						}
					}
				}
				// If dest is a ArrayName, process the index Name
				if (dest.getClass().equals(ArrayName.class)) {
					// the index Name was used so update Gen
					arrIndex = ((ArrayName)dest).getIndex();
					arg1Var = nameToVar.get(arrIndex);
					if (arg1Var != null) {	
						bFlow.getGen().set(arg1Var.getMyId());
					}
				}
			}
			processArgInMethodStatement(arg1, bFlow);	
			processArgInMethodStatement(arg2, bFlow);
		}
	}
	
	private void processArgInMethodStatement(Name arg, BlockDataFlowState bFlow) {
		Variable argVar;
		Name arrIndex;
		
		if (arg != null) {
			// Set arg2 -> id to true in current use set
			argVar = nameToVar.get(arg);
			if (argVar != null) {
				bFlow.getGen().set(argVar.getMyId());
			}
			// If arg2 is a ArrayName, process the index Name, and set
			// ALL Array names with the same ID to true in Gen set
			if (arg.getClass().equals(ArrayName.class)) {
				arrIndex = ((ArrayName)arg).getIndex();
				argVar = nameToVar.get(arrIndex);
				if (argVar != null) {	
					bFlow.getGen().set(argVar.getMyId());
				}
				String arrId = ((ArrayName)arg).getId();
				// Loop through all the names and find the ArrayNames with
				// the same ID
				for (Name n : nameToVar.keySet()) {
					if (n.getClass().equals(ArrayName.class)) {
						if (((ArrayName)n).getId().equals(arrId)) {
							// Note: if the index is a constant, then only set the ArrayNames
							// that have a non-constant index to true in the Gen set
							if (arrIndex.getClass().equals(ConstantName.class)) {
								// Index is not a constant
								if (!((ArrayName)n).getIndex().getClass().equals(ConstantName.class)) {
									bFlow.getGen().set(nameToVar.get(n).getMyId());
								}
							} else {
								bFlow.getGen().set(nameToVar.get(n).getMyId());
							}
						}
					}
				}
			}
		}
	}
	
	private void printNameToVar() {
		for (Name n : nameToVar.keySet()) {
			System.out.println("NAME: " + n + " --> " + nameToVar.get(n).getMyId());
		}
		System.out.println("----");
	}
	
	private void printBlockLiveMap() {
		for (CFGBlock block : blockLiveVars.keySet()) {
			System.out.println("BLOCK # " + block.getIndex());
			System.out.println(blockLiveVars.get(block));
			System.out.println("----");
		}
	}
	
	private void printGlobalVarIds() {
		System.out.println("GLOBAL VAR IDs: " + globalVarIDs);
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
	
	public HashMap<Name, List<ArrayName>> getNameToArrNames() {
		return nameToArrNames;
	}

	public void setNameToArrNames(HashMap<Name, List<ArrayName>> nameToArrNames) {
		this.nameToArrNames = nameToArrNames;
	}

	public List<Integer> getGlobalVarIDs() {
		return globalVarIDs;
	}

	public void setGlobalVarIDs(List<Integer> globalVarIDs) {
		this.globalVarIDs = globalVarIDs;
	}
}
