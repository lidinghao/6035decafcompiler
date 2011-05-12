package decaf.dataflow.global;

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
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

public class ConstReachingDef {
	private HashSet<CFGBlock> cfgBlocksToProcess;
	private HashMap<CFGBlock, BlockDataFlowState> cfgBlocksState;
	private HashMap<String, MethodIR> mMap;
	private HashMap<String, List<LIRStatement>> uniqueDefinitions;
	
	public ConstReachingDef(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.cfgBlocksToProcess = new HashSet<CFGBlock>();
		this.cfgBlocksState = new HashMap<CFGBlock, BlockDataFlowState>();
		this.uniqueDefinitions = new HashMap<String, List<LIRStatement>>();
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
		int totalDefs = this.uniqueDefinitions.get(methodName).size();
		
		CFGBlock entry = this.getBlockById(methodName, 0);
		BlockDataFlowState entryBlockFlow = new BlockDataFlowState(totalDefs); // OUT = GEN for entry block
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

	// Each QuadrupletStmt will have unique ID, as it is a definition
	private void initialize(String methodName) {
		QuadrupletStmt.setID(0);
		this.uniqueDefinitions.put(methodName, new ArrayList<LIRStatement>());
		this.cfgBlocksToProcess.clear();
		
		for (CFGBlock block: this.mMap.get(methodName).getCfgBlocks()) {
			List<LIRStatement> blockStmts = block.getStatements();
			for (int i = 0; i < blockStmts.size(); i++) {
				LIRStatement stmt = blockStmts.get(i);
				if (stmt.getClass().equals(QuadrupletStmt.class)) {
					QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
					
					// Ignore register assignments (only for calls)
					if (qStmt.getDestination().getClass().equals(RegisterName.class)) continue; // Ignore assignments to regs
					
					if (!qStmt.getOperator().equals(QuadrupletOp.MOVE)) continue;
					
					if (!qStmt.getArg1().getClass().equals(ConstantName.class)) continue;

					this.uniqueDefinitions.get(methodName).add(qStmt);
					qStmt.setMyId();
				}
			}
			
			this.cfgBlocksToProcess.add(block);
		}
	}
	
	private BlockDataFlowState generateDFState(CFGBlock block) {
		int totalDefs = this.uniqueDefinitions.get(block.getMethodName()).size();
		// Get the original out BitSet for this block
		BitSet origOut;
		if (this.cfgBlocksState.containsKey(block)) {
			origOut = this.cfgBlocksState.get(block).getOut();
		} else {
			origOut = new BitSet(totalDefs);
			origOut.set(0, totalDefs);
		}
		
		// Calculate the in BitSet by taking union of predecessors
		BlockDataFlowState bFlow = new BlockDataFlowState(totalDefs);
		
		// If there exists at least one predecessor, set In to all False
		if (block.getPredecessors().size() > 0) {
			bFlow.getIn().set(0, totalDefs);
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
			stmt.setReachingDefInSet(getReachingInSet(bFlow, block.getMethodName()));
			
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				qStmt = (QuadrupletStmt)stmt;
				if (!qStmt.getDestination().getClass().equals(RegisterName.class)) {
					updateKillGenSet(block.getMethodName(), qStmt, bFlow);
				}
			}
			else if (stmt.getClass().equals(CallStmt.class)) {
				CallStmt cStmt = (CallStmt) stmt;
				if (cStmt.getMethodLabel().equals(ProgramFlattener.exceptionHandlerLabel)) {
					continue;
				}
				
				invalidateFunctionCall(block, bFlow);
			}
			
		}
	}

	private BitSet getReachingInSet(BlockDataFlowState bFlow, String string) {
		BitSet out = new BitSet(this.uniqueDefinitions.get(string).size()); // OUT = (IN - KILL) U GEN
		out.or(bFlow.getIn());
		out.xor(bFlow.getKill());
		out.or(bFlow.getGen());
		
		return out;
	}

	private void invalidateFunctionCall(CFGBlock block, BlockDataFlowState bFlow) {
		for (LIRStatement def: this.uniqueDefinitions.get(block.getMethodName())) {
			QuadrupletStmt qStmt = (QuadrupletStmt) def;
			
			if (!qStmt.getDestination().isGlobal()) continue;
			
			if (bFlow.getIn().get(qStmt.getMyId())) {
				bFlow.getKill().set(qStmt.getMyId(), true);
			}
			
			bFlow.getGen().clear(qStmt.getMyId()); // Clear any previous gen bits for same dest var
		}
	}
	
	// invalidates defs for quadruplt stmt and loads
	public void updateKillGenSet(String methodName, LIRStatement stmt, BlockDataFlowState bFlow) {
		if (stmt == null) return;
		
		BitSet in = bFlow.getIn();
		BitSet gen = bFlow.getGen();
		BitSet kill = bFlow.getKill();

		QuadrupletStmt srcqStmt = (QuadrupletStmt) stmt;
		Name dest = srcqStmt.getDestination();
		
		// Invalidate reaching definitions
		for (LIRStatement s : this.uniqueDefinitions.get(methodName)) {
			QuadrupletStmt qStmt = (QuadrupletStmt) s;
			if (qStmt.getDestination().equals(dest)) { // Definitions to same var name
				if (in.get(qStmt.getMyId())) {
					kill.set(qStmt.getMyId(), true);
				}
				
				gen.clear(qStmt.getMyId()); // Clear any previous gen bits for same dest var
			}
			
			int myIndex = qStmt.getMyId();
			Name name = qStmt.getDestination();
				
			boolean resetName = false;
			
			if (name.isArray()) {
				Name myName = name;
				
				do {
					ArrayName array = (ArrayName) myName;
					if (array.getIndex().equals(dest)) { // Index being reassigned, KILL!
						resetName = true;
					}
					
					myName = array.getIndex();
					
				} while (myName.isArray());
				
				if (dest.isArray()) {
					ArrayName myDest = (ArrayName) dest;
					ArrayName arrName = (ArrayName) name;
					if (myDest.getIndex().getClass().equals(ConstantName.class) &&
							!arrName.getIndex().getClass().equals(ConstantName.class)) {
						if (arrName.getId().equals(myDest.getId())) {
							resetName = true;
						}
					}
				}
			}
			
			if (resetName) {
				if (in.get(myIndex)) {
					kill.set(myIndex);
				}
				
				gen.clear(myIndex); // Clear any previous gen bits for same dest var
			}
		}
		
		if (srcqStmt.getOperator() == QuadrupletOp.MOVE) {
			if (srcqStmt.getArg1().getClass().equals(ConstantName.class)) {
				gen.set(srcqStmt.getMyId()); // Set gen bit on
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

	public HashMap<String, List<LIRStatement>> getUniqueDefinitions() {
		return uniqueDefinitions;
	}

	public void setUniqueDefinitions(
			HashMap<String, List<LIRStatement>> uniqueDefinitions) {
		this.uniqueDefinitions = uniqueDefinitions;
	}
}
