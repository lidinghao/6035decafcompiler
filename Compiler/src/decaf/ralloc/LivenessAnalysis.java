package decaf.ralloc;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
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
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;
import decaf.dataflow.global.BlockDataFlowState;

public class LivenessAnalysis {
	private HashSet<CFGBlock> cfgBlocksToProcess;
	private HashMap<CFGBlock, BlockDataFlowState> cfgBlocksState;
	private HashMap<String, MethodIR> mMap;
	private HashMap<String, List<Name>> uniqueVariables;
	
	public LivenessAnalysis(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.cfgBlocksToProcess = new HashSet<CFGBlock>();
		this.cfgBlocksState = new HashMap<CFGBlock, BlockDataFlowState>();
		this.uniqueVariables = new HashMap<String, List<Name>>();
	}
	
	// Each QuadrupletStmt will have a unique ID
	public void analyze() {
		for (String methodName: this.mMap.keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			initialize(methodName);
			runWorkList(methodName);
		}
	}

	private void runWorkList(String methodName) {
		int totalDefs = this.uniqueVariables.get(methodName).size();
		
		CFGBlock exit = this.getExitBlock(methodName);
		BlockDataFlowState exitBlockFlow = new BlockDataFlowState(totalDefs); // IN = GEN for exit block
		calculateGenKillSets(exit, exitBlockFlow);
		exitBlockFlow.setIn(exitBlockFlow.getGen());
		cfgBlocksToProcess.remove(exit);
		
		this.cfgBlocksState.put(exit, exitBlockFlow);
		
		while (cfgBlocksToProcess.size() != 0) {
			CFGBlock block = (CFGBlock)(cfgBlocksToProcess.toArray())[0];
			BlockDataFlowState bFlow = generateDFState(block);
			this.cfgBlocksState.put(block, bFlow);
		}		
	}
	
	private CFGBlock getExitBlock(String methodName) {
		for (CFGBlock block: this.mMap.get(methodName).getCfgBlocks()) {
			if (block.getSuccessors().size() == 0) return block;
		}
		
		return null;
	}

	// Each QuadrupletStmt will have unique ID, as it is a definition
	private void initialize(String methodName) {
		HashSet<Name> temp = new HashSet<Name>();
		this.uniqueVariables.put(methodName, new ArrayList<Name>());
		this.cfgBlocksToProcess.clear();
		
		for (CFGBlock block: this.mMap.get(methodName).getCfgBlocks()) {
			List<LIRStatement> blockStmts = block.getStatements();
			for (int i = 0; i < blockStmts.size(); i++) {
				LIRStatement stmt = blockStmts.get(i);
				
				if (stmt.getClass().equals(QuadrupletStmt.class)) {
					QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
					
					temp.add(qStmt.getArg1());
					temp.add(qStmt.getDestination());					
					if (qStmt.getArg2() != null) temp.add(qStmt.getArg2());
				}
				else if (stmt.getClass().equals(LoadStmt.class)) {
					LoadStmt lStmt = (LoadStmt)stmt;
					temp.add(lStmt.getVariable());					
				}
				else if (stmt.getClass().equals(StoreStmt.class)) {
					StoreStmt sStmt = (StoreStmt)stmt;
					temp.add(sStmt.getVariable());
				}
				else if (stmt.getClass().equals(CmpStmt.class)) {
					CmpStmt cStmt = (CmpStmt)stmt;
					
					temp.add(cStmt.getArg1());	
					temp.add(cStmt.getArg2());
				}
				else if (stmt.getClass().equals(PushStmt.class)) {
					PushStmt pStmt = (PushStmt) stmt;
					temp.add(pStmt.getName());
				}
				else if (stmt.getClass().equals(PopStmt.class)) {
					PopStmt pStmt = (PopStmt) stmt;
					temp.add(pStmt.getName());
				}
			}
			
			this.cfgBlocksToProcess.add(block);
		}
		
		for (Name n: temp) {
			if (n.getClass().equals(ConstantName.class) || n.getClass().equals(RegisterName.class)) continue;
			
			this.uniqueVariables.get(methodName).add(n);
		}
		
		System.out.println("VARS : " + this.uniqueVariables.get(methodName));
	}
	
