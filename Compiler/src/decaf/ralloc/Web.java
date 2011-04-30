package decaf.ralloc;

import java.util.ArrayList;
import java.util.List;

import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.Register;

public class Web {
	private Name variable;
	private List<LIRStatement> definitions;
	private List<LIRStatement> uses;
	private Register register;
	private int firstStmtIndex;
	private int lastStmtIndex;
	
	public Web(Name variable) {
		this.variable = (Name)variable.clone(); // important to clone
		this.definitions = new ArrayList<LIRStatement>();
		this.uses = new ArrayList<LIRStatement>();
		this.register = null;
	}

	public Name getVariable() {
		return variable;
	}

	public void setVariable(Name variable) {
		this.variable = variable;
	}

	public List<LIRStatement> getDefinitions() {
		return definitions;
	}

	public void setDefinitions(List<LIRStatement> definitions) {
		this.definitions = definitions;
	}
	
	public void addDefinition(LIRStatement definition) {
		for (LIRStatement stmt: this.definitions) {
			if (stmt == definition) return;
		}
		this.definitions.add(definition);
	}

	public List<LIRStatement> getUses() {
		return uses;
	}

	public void setUses(List<LIRStatement> uses) {
		this.uses = uses;
	}
	
	public void addUse(LIRStatement use) {
		for (LIRStatement stmt: this.uses) {
			if (stmt == use) return;
		}
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
		String rtn = "VAR: " + this.variable.toString() + " - (" + this.firstStmtIndex + ", " + this.lastStmtIndex + ")\n";
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
	
	public void combineWeb(Web w) {
		List<LIRStatement> usesToAdd = new ArrayList<LIRStatement>();
		List<LIRStatement> defsToAdd = new ArrayList<LIRStatement>();
		
		for (LIRStatement s1: w.getUses()) {
			boolean add = true;
			for (LIRStatement s2: this.uses) {
				if (s1 == s2) add = false;
			}
			
			if (add) usesToAdd.add(s1);
		}
		
		for (LIRStatement s1: w.getDefinitions()) {
			boolean add = true;
			for (LIRStatement s2: this.definitions) {
				if (s1 == s2) add = false;
			}
			
			if (add) defsToAdd.add(s1);
		}
		
		this.uses.addAll(usesToAdd);
		this.definitions.addAll(defsToAdd);
	}
}
