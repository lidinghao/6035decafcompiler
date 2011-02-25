package ir.ast;

import java.util.List;

public class CalloutExpr extends CallExpr {
	private class CalloutArg {
		private String stringArg = null;
		private Expression exprArg = null;
		
		public CalloutArg(String arg) {
			this.stringArg = arg;
		}
		
		public CalloutArg(Expression expr) {
			this.exprArg = expr;
		}
	}
	
	private String methodName;
	private List<CalloutArg> args;
	
	public CalloutExpr(String name) {
		this.methodName = name;
	}
	
	public void addArgument(String arg) {
		this.args.add(new CalloutArg(arg));
	}
	
	public void addArgument(Expression arg) {
		this.args.add(new CalloutArg(arg));
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
}
