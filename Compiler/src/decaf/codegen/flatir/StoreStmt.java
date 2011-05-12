package decaf.codegen.flatir;

import java.io.PrintStream;

import decaf.ralloc.ASMGenerator;

public class StoreStmt extends LIRStatement {
	private Name variable;
	private int myId;

	public StoreStmt(Name variable) {
		this.setVariable(variable);
		this.myId = -1;
	}

	public void setVariable(Name variable) {
		this.variable = variable;
	}

	public Name getVariable() {
		return variable;
	}

	public Register getRegister() {
		return this.variable.getMyRegister();
	}
	
	@Override
	public String toString() {
		return "st " + this.variable;// + " = " + this.register;
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode() + 17 * this.variable.hashCode() + 13 * this.myId;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!o.getClass().equals(StoreStmt.class)) return false;
		
		StoreStmt stmt = (StoreStmt) o;
		if (!this.variable.equals(stmt.variable)) {
			return false;
		}
		
		return true;
	}
	
	@Override
	public void generateAssembly(PrintStream out) {
//		if (variable.isArray()) {
//			ArrayName arrayName = (ArrayName) variable;
//			String indexLocation = arrayName.getIndex().getLocation().getASMRepresentation();
//			out.println("\tmov\t" + indexLocation + ", " + Register.R10);
//			arrayName.setOffsetRegister(Register.R10);
//		}
//		
//		out.println("\tmov\t" + this.variable.getRegister() + ", " + variable.getLocation().getASMRepresentation());
	}

	@Override
	public Object clone() {
		return new StoreStmt(this.variable);
	}

	public void setMyId() {
		this.myId = QuadrupletStmt.getID();
		QuadrupletStmt.setID(this.myId+1);
	}

	public int getMyId() {
		return myId;
	}

	@Override
	public void generateRegAllocAssembly(PrintStream out) {
		out.println("\t// " + this.toString());
		
		if (this.variable.getMyRegister() == null) return; // // spilled values have no notion of store
		
		out.println("\tmov\t" + this.variable.getRegister() + ", " + ASMGenerator.getLocationForName(this.variable, out, true));
	}
}
