package decaf.codegen.flatir;

public class GlobalLocation extends Location {
	private String name;
	private Name offset; // For arrays
	private boolean isString;
	private String stringVal;
	
	public GlobalLocation(String name) {
		this.setName(name);
		this.setIsString(false);
		this.setStringVal(null);
		this.setOffset(null);
	}
	
	public GlobalLocation(String name, boolean isString, String stringVal) {
		this.setName(name);
		this.setIsString(isString);
		this.setStringVal(stringVal);
		this.setOffset(null);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	public boolean isString() {
		return isString;
	}

	public void setIsString(boolean isString) {
		this.isString = isString;
	}

	public String getStringVal() {
		return stringVal;
	}

	public void setStringVal(String stringVal) {
		this.stringVal = stringVal;
	}

	public void setOffset(Name offset) {
		this.offset = offset;
	}

	public Name getOffset() {
		return offset;
	}
	
	@Override
	public String toString() {
		return "." + name;
	}
}
