package decaf.codegen.flatir;

public class QuadrupletStmt extends LIRStatement {
	private QuadrupletOp operator;
	private Address destination;
	private Address arg1;
	private Address arg2;
	
	public QuadrupletStmt(QuadrupletOp operator, Address destination, Address arg1, Address arg2) {
		this.operator = operator;
		this.destination = destination;
		this.arg1 = arg1;
		this.arg2 = arg2;
	}

	public QuadrupletOp getOperator() {
		return operator;
	}

	public void setOperator(QuadrupletOp operator) {
		this.operator = operator;
	}

	public Address getDestination() {
		return destination;
	}

	public void setDestination(Address destination) {
		this.destination = destination;
	}

	public Address getArg1() {
		return arg1;
	}

	public void setArg1(Address arg1) {
		this.arg1 = arg1;
	}

	public Address getArg2() {
		return arg2;
	}

	public void setArg2(Address arg2) {
		this.arg2 = arg2;
	}
}
