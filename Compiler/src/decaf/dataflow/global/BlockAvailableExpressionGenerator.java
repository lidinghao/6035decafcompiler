package decaf.dataflow.global;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.Register;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flatir.VarName;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

public class BlockAvailableExpressionGenerator {
	private HashMap<String, MethodIR> mMap;
	private HashMap<CFGBlock, BlockDataFlowState> blockAvailableDefs;
	private List<AvailableExpression> availableExpressions;
	private HashMap<CFGBlock, List<AvailableExpression>> blockExpressions;
	private HashMap<String, List<AvailableExpression>> methodExpressions;
	private List<CFGBlock> orderProcessed;
	private HashSet<CFGBlock> cfgBlocksToProcess;
	// Map from Name to IDs of QuadrupletStmt which use that Name
	private HashMap<Name, HashSet<Integer>> nameToExprIds;
	private int totalExpressionStmts;

	public BlockAvailableExpressionGenerator(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		nameToExprIds = new HashMap<Name, HashSet<Integer>>();
		blockAvailableDefs = new HashMap<CFGBlock, BlockDataFlowState>();
		availableExpressions = new ArrayList<AvailableExpression>();
		blockExpressions = new HashMap<CFGBlock, List<AvailableExpression>>();
		methodExpressions = new HashMap<String, List<AvailableExpression>>();
		cfgBlocksToProcess = new HashSet<CFGBlock>();
		orderProcessed = new ArrayList<CFGBlock>();
		totalExpressionStmts = 0;
	}
	
