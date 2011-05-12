package decaf.codegen.flatir;

import java.io.PrintStream;

import decaf.ralloc.ASMGenerator;

public class PushStmt extends LIRStatement {
	private Name name; // Can be register, memory or immediate
	
	public PushStmt(Name address) {
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
		return "push " + name;
	}
	
	@Override
	public void generateAssembly(PrintStream out) {
		out.println("\tpush\t" + this.name.getLocation().getASMRepresentation());
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
		return new PushStmt(this.name);
	}

	@Override
	public void generateRegAllocAssembly(PrintStream out) {
		out.println("\t; " + this.toString());
		out.println("\tpush\t" + ASMGenerator.getLocationForName(this.name, out, false));
	}
}
