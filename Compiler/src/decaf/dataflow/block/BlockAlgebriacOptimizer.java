package decaf.dataflow.block;

import java.util.HashMap;

import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

public class BlockAlgebriacOptimizer {
	private HashMap<String, MethodIR> mMap;
	
	public BlockAlgebriacOptimizer(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
	}

	public void performAlgebriacSimplification() {
		for (String s: this.mMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			for (CFGBlock block: this.mMap.get(s).getCfgBlocks()) {
				optimize(block);
				reset();
			}
			
			this.mMap.get(s).regenerateStmts();
		}
		
	}
	
	private void reset() {
		// None	
	}

	private void optimize(CFGBlock block) {
		for (LIRStatement stmt: block.getStatements()) {
			if (!stmt.getClass().equals(QuadrupletStmt.class)) {
				continue;
			}
			
			QuadrupletStmt qStmt = (QuadrupletStmt) stmt;
			processStatement(qStmt);
		}		
	}

	private void processStatement(QuadrupletStmt qStmt) {
		if (qStmt.hasTwoArgs()) {
			Integer arg1 = null;
			Name arg2 = null;
			
			boolean inOrder = false;
			
			if (qStmt.getArg1().getClass().equals(ConstantName.class)) {
				arg1 = Integer.parseInt(((ConstantName)qStmt.getArg1()).getValue());
				inOrder = true;
				arg2 = qStmt.getArg2();
			}
			
			if (qStmt.getArg2().getClass().equals(ConstantName.class)) {
					if (arg1 != null) return; // Both constants, should be simplified by ConstProp
					arg1 = Integer.parseInt(((ConstantName)qStmt.getArg2()).getValue());
					
					arg2 = qStmt.getArg1();
			}
			
			if (arg1 != null && arg2 != null) {
				simplifyBinaryExpression(qStmt, arg1, arg2, inOrder);
			}
		}
		else {
			simplifyUnaryExpression(qStmt);
		}
	}

	private void simplifyUnaryExpression(QuadrupletStmt qStmt) {
		switch (qStmt.getOperator()) {
			case NOT:
				if (qStmt.getArg1().getClass().equals(ConstantName.class)) {
					qStmt.setOperator(QuadrupletOp.MOVE);
					int val = Integer.parseInt(((ConstantName)qStmt.getArg1()).getValue());
					if (val != 0) {
						qStmt.setArg1(new ConstantName(0));
					}
					else {
						qStmt.setArg1(new ConstantName(1));
					}
				}
				break;
			case MINUS:
				if (qStmt.getArg1().getClass().equals(ConstantName.class)) {
					qStmt.setOperator(QuadrupletOp.MOVE);
					int val = Integer.parseInt(((ConstantName)qStmt.getArg1()).getValue());
					if (val != 0) {
						qStmt.setArg1(new ConstantName(-1 * val));
					}
				}
				break;
		}		
	}

	private void simplifyBinaryExpression(QuadrupletStmt qStmt, int arg1, Name arg2, boolean inOrder) {
		switch (qStmt.getOperator()) {
			case ADD:
				if (arg1 == 0) {
					qStmt.setOperator(QuadrupletOp.MOVE);
					qStmt.setArg1(arg2);
					qStmt.setArg2(null);
				}
				break;
			case SUB:
				if (arg1 == 0) {
					if (inOrder) {
						qStmt.setOperator(QuadrupletOp.MINUS);
					}
					else {
						qStmt.setOperator(QuadrupletOp.MOVE);
					}
					qStmt.setArg1(arg2);
					qStmt.setArg2(null);
				}
				break;
			case MUL:
				if (arg1 == 1) {
					qStmt.setOperator(QuadrupletOp.MOVE);
					qStmt.setArg1(arg2);
					qStmt.setArg2(null);
				}
				else if (arg1 == 0) {
					qStmt.setOperator(QuadrupletOp.MOVE);
					qStmt.setArg1(new ConstantName(0));
					qStmt.setArg2(null);
				}
				break;
			case DIV:
				if (!inOrder) {
					if (arg1 == 1) { // x/1 = x
						qStmt.setOperator(QuadrupletOp.MOVE);
						qStmt.setArg1(arg2);
						qStmt.setArg2(null);
					}
				}
				break;
			case MOD:
				if (!inOrder) {
					if (arg1 == 1) { // x%1 = 0
						qStmt.setOperator(QuadrupletOp.MOVE);
						qStmt.setArg1(new ConstantName(0));
						qStmt.setArg2(null);
					}
				}
				break;
			case LT:
				break;
			case LTE:
				break;
			case GT:
				break;
			case GTE:
				break;
			case EQ:
				break;
			case NEQ:
				break;
		}
	}
}
