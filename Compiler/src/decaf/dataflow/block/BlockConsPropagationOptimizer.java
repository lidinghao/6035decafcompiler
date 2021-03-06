package decaf.dataflow.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.PopStmt;
import decaf.codegen.flatir.PushStmt;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.Register;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flatir.VarName;
import decaf.codegen.flattener.ProgramFlattener;
import decaf.dataflow.cfg.CFGBlock;
import decaf.dataflow.cfg.MethodIR;

public class BlockConsPropagationOptimizer {
	private HashMap<String, MethodIR> mMap;
	private HashMap<Name, Integer> constantMap;
	
	public BlockConsPropagationOptimizer(HashMap<String, MethodIR> mMap) {
		this.mMap = mMap;
		this.constantMap = new HashMap<Name, Integer>();
	}

	public void performConsPropagation() {
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
		this.constantMap.clear();		
	}

	private void optimize(CFGBlock block) {
		for (LIRStatement stmt: block.getStatements()) {
			if (!stmt.isUseStatement()) {
				// TODO: May have to change this after RegisterAllocator is implemented
				if (stmt.getClass().equals(CallStmt.class)) {		
					CallStmt callStmt = (CallStmt) stmt;
					if (callStmt.getMethodLabel().equals(ProgramFlattener.exceptionHandlerLabel)) continue;
					
					invalidateFunctionCall();
				}
				continue;
			}
			
			if (stmt.getClass().equals(QuadrupletStmt.class)) {
				QuadrupletStmt qStmt = (QuadrupletStmt) stmt;
				processStatement(qStmt);
			}
			else if (stmt.getClass().equals(CmpStmt.class)) {
				CmpStmt cStmt = (CmpStmt) stmt;
				
				cStmt.setArg1(processName(cStmt.getArg1()));
				cStmt.setArg2(processName(cStmt.getArg2()));
			}
			else if (stmt.getClass().equals(PushStmt.class)) {
				PushStmt pStmt = (PushStmt) stmt;
				pStmt.setName(processName(pStmt.getName()));
			}
			else if (stmt.getClass().equals(PopStmt.class)) {
				PopStmt pStmt = (PopStmt) stmt;
				pStmt.setName(processName(pStmt.getName()));
			}
		}		
	}

	private Name processName(Name name) {
		if (name != null && this.constantMap.containsKey(name)) {
			return new ConstantName(this.constantMap.get(name));
		}
		
		return name;
	}

	private void invalidateFunctionCall() {
		// Invalidate arg registers
		for (int i = 0; i < Register.argumentRegs.length; i++) {
			this.constantMap.remove(new RegisterName(Register.argumentRegs[i]));
		}
		
		// Reset symbolic value for %RAX
		this.constantMap.remove(new RegisterName(Register.RAX)); 
		
		// Invalidate global vars;
		List<Name> namesToRemove = new ArrayList<Name>();
		for (Name name: this.constantMap.keySet()) {
			if (name.getClass().equals(VarName.class)) {
				VarName var = (VarName) name;
				if (var.getBlockId() == -1) { // Global
					namesToRemove.add(name);
				}
			}
		}
		
		for (Name name: namesToRemove) {
			this.constantMap.remove(name);
		}
	}

	private void processStatement(QuadrupletStmt qStmt) {
		// If is a move (a = b)
		if (qStmt.getOperator() == QuadrupletOp.MOVE) {
			// If setting something to constant, put it in map
			if (qStmt.getArg1().getClass().equals(ConstantName.class)) {
				ConstantName c = (ConstantName) qStmt.getArg1();
				this.constantMap.put(qStmt.getDestination(), Integer.parseInt(c.getValue()));
			}
			else {
				// If setting to a variable that is constant, replace with constant, and mark dest as constant
				if (this.constantMap.containsKey(qStmt.getArg1())) {
					int val = this.constantMap.get(qStmt.getArg1());
					qStmt.setArg1(new ConstantName(val));
					this.constantMap.put(qStmt.getDestination(), val);
				}
				// Else remove dest if its in constant map
				else {
					this.constantMap.remove(qStmt.getDestination());
				}
			}
		}
		else {
			Integer arg1 = null;
			Integer arg2 = null;
			
			// Check arg1 is in constant map
			if (this.constantMap.containsKey(qStmt.getArg1())) {
				int val = this.constantMap.get(qStmt.getArg1());
				qStmt.setArg1(new ConstantName(val));
				
				arg1 = val;
			}
			
			// Check arg1 is constant
			if (qStmt.getArg1() != null && qStmt.getArg1().getClass().equals(ConstantName.class)) {
				arg1 = Integer.parseInt(((ConstantName)qStmt.getArg1()).getValue());
			}
			
			// Check arg2 is in constant map
			if (this.constantMap.containsKey(qStmt.getArg2())) {
				int val = this.constantMap.get(qStmt.getArg2());
				qStmt.setArg2(new ConstantName(val));
				
				arg2 = val;
			}
			
			// Check arg2 is constant
			if (qStmt.getArg2() != null && qStmt.getArg2().getClass().equals(ConstantName.class)) {
				arg2 = Integer.parseInt(((ConstantName)qStmt.getArg2()).getValue());
			}
			
			// Remove dest
			this.constantMap.remove(qStmt.getDestination());
			
			// Static evaluation!
			if (arg1 != null && arg2 != null) {
				evaluateExpression(qStmt, arg1, arg2);
			}
		}
	}

	private void evaluateExpression(QuadrupletStmt qStmt, int arg1, int arg2) {
		int result = 0;
		Boolean boolResult = null;
		
		switch (qStmt.getOperator()) {
			case ADD:
				result = arg1 + arg2;
				break;
			case SUB:
				result = arg1 - arg2;
				break;
			case MUL:
				result = arg1 * arg2;
				break;
			case DIV:
				result = arg1 / arg2;
				break;
			case MOD:
				result = arg1 % arg2;
				break;
			case LT:
				boolResult = arg1 < arg2;
				break;
			case LTE:
				boolResult = arg1 <= arg2;
				break;
			case GT:
				boolResult = arg1 > arg2;
				break;
			case GTE:
				boolResult = arg1 >= arg2;
				break;
			case EQ:
				boolResult = arg1 == arg2;
				break;
			case NEQ:
				boolResult = arg1 != arg2;
				break;
		}
		
		if (boolResult != null) {
			if (boolResult) { 
				result = 1;
			}
			else { 
				result = 0;
			}
		}
		
		qStmt.setArg2(null);
		qStmt.setArg1(new ConstantName(result));
		qStmt.setOperator(QuadrupletOp.MOVE);
		this.constantMap.put(qStmt.getDestination(), result);
	}
}
