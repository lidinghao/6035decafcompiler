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

public class ReachingStoresAnalyzer {
	private HashSet<CFGBlock> cfgBlocksToProcess;
	private HashMap<CFGBlock, BlockDataFlowState> cfgBlocksState;
	private HashMap<String, MethodIR> mMap;
	private HashMap<String, List<Name>> uniqueStores;
	
	public ReachingStoresAnalyzer(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.cfgBlocksToProcess = new HashSet<CFGBlock>();
		this.cfgBlocksState = new HashMap<CFGBlock, BlockDataFlowState>();
		this.uniqueStores = new HashMap<String, List<Name>>();
	}
	
	public void analyze() {
		this.cfgBlocksState.clear();
		this.uniqueStores.clear();
		
		for (String methodName: this.mMap.keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			initialize(methodName);
			runWorkList(methodName);
		}
	}

	private void runWorkList(String methodName) {
		int totalGlobals = this.uniqueStores.get(methodName).size();
		
		CFGBlock entry = this.getBlockById(methodName, 0);
		BlockDataFlowState entryBlockFlow = new BlockDataFlowState(totalGlobals); // OUT = GEN for entry block
		calculateGenKillSets(entry, entryBlockFlow);
		entryBlockFlow.setOut(entryBlockFlow.getGen());
		cfgBlocksToProcess.remove(entry);
		
		this.cfgBlocksState.put(entry, entryBlockFlow);
		
		while (cfgBlocksToProcess.size() != 0) {
			CFGBlock block = (CFGBlock)(cfgBlocksToProcess.toArray())[0];
			BlockDataFlowState bFlow = generateDFState(block);
			this.cfgBlocksState.put(block, bFlow);
		}		
	}
	
	private CFGBlock getBlockById(String name, int i) {
		if (this.mMap.containsKey(name)) {
			for (CFGBlock b: this.mMap.get(name).getCfgBlocks()) {
				if (b.getIndex() == i) return b;
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
				if (stmt.getClass().equals(QuadrupletStmt.class)) {
					QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
					
					// Only care about global assignments
					if (qStmt.getDestination().isGlobal()) temp.add(qStmt.getDestination());
				}
			}
			
			this.cfgBlocksToProcess.add(block);
		}
		
		this.uniqueStores.put(methodName, new ArrayList<Name>());
		this.uniqueStores.get(methodName).addAll(temp);
	}
	
	private BlockDataFlowState generateDFState(CFGBlock block) {
		int totalGlobals = this.uniqueStores.get(block.getMethodName()).size();
		// Get the original IN BitSet for this block
		BitSet origIn;
		if (this.cfgBlocksState.containsKey(block)) {
			origIn = this.cfgBlocksState.get(block).getOut();
		} else {
			origIn = new BitSet(totalGlobals);
			origIn.set(0, totalGlobals);
		}
		
		// Calculate the in BitSet by taking intersection of successor INs
		BlockDataFlowState bFlow = new BlockDataFlowState(totalGlobals);
		
		// If there exists at least one successor, set OUT to all True
		if (block.getSuccessors().size() > 0) {
			bFlow.getOut().set(0, totalGlobals);
		}
		
		BitSet out = bFlow.getOut();
		for (CFGBlock succ : block.getSuccessors()) {
			if (this.cfgBlocksState.containsKey(succ)) {
				out.and(this.cfgBlocksState.get(succ).getIn());
			}
		}
		
		calculateGenKillSets(block, bFlow);
		
		// Calculate Out
		BitSet in = bFlow.getIn(); // IN = (OUT - KILL) U GEN
		in.or(out);
		in.xor(bFlow.getKill());
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
	
	private void calculateGenKillSets(CFGBlock block, BlockDataFlowState bFlow) {		
		for (int i = block.getStatements().size()-1; i >= 0; i--) {
			LIRStatement stmt = block.getStatements().get(i);
			
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
				if (!qStmt.getDestination().getClass().equals(RegisterName.class)) {
					updateKillGenSet(block.getMethodName(), qStmt, bFlow);
				}
			}
			else if (stmt.getClass().equals(StoreStmt.class)) {
				StoreStmt sStmt = (StoreStmt) stmt;
				updateKillGenSet(block.getMethodName(), sStmt, bFlow);
			}
			else if (stmt.getClass().equals(CallStmt.class)) {
				updateKillGenSet(block.getMethodName(), (CallStmt)stmt, bFlow);
			}
		}
	}
	
	private void updateKillGenSet(String methodName, StoreStmt stmt,	BlockDataFlowState bFlow) {
		if (stmt == null) return;
		
		BitSet gen = bFlow.getGen();
		
		gen.set(this.uniqueStores.get(methodName).indexOf(stmt.getVariable()));	
	}

	private void updateKillGenSet(String methodName, CallStmt stmt, BlockDataFlowState bFlow) {
		if (stmt.getMethodLabel().equals(ProgramFlattener.exceptionHandlerLabel)) return;
		
		// Kill all globals!
		for (int i = 0; i < bFlow.getOut().size(); i++) {
			if (bFlow.getOut().get(i)) {
				bFlow.getKill().set(i);
			}
		}
		
		bFlow.getGen().clear();
	}

	public void updateKillGenSet(String methodName, QuadrupletStmt stmt, BlockDataFlowState bFlow) {
		if (stmt == null) return;
		
		BitSet out = bFlow.getOut();
		BitSet gen = bFlow.getGen();
		BitSet kill = bFlow.getKill();
		
		for (Name name : this.uniqueStores.get(methodName)) {
			if (name.equals(stmt.getDestination())) continue; // One store can cover multiple assignments
			
			int i = this.uniqueStores.get(methodName).indexOf(name);
			
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
					if (!arrName.getIndex().getClass().equals(ConstantName.class)) {
						if (arrName.getId().equals(dest.getId())) {
							resetName = true;
						}
					}
				}
			}
			
			if (resetName) {
				if (out.get(i)) {
					kill.set(i);
				}
				
				gen.clear(i);
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

	public HashMap<String, List<Name>> getStores() {
		return uniqueStores;
	}
}
