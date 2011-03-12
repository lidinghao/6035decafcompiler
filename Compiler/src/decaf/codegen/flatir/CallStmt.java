package decaf.codegen.flatir;

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
}
