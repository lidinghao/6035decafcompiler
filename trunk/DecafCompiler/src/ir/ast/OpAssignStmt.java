package ir.ast;

public class OpAssignStmt extends Statement {
	private Location location;
	private Expression expression;
	private AssignOpType operator;
	
	public OpAssignStmt(Location l, String op, Expression r) {
		this.location = l;
		this.expression = r;
		
		if (op.equals("==")) {
			operator = AssignOpType.ASSIGN;
		}
		else if (op.equals("+=")) {
			operator = AssignOpType.INCREMENT;
		}
		else if (op.equals("-=")) {
			operator = AssignOpType.DECREMENT;
		}
		else {
			operator = null;
		}
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public Expression getExpression() {
		return expression;
	}

	public void setExpression(Expression expression) {
		this.expression = expression;
	}

	public AssignOpType getOperator() {
		return operator;
	}

	public void setOperator(AssignOpType operator) {
		this.operator = operator;
	}
}
