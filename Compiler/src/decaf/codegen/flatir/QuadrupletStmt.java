package decaf.codegen.flatir;

public class QuadrupletStmt extends LIRStatement {
	private QuadrupletOp operator;
	private Name dest;
	private Name arg1;
	private Name arg2;
	
	public QuadrupletStmt(QuadrupletOp operator, Name dest, Name arg1, Name arg2) {
		this.operator = operator;
		this.dest = dest;
		this.arg1 = arg1;
		this.arg2 = arg2;
	}

	public QuadrupletOp getOperator() {
		return operator;
	}

	public void setOperator(QuadrupletOp operator) {
		this.operator = operator;
	}

	public Name getDestination() {
		return dest;
	}

	public void setDestination(Name destination) {
		this.dest = destination;
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
	
	@Override
	public String toString() {
		switch (this.operator) {
			case CMP:
				return "CMP " + arg1 + ", " + arg2;
			case ADD:
				return dest + " = " + arg1 + " + " + arg2;
			case SUB:
				return dest + " = " + arg1 + " - " + arg2;
			case MUL:
				return dest + " = " + arg1 + " * " + arg2;
			case DIV:
				return dest + " = " + arg1 + " / " + arg2;
			case MOD:
				return dest + " = " + arg1 + " % " + arg2;
			case MOVE:
				return dest + " = " + arg1;
			case EQ:
				return dest + " = " + arg1 + " == " + arg2;
			case NEQ:
				return dest + " = " + arg1 + " != " + arg2;
			case LT:
				return dest + " = " + arg1 + " < " + arg2;
			case LTE:
				return dest + " = " + arg1 + " <= " + arg2;
			case GT:
				return dest + " = " + arg1 + " > " + arg2;
			case GTE:
				return dest + " = " + arg1 + " <= " + arg2;
		}
		
		return null;
	}
}
