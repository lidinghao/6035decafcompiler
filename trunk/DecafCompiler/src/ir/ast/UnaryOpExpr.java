package ir.ast;

public class UnaryOpExpr extends Expression {
	private UnaryOpType operator;
	private Expression expression;
	
	/*
	 * @param: String enumValue should be either '-' or '!'. Expression expr is an expression
	 */
	public UnaryOpExpr(String operator, Expression expr){
		if(operator == "!") {
			this.operator = UnaryOpType.NOT;
		}
		else {
			this.operator = UnaryOpType.MINUS;
		}
		
		this.expression = expr;
	}

	public UnaryOpType getOperator() {
		return operator;
	}

	public void setOperator(UnaryOpType operator) {
		this.operator = operator;
	}

	public Expression getExpression() {
		return expression;
	}

	public void setExpression(Expression expression) {
		this.expression = expression;
	}
}
