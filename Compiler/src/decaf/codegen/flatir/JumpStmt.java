package decaf.codegen.flatir;

public class JumpStmt {
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
}