	private BlockDataFlowState generateDFState(CFGBlock block) {
		int totalVars = this.uniqueVariables.get(block.getMethodName()).size();
		// Get the original in BitSet for this block
		BitSet origIn;
		if (this.cfgBlocksState.containsKey(block)) {
			origIn = this.cfgBlocksState.get(block).getIn();
		} else {
			origIn = new BitSet(totalVars);
			origIn.set(0, totalVars);
		}
		
		// Calculate the in BitSet by taking union of predecessors
		BlockDataFlowState bFlow = new BlockDataFlowState(totalVars);
		
		// If there exist at least one successor, set Out to all True
		if (block.getSuccessors().size() > 0) {
			bFlow.getOut().set(0, totalVars);
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
	
	private BitSet getCurrentInSet(BlockDataFlowState bFlow) {
		BitSet in = (BitSet)bFlow.getIn().clone(); // IN = (OUT - KILL) U GEN
		in.or(bFlow.getOut());
		in.xor(bFlow.getKill());
		in.or(bFlow.getGen());
		
		return in;
	}
	
	private void calculateGenKillSets(CFGBlock block, BlockDataFlowState bFlow) {
		List<LIRStatement> blockStmts = block.getStatements();
		String methodName = block.getMethodName();
		
		for (int i = blockStmts.size() - 1; i >= 0; i --) {
			LIRStatement stmt = blockStmts.get(i);
			
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
				
				markLive(methodName, qStmt.getArg1(), bFlow);
				markLive(methodName, qStmt.getArg2(), bFlow);
				
				updateKillGenSet(methodName, qStmt, bFlow);
			}
			else if (stmt.getClass().equals(LoadStmt.class)) {
				LoadStmt lStmt = (LoadStmt) stmt;
				
				if (lStmt.getVariable().isArray()) {
					ArrayName arr = (ArrayName) lStmt.getVariable();
					markLive(methodName, arr.getIndex(), bFlow);
				}
				
				updateKillGenSet(methodName, lStmt, bFlow);
			}
			else if (stmt.getClass().equals(StoreStmt.class)) {
				StoreStmt sStmt = (StoreStmt) stmt;
				
				markLive(methodName, sStmt.getVariable(), bFlow);
				
				if (sStmt.getVariable().isArray()) {
					ArrayName arr = (ArrayName) sStmt.getVariable();
					markLive(methodName, arr.getIndex(), bFlow);
				}
			}
			else if (stmt.getClass().equals(CmpStmt.class)) {
				CmpStmt cStmt = (CmpStmt)stmt;
				markLive(methodName, cStmt.getArg1(), bFlow);
				markLive(methodName, cStmt.getArg2(), bFlow);
			}
			else if (stmt.getClass().equals(PushStmt.class)) {
				PushStmt pStmt = (PushStmt) stmt;
				markLive(methodName, pStmt.getName(), bFlow);
			}
			else if (stmt.getClass().equals(PopStmt.class)) {
				PopStmt pStmt = (PopStmt) stmt;
				markLive(methodName, pStmt.getName(), bFlow);
			}
			
			// Set to stmt in set
			stmt.setInSet(getCurrentInSet(bFlow));
		}
	}
	
	public void markLive(String methodName, Name name, BlockDataFlowState bFlow) {
		if (name == null) return;
		if (name.getClass().equals(RegisterName.class) || name.getClass().equals(ConstantName.class)) return;
		
		int index = this.uniqueVariables.get(methodName).indexOf(name);
		bFlow.getGen().set(index);
	}
	
	public void updateKillGenSet(String methodName, QuadrupletStmt stmt, BlockDataFlowState bFlow) {
		if (stmt == null) return;
		
		BitSet out = bFlow.getOut();
		BitSet gen = bFlow.getGen();
		BitSet kill = bFlow.getKill();

		Name dest = stmt.getDestination();
		if (dest.getClass().equals(RegisterName.class)) return;
		
		int i = this.uniqueVariables.get(methodName).indexOf(dest);
		
		if (out.get(i)) {
			kill.set(i, true);
		}
		gen.clear(i);
	}
	
	public void updateKillGenSet(String methodName, LoadStmt stmt, BlockDataFlowState bFlow) {
		if (stmt == null) return;
		
		BitSet out = bFlow.getOut();
		BitSet gen = bFlow.getGen();
		BitSet kill = bFlow.getKill();

		Name dest = stmt.getVariable();
		int i = this.uniqueVariables.get(methodName).indexOf(dest);
		
		if (out.get(i)) {
			kill.set(i, true);
		}
		gen.clear(i);
	}

	public HashMap<CFGBlock, BlockDataFlowState> getCfgBlocksState() {
		return cfgBlocksState;
	}

	public void setCfgBlocksState(
			HashMap<CFGBlock, BlockDataFlowState> cfgBlocksState) {
		this.cfgBlocksState = cfgBlocksState;
	}

	public HashMap<String, List<Name>> getUniqueVariables() {
		return uniqueVariables;
	}

	public void setUniqueVariables(
			HashMap<String, List<Name>> vars) {
		this.uniqueVariables = vars;
	}
}
