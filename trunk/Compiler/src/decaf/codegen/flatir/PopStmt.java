package decaf.codegen.flatir;

import java.io.PrintStream;

import decaf.ralloc.ASMGenerator;

public class PopStmt extends LIRStatement {
	private Name name; // Can be register or memory
	
	public PopStmt(Name address) {
		this.setName(address);
		this.isLeader = false;
		this.setDepth();
	}

	public void setName(Name address) {
		this.name = address;
	}

	public Name getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return "pop " + name;
	}

	@Override
	public void generateAssembly(PrintStream out) {
		out.println("\tpop\t" + this.name.getLocation().getASMRepresentation());
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
		if (stmt.getName().equals(this.name)) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean isUseStatement() {
		return true;
	}
	
	@Override
	public Object clone() {
		return new PopStmt(this.name);
	}

	@Override
	public void generateRegAllocAssembly(PrintStream out) {
		out.println("\t; " + this.toString());
		
		out.println("\tpop\t" + ASMGenerator.getLocationForName(this.name, out, false));
	}
}
