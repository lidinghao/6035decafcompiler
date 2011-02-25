package ir.ast;

public class AssignStmt extends Statement {
	private Location location;
	private Expression expr;
	private AssignOpType operator;

	public AssignStmt(Location loc, AssignOpType op, Expression e) {
		this.location = loc;
		this.expr = e;
		this.operator = op;
	}
	
	public void setLocation(Location loc) {
		this.location = loc;
	}
	
    /*
     * @return Left hand side of the assignment statement
     */
	public Location getLocation() {
		return this.location;
	}
	
	public void setExpression(Expression e) {
		this.expr = e;
	}
	
	public AssignOpType getOperator() {
		return operator;
	}

	public void setOperator(AssignOpType operator) {
		this.operator = operator;
	}
	
	/*
     * @return Right hand side of the assignment statement
     */
	public Expression getExrpression() {
		return this.expr;
	}
}
