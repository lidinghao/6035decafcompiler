package decaf.codegen.flatir;

public class JumpStmt extends LIRStatement {
	private JumpCondOp condition;
	private LabelStmt label;
	
	public JumpStmt(JumpCondOp condition, LabelStmt label) {
		this.setCondition(condition);
		this.setLabel(label);
	}

	public void setLabel(LabelStmt label) {
		this.label = label;
	}

	public LabelStmt getLabel() {
		return label;
	}

	public void setCondition(JumpCondOp condition) {
		this.condition = condition;
	}

	public JumpCondOp getCondition() {
		return condition;
	}
	
	@Override
	public String toString() {
		String rtn = "jump (";
		
		switch(this.condition) {
			case EQ:
				rtn += "=";
				break;
			case NEQ:
				rtn += "!=";
				break;
			case ZERO:
				rtn += "0";
				break;
			case GT:
				rtn += ">";
				break;
			case GTE:
				rtn += ">=";
				break;
			case LT:
				rtn += "<";
				break;
			case LTE:
				rtn += "<=";
				break;
		}
		
		return rtn + ") " + "'" + label.getLabel() + "'";
	}
}
