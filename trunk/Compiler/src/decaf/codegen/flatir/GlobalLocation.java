package decaf.codegen.flatir;

public class GlobalLocation extends Location {
	private String name;
	private Location offset; // For arrays
	private boolean isString;
	private String stringVal;
	private Location offsetRegister;
	
	public GlobalLocation(String name) {
		this.setName(name);
		this.setIsString(false);
		this.setStringVal(null);
		this.setOffset(null);
		this.offsetRegister = null;
	}
	
	public GlobalLocation(String name, boolean isString, String stringVal) {
		this.setName(name);
		this.setIsString(isString);
		this.setStringVal(stringVal);
		this.setOffset(null);
		this.offsetRegister = null;
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

	public void setOffset(Location offset) {
		this.offset = offset;
	}

	public Location getOffset() {
		return offset;
	}
	
	@Override
	public String toString() {
		String rtn = "." + name;
		
		if (offset != null) {
			rtn += "[" + offset + "]";
		}
		
		return rtn;
	}

	@Override
	/**
	 * Ensure that you set the offset register before hand!
	 */
	public String getASMRepresentation() {
		if (this.isString) {
			return "." + this.name;
		}
		else if (this.offset == null) {
			return this.name;
		}
		else {
			return this.name + "(, " + this.offsetRegister.getASMRepresentation() + ", 8)";
		}
	}

	public void setOffsetRegister(Location offsetRegister) {
		this.offsetRegister = offsetRegister;
	}

	public Location getOffsetRegister() {
		return offsetRegister;
	}
}