	public void generate() {
		initialize();
		if (totalExpressionStmts == 0)
			return;
		// Get the first block in the main function - TODO: is there a better way?
		CFGBlock entry = this.getBlockById("main", 0);
		BlockDataFlowState entryBlockFlow = new BlockDataFlowState(totalExpressionStmts);
		calculateGenKillSets(entry, entryBlockFlow);
		entryBlockFlow.setOut(entryBlockFlow.getGen());
		cfgBlocksToProcess.remove(entry);
		orderProcessed.add(entry);
		blockAvailableDefs.put(entry, entryBlockFlow);
		
		while (cfgBlocksToProcess.size() != 0) {
			CFGBlock block = (CFGBlock)(cfgBlocksToProcess.toArray())[0];
			BlockDataFlowState bFlow = generateForBlock(block);
			blockAvailableDefs.put(block, bFlow);
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
	
	public void initialize() {
		// AvailableExpression IDs that we assign should start from 0, so they can
		// correspond to the appropriate index in the BitSet
		AvailableExpression.setID(0);
		for (String s: this.mMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			for (CFGBlock block: this.mMap.get(s).getCfgBlocks()) {
				List<LIRStatement> blockStmts = block.getStatements();
				for (int i = 0; i < blockStmts.size(); i++) {
					LIRStatement stmt = blockStmts.get(i);
					if (stmt.isAvailableExpression()) {
						QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
						Name arg1 = qStmt.getArg1();
						Name arg2 = qStmt.getArg2();
						AvailableExpression expr = new AvailableExpression(arg1, 
								arg2, qStmt.getOperator());
						if (availableExpressions.contains(expr)) {
							expr = availableExpressions.get(availableExpressions.indexOf(expr));
							// Decrement global AvailableExpression ID
							AvailableExpression.setID(AvailableExpression.getID()-1);
						} else {
							availableExpressions.add(expr);
							totalExpressionStmts++;
						}
						// Update mapping from CFGBlock to AvailableExpression list
						if (!blockExpressions.containsKey(block)) {
							blockExpressions.put(block, new ArrayList<AvailableExpression>());
						}
						blockExpressions.get(block).add(expr);
						// Update mapping from method to AvailableExpression list
						if (!methodExpressions.containsKey(s)) {
							methodExpressions.put(s, new ArrayList<AvailableExpression>());
						}
						methodExpressions.get(s).add(expr);
						
						// Update mapping between Name and the AvailableExpressions that 
						// contain that Name
						if (!nameToExprIds.containsKey(arg1)) {
							nameToExprIds.put(arg1, new HashSet<Integer>());
						}
						nameToExprIds.get(arg1).add(expr.getMyId());
						// If argument is ArrayName, add the index Name mappings recursively too
						while (arg1.getClass().equals(ArrayName.class)) {
							arg1 = ((ArrayName)arg1).getIndex();
							if (!nameToExprIds.containsKey(arg1)) {
								nameToExprIds.put(arg1, new HashSet<Integer>());
							}
							nameToExprIds.get(arg1).add(expr.getMyId());
						}
						// Process arg2
						if (arg2 != null) {
							// Update mapping between Name and the AvailableExpressions that 
							// contain that Name
							if (!nameToExprIds.containsKey(arg2)) {
								nameToExprIds.put(arg2, new HashSet<Integer>());
							}
							nameToExprIds.get(arg2).add(expr.getMyId());
							// If argument is ArrayName, add the index Name mappings recursively too
							while (arg2.getClass().equals(ArrayName.class)) {
								arg2 = ((ArrayName)arg2).getIndex();
								if (!nameToExprIds.containsKey(arg2)) {
									nameToExprIds.put(arg2, new HashSet<Integer>());
								}
								nameToExprIds.get(arg2).add(expr.getMyId());
							}
						}
					}
				}
				cfgBlocksToProcess.add(block);
			}
		}
	}
	
	public BlockDataFlowState generateForBlock(CFGBlock block) {
		BitSet origOut;
		if (blockAvailableDefs.containsKey(block)) {
			origOut = blockAvailableDefs.get(block).getOut();
		} else {
			origOut = new BitSet(totalExpressionStmts);
			// Confluence operator is AND, so initialize out set to all 1s
			origOut.set(0, totalExpressionStmts, true);
		}
		BlockDataFlowState bFlow = new BlockDataFlowState(totalExpressionStmts);
		// If there exists at least one predecessor, set In to all True
		if (block.getPredecessors().size() > 0) {
			bFlow.getIn().set(0, totalExpressionStmts);
		}
		BitSet in = bFlow.getIn();
		for (CFGBlock pred : block.getPredecessors()) {
			if (blockAvailableDefs.containsKey(pred)) {
				in.and(blockAvailableDefs.get(pred).getOut());
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
		if (orderProcessed.contains(block)) {
			orderProcessed.remove(block);
		}
		orderProcessed.add(block);
		return bFlow;
	}
	
	private void calculateGenKillSets(CFGBlock block, BlockDataFlowState bFlow) {
		BitSet gen = bFlow.getGen();
		List<AvailableExpression> blockExprs = blockExpressions.get(block);
		List<LIRStatement> blockStmts = block.getStatements();
		QuadrupletStmt qStmt;
		
		int exprIndex = 0;
		for (LIRStatement stmt : blockStmts) {
			if (!stmt.isAvailableExpression()) {
				if (stmt.getClass().equals(CallStmt.class)) {
					CallStmt callStmt = (CallStmt) stmt;
					if (callStmt.getMethodLabel().equals(ProgramFlattener.exceptionHandlerLabel)) continue;
					
					// Invalidate arg registers
					for (int i = 0; i < Register.argumentRegs.length; i++) {
						updateKillSet(new RegisterName(Register.argumentRegs[i]), bFlow);
					}
					
					// Reset symbolic value for %RAX
					updateKillSet(new RegisterName(Register.RAX), bFlow);
					
					// Invalidate global vars;
					for (Name name: this.nameToExprIds.keySet()) {
						if (name.getClass().equals(VarName.class)) {
							VarName var = (VarName) name;
							if (var.getBlockId() == -1) { // Global
								updateKillSet(name, bFlow);
							}
						}
						if (name.getClass().equals(ArrayName.class)) {
							updateKillSet(name, bFlow);
						}
					}
				}
				
				if (stmt.getClass().equals(QuadrupletStmt.class)) {
					qStmt = (QuadrupletStmt)stmt;
					// MOVE is not considered an AvailableExpression but it still effects the Kill set
					if (qStmt.getOperator() == QuadrupletOp.MOVE) {
						updateKillSet(qStmt.getDestination(), bFlow);
					}
				}
				continue;
			}
			
			qStmt = (QuadrupletStmt)stmt;
			// Get corresponding AvailableExpression
			AvailableExpression expr = blockExprs.get(exprIndex);
			// Gen - add current expression id
			gen.set(expr.getMyId(), true);
			// Update kill set with destination
			updateKillSet(qStmt.getDestination(), bFlow);
			exprIndex++;
		}
	}
	
	private void updateKillSet(Name dest, BlockDataFlowState bFlow) {
		BitSet kill = bFlow.getKill();
		BitSet in = bFlow.getIn();
		HashSet<Integer> stmtIdsForDest = nameToExprIds.get(dest);
		if (stmtIdsForDest != null) {
			// Kill if it is part of In
			Iterator<Integer> it = stmtIdsForDest.iterator();
			while (it.hasNext()) {
				int index = it.next();
				if (in.get(index)) {
					kill.set(index, true); // Ensures Kill is always a subset of In
				}
				// Remove from Gen if it exists
				bFlow.getGen().set(index, false);
			}
		}
	}
	
	public void printBlocksAvailableExpressions(PrintStream out) {
		for (CFGBlock block : orderProcessed) {
			printBlockAvailableExpressions(out, block);
		}
	}
	
	public void printBlockAvailableExpressions(PrintStream out, CFGBlock block) {
		out.println("----- NEW BLOCK -----");
		out.println(block);
		out.println("Block available expressions: ");
		if (blockExpressions.containsKey(block)) {
			for (AvailableExpression expr : blockExpressions.get(block))  {
				out.println("\t"+expr);
			}
		}
		out.println(blockAvailableDefs.get(block));
	}
	
	public HashMap<Name, HashSet<Integer>> getNameToExprIds() {
		return nameToExprIds;
	}

	public void setNameToExprIds(HashMap<Name, HashSet<Integer>> nameToStmtIds) {
		this.nameToExprIds = nameToStmtIds;
	}
	
	public HashMap<CFGBlock, BlockDataFlowState> getBlockAvailableDefs() {
		return blockAvailableDefs;
	}

	public void setBlockAvailableDefs(HashMap<CFGBlock, BlockDataFlowState> blockReachingDefs) {
		this.blockAvailableDefs = blockReachingDefs;
	}

	public HashMap<CFGBlock, List<AvailableExpression>> getBlockExpressions() {
		return blockExpressions;
	}

	public void setBlockExpressions(
			HashMap<CFGBlock, List<AvailableExpression>> blockExpressions) {
		this.blockExpressions = blockExpressions;
	}
	
	public int getTotalExpressionStmts() {
		return totalExpressionStmts;
	}

	public void setTotalExpressionStmts(int totalExpressionStmts) {
		this.totalExpressionStmts = totalExpressionStmts;
	}

	public List<AvailableExpression> getAvailableExpressions() {
		return availableExpressions;
	}

	public void setAvailableExpressions(
			List<AvailableExpression> availableExpressions) {
		this.availableExpressions = availableExpressions;
	}

	public HashMap<String, List<AvailableExpression>> getMethodExpressions() {
		return methodExpressions;
	}

	public void setMethodExpressions(
			HashMap<String, List<AvailableExpression>> methodExpressions) {
		this.methodExpressions = methodExpressions;
	}
}
