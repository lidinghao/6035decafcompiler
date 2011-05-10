package decaf.ralloc;

import java.util.ArrayList;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LoadStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.PopStmt;
import decaf.codegen.flatir.PushStmt;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.Register;
import decaf.codegen.flatir.StoreStmt;

public class Web {
	public static int ID = 0;
	private Name variable;
	private List<Web> interferingWebs;
	private List<LIRStatement> definitions;
	private List<LIRStatement> uses;
	private Register register;
	private int firstStmtIndex;
	private int lastStmtIndex;
	private int id;
	
	public Web(Name variable) {
		this.variable = (Name)variable.clone(); // important to clone
		this.definitions = new ArrayList<LIRStatement>();
		this.uses = new ArrayList<LIRStatement>();
		this.register = null;
		this.interferingWebs = new ArrayList<Web>();
		this.id = ID;
		ID++;
	}

	public List<Web> getInterferingWebs() {
		return interferingWebs;
	}

	public void setInterferingWebs(List<Web> interferingWebs) {
		this.interferingWebs = interferingWebs;
	}
	
	public void addInterferingWeb(Web w) {
		if (w == null) return;
		if (this.equals(w)) return; // Dont add self edges
		
		if (!this.interferingWebs.contains(w)) { // Undirected edge
			this.interferingWebs.add(w);
			w.addInterferingWeb(this);
		}
	}
	
	public void removeInterferingWeb(Web w) {
		if (w == null) return;
		
		this.interferingWebs.remove(w);
		w.interferingWebs.remove(this);
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
		
		processDefinition(definition);
		
		this.definitions.add(definition);
	}

	private void processDefinition(LIRStatement definition) {
		if (definition.getClass().equals(LoadStmt.class)) {
			LoadStmt lStmt = (LoadStmt) definition;
			lStmt.setVariable(this.variable);
		}
		else if (definition.getClass().equals(QuadrupletStmt.class)) {
			QuadrupletStmt qStmt = (QuadrupletStmt) definition;
			qStmt.setDestination(this.variable);
		}
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
		
		processUse(use);
		
		this.uses.add(use);
	}

	private void processUse(LIRStatement use) {
		if (use.getClass().equals(QuadrupletStmt.class)) {
			QuadrupletStmt qStmt = (QuadrupletStmt) use;
			if (this.variable.equals(qStmt.getArg1())) {
				qStmt.setArg1(this.variable);
			}
			if (this.variable.equals(qStmt.getArg2())) {
				qStmt.setArg2(this.variable);
			}
		}
		else if (use.getClass().equals(CmpStmt.class)) {
			CmpStmt cStmt = (CmpStmt) use;
			if (this.variable.equals(cStmt.getArg1())) {
				cStmt.setArg1(this.variable);
			}
			if (this.variable.equals(cStmt.getArg2())) {
				cStmt.setArg2(this.variable);
			}
		}
		else if (use.getClass().equals(PushStmt.class)) {
			PushStmt pStmt = (PushStmt) use;
			if (pStmt.getName().equals(this.variable)) {
				pStmt.setName(this.variable);
			}
		}
		else if (use.getClass().equals(PopStmt.class)) {
			PopStmt pStmt = (PopStmt) use;
			if (pStmt.getName().equals(this.variable)) {
				pStmt.setName(this.variable);
			}
		}
		else if (use.getClass().equals(StoreStmt.class)) {
			StoreStmt sStmt = (StoreStmt) use;
			if (sStmt.getVariable().equals(this.variable)) {
				sStmt.setVariable(this.variable);
			}
			
			Name dest = sStmt.getVariable();
			
			if (dest.isArray()) {
				ArrayName aDest = (ArrayName) dest;
				if (aDest.getIndex().equals(this.variable)) {
					aDest.setIndex(this.variable);
				}
			}
		}
		else if (use.getClass().equals(LoadStmt.class)) {
			Name dest = ((LoadStmt)use).getVariable();
			
			if (dest.isArray()) {
				ArrayName aDest = (ArrayName) dest;
				if (aDest.getIndex().equals(this.variable)) {
					aDest.setIndex(this.variable);
				}
			}
		}
	}

	public Register getRegister() {
		return register;
	}

	public void setRegister(Register register) {
		this.register = register;
		this.variable.setRegister(register);
		
		for (LIRStatement use: this.uses) {
			processUse(use);
		}
		for (LIRStatement def: this.definitions) {
			processDefinition(def);
		}
	}
	
	@Override
	public String toString() {
		String rtn = "VAR: " + this.getIdentifier() + " - (" + this.firstStmtIndex + ", " + this.lastStmtIndex + ")\n";
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
	
	public void combineWeb(Web web) {	
		if (web == null) return;
		if (this == web) return;
		
		for (LIRStatement s1: web.getUses()) {
			boolean add = true;
			for (LIRStatement s2: this.uses) {
				if (s1 == s2) add = false;
			}
			
			if (add) {
				this.addUse(s1);
			}
		}
		
		for (LIRStatement s1: web.getDefinitions()) {
			boolean add = true;
			for (LIRStatement s2: this.definitions) {
				if (s1 == s2) add = false;
			}
			
			if (add) {
				this.addDefinition(s1);
			}
		}
		
		List<Web> remove = new ArrayList<Web>();
		for (Web w: web.getInterferingWebs()) {
			remove.add(w);
		}
		for (Web w: remove) {
			web.removeInterferingWeb(w);
			this.addInterferingWeb(w);
		}
		
		this.interferingWebs.remove(web); // Web can't interfere with self
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}
	
	public String getIdentifier() {
		return this.id + ":" +this.variable.toString();
	}
	
	@Override 
	public int hashCode() {
		return 13*this.id + 17*this.variable.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!o.getClass().equals(Web.class)) return false;
		
		Web web = (Web)o;
		if (this.id != web.id) return false;
		if (!this.variable.equals(web.variable)) return false;
		
		return true;		
	}
}