package decaf.codegen.flatir;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;

public class LeaveStmt extends LIRStatement {
	public LeaveStmt() { };
	
	@Override
	public String toString() {
		String rtn = "leave, rtn";
		
		return rtn;
	}

	@Override
	public void generateAssembly(FileWriter out) throws IOException {
		out.write("\tleave");
		out.write("\tret");
	}
}
