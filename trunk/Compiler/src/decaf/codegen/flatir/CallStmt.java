package decaf.codegen.flatir;

import java.io.PrintStream;

public class CallStmt extends LIRStatement {
	private String methodLabel;
	
	public CallStmt(String methodLabel) {
		this.setMethodLabel(methodLabel);
		this.isLeader = false;
		this.setDepth();
	}

	public void setMethodLabel(String methodLabel) {
		this.methodLabel = methodLabel;
	}

	public String getMethodLabel() {
		return methodLabel;
	}
	
	@Override
	public String toString() {
		return "call " + methodLabel;
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!o.getClass().equals(CallStmt.class)) return false;
		
		CallStmt stmt = (CallStmt) o;
		if (stmt.getMethodLabel().equals(this.methodLabel)) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public Object clone() {
		return new CallStmt(this.methodLabel);
	}

	@Override
	public void generateAssembly(PrintStream out) {
		if (methodLabel.charAt(0) == '"')
			out.println("\tcall\t" + methodLabel.substring(1,methodLabel.length()-1));		
		else
			out.println("\tcall\t" + methodLabel);
	}
}
