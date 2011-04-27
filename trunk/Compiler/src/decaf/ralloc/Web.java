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
	private int firstStmtIndex;
	private int lastStmtIndex;
	
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
	
	@Override
	public String toString() {
		String rtn = "VAR: " + this.variable.toString() + " (" + this.firstStmtIndex + ", " + this.lastStmtIndex + ")\n";
		rtn += "DEF: " + this.definitions.toString() + "\n";
		rtn += "USE: " + this.uses.toString();
		
		return rtn;
	}

	public void setLastStmtIndex(int lastStmtIndex) {
		this.lastStmtIndex = lastStmtIndex;
	}

	public int getLastStmtIndex() {
		return lastStmtIndex;
	}

	public void setFirstStmtIndex(int firstStmtIndex) {
		this.firstStmtIndex = firstStmtIndex;
	}

	public int getFirstStmtIndex() {
		return firstStmtIndex;
	}
	
	/**
	 * Param that are loaded off the stack or global vars. Require no explicit definition before use!
	 * @return
	 */
	public boolean loadExplicitly() {
		return this.definitions.isEmpty();
	}
}
