package decaf.dataflow.global;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.dataflow.cfg.CFGBlock;

public class BlockReachingDefinitionGenerator {
	private HashMap<String, List<CFGBlock>> cfgMap;
	private HashMap<CFGBlock, BlockFlow> blockReachingDefs;
	private HashSet<CFGBlock> cfgBlocksToProcess;
	// Map from Name to IDs of QuadrupletStmt which assign to that Name
	private HashMap<Name, HashSet<Integer>> nameToStmtIds;
	private int totalExpressionStmts;
	
	public BlockReachingDefinitionGenerator(HashMap<String, List<CFGBlock>> cMap) {
		cfgMap = cMap;
		nameToStmtIds = new HashMap<Name, HashSet<Integer>>();
		blockReachingDefs = new HashMap<CFGBlock, BlockFlow>();
		cfgBlocksToProcess = new HashSet<CFGBlock>();
		totalExpressionStmts = 0;
	}
	
	public void generate() {
		initialize();
		// Get the first block in the main function - TODO: is there a better way?
		CFGBlock entry = cfgMap.get("main").get(0);
		BlockFlow entryBlockFlow = new BlockFlow(totalExpressionStmts);
		calculateGenKillSets(entry, entryBlockFlow);
		entryBlockFlow.setOut(entryBlockFlow.getGen());
		cfgBlocksToProcess.remove(entry);
		
		while (cfgBlocksToProcess.size() != 0) {
			CFGBlock block = (CFGBlock)(cfgBlocksToProcess.toArray())[0];
			BlockFlow bFlow = generateForBlock(block);
			blockReachingDefs.put(block, bFlow);
		}
	}
	
	private void initialize() {
		for (String s: this.cfgMap.keySet()) {
			for (CFGBlock block: this.cfgMap.get(s)) {
				List<LIRStatement> blockStmts = block.getStatements();
				for (int i = 0; i < blockStmts.size(); i++) {
					LIRStatement stmt = blockStmts.get(i);
					if (stmt.isExpressionStatement()) {
						QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
						qStmt.setMyId();
						Name dest = qStmt.getDestination();
						if (!nameToStmtIds.containsKey(dest)) {
							nameToStmtIds.put(dest, new HashSet<Integer>());
						}
						nameToStmtIds.get(dest).add(new Integer(qStmt.getMyId()));
						totalExpressionStmts++;
					}
				}
				cfgBlocksToProcess.add(block);
			}
		}
	}
	
	private BlockFlow generateForBlock(CFGBlock block) {
		BlockFlow bFlow = new BlockFlow(totalExpressionStmts);
		BitSet in = bFlow.getIn();
		for (CFGBlock pred : block.getPredecessors()) {
			if (blockReachingDefs.containsKey(pred)) {
				in.or(blockReachingDefs.get(pred).getOut());
			}
		} 
		calculateGenKillSets(block, bFlow);
		// Calculate Out
		BitSet out = bFlow.getOut();
		BitSet origOut = (BitSet)out.clone();
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
		blockReachingDefs.put(block, bFlow);
		return bFlow;
	}
	
	private void calculateGenKillSets(CFGBlock block, BlockFlow bFlow) {
		BitSet gen = bFlow.getGen();
		BitSet in = bFlow.getIn();
		BitSet kill = bFlow.getKill();
		List<LIRStatement> blockStmts = block.getStatements();
		for (LIRStatement stmt : blockStmts) {
			if (!stmt.isExpressionStatement()) {
				continue;
			}
			QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
			Name dest = qStmt.getDestination();
			HashSet<Integer> stmtIdsForDest = nameToStmtIds.get(dest);
			// Kill if it is part of In
			Iterator<Integer> it = stmtIdsForDest.iterator();
			while (it.hasNext()) {
				int index = it.next();
				if (in.get(index)) {
					kill.set(index, true); // Ensures Kill is always a subset of In
				}
			}
			// Gen - add current statement id
			gen.set(qStmt.getMyId(), true);
		}
	}
	
	public HashMap<Name, HashSet<Integer>> getNameToStmtIds() {
		return nameToStmtIds;
	}

	public void setNameToStmtIds(HashMap<Name, HashSet<Integer>> nameToStmtIds) {
		this.nameToStmtIds = nameToStmtIds;
	}
	
	public HashMap<CFGBlock, BlockFlow> getBlockReachingDefs() {
		return blockReachingDefs;
	}

	public void setBlockReachingDefs(HashMap<CFGBlock, BlockFlow> blockReachingDefs) {
		this.blockReachingDefs = blockReachingDefs;
	}
}
