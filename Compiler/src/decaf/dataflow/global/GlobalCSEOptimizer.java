package decaf.dataflow.global;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import decaf.codegen.flatir.DynamicVarName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

public class GlobalCSEOptimizer {
	private HashMap<String, MethodIR> mMap;
	private HashMap<AvailableExpression, DynamicVarName> exprToTemp;
	private BlockAvailableExpressionGenerator availableGenerator;
	private HashSet<Integer> exprsClobbered;
	
	public GlobalCSEOptimizer(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.availableGenerator = new BlockAvailableExpressionGenerator(mMap);
		this.exprToTemp = new HashMap<AvailableExpression, DynamicVarName>();
		this.exprsClobbered = new HashSet<Integer>();
		// Generate Available Expressions for CFG
		this.availableGenerator.generate();
	}
	
	public void performGlobalCSE() {
		//DynamicVarName.reset();
		
		if (availableGenerator.getTotalExpressionStmts() == 0)
			return;

		for (String s: this.mMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			// Create temporary variables for each unique AvailableExpression
			initializeTemporaryMap(s);
			
			// Optimize blocks
			for (CFGBlock block: this.mMap.get(s).getCfgBlocks()) {
				optimize(block);
				resetBlock();
			}
		
			this.mMap.get(s).regenerateStmts();
			
			resetMethod();
		}
	}
	
	private void resetBlock() {
		exprsClobbered = new HashSet<Integer>();
	}
	
	private void resetMethod() {
		exprToTemp = new HashMap<AvailableExpression, DynamicVarName>();
	}
	
	private void initializeTemporaryMap(String method) {
		List<AvailableExpression> availExprs = 
			availableGenerator.getMethodExpressions().get(method);
		if (availExprs != null) {
			for (AvailableExpression expr : availExprs) {
				exprToTemp.put(expr, new DynamicVarName(true));
			}
		}
	}
	
	private void optimize(CFGBlock block) {
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		List<AvailableExpression> exprs = availableGenerator.getBlockExpressions().get(block);
		HashMap<Name, HashSet<Integer>> nameToExprs = availableGenerator.getNameToExprIds();
		BlockDataFlowState bFlow = availableGenerator.getBlockAvailableDefs().get(block);
		
		int exprIndex = 0;
		for (LIRStatement stmt: block.getStatements()) {
			if (!stmt.getClass().equals(QuadrupletStmt.class)) {
				newStmts.add(stmt);
				continue;
			}
			
			QuadrupletStmt qStmt = (QuadrupletStmt)stmt;
			Name dest = qStmt.getDestination();
			
			if (!stmt.isAvailableExpression()) {
				newStmts.add(stmt);
				continue;
			}
			
			// Statement is AvailableExpression
			AvailableExpression expr = exprs.get(exprIndex);
			DynamicVarName temp = exprToTemp.get(expr);
			
			// If available
			if (bFlow.getIn().get(expr.getMyId())) {
				if (!exprsClobbered.contains(expr.getMyId())) {
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
			
			// Update the set of Names that have been assigned
			if (nameToExprs.containsKey(dest))
				exprsClobbered.addAll(nameToExprs.get(dest));
			exprIndex++;
		}
		
		block.setStatements(newStmts);
	}
	
	public void printExprToTemp(PrintStream out) {
		out.println("EXPR TO GLOBAL TEMP MAPS: ");
		for (String s: this.mMap.keySet()) {
			exprToTemp = new HashMap<AvailableExpression, DynamicVarName>();
			initializeTemporaryMap(s);
			System.out.println("METHOD: " + s);
			for (Entry<AvailableExpression, DynamicVarName> e : exprToTemp.entrySet()) {
				out.println(e.getKey() + " --> " + e.getValue());
			}
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
