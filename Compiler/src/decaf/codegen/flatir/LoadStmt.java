package decaf.codegen.flatir;

import java.io.PrintStream;
import java.util.List;

public class LoadStmt extends LIRStatement {
	private Name variable;
	private int myId;
	private List<LIRStatement> boundCheck;
	private Register explicitLoad;

	public LoadStmt(Name variable) {
		this.setVariable(variable);
		this.myId = -1;
		this.setBoundCheck(null);
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
		return "ld " + this.variable; //.getRegister() + " = " + this.variable;
	}
	
	@Override
	public int hashCode() {
		return  this.toString().hashCode() + 17 * this.variable.hashCode() + 13 * this.myId;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!o.getClass().equals(LoadStmt.class)) return false;
		
		LoadStmt stmt = (LoadStmt) o;
	
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
//			out.println("\tmov\t" + indexLocation + ", " + this.variable.getRegister());
//			arrayName.setOffsetRegister(this.variable.getRegister());
//		}
//		
//		out.println("\tmov\t" + variable.getLocation().getASMRepresentation() + ", " + this.variable.getRegister());
	}

	@Override
	public Object clone() {
		return new LoadStmt(this.variable);
	}
	
	public int getMyId() {
		return myId;
	}

	public void setMyId() {
		this.myId = QuadrupletStmt.getID();
		QuadrupletStmt.setID(this.myId+1);
	}

	public void setBoundCheck(List<LIRStatement> boundCheck) {
		this.boundCheck = boundCheck;
	}

	public List<LIRStatement> getBoundCheck() {
		return boundCheck;
	}

	@Override
	public void generateRegAllocAssembly(PrintStream out) {
		out.println("\t;" + this.toString());
		String from = "";
		
		if (this.variable.isGlobal()) {
			if (this.variable.isArray()) {
				ArrayName arr = (ArrayName) this.variable;
				
				from = arr.getId() + "(, " + arr.getIndex().getRegister() + ", 8)";
			}
			else {
				VarName var = (VarName) this.variable;
				if (var.isString()) {
					from = "$." + var.getId();
				}
				else {
					from = var.getId();
				}
			}
		}
		else {
			from = this.variable.getLocation().getASMRepresentation();
		}
		
		String to = "";
		
		if (this.explicitLoad != null) {
			to = this.explicitLoad.toString();
		}
		else {
			to = this.variable.getRegister();
		}
		
		out.println("\tmov\t" + from + ", " + to);
	}

	public void setExplicitLoadRegister(Register explicitLoad) {
		this.explicitLoad = explicitLoad;
	}

	public Register getExplicitLoad() {
		return explicitLoad;
	}
}
