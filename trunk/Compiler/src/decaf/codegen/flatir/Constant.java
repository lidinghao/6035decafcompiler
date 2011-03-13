package decaf.codegen.flatir;

public class Constant extends Name { // Hack for array index
	private String value;
	
	public Constant() {
	}
	
	public Constant(int value) {
		this.setValue(value);
	}
	
	public Constant(String value) {
		this.setValue(value);
	}

	public void setValue(int value) {
		this.value = Integer.toString(value);
	}
	
	public void setValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return value;
	}

	@Override
	public boolean isArray() {
		return false;
	}
}
