package decaf.codegen.flatir;

public class ConstantLocation extends Location {
	String value;
	
	public ConstantLocation(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return "$" + this.value;
	}

	@Override
	public String getASMRepresentation() {
		return this.toString();
	}
}
