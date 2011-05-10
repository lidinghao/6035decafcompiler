package decaf.codegen.flatir;

public class VarName extends Name {
	private String id;
	private int blockId; // -1 for global field, -2 for parameter
	private boolean isString;
	private String stringValue; // non-null only for string var names
	private boolean isStackParam;
	
	public boolean isStackParam() {
		return isStackParam;
	}

	public void setStackParam(boolean isStackParam) {
		this.isStackParam = isStackParam;
	}

	public VarName(String id) {
		this.isStackParam = false;
		this.setId(id);
		this.setBlockId(-1);
		this.setIsString(false);
		this.setStringValue(null);
	}
	
	public VarName(String id, boolean forString, String val) {
		this.isStackParam = false;
		this.setId(id);
		this.setBlockId(-1);
		this.setIsString(forString);
		this.setStringValue(val);
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}
	
	@Override
	public String toString() {
		return id;
	}

	public void setBlockId(int blockId) {
		this.blockId = blockId;
	}

	public int getBlockId() {
		return blockId;
	}

	public boolean isString() {
		return isString;
	}

	public void setIsString(boolean forString) {
		this.isString = forString;
	}

	public String getStringValue() {
		return stringValue;
	}

	public void setStringValue(String stringVal) {
		this.stringValue = stringVal;
	}
	
	@Override
	public int hashCode() {
		return hashString().hashCode();
	}
	
	public String hashString() {
		return ("Variable#" + id + "%" + blockId);
	}
	
	@Override 
	public boolean equals(Object name) {
		if (name == null) return false;
		if (this == name) return true;
		if (!name.getClass().equals(VarName.class)) return false;
		
		VarName vName = (VarName)name;
		
		return this.hashString().equals(vName.hashString());
	}

	@Override
	public boolean isArray() {
		return false;
	}

	@Override
	public Object clone() {
		VarName v = new VarName(this.id);
		v.isString = this.isString;
		v.stringValue = this.stringValue;
		v.blockId = this.blockId;
		v.setLocation(this.getLocation());
		return v;
	}
	
	@Override
	public boolean isGlobal() {
		return (this.blockId == -1);
	}
	
	@Override
	public boolean needsLoad() {
		if (this.blockId == -1) {
			return true;
		}
		else if (this.blockId == -2) {
			
		}
		
		return false;
		
	}
}
