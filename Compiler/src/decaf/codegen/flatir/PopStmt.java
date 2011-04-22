package decaf.codegen.flatir;

import java.io.PrintStream;

public class PopStmt extends LIRStatement {
	private Name address; // Can be register or memory
	
	public PopStmt(Name address) {
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
		return "pop " + address;
	}

	@Override
	public void generateAssembly(PrintStream out) {
		out.println("\tpop\t" + this.address.getLocation().getASMRepresentation());
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!o.getClass().equals(PopStmt.class)) return false;
		
		PopStmt stmt = (PopStmt) o;
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
