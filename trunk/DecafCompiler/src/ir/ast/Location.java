package ir.ast;

public class Location extends Expression {
	private final String id;
	private final Expression expr;
	
	
	/*
	 * @Constructor: that takes string ID as an input. location : ID;
	 * @param: String which is ID
	 */
	public Location(String id){
		this.id = id;
		this.expr = null;
	}
	
	/*
	 * @Constructor: takes String and expression. location : ID [ expr ] ;
	 * @param: String id, Expression expr
	 */
	public Location(String id, Expression expr){
		this.id = id;
		this.expr = expr;
	}
	
	/*
	 * returns ID
	 * @return String id
	 */
	public String getID(){
		return this.id;
	}
	
	/*
	 * returns expression. If expression is null then location is defined without expr
	 * @return expr can be null.
	 */
	public Expression getExpression(){
		return this.expr;
	}
	
	/*
	 * Checks if Location contains square brackets and expression
	 */
	public Boolean isWithExpression(){
		return expr != null;
	}

}
