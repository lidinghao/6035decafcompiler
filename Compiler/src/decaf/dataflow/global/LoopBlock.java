package decaf.dataflow.global;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletStmt;

public class LoopBlock {
	private int startBlockID;
	private int endBlockID;
	private HashMap<Name, Integer> nameToInt;
	private HashMap<Name, QuadrupletStmt> nameToQStmt;
	
	public LoopBlock(int startBlockID, int endBlockID){
		this.startBlockID = startBlockID;
		this.endBlockID = endBlockID;
	}

	public void setStartBlockID(int startBlockID) {
		this.startBlockID = startBlockID;
	}

	public int getStartBlockID() {
		return startBlockID;
	}

	public void setEndBlockID(int endBlockID) {
		this.endBlockID = endBlockID;
	}

	public int getEndBlockID() {
		return endBlockID;
	}

	public void setNameToInt(HashMap<Name, Integer> nameToInt) {
		this.nameToInt = nameToInt;
	}

	public HashMap<Name, Integer> getNameToInt() {
		return nameToInt;
	}



	
	
}
