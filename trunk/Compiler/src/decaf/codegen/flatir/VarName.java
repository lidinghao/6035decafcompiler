package decaf.codegen.flatir;

public class VarName extends Name {
	private String id;
	private int blockId;
	private boolean forString;
	private String stringVal;
	
	public VarName(String id) {
		this.setId(id);
		this.setBlockId(-1);
		this.setForString(false);
		this.setStringVal(null);
	}
	
	public VarName(String id, boolean forString, String val) {
		this.setId(id);
		this.setBlockId(-1);
		this.setForString(forString);
		this.setStringVal(val);
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
	
	@Override
	public int hashCode() {
		return (id + blockId + "").hashCode();
	}
}
