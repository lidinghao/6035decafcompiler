package decaf.codegen.flatir;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;

public class DataStmt extends LIRStatement {
	private String label;
	private String stringVal;
	private int arrLength;
	private DataStmtType type;
	
	// Create DataStmt for primitive int or boolean
	public DataStmt(String name) {
		setLabel(name);
		setStringVal(null);
		setArrLength(-1);
		this.type = DataStmtType.VARIABLE;
	}
	
	// Create DataStmt for an array
	public DataStmt(String name, int arrLength) {
		setLabel(name);
		setStringVal(null);
		setArrLength(arrLength);
		this.type = DataStmtType.ARRAY;
	}
	
	// Create DataStmt for a string
	public DataStmt(String name, String val) {
		setLabel(name);
		setStringVal(val);
		setArrLength(-1);
		this.type = DataStmtType.STRING;
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String name) {
		this.label = name;
	}
	
	
	public String getStringVal() {
		return stringVal;
	}
	
	public void setStringVal(String stringVal) {
		this.stringVal = stringVal;
	}
	
	public int getArrLength() {
		return arrLength;
	}
	
	public void setArrLength(int arrLength) {
		this.arrLength = arrLength;
	}
	
	public DataStmtType getType() {
		return type;
	}

	public void setType(DataStmtType type) {
		this.type = type;
	}

	@Override
	public String toString() {
		String rtn = label + ":\t" + this.type.toString().toLowerCase();
		switch(this.type) {
			case VARIABLE:
				break;
			case ARRAY:
				rtn += "[" + this.arrLength + "]";
				break;
			case STRING:
				rtn += "(" + this.stringVal + ")";
				break;
		}
		
		return rtn;
	}

	@Override
	public void generateAssembly(FileWriter out) throws IOException {
		switch(this.type) {
			case VARIABLE:
				out.write("\t.comm\t" + this.label + ", 1, 8"); // Length 1, Size 8 bytes (64 bit)
				break;
			case ARRAY:
				out.write("\t.comm\t" + this.label + ", " + (this.arrLength * 8)); // Length 1, Size 8 bytes (64 bit)
				break;
			case STRING:
				out.write("." + this.label + ":");
				out.write("\t.string\t" + this.stringVal);
				break;
		}
	}
}
