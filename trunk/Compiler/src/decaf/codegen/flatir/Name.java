package decaf.codegen.flatir;

public abstract class Name {
	private int stackOffset;

	public void setStackOffset(int stackOffset) {
		this.stackOffset = stackOffset;
	}

	public int getStackOffset() {
		return stackOffset;
	}
}
