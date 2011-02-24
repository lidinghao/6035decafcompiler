package ir.ast;

public class BinopExpr extends Expression {
	
	private final BinOpType operator; //operator in the expr = expr operator expr
	private final Expression lhs; //left expression
	private final Expression rhs; //right expression
	
	public BinopExpr(Expression l, BinOpType o, Expression r){
		operator = o;
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
