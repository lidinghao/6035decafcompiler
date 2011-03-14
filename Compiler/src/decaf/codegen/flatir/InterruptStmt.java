package decaf.codegen.flatir;

import java.io.PrintStream;

public class InterruptStmt extends LIRStatement {
	String interruptId;
	
	public InterruptStmt(String id) {
		this.interruptId = id;
	}
	
	@Override
	public String toString() {
		return "interrupt";
	}

	@Override
	public void generateAssembly(PrintStream out) {
		out.println("\tint\t" + interruptId);		
	}
}
