package decaf.codegen.flatir;

import java.io.PrintStream;

public class LabelStmt extends LIRStatement {
	private String label;
	private boolean isMethodLabel;
	
	public LabelStmt(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
	
	@Override
	public String toString() {
		return label + ":";
	}

	public void setMethodLabel(boolean isMethodLabel) {
		this.isMethodLabel = isMethodLabel;
	}

	public boolean isMethodLabel() {
		return isMethodLabel;
	}

	@Override
	public void generateAssembly(PrintStream out) {
		if (this.isMethodLabel) {
			out.println(this.label + ":");
		}
		else {
			out.println("." + this.label + ":");
		}
	}
}