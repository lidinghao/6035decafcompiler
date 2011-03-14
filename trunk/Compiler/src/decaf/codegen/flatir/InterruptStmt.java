package decaf.codegen.flatir;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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
	public void generateAssembly(FileWriter out) throws IOException {
		out.write("\tint\t" + interruptId);		
	}
}
