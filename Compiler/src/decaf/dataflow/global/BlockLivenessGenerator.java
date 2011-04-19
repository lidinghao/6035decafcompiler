package decaf.dataflow.global;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;

public class BlockLivenessGenerator {
	private HashMap<String, List<CFGBlock>> cfgMap;
	private HashMap<CFGBlock, BlockDataFlowState> blockLiveVars;
	private HashSet<CFGBlock> cfgBlocksToProcess;
	private HashMap<Name, HashSet<Integer>> nameToVarIds;
	private List<LiveVar> variables; // all the variables in the program
	private HashMap<QuadrupletStmt, List<LiveVar>> qStmtToVars; // each qStmt to the variables in it
	private int totalVars;
	
	public BlockLivenessGenerator(HashMap<String, List<CFGBlock>> cMap) {
		this.cfgMap = cMap;
		setNameToStmtIds(new HashMap<Name, HashSet<Integer>>());
		setBlockLiveVars(new HashMap<CFGBlock, BlockDataFlowState>());
		cfgBlocksToProcess = new HashSet<CFGBlock>();
		totalVars = 0;
	}
	
	public void generate() {
		initialize();
		List<CFGBlock> main = cfgMap.get("main");
		CFGBlock exit = main.get(main.size()-1); // getting the last block... is it always the exit node?
		BlockDataFlowState exitBlockFlow = new BlockDataFlowState(totalVars);
		calculateKillGenSets(exit, exitBlockFlow);
		exitBlockFlow.setIn(exitBlockFlow.getGen());
		cfgBlocksToProcess.remove(exit);
		blockLiveVars.put(exit, exitBlockFlow);
		
		while (cfgBlocksToProcess.size() != 0) {
			int lastIndex = cfgBlocksToProcess.toArray().length-1; // the last element for reverse order 
			CFGBlock block = (CFGBlock)(cfgBlocksToProcess.toArray())[lastIndex]; 
			BlockDataFlowState bFlow = generateForBlock(block);
			blockLiveVars.put(block, bFlow);
		}
		
	}
	
	private void initialize() {
		// LiveVar IDs that we assign should start from 0, so they can
		// correspond to the appropriate index in the BitSet
		LiveVar.setID(0);
		for (String s: this.cfgMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			for (CFGBlock block: this.cfgMap.get(s)) {
				List<LIRStatement> blockStmts = block.getStatements();
				for (int i = 0; i < blockStmts.size(); i++) {
					LIRStatement stmt = blockStmts.get(i);
					if (stmt.isExpressionStatement()) {
						QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
						Name dest = qStmt.getDestination(); 
						LiveVar dest_var = new LiveVar(dest);
						addToVariables(dest_var);
						Name arg1 = qStmt.getArg1();
						LiveVar arg1_var = new LiveVar(arg1);
						addToVariables(arg1_var);
						Name arg2 = qStmt.getArg2();
						LiveVar arg2_var = new LiveVar(arg2);
						addToVariables(arg2_var);
						//update the map q statement to the variables
						ArrayList<LiveVar> l = new ArrayList<LiveVar>(); 
						l.add(dest_var);
						l.add(arg1_var);
						l.add(arg2_var);
						qStmtToVars.put(qStmt, l); // can contain null for MOVE statements
					} 
						
				}
				cfgBlocksToProcess.add(block);
			}
		}
		totalVars = variables.size(); // the number of all variables
	}

	private void addToVariables(LiveVar var) {
		if(var == null) {
			LiveVar.setID(LiveVar.getID() - 1); // decrement the global ID back
		} else {
			if(variables.contains(var)) {
				var = variables.get(variables.indexOf(var));
				LiveVar.setID(LiveVar.getID() - 1); 
			} else {
				variables.add(var);
			}
		}
	}

	private BlockDataFlowState generateForBlock(CFGBlock block){
		BlockDataFlowState bFlow = new BlockDataFlowState(totalVars);
		BitSet out = bFlow.getOut();
		for (CFGBlock succ : block.getSuccessors()) {
			if (blockLiveVars.containsKey(succ)) {
				out.or(blockLiveVars.get(succ).getIn());
			}
		} 
		calculateKillGenSets(block, bFlow);
		// Calculate In
		BitSet in = bFlow.getIn();
		BitSet origIn = (BitSet)in.clone();
		in.or(out);
		in.xor(bFlow.getKill()); // Invariant: kill(def) is a subset of out
		in.or(bFlow.getGen());
		if (!in.equals(origIn)) {
			// Add successors to cfgBlocks list
			for (CFGBlock pred : block.getPredecessors()) {
				if (!cfgBlocksToProcess.contains(pred)) {
					cfgBlocksToProcess.add(pred);
				}
			}
		}
		// Remove this block, since it has been processed
		cfgBlocksToProcess.remove(block);
		blockLiveVars.put(block, bFlow);
		return bFlow;
	}
	
	private void calculateKillGenSets(CFGBlock block, BlockDataFlowState bFlow){
		//go in the reverse order and for each statement look at the qStmtToVars list and update the kill and gen bit vectors accordingly
		List<LIRStatement> blockStmts = block.getStatements();
		
		for (int i = blockStmts.size()-1; i >= 0; i--) {
			LIRStatement stmt = blockStmts.get(i);
			if (stmt.isExpressionStatement()) {
				QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
				if (qStmtToVars.containsKey(qStmt)) {
					bFlow.getKill().set(qStmtToVars.get(qStmt).get(0).getMyId(), true); // dest defined
					if (bFlow.getGen().get(qStmtToVars.get(qStmt).get(0).getMyId()) == true ) { //is dest is used after defined, remove from used/gen
						bFlow.getGen().set(qStmtToVars.get(qStmt).get(0).getMyId(), false); // dest set to false in used
					}
					bFlow.getGen().set(qStmtToVars.get(qStmt).get(1).getMyId(), true); // arg1 used
					if (qStmtToVars.get(qStmt).get(2) != null) {
						bFlow.getGen().set(qStmtToVars.get(qStmt).get(2).getMyId(), true); // arg2 used
					}
					
					
				}
			}
		}
		
	}
	/**
	private void updateKillSet(Name newDest, BlockDataFlowState bFlow) {
		BitSet kill = bFlow.getKill();
		BitSet out = bFlow.getOut();
		HashSet<Integer> stmtIdsForDest = nameToVarIds.get(newDest);
		if (stmtIdsForDest != null) {
			// Kill if it is part of In
			Iterator<Integer> it = stmtIdsForDest.iterator();
			while (it.hasNext()) {
				int index = it.next();
				if (out.get(index)) {
					kill.set(index, true); // Ensures def is always a subset of out
				}
			}
		}
	} **/
	
	public void setNameToStmtIds(HashMap<Name, HashSet<Integer>> nameToStmtIds) {
		this.nameToVarIds = nameToStmtIds;
	}

	public HashMap<Name, HashSet<Integer>> getNameToStmtIds() {
		return nameToVarIds;
	}

	public void setBlockLiveVars(HashMap<CFGBlock, BlockDataFlowState> blockLiveVars) {
		this.blockLiveVars = blockLiveVars;
	}

	public HashMap<CFGBlock, BlockDataFlowState> getBlockLiveVars() {
		return blockLiveVars;
	}
	
	

}
