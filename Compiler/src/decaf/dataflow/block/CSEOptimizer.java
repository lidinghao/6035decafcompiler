package decaf.dataflow.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.DynamicVarName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.dataflow.cfg.CFGBlock;

public class CSEOptimizer {
	private HashMap<Name, SymbolicValue> varToVal;
	private HashMap<ValueExpr, SymbolicValue> expToVal;
	private HashMap<ValueExpr, DynamicVarName> expToTemp;
	
	public CSEOptimizer() {
		this.varToVal = new HashMap<Name, SymbolicValue>();
		this.expToVal = new HashMap<ValueExpr, SymbolicValue>();
		this.expToTemp = new HashMap<ValueExpr, DynamicVarName>();
	}
	
	public void optimize(CFGBlock block) {
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		
		for (LIRStatement stmt: block.getStatements()) {
			if (!stmt.isExpressionStatement()) {
				newStmts.add(stmt);
				continue;
			}
			
			QuadrupletStmt qStmt = (QuadrupletStmt) stmt;
			processStatemet(qStmt);
		}
	}

	private void processStatemet(QuadrupletStmt qStmt) {
		ValueExpr expr = getValueExpression(qStmt);		
	}

	private ValueExpr getValueExpression(QuadrupletStmt qStmt) {
		SymbolicValue val1, val2;
		
		if (varToVal.containsKey(qStmt.getArg1())) {
			val1 = varToVal.get(qStmt.getArg1());
		}
		else {
			val1 = new SymbolicValue();
			varToVal.put(qStmt.getArg1(), val1);
		}
	}
}
