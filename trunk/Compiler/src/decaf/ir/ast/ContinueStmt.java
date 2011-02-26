package decaf.ir.ast;

import decaf.ir.ASTVisitor;

public class ContinueStmt extends Statement {
	public ContinueStmt() {	}
	
	@Override
	public String toString() {
		return "continue";
	}

	@Override
	public <T> T accept(ASTVisitor<T> v) {
		// TODO Auto-generated method stub
		return null;
	}
}
