package ir.ast;

public class UnaryOpExpr extends Expression {
	private UnaryOpType unaryEnum;
	private Expression expr;
	
	/*
	 * @param: String enumValue should be either '-' or '!'. Expression expr is an expression
	 */
	public UnaryOpExpr(String enumValue, Expression expr){
		if(enumValue == "!")
			unaryEnum = UnaryOpType.NOT;
		else
			unaryEnum = UnaryOpType.MINUS;
		this.expr = expr;
	}
	
	/*
	 * @return: returns expression in the UnaryOpExpr
	 */
	public Expression getExpression(){
		return this.expr;
	}
	
	/*
	 * @return: returns UnaryOpType of the UnaryOpExpr
	 */
	public UnaryOpType getOperatorType(){
		return unaryEnum;
	}
	
	

}
