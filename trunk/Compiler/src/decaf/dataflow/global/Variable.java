package decaf.dataflow.global;

import decaf.codegen.flatir.Name;

public class Variable {
	private Name var;
	private int myId;
	private static int ID = 0;

	public Variable(Name a1) {
		var = a1;
		myId = ID;
		ID++;
	}
	
	public static int getID() {
		return ID;
	}

	public static void setID(int iD) {
		ID = iD;
	}
	
	public int getMyId() {
		return myId;
	}

	public void setMyId(int myId) {
		this.myId = myId;
		ID++;
	}
	
	public Name getVar() {
		return var;
	}
	
	public void setVar(Name arg1) {
		this.var = arg1;
	}
	
	@Override
	public String toString() {
		return var.toString();
	}
}
