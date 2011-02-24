package ir.ast;

public abstract class Literal extends Expression {
	protected String value;
	
	/*
	 * @return: returns string value of int literal
	 */
	public String getIntLiteral(){
		return value;
	}
}
