package decaf.codegen.flatir;

public class Constant extends Name { // Hack for array index
	private int value;
	
	public Constant(int value) {
		this.setValue(value);
	}

	public void setValue(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}
