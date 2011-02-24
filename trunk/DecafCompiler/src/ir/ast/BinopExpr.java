package ir.ast;

public class BinopExpr extends Expression {
	
	private final BinOpType operator; //operator in the expr = expr operator expr
	private final Expression lhs; //left expression
	private final Expression rhs; //right expression
	
	public BinopExpr(Expression l, String operatorString, Expression r){
		if(operatorString == "+")
			operator = BinOpType.PLUS;
		else if (operatorString == "-")
			operator = BinOpType.MINUS;
		else if (operatorString == "*")
			operator = BinOpType.MULTIPLY;
		else if (operatorString == "/")
			operator = BinOpType.DIVIDE;
		else if (operatorString == "%")
			operator = BinOpType.MOD;
		else if (operatorString == "<")
			operator = BinOpType.LE;
		else if (operatorString == "<=")
			operator = BinOpType.LEQ;
		else if (operatorString == ">")
			operator = BinOpType.GE;
		else if (operatorString == ">=")
			operator = BinOpType.GEQ;
		else if (operatorString == "==")
			operator = BinOpType.EQ;
		else if (operatorString == "!=")
			operator = BinOpType.NEQ;
		else if (operatorString == "&&")
			operator = BinOpType.AND;
		else if (operatorString == "||")
			operator = BinOpType.OR;	
		else
			operator = null;
		lhs = l;
		rhs = r;
	}
	
	/*
	 * @return returns left child of the expression
	 */
	public Expression getLeftSubExpression(){
		return lhs;
	}
	
	/*
	 * @return returns right child of the expression
	 */
	public Expression getRightSubExpression(){
		return rhs;
	}
	
	/*
	 * @return binary operator used in the binoopexpr
	 */
	public BinOpType getOperatorType(){
		return operator;
	}

}
