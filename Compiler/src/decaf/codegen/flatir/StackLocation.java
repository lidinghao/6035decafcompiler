package decaf.codegen.flatir;

public class StackLocation extends Location {
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
	
	@Override
	public String toString() {
		return "stack(" + offset + ")";
	}

	@Override
	public String getASMRepresentation() {
		return "-" + (offset * 8) + "(%rsp)";
	}
}
