package decaf.codegen.flatir;

public class DataStmt extends LIRStatement {
	private String name;
	private boolean isString;
	private String stringVal;
	private boolean isArray;
	private int arrLength;
	
	// Create DataStmt for primitive int or boolean
	public DataStmt(String name) {
		setName(name);
		setString(false);
		setStringVal(null);
		setArray(false);
		setArrLength(-1);
	}
	
	// Create DataStmt for an array
	public DataStmt(String name, int arrLength) {
		setName(name);
		setString(false);
		setStringVal(null);
		setArray(true);
		setArrLength(arrLength);
	}
	
	// Create DataStmt for a string
	public DataStmt(String name, String val) {
		setName(name);
		setString(true);
		setStringVal(val);
		setArray(false);
		setArrLength(-1);
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public boolean isString() {
		return isString;
	}
	
	public void setString(boolean isString) {
		this.isString = isString;
	}
	
	public String getStringVal() {
		return stringVal;
	}
	
	public void setStringVal(String stringVal) {
		this.stringVal = stringVal;
	}
	
	public boolean isArray() {
		return isArray;
	}
	
	public void setArray(boolean isArray) {
		this.isArray = isArray;
	}
	
	public int getArrLength() {
		return arrLength;
	}
	
	public void setArrLength(int arrLength) {
		this.arrLength = arrLength;
	}
	
	@Override
	public String toString() {
		String ret = "data stmt: " + name;
		if (isString) {
			ret += "[String] " + stringVal;
		}
		if (isArray) {
			ret += "[Array] size: " + Integer.toString(arrLength);
		}
		return ret;
	}
}
