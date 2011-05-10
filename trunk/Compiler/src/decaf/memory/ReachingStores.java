package decaf.memory;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flatir.StoreStmt;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;
import decaf.dataflow.global.BlockDataFlowState;

public class ReachingStores {
	private HashSet<CFGBlock> cfgBlocksToProcess;
	private HashMap<CFGBlock, BlockDataFlowState> cfgBlocksState;
	private HashMap<String, MethodIR> mMap;
	private HashMap<String, List<Name>> globalStores;
	
	public ReachingStores(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.cfgBlocksToProcess = new HashSet<CFGBlock>();
		this.cfgBlocksState = new HashMap<CFGBlock, BlockDataFlowState>();
		this.globalStores = new HashMap<String, List<Name>>();
	}
	
	// Each QuadrupletStmt will have a unique ID
	public void analyze() {
		this.cfgBlocksState.clear();
		this.globalStores.clear();
		
		for (String methodName: this.mMap.keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			initialize(methodName);
			runWorkList(methodName);
		}
	}

	private void runWorkList(String methodName) {
//		int totalGlobals = this.globalStores.get(methodName).size();
		
		this.setExitBlock(methodName);
		
//		CFGBlock exit = this.getExitBlock(methodName);
//		BlockDataFlowState entryBlockFlow = new BlockDataFlowState(totalGlobals); // IN = GEN for entry block
//		calculateGenKillSets(exit, entryBlockFlow);
//		entryBlockFlow.setIn(entryBlockFlow.getGen());
//		cfgBlocksToProcess.remove(exit);
		
//		this.cfgBlocksState.put(exit, entryBlockFlow);
		
		while (cfgBlocksToProcess.size() != 0) {
			CFGBlock block = (CFGBlock)(cfgBlocksToProcess.toArray())[0];
			BlockDataFlowState bFlow = generateDFState(block);
			this.cfgBlocksState.put(block, bFlow);
		}		
	}
	
	private void setExitBlock(String methodName) {
		for (CFGBlock block: this.mMap.get(methodName).getCfgBlocks()) {
			if (block.getSuccessors().size() == 0) {
//				return block;
				CFGBlock exit = block;
				BlockDataFlowState entryBlockFlow = new BlockDataFlowState(this.globalStores.get(methodName).size()); // IN = GEN for entry block
				calculateGenKillSets(exit, entryBlockFlow);
				entryBlockFlow.setIn(entryBlockFlow.getGen());
				cfgBlocksToProcess.remove(exit);
				
				this.cfgBlocksState.put(exit, entryBlockFlow);
			}
		}
	}
	
	private CFGBlock getExitBlock(String name) {
		if (this.mMap.containsKey(name)) {
			for (CFGBlock b: this.mMap.get(name).getCfgBlocks()) {
				if (b.getSuccessors().size() == 0) return b;
			}
		}
		
		return null;
	}

	private void initialize(String methodName) {
		this.cfgBlocksToProcess.clear();
		HashSet<Name> temp = new HashSet<Name>();
		
		for (CFGBlock block: this.mMap.get(methodName).getCfgBlocks()) {
			List<LIRStatement> blockStmts = block.getStatements();
			for (int i = 0; i < blockStmts.size(); i++) {
				LIRStatement stmt = blockStmts.get(i);
				if (stmt.getClass().equals(StoreStmt.class)) {
					StoreStmt lStmt = (StoreStmt)stmt;
					
					temp.add(lStmt.getVariable());
				}
				else if (stmt.getClass().equals(QuadrupletStmt.class)) {
					QuadrupletStmt qStmt = (QuadrupletStmt) stmt;
					if (qStmt.getDestination().isGlobal()) {
						temp.add(qStmt.getDestination());
					}
				}
			}
			
			this.cfgBlocksToProcess.add(block);
		}
		
		this.globalStores.put(methodName, new ArrayList<Name>());
		this.globalStores.get(methodName).addAll(temp);
	}
	
