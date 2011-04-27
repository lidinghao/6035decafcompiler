package decaf.codegen.flatir;

import java.io.PrintStream;

public class LabelStmt extends LIRStatement {
	private String label;
	private boolean isMethodLabel;
	
	public LabelStmt(String label) {
		this.label = label;
		this.isLeader = false;
		this.setDepth();
	}

	public String getLabelString() {
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
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!o.getClass().equals(LabelStmt.class)) return false;
		
		LabelStmt stmt = (LabelStmt) o;
		if (stmt.getLabelString().equals(this.label)) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public Object clone() {
		return new LabelStmt(this.label);
	}
}
