package decaf.ir.ast;

public class InvokeStmt extends Statement {
	private MethodCallExpr methodCall;
	
	public InvokeStmt(MethodCallExpr e) {
		this.methodCall = e;
	}

	public MethodCallExpr getMethodCall() {
		return methodCall;
	}

	public void setMethodCall(MethodCallExpr methodCall) {
		this.methodCall = methodCall;
	}
	
	@Override
	public String toString() {
		return methodCall.toString();
	}
}
