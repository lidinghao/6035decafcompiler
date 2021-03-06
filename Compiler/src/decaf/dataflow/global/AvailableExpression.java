package decaf.dataflow.global;

import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletOp;

public class AvailableExpression {
	private Name arg1;
	private Name arg2;
	private QuadrupletOp operator;
	private int myId;
	private static int ID = 0;

	public AvailableExpression(Name a1, Name a2, QuadrupletOp op) {
		arg1 = a1;
		arg2 = a2;
		operator = op;
		myId = ID;
		ID++;
	}
	
	public static int getID() {
		return ID;
	}

	public static void setID(int iD) {
		ID = iD;
	}
	
	public int getMyId() {
		return myId;
	}

	public void setMyId(int myId) {
		this.myId = myId;
		ID++;
	}
	
	public Name getArg1() {
		return arg1;
	}
	
	public void setArg1(Name arg1) {
		this.arg1 = arg1;
	}
	
	public Name getArg2() {
		return arg2;
	}
	
	public void setArg2(Name arg2) {
		this.arg2 = arg2;
	}
	
	public QuadrupletOp getOperator() {
		return operator;
	}
	
	public void setOperator(QuadrupletOp operator) {
		this.operator = operator;
	}
	
	@Override
	public String toString() {
		String out = "ID : " + Integer.toString(myId) + " -- ";
		switch (this.operator) {
			case ADD: 
				out += arg1 + " + " + arg2; break;
			case SUB:
				out += arg1 + " - " + arg2; break;
			case MUL:
				out += arg1 + " * " + arg2; break;
			case DIV:
				out += arg1 + " / " + arg2; break;
			case MOD:
				out += arg1 + " % " + arg2; break;
			case NOT:
				out += "!" + arg1; break;
			case MINUS:
				out += "-" + arg1; break;
			case EQ:
				out += arg1 + " == " + arg2; break;
			case NEQ:
				out += arg1 + " != " + arg2; break;
			case LT:
				out += arg1 + " < " + arg2; break;
			case LTE:
				out += arg1 + " <= " + arg2; break;
			case GT:
				out += arg1 + " > " + arg2; break;
			case GTE:
				out += arg1 + " <= " + arg2; break;
		}
		
		return out;
	}
	
	@Override
	public int hashCode() {
		if (operator == QuadrupletOp.ADD || operator == QuadrupletOp.MUL) {
			return 13 * (arg1.hashCode() + arg2.hashCode()) + 23 * operator.toString().hashCode();
		}
		if (arg2 == null) {
			return 17 * arg1.hashCode() + 23 * operator.toString().hashCode();
		}
		return 17 * arg1.hashCode() + 19 * arg2.hashCode() + 23 * operator.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!o.getClass().equals(AvailableExpression.class)) return false;
		
		AvailableExpression expr = (AvailableExpression) o;
		
		if (this.operator != expr.getOperator()) {
			return false;
		}
		
		if (operator == QuadrupletOp.ADD || operator == QuadrupletOp.MUL) {
			return checkCommutative(expr);
		}
		
		if (this.arg1 != null) {
			if (!this.arg1.equals(expr.getArg1())) {
				return false;
			}
		} 
		else {
			if (expr.getArg1() != null) {
				return false;
			}
		}
		
		if (this.arg2 != null) {
			if (!this.arg2.equals(expr.getArg2())) {
				return false;
			}
		} 
		else {
			if (expr.getArg2() != null) {
				return false;
			}
		}
		
		return true;
	}

	private boolean checkCommutative(AvailableExpression expr) {
		if (expr.getArg1().equals(this.arg1) && expr.getArg2().equals(this.arg2)) {
			return true;
		}
		
		if (expr.getArg1().equals(this.arg2) && expr.getArg2().equals(this.arg1)) {
			return true;
		}
		
		return false;
	}
}
