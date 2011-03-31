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
		switch (this.operator) {
			case CMP:
				return "compare " + arg1 + ", " + arg2;
			case ADD:
				return arg1 + " + " + arg2;
			case SUB:
				return arg1 + " - " + arg2;
			case MUL:
				return arg1 + " * " + arg2;
			case DIV:
				return arg1 + " / " + arg2;
			case MOD:
				return arg1 + " % " + arg2;
			case NOT:
				return "!" + arg1;
			case MINUS:
				return "-" + arg1;
			case EQ:
				return arg1 + " == " + arg2;
			case NEQ:
				return arg1 + " != " + arg2;
			case LT:
				return arg1 + " < " + arg2;
			case LTE:
				return arg1 + " <= " + arg2;
			case GT:
				return arg1 + " > " + arg2;
			case GTE:
				return arg1 + " <= " + arg2;
		}
		
		return null;
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!o.getClass().equals(AvailableExpression.class)) return false;
		
		AvailableExpression expr = (AvailableExpression) o;
		if (this.operator != expr.getOperator()) {
			return false;
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
}
