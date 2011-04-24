package decaf.ralloc;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.global.BlockDataFlowState;

public class DefDataFlowAnalyzer {
	private int totalDefinitions;
	private HashSet<CFGBlock> cfgBlocksToProcess;
	private HashMap<CFGBlock, BlockDataFlowState> cfgBlocksState;
	private HashMap<String, List<CFGBlock>> cfgMap;
	private HashMap<String, List<QuadrupletStmt>> uniqueDefinitions;
	
	public DefDataFlowAnalyzer(HashMap<String, List<CFGBlock>> cfgMap) {
		this.cfgMap = cfgMap;
		this.totalDefinitions = 0;
		this.cfgBlocksToProcess = new HashSet<CFGBlock>();
		this.cfgBlocksState = new HashMap<CFGBlock, BlockDataFlowState>();
		this.uniqueDefinitions = new HashMap<String, List<QuadrupletStmt>>();
	}
	
	// Each QuadrupletStmt will have a unique ID
	public void analyze() {
		for (String methodName: this.cfgMap.keySet()) {
			if (methodName.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			initialize(methodName);
			runWorkList(methodName);
		}
	}

	private void runWorkList(String methodName) {
		if (this.totalDefinitions == 0) return;
		
		CFGBlock entry = cfgMap.get(methodName).get(0);
		BlockDataFlowState entryBlockFlow = new BlockDataFlowState(this.totalDefinitions); // OUT = GEN for entry block
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

	// Each QuadrupletStmt will have unique ID, as it is a definition
	private void initialize(String methodName) {
		QuadrupletStmt.setID(0);
		this.uniqueDefinitions.put(methodName, new ArrayList<QuadrupletStmt>());
		this.totalDefinitions = 0;
		this.cfgBlocksToProcess.clear();
		
		for (CFGBlock block: this.cfgMap.get(methodName)) {
			List<LIRStatement> blockStmts = block.getStatements();
			for (int i = 0; i < blockStmts.size(); i++) {
				LIRStatement stmt = blockStmts.get(i);
				if (stmt.getClass().equals(QuadrupletStmt.class)) {
					QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
					if (!qStmt.getDestination().getClass().equals(RegisterName.class)) {
						this.totalDefinitions++;
						this.uniqueDefinitions.get(methodName).add(qStmt);
						qStmt.setMyId();
					}
				}
			}
			
			this.cfgBlocksToProcess.add(block);
		}
	}
	
	private BlockDataFlowState generateDFState(CFGBlock block) {
		// Get the original out BitSet for this block
		BitSet origOut;
		if (this.cfgBlocksState.containsKey(block)) {
			origOut = this.cfgBlocksState.get(block).getOut();
		} else {
			origOut = new BitSet(this.totalDefinitions);
		}
		
		// Calculate the in BitSet by taking union of predecessors
		BlockDataFlowState bFlow = new BlockDataFlowState(this.totalDefinitions);
		
		// If there exists at least one predecessor, set In to all False
		if (block.getPredecessors().size() > 0) {
			bFlow.getIn().clear();
		}
		
		BitSet in = bFlow.getIn();
		for (CFGBlock pred : block.getPredecessors()) {
			if (this.cfgBlocksState.containsKey(pred)) {
				in.or(this.cfgBlocksState.get(pred).getOut());
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
		}
	}
	
	public void updateKillGenSet(String methodName, QuadrupletStmt stmt, BlockDataFlowState bFlow) {
		if (stmt == null) return;
		
		BitSet in = bFlow.getIn();
		BitSet gen = bFlow.getGen();
		BitSet kill = bFlow.getKill();
		
		// Invalidate reaching definitions
		for (QuadrupletStmt qStmt : this.uniqueDefinitions.get(methodName)) {
			if (qStmt.getDestination().equals(stmt.getDestination())) { // Definitions to same var name
				if (in.get(qStmt.getMyId())) {
					kill.set(qStmt.getMyId(), true);
				}
				
				gen.clear(qStmt.getMyId()); // Clear any previous gen bits for same dest var
			}
		}
		
		gen.set(stmt.getMyId()); // Set gen bit on
	}
}
