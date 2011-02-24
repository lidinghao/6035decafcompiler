package ir.ast;

public class OpAssignStmt extends Statement {
	private final Location lhs;
	private final Expression rhs;
	
	public OpAssignStmt(Location l, Expression r) {
		this.lhs = l;
		this.rhs = r;
	}
	
    /*
     * @return Left hand side of the assignment statement
     */
	public Location getLocation() {
		return this.lhs;
	}
	
	/*
     * @return Right hand side of the assignment statement
     */
	public Expression getExrpression() {
		return this.rhs;
	}

}
