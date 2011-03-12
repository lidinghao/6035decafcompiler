package decaf.codegen.flatir;

public class StackLocation {
	private int offset;
	
	public StackLocation(int offset) {
		this.setOffset(offset);
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public int getOffset() {
		return offset;
	}

}
