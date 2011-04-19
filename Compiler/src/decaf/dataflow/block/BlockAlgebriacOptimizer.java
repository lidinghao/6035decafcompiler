package decaf.dataflow.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;

public class BlockAlgebriacOptimizer {
	private HashMap<String, List<CFGBlock>> cfgMap;
	private ProgramFlattener pf;
	
	public BlockAlgebriacOptimizer(HashMap<String, List<CFGBlock>> cfgMap, ProgramFlattener pf) {
		this.cfgMap = cfgMap;
		this.pf = pf;
	}

	public void performAlgebriacSimplification() {
		for (String s: this.cfgMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			for (CFGBlock block: this.cfgMap.get(s)) {
				optimize(block);
				reset();
			}
			
			// Change statements
			List<LIRStatement> stmts = new ArrayList<LIRStatement>();
			
			for (int i = 0; i < this.cfgMap.get(s).size(); i++) {
				stmts.addAll(getBlockWithIndex(i, this.cfgMap.get(s)).getStatements());
			}
			
			pf.getLirMap().put(s, stmts);
		}
		
	}
	
	private void reset() {
		// None	
	}

	private void optimize(CFGBlock block) {
		for (LIRStatement stmt: block.getStatements()) {
			if (!stmt.isExpressionStatement()) {
				continue;
			}
			
			QuadrupletStmt qStmt = (QuadrupletStmt) stmt;
			processStatement(qStmt);
		}		
	}

	private void processStatement(QuadrupletStmt qStmt) {
		if (qStmt.getOperator() != QuadrupletOp.MOVE &&
				qStmt.getOperator() != QuadrupletOp.CMP) {
			Integer arg1 = null;
			Name arg2 = null;
			
			boolean inOrder = false;
			
			if (qStmt.getArg1() != null) {
				if (qStmt.getArg1().getClass().equals(ConstantName.class)) {
					arg1 = Integer.parseInt(((ConstantName)qStmt.getArg1()).getValue());
					inOrder = true;
				}
				
				arg2 = qStmt.getArg1();
			}
			
			if (qStmt.getArg2() != null) {
				if (qStmt.getArg2().getClass().equals(ConstantName.class)) {
					if (arg1 != null) return;
					arg1 = Integer.parseInt(((ConstantName)qStmt.getArg2()).getValue());
				}
				
				if (arg2 != null) return;
				arg2 = qStmt.getArg2();
			}
			
			if (arg1 != null && arg2 != null) {
				simplifyExpression(qStmt, arg1, arg2, inOrder);
			}
		}
	}

	private void simplifyExpression(QuadrupletStmt qStmt, int arg1, Name arg2, boolean inOrder) {
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
					if (arg1 == 1) { // x / 1 = x
						qStmt.setOperator(QuadrupletOp.MOVE);
						qStmt.setArg1(arg2);
						qStmt.setArg2(null);
					}
				}
				break;
			case MOD:
				if (!inOrder) {
					if (arg1 == 1) { // x % 1 = 0
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

	private CFGBlock getBlockWithIndex(int i, List<CFGBlock> list) {
		for (CFGBlock block: list) {
			if (block.getIndex() == i) {
				return block;
			}
		}
		
		return null;
	}
}