	private BlockDataFlowState generateDFState(CFGBlock block) {
		int totalGlobals = this.globalStores.get(block.getMethodName()).size();
		// Get the original out BitSet for this block
		BitSet origIn;
		if (this.cfgBlocksState.containsKey(block)) {
			origIn = this.cfgBlocksState.get(block).getOut();
		} else {
			origIn = new BitSet(totalGlobals);
			origIn.set(0, totalGlobals);
		}
		
		// Calculate the in BitSet by taking intersection of predecessors
		BlockDataFlowState bFlow = new BlockDataFlowState(totalGlobals);
		
		// If there exists at least one successor, set Out to all True
		if (block.getSuccessors().size() > 0) {
			bFlow.getOut().set(0, totalGlobals);
		}
		
		BitSet out = bFlow.getOut();
		for (CFGBlock pred : block.getSuccessors()) {
			if (this.cfgBlocksState.containsKey(pred)) {
				out.and(this.cfgBlocksState.get(pred).getOut());
			}
		}
		
		calculateGenKillSets(block, bFlow);
		
		// Calculate Out
		BitSet in = bFlow.getOut(); // IN = (OUT - KILL) U GEN
		in.or(out);
		in.xor(bFlow.getKill());
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
		
		return bFlow;
	}
	
	private void calculateGenKillSets(CFGBlock block, BlockDataFlowState bFlow) {
		List<LIRStatement> blockStmts = block.getStatements();
		QuadrupletStmt qStmt;
		
		for (int i = blockStmts.size()-1; i >= 0; i--) {
			LIRStatement stmt = blockStmts.get(i);
			
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				qStmt = (QuadrupletStmt)stmt;
				if (!qStmt.getDestination().getClass().equals(RegisterName.class)) {
					updateKillGenSet(block.getMethodName(), qStmt, bFlow);
				}
			}
			else if (stmt.getClass().equals(StoreStmt.class)) {
				updateKillGenSet(block.getMethodName(), (StoreStmt)stmt, bFlow);
			}
			else if (stmt.getClass().equals(CallStmt.class)) {
				updateKillGenSet(block.getMethodName(), (CallStmt)stmt, bFlow);
			}
		}
	}
	
	private void updateKillGenSet(String methodName, CallStmt stmt, BlockDataFlowState bFlow) {
		if (stmt.getMethodLabel().equals(ProgramFlattener.exceptionHandlerLabel)) return;
		
		// Kill all stores!
		for (int i = 0; i < bFlow.getIn().size(); i++) {
			if (bFlow.getOut().get(i)) {
				bFlow.getKill().set(i);
			}
		}
		
		bFlow.getGen().clear();
	}

	private void updateKillGenSet(String methodName, StoreStmt stmt, BlockDataFlowState bFlow) {
		if (stmt == null) return;
		
		BitSet gen = bFlow.getGen();
		
		int i = this.globalStores.get(methodName).indexOf(stmt.getVariable());
		if (bFlow.getOut().get(i) || bFlow.getGen().get(i)) {
			stmt.setDead(true);
		}
		else {
			stmt.setDead(false);
			gen.set(this.globalStores.get(methodName).indexOf(stmt.getVariable()));	
		}
	}

	public void updateKillGenSet(String methodName, QuadrupletStmt stmt, BlockDataFlowState bFlow) {
		if (stmt == null) return;
		
		BitSet in = bFlow.getIn();
		BitSet gen = bFlow.getGen();
		
		for (Name name : this.globalStores.get(methodName)) {
			int i = this.globalStores.get(methodName).indexOf(name);
			
			boolean resetName = false;
			
			if (name.isArray()) {
				Name myName = name;
				
				do {
					ArrayName array = (ArrayName) myName;
					if (array.getIndex().equals(stmt.getDestination())) { // Index being reassigned, KILL!
						resetName = true;
					}
					
					myName = array.getIndex();
					
				} while (myName.isArray());
				
				if (stmt.getDestination().isArray()) {
					ArrayName dest = (ArrayName) stmt.getDestination();
					ArrayName arrName = (ArrayName) name;
					if (dest.getIndex().getClass().equals(ConstantName.class) &&
							!arrName.getIndex().getClass().equals(ConstantName.class)) {
						if (arrName.getId().equals(dest.getId())) {
							resetName = true;
						}
					}
				}
			}
			
			if (resetName) {
				in.clear(i);
				gen.clear(i);
			}
			
			if (name.equals(stmt.getDestination())) {
				gen.set(i);
			}
		}		
	}

	public HashMap<CFGBlock, BlockDataFlowState> getCfgBlocksState() {
		return cfgBlocksState;
	}

	public void setCfgBlocksState(
			HashMap<CFGBlock, BlockDataFlowState> cfgBlocksState) {
		this.cfgBlocksState = cfgBlocksState;
	}

	public HashMap<String, List<Name>> getUniqueGlobals() {
		return globalStores;
	}

	public void setUniqueGlobals(HashMap<String, List<Name>> uniqueGlobals) {
		this.globalStores = uniqueGlobals;
	}
}
