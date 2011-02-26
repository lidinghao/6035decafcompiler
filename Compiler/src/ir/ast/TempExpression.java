package ir.ast;

public class TempExpression extends AST {	
	private BinOpType operator; 
	private Expression rOperand;

	public TempExpression(BinOpType op, Expression rOper) {
		operator = op;
		rOperand = rOper;
	}

	public BinOpType getOperator() {
		return operator;
	}

	public void setOperator(BinOpType operator) {
		this.operator = operator;
	}
	
	public Expression getRightOperand() {
		return rOperand;
	}

	public void setRightOperand(Expression rOperand) {
		this.rOperand = rOperand;
	}
}
