package decaf.codegen.flatir;

import java.io.PrintStream;

public class LoadStmt extends LIRStatement {
	private Name variable;
	private Register register;

	public LoadStmt(Name variable, Register register) {
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
		return this.register + " = " + this.variable;
	}
	
	@Override
	public int hashCode() {
		return this.register.toString().hashCode() + 17 * this.variable.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!o.getClass().equals(LoadStmt.class)) return false;
		
		LoadStmt stmt = (LoadStmt) o;
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
			out.println("\tmov\t" + indexLocation + ", " + register);
			arrayName.setOffsetRegister(register);
		}
		
		out.println("\tmov\t" + variable.getLocation().getASMRepresentation() + ", " + register);
	}

	@Override
	public Object clone() {
		return new LoadStmt(this.variable, this.register);
	}
}
