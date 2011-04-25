package decaf.ralloc;

import java.util.HashSet;
import java.util.Set;

import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.Register;

public class Web {
	private Name variable;
	private Set<LIRStatement> definitions;
	private Set<LIRStatement> uses;
	private Register register;
	private int firstStmt;
	private int lastStmt;
	
	public Web(Name variable) {
		this.variable = (Name)variable.clone(); // important to clone
		this.definitions = new HashSet<LIRStatement>();
		this.uses = new HashSet<LIRStatement>();
		this.register = null;
	}

	public Name getVariable() {
		return variable;
	}

	public void setVariable(Name variable) {
		this.variable = variable;
	}

	public Set<LIRStatement> getDefinitions() {
		return definitions;
	}

	public void setDefinitions(Set<LIRStatement> definitions) {
		this.definitions = definitions;
	}
	
	public void addDefinition(LIRStatement definition) {
		this.definitions.add(definition);
	}

	public Set<LIRStatement> getUses() {
		return uses;
	}

	public void setUses(Set<LIRStatement> uses) {
		this.uses = uses;
	}
	
	public void addUse(LIRStatement use) {
		this.uses.add(use);
	}

	public Register getRegister() {
		return register;
	}

	public void setRegister(Register register) {
		this.register = register;
	}

	public void setFirstStmt(int firstStmt) {
		this.firstStmt = firstStmt;
	}

	public int getFirstStmt() {
		return firstStmt;
	}

	public void setLastStmt(int lastStmt) {
		this.lastStmt = lastStmt;
	}

	public int getLastStmt() {
		return lastStmt;
	}
	
	@Override
	public String toString() {
		String rtn = "VAR: " + this.variable.toString() + "\n";
		rtn += "DEF: " + this.definitions.toString() + "\n";
		rtn += "USE: " + this.uses.toString();
		
		return rtn;
	}
}
