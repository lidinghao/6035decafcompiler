package decaf.memory;

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
import decaf.codegen.flatir.LoadStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.PopStmt;
import decaf.codegen.flatir.PushStmt;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flatir.StoreStmt;
import decaf.codegen.flatir.VarName;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;
import decaf.dataflow.global.BlockDataFlowState;

public class ReachingGlobalDefinitions {
	private HashSet<CFGBlock> cfgBlocksToProcess;
	private HashMap<CFGBlock, BlockDataFlowState> cfgBlocksState;
	private HashMap<String, MethodIR> mMap;
	private HashMap<String, List<Name>> uniqueGlobals;
	
	public ReachingGlobalDefinitions(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.cfgBlocksToProcess = new HashSet<CFGBlock>();
		this.cfgBlocksState = new HashMap<CFGBlock, BlockDataFlowState>();
		this.uniqueGlobals = new HashMap<String, List<Name>>();
	}
	
	// Each QuadrupletStmt will have a unique ID
	public void analyze() {
		this.cfgBlocksState.clear();
		this.uniqueGlobals.clear();
		
		for (String methodName: this.mMap.keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			initialize(methodName);
			runWorkList(methodName);
		}
	}

	private void runWorkList(String methodName) {
		int totalGlobals = this.uniqueGlobals.get(methodName).size();
		
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
					
					if (isValidName(qStmt.getDestination())) temp.add(qStmt.getDestination());
					if (isValidName(qStmt.getArg1())) temp.add(qStmt.getArg1());
					if (isValidName(qStmt.getArg2())) temp.add(qStmt.getArg2());
				}
				else if (stmt.getClass().equals(CmpStmt.class)) {
					CmpStmt cStmt = (CmpStmt) stmt;
					if (isValidName(cStmt.getArg1())) temp.add(cStmt.getArg1());
					if (isValidName(cStmt.getArg2())) temp.add(cStmt.getArg2());
				}
				else if (stmt.getClass().equals(PushStmt.class)) {
					PushStmt pStmt = (PushStmt) stmt;
					
					if (isValidName(pStmt.getName())) temp.add(pStmt.getName());
				}
				else if (stmt.getClass().equals(PopStmt.class)) {
					PopStmt pStmt = (PopStmt) stmt;
					
					if (isValidName(pStmt.getName())) temp.add(pStmt.getName());
				}
				else if (stmt.getClass().equals(LoadStmt.class)) {
					LoadStmt lStmt = (LoadStmt) stmt;
					temp.add(lStmt.getVariable());
				}
				else if (stmt.getClass().equals(StoreStmt.class)) {
					StoreStmt sStmt = (StoreStmt) stmt;
					temp.add(sStmt.getVariable());
				}
			}
			
			this.cfgBlocksToProcess.add(block);
		}
		
		this.uniqueGlobals.put(methodName, new ArrayList<Name>());
		this.uniqueGlobals.get(methodName).addAll(temp);
	}
	
	private boolean isValidName(Name name) {
		if (name == null) return false;
		
		if (name.getClass().equals(VarName.class)) {
			VarName var = (VarName) name;
			
			if (var.isString()) return false;
			
			if (var.isStackParam() && var.getBlockId() == -2) {
				return true;
			}
		}
		
		if (name.isGlobal()) return true;
		
		return false;
	}
	
	private BlockDataFlowState generateDFState(CFGBlock block) {
		int totalGlobals = this.uniqueGlobals.get(block.getMethodName()).size();
		// Get the original out BitSet for this block
		BitSet origOut;
		if (this.cfgBlocksState.containsKey(block)) {
			origOut = this.cfgBlocksState.get(block).getOut();
		} else {
			origOut = new BitSet(totalGlobals);
			origOut.set(0, totalGlobals);
		}
		
		// Calculate the in BitSet by taking intersection of predecessors
		BlockDataFlowState bFlow = new BlockDataFlowState(totalGlobals);
		
		// If there exists at least one predecessor, set In to all True
		if (block.getPredecessors().size() > 0) {
			bFlow.getIn().set(0, totalGlobals);
		}
		
		BitSet in = bFlow.getIn();
		for (CFGBlock pred : block.getPredecessors()) {
			if (this.cfgBlocksState.containsKey(pred)) {
				in.and(this.cfgBlocksState.get(pred).getOut());
			}
		}
		
		calculateGenKillSets(block, bFlow);
		
		// Calculate Out
		BitSet out = bFlow.getOut(); // OUT = (IN - KILL) U GEN
		out.or(in);
		out.xor(bFlow.getKill());
		out.or(bFlow.getGen());
		
		if (!out.equals(origOut)) {
			// Add successors to cfgBlocks list
			for (CFGBlock succ : block.getSuccessors()) {
				if (!cfgBlocksToProcess.contains(succ)) {
					cfgBlocksToProcess.add(succ);
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
		
		for (LIRStatement stmt : blockStmts) {
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				qStmt = (QuadrupletStmt)stmt;
				if (!qStmt.getDestination().getClass().equals(RegisterName.class)) {
					updateKillGenSet(block.getMethodName(), qStmt, bFlow);
				}
			}
			else if (stmt.getClass().equals(LoadStmt.class)) {
				updateKillGenSet(block.getMethodName(), (LoadStmt)stmt, bFlow);
			}
			else if (stmt.getClass().equals(CallStmt.class)) {
				updateKillGenSet(block.getMethodName(), (CallStmt)stmt, bFlow);
			}
		}
	}
	
	private void updateKillGenSet(String methodName, CallStmt stmt, BlockDataFlowState bFlow) {
		if (stmt.getMethodLabel().equals(ProgramFlattener.exceptionHandlerLabel)) return;
		
		// Kill all globals!
		for (int i = 0; i < bFlow.getIn().size(); i++) {
			if (bFlow.getIn().get(i)) {
				if (this.uniqueGlobals.get(methodName).get(i).getClass().equals(VarName.class)) {
					VarName var = (VarName) this.uniqueGlobals.get(methodName).get(i);
					if (var.getBlockId() == -2) continue; // dont kill stack param
				}
				bFlow.getKill().set(i);
			}
		}
		
		bFlow.getGen().clear();
	}

	private void updateKillGenSet(String methodName, LoadStmt stmt, BlockDataFlowState bFlow) {
		if (stmt == null) return;
		
		BitSet gen = bFlow.getGen();
		
		gen.set(this.uniqueGlobals.get(methodName).indexOf(stmt.getVariable()));	
	}

	public void updateKillGenSet(String methodName, QuadrupletStmt stmt, BlockDataFlowState bFlow) {
		if (stmt == null) return;
		
		BitSet in = bFlow.getIn();
		BitSet gen = bFlow.getGen();
		BitSet kill = bFlow.getKill();
		
		for (Name name : this.uniqueGlobals.get(methodName)) {
			int i = this.uniqueGlobals.get(methodName).indexOf(name);
			
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
						if (dest.getId().equals(arrName.getId())) {
							resetName = true;
						}
					}
				}
			}
			
			if (resetName) {
				if (in.get(i)) {
					kill.set(i);
				}
				
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
		return uniqueGlobals;
	}

	public void setUniqueGlobals(HashMap<String, List<Name>> uniqueGlobals) {
		this.uniqueGlobals = uniqueGlobals;
	}
}
