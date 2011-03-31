package decaf.dataflow.global;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import decaf.codegen.flatir.DynamicVarName;
import decaf.codegen.flatir.EnterStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;

public class GlobalCSEOptimizer {
	private HashMap<String, List<CFGBlock>> cfgMap;
	private HashMap<AvailableExpression, DynamicVarName> exprToTemp;
	private BlockAvailableExpressionGenerator availableGenerator;
	private HashSet<Integer> exprsClobbered;
	private ProgramFlattener pf;
	
	public GlobalCSEOptimizer(HashMap<String, List<CFGBlock>> cfgMap, ProgramFlattener pf) {
		this.cfgMap = cfgMap;
		this.pf = pf;
		this.availableGenerator = new BlockAvailableExpressionGenerator(cfgMap);
		this.exprToTemp = new HashMap<AvailableExpression, DynamicVarName>();
		this.exprsClobbered = new HashSet<Integer>();
		// Generate Available Expressions for CFG
		this.availableGenerator.generate();
	}
	
	public void performGlobalCSE() {
		initialize();
		for (String s: this.cfgMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			// Optimize blocks
			for (CFGBlock block: this.cfgMap.get(s)) {
				optimize(block);
				reset();
			}
		
			// Change statements
			List<LIRStatement> stmts = new ArrayList<LIRStatement>();
			
			for (int i = 0; i < this.cfgMap.get(s).size(); i++) {
				stmts.addAll(getBlockWithIndex(i, 
						this.cfgMap.get(s)).getStatements());
			}
			
			pf.getLirMap().put(s, stmts);
		}
	}
	
	private void initialize() {
		// Create temporary variables for each unique AvailableExpression
		initializeTemporaryMap();
		// Update the stack sizes of each method based on how many 
		// additionally temporary variables it uses
		updateStackSizes();
	}
	
	private void reset() {
		exprsClobbered = new HashSet<Integer>();
	}
	
	private CFGBlock getBlockWithIndex(int i, List<CFGBlock> list) {
		for (CFGBlock block: list) {
			if (block.getIndex() == i) {
				return block;
			}
		}
		
		return null;
	}
	
	private void initializeTemporaryMap() {
		DynamicVarName.reset();
		List<AvailableExpression> availExprs = 
			availableGenerator.getAvailableExpressions();
		for (AvailableExpression expr : availExprs) {
			exprToTemp.put(expr, new DynamicVarName(true));
		}
	}
	
	private void updateStackSizes() {
		HashMap<CFGBlock, BlockFlow> blockFlowMap =  availableGenerator.getBlockAvailableDefs();
		for (String s: this.cfgMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			BitSet availableExprsInMethod = new BitSet(availableGenerator.getTotalExpressionStmts());
			for (CFGBlock block: this.cfgMap.get(s)) {
				availableExprsInMethod.or(blockFlowMap.get(block).getIn());
				availableExprsInMethod.or(blockFlowMap.get(block).getOut());
			}
			
			int tempsNeeded = availableExprsInMethod.cardinality();
			// Fix stack size
			for (CFGBlock block: this.cfgMap.get(s)) {
				if (block.getIndex() == 0) {
					for (LIRStatement stmt: block.getStatements()) {
						if (stmt.getClass().equals(EnterStmt.class)) {
							EnterStmt enter = (EnterStmt) stmt;
							enter.setStackSize(enter.getStackSize() + tempsNeeded);
						}
					}
				}
			}
		}
	}
	
	private void optimize(CFGBlock block) {
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		List<AvailableExpression> exprs = availableGenerator.getBlockExpressions().get(block);
		HashMap<Name, HashSet<Integer>> nameToExprs = availableGenerator.getNameToExprIds();
		BlockFlow bFlow = availableGenerator.getBlockAvailableDefs().get(block);
		
		int exprIndex = 0;
		for (LIRStatement stmt: block.getStatements()) {
			if (!stmt.isExpressionStatement()) {
				newStmts.add(stmt);
				continue;
			}
			
			QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
			Name dest = qStmt.getDestination();
			// Update the set of Names that have been assigned
			if (nameToExprs.containsKey(dest))
				exprsClobbered.addAll(nameToExprs.get(dest));
			
			if (!stmt.isAvailableExpression()) {
				newStmts.add(stmt);
				continue;
			}
			
			// Statement is AvailableExpression
			AvailableExpression expr = exprs.get(exprIndex);
			DynamicVarName temp = exprToTemp.get(expr);
			// If available
			if (bFlow.getIn().get(expr.getMyId())) {
				if (!exprsClobbered.contains(expr)) {
					// Replace AvailableExpression with corresponding temporary
					qStmt.setArg1(temp);
					qStmt.setArg2(null);
					qStmt.setOperator(QuadrupletOp.MOVE);
					newStmts.add(stmt);
				} else {
					newStmts.add(stmt);
					// Re-assign temporary to destination
					QuadrupletStmt newStmt = new QuadrupletStmt(QuadrupletOp.MOVE, temp, dest, null);
					newStmts.add(newStmt);
					// Remove AvailableExpression from exprsClobbered
					exprsClobbered.remove(expr.getMyId());
				}
			} else {
				newStmts.add(stmt);
				// Check if expr is in Out set, and if so, assign corresponding temporary to it
				if (bFlow.getOut().get(expr.getMyId())) {
					QuadrupletStmt newStmt = new QuadrupletStmt(QuadrupletOp.MOVE, temp, dest, null);
					newStmts.add(newStmt);
				}
			}
			exprIndex++;
		}
	}
	
	public void printExprToTemp(PrintStream out) {
		out.println("EXPR TO GLOBAL TEMP MAPS: ");
		for (Entry<AvailableExpression, DynamicVarName> e : exprToTemp.entrySet()) {
			out.println(e.getKey() + " --> " + e.getValue());
		}
	}
	
	public BlockAvailableExpressionGenerator getAvailableGenerator() {
		return availableGenerator;
	}

	public void setAvailableGenerator(
			BlockAvailableExpressionGenerator availableGenerator) {
		this.availableGenerator = availableGenerator;
	}
}
