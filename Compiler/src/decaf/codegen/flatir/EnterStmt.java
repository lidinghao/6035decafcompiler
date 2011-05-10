package decaf.codegen.flatir;

import java.io.PrintStream;

public class EnterStmt extends LIRStatement {
	private int stackSize;
	
	public EnterStmt(int stackSize) {
		this.setStackSize(stackSize);
		this.setDepth();
	}
	
	public EnterStmt() {
		stackSize = -1;
	}

	public void setStackSize(int stackSize) {
		this.stackSize = stackSize;
	}

	public int getStackSize() {
		return stackSize;
	}
	
	@Override 
	public String toString() {
		String rtn = "enter ";
		if (stackSize == -1) {
			rtn += "<undefined>";
		}
		else {
			rtn += Integer.toString(stackSize);
		}
		
		return rtn;
	}

	@Override
	public void generateAssembly(PrintStream out) {
		out.println("\tenter\t" + "$" + (this.stackSize * 8) + ", $0");		
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!o.getClass().equals(EnterStmt.class)) return false;
		
		EnterStmt stmt = (EnterStmt) o;
		if (stmt.getStackSize() == this.stackSize) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public Object clone() {
		return new EnterStmt(this.stackSize);
	}

	@Override
	public void generateRegAllocAssembly(PrintStream out) {
		this.generateAssembly(out);
	}
}
