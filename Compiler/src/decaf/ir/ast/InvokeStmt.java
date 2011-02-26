package decaf.ir.ast;

import decaf.ir.semcheck.ASTVisitor;

public class InvokeStmt extends Statement {
	private CallExpr methodCall;
	
	public InvokeStmt(CallExpr e) {
		this.methodCall = e;
	}

	public CallExpr getMethodCall() {
		return methodCall;
	}

	public void setMethodCall(CallExpr methodCall) {
		this.methodCall = methodCall;
	}
	
	@Override
	public String toString() {
		return methodCall.toString();
	}

	@Override
	public <T> T accept(ASTVisitor<T> v) {
		// TODO Auto-generated method stub
		return null;
	}
}
