package ir.ast;

import ir.semcheck.*;

public abstract class AST {
	protected int lineNumber;
	protected int colNumber;
	
	public int getLineNumber() {
		return lineNumber;
	}
	
	public void setLineNumber(int ln) {
		lineNumber = ln;
	}
	
	public int getColumnNumber() {
		return colNumber;
	}
	
	public void setColumnNumber(int cn) {
		colNumber = cn;
	}
	
	//public abstract <T> T accept(ASTVisitor<T> v);
}
