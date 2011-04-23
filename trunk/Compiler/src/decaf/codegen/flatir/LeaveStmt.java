package decaf.codegen.flatir;

import java.io.PrintStream;

public class LeaveStmt extends LIRStatement {
	public LeaveStmt() { 
		this.isLeader = false;
	};
	
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
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!o.getClass().equals(LeaveStmt.class)) return false;
		
		return true;
	}
	
	@Override
	public Object clone() {
		return new LeaveStmt();
	}
}
