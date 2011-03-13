package decaf.codegen.flatir;

import java.io.PrintStream;

public class CallStmt extends LIRStatement {
	private String methodLabel;
	
	public CallStmt(String methodLabel) {
		this.setMethodLabel(methodLabel);
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
	public void generateAssembly(PrintStream out) {
		if (methodLabel.charAt(0) == '"')
			out.println("\tcall\t" + methodLabel.substring(1,methodLabel.length()-1));		
		else
			out.println("\tcall\t" + methodLabel);
	}
}
