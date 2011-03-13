package decaf.codegen.flatir;

import java.io.PrintStream;

public class LeaveStmt extends LIRStatement {
	public LeaveStmt() { };
	
	@Override
	public String toString() {
		String rtn = "leave, rtn";
		
		return rtn;
	}

	@Override
	public void generateAssembly(PrintStream out) {
		out.println("\tleave");
		out.println("\tret");
	}
}
