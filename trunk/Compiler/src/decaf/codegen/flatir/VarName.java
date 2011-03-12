package decaf.codegen.flatir;

public class VarName extends Name {
	private String id;
	private int blockId;
	
	public VarName(String id) {
		this.setId(id);
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
	
	@Override
	public int hashCode() {
		return (id + blockId + "").hashCode();
	}
}
