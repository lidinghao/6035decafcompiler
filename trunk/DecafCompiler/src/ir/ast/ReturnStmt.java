package ir.ast;

public class ReturnStmt extends Statement {
	private Expression expr; // the return expression
	
	public ReturnStmt(Expression e) {
		this.expr = e;
	}
	
	public ReturnStmt() {
		
	}
	
	/*
	 * @return the return expression of the statement
	 */
	public Expression getExpression() {
		if (this.expr != null) {
			return this.expr;
		} else {
			return null;
		}
		
	}

}
