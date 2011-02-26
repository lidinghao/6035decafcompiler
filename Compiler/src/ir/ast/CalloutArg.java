package ir.ast;

public class CalloutArg {
	private String stringArg = null;
	private Expression exprArg = null;
	
	public CalloutArg(String arg) {
		this.stringArg = arg;
	}
	
	public CalloutArg(Expression expr) {
		this.exprArg = expr;
	}
	
	@Override
	public String toString() {
		if (stringArg == null) {
			return exprArg.toString();
		}
		else {
			return stringArg;
		}
	}
}
