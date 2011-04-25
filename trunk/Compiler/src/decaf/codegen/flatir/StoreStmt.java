package decaf.codegen.flatir;

import java.io.PrintStream;

public class StoreStmt extends LIRStatement {
	private Name variable;
	private Register register;

	public StoreStmt(Register register, Name variable) {
		this.setVariable(variable);
		this.setRegister(register);
	}

	public void setVariable(Name variable) {
		this.variable = variable;
	}

	public Name getVariable() {
		return variable;
	}

	public void setRegister(Register register) {
		this.register = register;
	}

	public Register getRegister() {
		return register;
	}
	
	@Override
	public String toString() {
		return this.variable + " = " + this.register;
	}
	
	@Override
	public int hashCode() {
		return this.register.toString().hashCode() + 17 * this.variable.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!o.getClass().equals(LoadStmt.class)) return false;
		
		StoreStmt stmt = (StoreStmt) o;
		if (this.register != stmt.register) {
			return false;
		}
		
		if (!this.variable.equals(stmt.variable)) {
			return false;
		}
		
		return true;
	}
	
	@Override
	public void generateAssembly(PrintStream out) {
		if (variable.isArray()) {
			ArrayName arrayName = (ArrayName) variable;
			String indexLocation = arrayName.getIndex().getLocation().getASMRepresentation();
			out.println("\tmov\t" + indexLocation + ", " + Register.R10);
			arrayName.setOffsetRegister(Register.R10);
		}
		
		out.println("\tmov\t" + register + ", " + variable.getLocation().getASMRepresentation());
	}

	@Override
	public Object clone() {
		return new StoreStmt(this.register, this.variable);
	}
}
