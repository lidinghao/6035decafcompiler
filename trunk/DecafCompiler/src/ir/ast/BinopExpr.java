package ir.ast;

public class BinopExpr extends Expression {
	private BinOpType operator; //operator in the expr = expr operator expr
	private Expression lOperand; //left expression
	private Expression rOperand; //right expression
	
	public BinopExpr(Expression l, String opToken, Expression r){
		if(opToken.equals("+"))
			operator = BinOpType.PLUS;
		else if (opToken.equals("-"))
			operator = BinOpType.MINUS;
		else if (opToken.equals("*"))
			operator = BinOpType.MULTIPLY;
		else if (opToken.equals("/"))
			operator = BinOpType.DIVIDE;
		else if (opToken.equals("%"))
			operator = BinOpType.MOD;
		else if (opToken.equals("<"))
			operator = BinOpType.LE;
		else if (opToken.equals("<="))
			operator = BinOpType.LEQ;
		else if (opToken.equals(">"))
			operator = BinOpType.GE;
		else if (opToken.equals(">="))
			operator = BinOpType.GEQ;
		else if (opToken.equals("=="))
			operator = BinOpType.EQ;
		else if (opToken.equals("!="))
			operator = BinOpType.NEQ;
		else if (opToken.equals("&&"))
			operator = BinOpType.AND;
		else if (opToken.equals("||"))
			operator = BinOpType.OR;	
		else
			operator = null;
		
		lOperand = l;
		rOperand = r;
	}
	
	public BinOpType getOperator() {
		return operator;
	}

	public void setOperator(BinOpType operator) {
		this.operator = operator;
	}

	public Expression getLeftOperand() {
		return lOperand;
	}

	public void setLeftOperand(Expression lOperand) {
		this.lOperand = lOperand;
	}

	public Expression getRightOperand() {
		return rOperand;
	}

	public void setRightOperand(Expression rOperand) {
		this.rOperand = rOperand;
	}
}
