package decaf.codegen.flatir;

import java.io.PrintStream;

public class InterruptStmt extends LIRStatement {
	private String interruptId;
	
	public InterruptStmt(String id) {
		this.interruptId = id;
		this.isLeader = false;
	}
	
	public String getInterruptId() {
		return this.interruptId;
	}
	
	@Override
	public String toString() {
		return "interrupt " + interruptId;
	}

	@Override
	public void generateAssembly(PrintStream out) {
		out.println("\tint\t" + interruptId);		
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!o.getClass().equals(InterruptStmt.class)) return false;
		
		InterruptStmt stmt = (InterruptStmt) o;
		if (stmt.interruptId == this.interruptId) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public Object clone() {
		return new InterruptStmt(this.interruptId);
	}
}
