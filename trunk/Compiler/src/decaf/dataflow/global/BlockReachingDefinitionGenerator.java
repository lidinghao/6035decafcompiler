package decaf.dataflow.global;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.Register;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flatir.VarName;
import decaf.codegen.flattener.ExpressionFlattenerVisitor;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;

public class BlockReachingDefinitionGenerator {
	private HashMap<String, List<CFGBlock>> cfgMap;
	private HashMap<CFGBlock, BlockDataFlowState> blockReachingDefs;
	private HashSet<CFGBlock> cfgBlocksToProcess;
	// Map from Name to QuadrupletStmt which assign to that Name
	private HashMap<Name, HashSet<QuadrupletStmt>> nameToQStmts;
	private int totalDefinitions;

	public BlockReachingDefinitionGenerator(HashMap<String, List<CFGBlock>> cMap) {
		cfgMap = cMap;
		nameToQStmts = new HashMap<Name, HashSet<QuadrupletStmt>>();
		blockReachingDefs = new HashMap<CFGBlock, BlockDataFlowState>();
		cfgBlocksToProcess = new HashSet<CFGBlock>();
		totalDefinitions = 0;
	}
	
	public void generate() {
		initialize();
		if (totalDefinitions == 0)
			return;
		// Get the first block in the main function - TODO: is there a better way?
		CFGBlock entry = cfgMap.get("main").get(0);
		BlockDataFlowState entryBlockFlow = new BlockDataFlowState(totalDefinitions);
		calculateGenKillSets(entry, entryBlockFlow);
		entryBlockFlow.setOut(entryBlockFlow.getGen());
		cfgBlocksToProcess.remove(entry);
		blockReachingDefs.put(entry, entryBlockFlow);
		
		while (cfgBlocksToProcess.size() != 0) {
			CFGBlock block = (CFGBlock)(cfgBlocksToProcess.toArray())[0];
			BlockDataFlowState bFlow = generateForBlock(block);
			blockReachingDefs.put(block, bFlow);
		}
	}
	
	private void initialize() {
		// QuadrupletStmt IDs that we assign should start from 0, so they can
		// correspond to the appropriate index in the BitSet
		QuadrupletStmt.setID(0);
		for (String s: this.cfgMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			for (CFGBlock block: this.cfgMap.get(s)) {
				List<LIRStatement> blockStmts = block.getStatements();
				for (int i = 0; i < blockStmts.size(); i++) {
					LIRStatement stmt = blockStmts.get(i);
					if (stmt.getClass().equals(QuadrupletStmt.class)) {
						QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
						Name dest = qStmt.getDestination();
						if (dest != null) {
							// Destination has to be non-null for this to be a valid definition
							// of something
							qStmt.setMyId();
							if (!nameToQStmts.containsKey(dest)) {
								nameToQStmts.put(dest, new HashSet<QuadrupletStmt>());
							}
							nameToQStmts.get(dest).add(qStmt);
							totalDefinitions++;
						}
					}
				}
				cfgBlocksToProcess.add(block);
			}
		}
	}
	
	private BlockDataFlowState generateForBlock(CFGBlock block) {
		// Get the original out BitSet for this block
		BitSet origOut;
		if (blockReachingDefs.containsKey(block)) {
			origOut = blockReachingDefs.get(block).getOut();
		} else {
			origOut = new BitSet(totalDefinitions);
		}
		// Calculate the in BitSet by taking union of predecessors
		BlockDataFlowState bFlow = new BlockDataFlowState(totalDefinitions);
		BitSet in = bFlow.getIn();
		for (CFGBlock pred : block.getPredecessors()) {
			if (blockReachingDefs.containsKey(pred)) {
				in.or(blockReachingDefs.get(pred).getOut());
			}
		} 
		calculateGenKillSets(block, bFlow);
		// Calculate Out
		BitSet out = bFlow.getOut();
		out.or(in);
		out.xor(bFlow.getKill()); // Invariant: kill is a subset of in
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
		BitSet gen = bFlow.getGen();
		List<LIRStatement> blockStmts = block.getStatements();
		
		for (LIRStatement stmt : blockStmts) {
			if (!stmt.isExpressionStatement()) {
				if (stmt.getClass().equals(CallStmt.class)) {
					invalidateContextSwitch(bFlow);
				}
				continue;
			}
			
			QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
			Name dest = qStmt.getDestination();
			if (dest != null) {
				// Valid reaching definition
				updateKillSet(dest, bFlow);
				// Gen - add current statement id
				gen.set(qStmt.getMyId(), true);
			}
		}
	}
	
	public void invalidateContextSwitch(BlockDataFlowState bFlow) {
		// Invalidate arg registers
		for (int i = 0; i < ExpressionFlattenerVisitor.argumentRegs.length; i++) {
			updateKillSet(new RegisterName(ExpressionFlattenerVisitor.argumentRegs[i]), bFlow);
		}
		
		// Reset symbolic value for %RAX
		updateKillSet(new RegisterName(Register.RAX), bFlow);
		
		// Invalidate global vars;
		for (Name name: this.nameToQStmts.keySet()) {
			if (name.getClass().equals(VarName.class)) {
				VarName var = (VarName) name;
				if (var.getBlockId() == -1) { // Global
					updateKillSet(name, bFlow);
				}
			}
		}
	}
	
	public void updateKillSet(Name newDest, BlockDataFlowState bFlow) {
		BitSet kill = bFlow.getKill();
		BitSet in = bFlow.getIn();
		HashSet<QuadrupletStmt> qStmtsForDest = nameToQStmts.get(newDest);
		if (qStmtsForDest != null) {
			// Kill if it is part of In
			for (QuadrupletStmt q : qStmtsForDest) {
				int index = q.getMyId();
				if (in.get(index)) {
					kill.set(index, true); // Ensures Kill is always a subset of In
				}
			}
		}
	}
	
	public HashMap<Name, HashSet<QuadrupletStmt>> getNameToQStmts() {
		return nameToQStmts;
	}

	public void setNameToQStmts(HashMap<Name, HashSet<QuadrupletStmt>> nameToQStmts) {
		this.nameToQStmts = nameToQStmts;
	}
	
	public HashMap<CFGBlock, BlockDataFlowState> getBlockReachingDefs() {
		return blockReachingDefs;
	}

	public void setBlockReachingDefs(HashMap<CFGBlock, BlockDataFlowState> blockReachingDefs) {
		this.blockReachingDefs = blockReachingDefs;
	}
	
	public int getTotalDefinitions() {
		return totalDefinitions;
	}

	public void setTotalDefinitions(int totalDefinitions) {
		this.totalDefinitions = totalDefinitions;
	}
}
