package decaf.codegen.flatir;

public class QuadrupletStmt extends LIRStatement {
	private QuadrupletOp operator;
	private Name destination;
	private Name arg1;
	private Name arg2;
	
	public QuadrupletStmt(QuadrupletOp operator, Name dest, Name arg1, Name arg2) {
		this.operator = operator;
		this.destination = dest;
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
		return destination;
	}

	public void setDestination(Name destination) {
		this.destination = destination;
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
}
