package decaf.ir.ast;

import decaf.ir.ASTVisitor;

public class CalloutArg extends AST {
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

	@Override
	public <T> T accept(ASTVisitor<T> v) {
		// TODO Auto-generated method stub
		return null;
	}
}
