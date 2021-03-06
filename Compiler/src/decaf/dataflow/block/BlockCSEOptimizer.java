package decaf.dataflow.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.DynamicVarName;
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

public class BlockCSEOptimizer {
	private HashMap<Name, SymbolicValue> varToVal;
	private HashMap<ValueExpr, SymbolicValue> expToVal;
	private HashMap<ValueExpr, DynamicVarName> expToTemp;
	private HashMap<String, MethodIR> mMap;
	
	public BlockCSEOptimizer(HashMap<String, MethodIR> mMap) {
		this.varToVal = new HashMap<Name, SymbolicValue>();
		this.expToVal = new HashMap<ValueExpr, SymbolicValue>();
		this.expToTemp = new HashMap<ValueExpr, DynamicVarName>();
		this.mMap = mMap;
	}
	
	public void performCSE() {
		//DynamicVarName.reset();
		
		for (String s: this.mMap.keySet()) {
			if (s.equals(ProgramFlattener.exceptionHandlerLabel)) continue;
			
			for (CFGBlock block: this.mMap.get(s).getCfgBlocks()) {
				optimize(block);
				reset();
			}
			
			this.mMap.get(s).regenerateStmts();
		}
	}
	
	public void optimize(CFGBlock block) {
		SymbolicValue.reset();
		List<LIRStatement> newStmts = new ArrayList<LIRStatement>();
		
		for (LIRStatement stmt: block.getStatements()) {
			if (!stmt.getClass().equals(QuadrupletStmt.class)) {
				newStmts.add(stmt);
				
				// TODO: May have to change this after RegisterAllocator is implemented
				if (stmt.getClass().equals(CallStmt.class)) {			
					CallStmt callStmt = (CallStmt) stmt;
					if (callStmt.getMethodLabel().equals(ProgramFlattener.exceptionHandlerLabel)) continue;
					
					// Invalidate arg registers
					for (int i = 0; i < Register.argumentRegs.length; i++) {
						this.varToVal.put(new RegisterName(Register.argumentRegs[i]), new SymbolicValue());
					}
					
					// Reset symbolic value for %RAX
					this.varToVal.put(new RegisterName(Register.RAX), new SymbolicValue()); 
					
					// Invalidate global vars;
					for (Name name: this.varToVal.keySet()) {
						if (name.getClass().equals(VarName.class)) {
							VarName var = (VarName) name;
							if (var.getBlockId() == -1) { // Global
								this.varToVal.put(name, new SymbolicValue()); 
							}
						}
					}
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
		
		if (!expToVal.containsKey(expr)) {
			dest = new SymbolicValue();
			expToVal.put(expr, dest);
			varToVal.put(qStmt.getDestination(), dest);
			newStmts.add(qStmt);
			
			// Add new temp for dest
			DynamicVarName temp = new DynamicVarName();
			expToTemp.put(expr, temp);
			newStmts.add(new QuadrupletStmt(QuadrupletOp.MOVE, temp, qStmt.getDestination(), null));
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
