package decaf.ir.ast;

import decaf.ir.ASTVisitor;

public class IntLiteral extends Literal {
	private String rawValue;
	private int value;
	
	/*
	 * Constructor for int literal that takes a string as an input
	 * @param: String integer
	 */
	public IntLiteral(String val){
		rawValue = val; // Will convert to int value in semantic check
	}

	@Override
	public Type getType() {
		return Type.INT;
	}

	public String getStringValue() {
		return rawValue;
	}

	public void setStringValue(String stringValue) {
		this.rawValue = stringValue;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return rawValue;
	}

	@Override
	public <T> T accept(ASTVisitor<T> v) {
		return v.visit(this);
	}
}
