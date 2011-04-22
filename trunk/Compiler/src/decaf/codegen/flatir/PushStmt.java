package decaf.codegen.flatir;

import java.io.PrintStream;

public class PushStmt extends LIRStatement {
	private Name address; // Can be register, memory or immediate
	
	public PushStmt(Name address) {
		this.setAddress(address);
		this.isLeader = false;
	}

	public void setAddress(Name address) {
		this.address = address;
	}

	public Name getAddress() {
		return address;
	}
	
	@Override
	public String toString() {
		return "push " + address;
	}
	
	@Override
	public void generateAssembly(PrintStream out) {
		out.println("\tpush\t" + this.address.getLocation().getASMRepresentation());
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!o.getClass().equals(PushStmt.class)) return false;
		
		PushStmt stmt = (PushStmt) o;
		if (stmt.getAddress().equals(this.address)) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean isUseStatement() {
		return true;
	}
}
