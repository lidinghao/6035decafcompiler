package decaf.codegen.flatir;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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
	public void generateAssembly(FileWriter out) throws IOException {
		if (methodLabel.charAt(0) == '"')
			out.write("\tcall\t" + methodLabel.substring(1,methodLabel.length()-1));		
		else
			out.write("\tcall\t" + methodLabel);
	}
}
