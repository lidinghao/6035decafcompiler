package decaf.codegen.flatir;

public class LabelStmt extends LIRStatement {
	private String label;
	
	public LabelStmt(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
}
