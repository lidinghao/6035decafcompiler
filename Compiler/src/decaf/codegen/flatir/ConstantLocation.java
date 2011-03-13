package decaf.codegen.flatir;

public class ConstantLocation extends Location {
	int value;
	
	public ConstantLocation(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	public void setValue(int value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return "$" + this.value;
	}
}
