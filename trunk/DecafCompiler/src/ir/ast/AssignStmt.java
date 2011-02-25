package ir.ast;

public class AssignStmt extends Statement {
	private Location location;
	private Expression expr;
	private BinOpType operator;

	public AssignStmt(Location loc, BinOpType op, Expression e) {
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
	
	public BinOpType getOperator() {
		return operator;
	}

	public void setOperator(BinOpType operator) {
		this.operator = operator;
	}
	
	/*
     * @return Right hand side of the assignment statement
     */
	public Expression getExrpression() {
		return this.expr;
	}
}
