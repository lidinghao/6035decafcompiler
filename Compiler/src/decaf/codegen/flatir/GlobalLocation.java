package decaf.codegen.flatir;

public class GlobalLocation extends Location {
	private String name;
	private boolean forString;
	private String stringVal;
	
	public GlobalLocation(String name) {
		this.setName(name);
		this.setForString(false);
		this.setStringVal(null);
	}
	
	public GlobalLocation(String name, boolean isString, String stringVal) {
		this.setName(name);
		this.setForString(isString);
		this.setStringVal(stringVal);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	public boolean isForString() {
		return forString;
	}

	public void setForString(boolean forString) {
		this.forString = forString;
	}

	public String getStringVal() {
		return stringVal;
	}

	public void setStringVal(String stringVal) {
		this.stringVal = stringVal;
	}
}
