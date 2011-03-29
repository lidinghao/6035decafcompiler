package decaf.dataflow.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.DynamicVarName;
import decaf.codegen.flatir.EnterStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.Register;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;

public class BlockCSEOptimizer {
	private HashMap<Name, SymbolicValue> varToVal;
	private HashMap<ValueExpr, SymbolicValue> expToVal;
	private HashMap<ValueExpr, DynamicVarName> expToTemp;
	private HashMap<String, List<CFGBlock>> cfgMap;
	private ProgramFlattener pf;
	private int tempCount;
	
	public BlockCSEOptimizer(HashMap<String, List<CFGBlock>> cfgMap, ProgramFlattener pf) {
		this.varToVal = new HashMap<Name, SymbolicValue>();
		this.expToVal = new HashMap<ValueExpr, SymbolicValue>();
		this.expToTemp = new HashMap<ValueExpr, DynamicVarName>();
		this.cfgMap = cfgMap;
		this.pf = pf;
		this.tempCount = 0;
	}
	
	public void performCSE() {
		for (String s: this.cfgMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			DynamicVarName.reset();
			for (CFGBlock block: this.cfgMap.get(s)) {
				optimize(block);
				reset();
			}
			
			// Fix stack size
			for (CFGBlock block: this.cfgMap.get(s)) {
				if (block.getIndex() == 0) {
					for (LIRStatement stmt: block.getStatements()) {
						if (stmt.getClass().equals(EnterStmt.class)) {
							EnterStmt enter = (EnterStmt) stmt;
							enter.setStackSize(enter.getStackSize() + tempCount);
						}
					}
				}
			}
			
			// Change statements
			List<LIRStatement> stmts = new ArrayList<LIRStatement>();
			
			for (int i = 0; i < this.cfgMap.get(s).size(); i++) {
				stmts.addAll(getBlockWithIndex(i, this.cfgMap.get(s)).getStatements());
			}
			
			pf.getLirMap().put(s, stmts);
			
			this.tempCount = 0;
		}
	}
	
	public CFGBlock getBlockWithIndex(int i, List<CFGBlock> list) {
		for (CFGBlock block: list) {
			if (block.getIndex() == i) {
				return block;
			}
		}
		
		return null;
	}
	
	public void optimize(CFGBlock block) {
		SymbolicValue.reset();
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		
		for (LIRStatement stmt: block.getStatements()) {
			if (!stmt.isExpressionStatement()) {
				newStmts.add(stmt);
				if (stmt.getClass().equals(CallStmt.class)) {
					this.varToVal.put(new RegisterName(Register.RAX), new SymbolicValue()); // Reset symbolic value for %RAX
				}
				continue;
			}
			
			QuadrupletStmt qStmt = (QuadrupletStmt) stmt;
			processStatement(qStmt, newStmts);
		}
		
		block.setStatements(newStmts);
	}

	private void reset() {
		this.expToTemp.clear();
		this.expToVal.clear();
		this.varToVal.clear();
	}

	private void processStatement(QuadrupletStmt qStmt, List<LIRStatement> newStmts) {
		ValueExpr expr = getValueExpression(qStmt);
		SymbolicValue dest;
		
		System.out.println(varToVal);
//		System.out.println(expToVal);
		System.out.println(expToTemp);
		System.out.println();
//		System.out.println(newStmts);
		
		if (!expToVal.containsKey(expr)) {
			dest = new SymbolicValue();
			expToVal.put(expr, dest);
			varToVal.put(qStmt.getDestination(), dest);
			newStmts.add(qStmt);
			
			// Add new temp for dest
			DynamicVarName temp = new DynamicVarName();
			expToTemp.put(expr, temp);
			newStmts.add(new QuadrupletStmt(QuadrupletOp.MOVE, temp, qStmt.getDestination(), null));
			tempCount++;
		}
		else {
			dest = expToVal.get(expr);
			varToVal.put(qStmt.getDestination(), dest);
			newStmts.add(new QuadrupletStmt(QuadrupletOp.MOVE, qStmt.getDestination(), expToTemp.get(expr), null));
		}
	}

	private ValueExpr getValueExpression(QuadrupletStmt qStmt) {
		SymbolicValue val1, val2 = null;
		
		if (varToVal.containsKey(qStmt.getArg1())) {
			val1 = varToVal.get(qStmt.getArg1());
		}
		else {
			val1 = new SymbolicValue();
			varToVal.put(qStmt.getArg1(), val1);
		}
		
		ValueExprOp op = null;
		switch(qStmt.getOperator()) {
			case MOVE:
				op = ValueExprOp.NONE;
				break;
			case ADD:
				op = ValueExprOp.ADD;
				break;
			case SUB:
				op = ValueExprOp.SUB;
				break;
			case MUL:
				op = ValueExprOp.MUL;
				break;
			case DIV:
				op = ValueExprOp.DIV;
				break;
			case MOD:
				op = ValueExprOp.MOD;
				break;
			case LT:
				op = ValueExprOp.LT;
				break;
			case LTE:
				op = ValueExprOp.LTE;
				break;
			case GT:
				op = ValueExprOp.GT;
				break;
			case GTE:
				op = ValueExprOp.GTE;
				break;
			case EQ:
				op = ValueExprOp.EQ;
				break;
			case NEQ:
				op = ValueExprOp.NEQ;
				break;
			case NOT:
				op = ValueExprOp.NOT;
				break;
			case MINUS:
				op = ValueExprOp.MINUS;
				break;
		}
		
		if (op != ValueExprOp.NONE && op != ValueExprOp.MINUS && op != ValueExprOp.NOT) {
			if (varToVal.containsKey(qStmt.getArg2())) {
				val2 = varToVal.get(qStmt.getArg2());
			}
			else {
				val2 = new SymbolicValue();
				varToVal.put(qStmt.getArg2(), val2);
			}
		}
		
		return new ValueExpr(op, val1, val2);
	}
}
