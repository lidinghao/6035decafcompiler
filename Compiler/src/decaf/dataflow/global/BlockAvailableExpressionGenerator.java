package decaf.dataflow.global;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;

public class BlockAvailableExpressionGenerator {
	private HashMap<String, List<CFGBlock>> cfgMap;
	private HashMap<CFGBlock, BlockFlow> blockAvailableDefs;
	private HashMap<CFGBlock, List<AvailableExpression>> blockExpressions;
	private HashSet<CFGBlock> cfgBlocksToProcess;
	// Map from Name to IDs of QuadrupletStmt which assign to that Name
	private HashMap<Name, HashSet<Integer>> nameToStmtIds;
	private int totalExpressionStmts;
	
	public BlockAvailableExpressionGenerator(HashMap<String, List<CFGBlock>> cMap) {
		cfgMap = cMap;
		nameToStmtIds = new HashMap<Name, HashSet<Integer>>();
		blockAvailableDefs = new HashMap<CFGBlock, BlockFlow>();
		blockExpressions = new HashMap<CFGBlock, List<AvailableExpression>>();
		cfgBlocksToProcess = new HashSet<CFGBlock>();
		totalExpressionStmts = 0;
		
		while (cfgBlocksToProcess.size() != 0) {
			CFGBlock block = (CFGBlock)(cfgBlocksToProcess.toArray())[0];
			BlockFlow bFlow = generateForBlock(block);
			blockAvailableDefs.put(block, bFlow);
		}
	}
	
	public void generate() {
		initialize();
		// Get the first block in the main function - TODO: is there a better way?
		CFGBlock entry = cfgMap.get("main").get(0);
		BlockFlow entryBlockFlow = new BlockFlow(totalExpressionStmts);
		calculateGenKillSets(entry, entryBlockFlow);
		entryBlockFlow.setOut(entryBlockFlow.getGen());
		cfgBlocksToProcess.remove(entry);
	}
	
	public void initialize() {
		// AvailableExpression IDs that we assign should start from 0, so they can
		// correspond to the appropriate index in the BitSet
		AvailableExpression.setID(0);
		for (String s: this.cfgMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			for (CFGBlock block: this.cfgMap.get(s)) {
				List<LIRStatement> blockStmts = block.getStatements();
				for (int i = 0; i < blockStmts.size(); i++) {
					LIRStatement stmt = blockStmts.get(i);
					if (stmt.isExpressionStatement()) {
						QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
						QuadrupletOp qOp = qStmt.getOperator();
						// Ensure it is of the form [var op var] or [op var]
						if (qOp != QuadrupletOp.MOVE) {
							Name arg1 = qStmt.getArg1();
							Name arg2 = qStmt.getArg2();
							AvailableExpression expr = new AvailableExpression(arg1, 
									arg2, qStmt.getOperator());
							// Update mapping from CFGBlock to AvailableExpression list
							if (!blockExpressions.containsKey(block)) {
								blockExpressions.put(block, new ArrayList<AvailableExpression>());
							}
							blockExpressions.get(block).add(expr);
							// Update mapping between Name and the AvailableExpressions that 
							// contain that Name
							if (!nameToStmtIds.containsKey(arg1)) {
								nameToStmtIds.put(arg1, new HashSet<Integer>());
							}
							nameToStmtIds.get(arg1).add(expr.getMyId());
							if (arg2 != null) {
								if (!nameToStmtIds.containsKey(arg2)) {
									nameToStmtIds.put(arg2, new HashSet<Integer>());
								}
								nameToStmtIds.get(arg2).add(expr.getMyId());
								totalExpressionStmts++;
							}
						}
					}
				}
				cfgBlocksToProcess.add(block);
			}
		}
	}
	
	public BlockFlow generateForBlock(CFGBlock block) {
		BlockFlow bFlow = new BlockFlow(totalExpressionStmts);
		BitSet in = bFlow.getIn();
		for (CFGBlock pred : block.getPredecessors()) {
			if (blockAvailableDefs.containsKey(pred)) {
				in.and(blockAvailableDefs.get(pred).getOut());
			} else {
				// If a predecessor hasn't been processed, assume In is empty set
				in.clear();
				break;
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
		blockAvailableDefs.put(block, bFlow);
		return bFlow;
	}
	
	private void calculateGenKillSets(CFGBlock block, BlockFlow bFlow) {
		BitSet gen = bFlow.getGen();
		List<AvailableExpression> blockExprs = blockExpressions.get(block);
		
		for (AvailableExpression expr : blockExprs) {
//			if (!stmt.isExpressionStatement()) {
//				if (stmt.getClass().equals(CallStmt.class)) {
//					// Invalidate arg registers
//					for (int i = 0; i < ExpressionFlattenerVisitor.argumentRegs.length; i++) {
//						updateKillSet(new RegisterName(ExpressionFlattenerVisitor.argumentRegs[i]), bFlow);
//					}
//					
//					// Reset symbolic value for %RAX
//					updateKillSet(new RegisterName(Register.RAX), bFlow);
//					
//					// Invalidate global vars;
//					for (Name name: this.nameToStmtIds.keySet()) {
//						if (name.getClass().equals(VarName.class)) {
//							VarName var = (VarName) name;
//							if (var.getBlockId() == -1) { // Global
//								updateKillSet(name, bFlow);
//							}
//						}
//					}
//				}
//				continue;
//			}
			
			updateKillSet(expr, bFlow);
			// Gen - add current expression id
			gen.set(expr.getMyId(), true);
		}
	}
	
	private void updateKillSet(AvailableExpression expr, BlockFlow bFlow) {
//		BitSet kill = bFlow.getKill();
//		BitSet in = bFlow.getIn();
//		HashSet<Integer> stmtIdsForDest = nameToStmtIds.get(newDest);
//		// Kill if it is part of In
//		Iterator<Integer> it = stmtIdsForDest.iterator();
//		while (it.hasNext()) {
//			int index = it.next();
//			if (in.get(index)) {
//				kill.set(index, true); // Ensures Kill is always a subset of In
//			}
//		}
	}
	
	// for a statement, add ID of RHS expr to gen, kill all the IDs which are currently available and contain the dest Name
	// when there is a call stmt, kill all the IDs which are currently available and contain global vars, arg registers, and rax
	
	public HashMap<Name, HashSet<Integer>> getNameToStmtIds() {
		return nameToStmtIds;
	}

	public void setNameToStmtIds(HashMap<Name, HashSet<Integer>> nameToStmtIds) {
		this.nameToStmtIds = nameToStmtIds;
	}
	
	public HashMap<CFGBlock, BlockFlow> getBlockReachingDefs() {
		return blockAvailableDefs;
	}

	public void setBlockReachingDefs(HashMap<CFGBlock, BlockFlow> blockReachingDefs) {
		this.blockAvailableDefs = blockReachingDefs;
	}

	public HashMap<CFGBlock, List<AvailableExpression>> getBlockExpressions() {
		return blockExpressions;
	}

	public void setBlockExpressions(
			HashMap<CFGBlock, List<AvailableExpression>> blockExpressions) {
		this.blockExpressions = blockExpressions;
	}
}
