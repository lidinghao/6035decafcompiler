package ir.ast;

public class InvokeStmt extends Statement {
	private final MethodCallExpr MethodCall;
	
	public InvokeStmt(MethodCallExpr e) {
		this.MethodCall = e;
	}
	
	/*
	 * @return The method call of the statement
	 */
	public MethodCallExpr getMethodCall() {
		return this.MethodCall;
	}
}
