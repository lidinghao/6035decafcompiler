package decaf.codegen.flatir;

public class LeaveStmt extends LIRStatement {
	public LeaveStmt() { };
	
	@Override
	public String toString() {
		String rtn = "leave\n";
		rtn += "rtn";
		
		return rtn;
	}
}
