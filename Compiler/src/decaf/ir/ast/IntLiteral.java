package decaf.ir.ast;

public class IntLiteral extends Literal {
	private String stringValue;
	private int value;
	
	/*
	 * Constructor for int literal that takes a string as an input
	 * @param: String integer
	 */
	public IntLiteral(String inp){
		stringValue = inp; // Will convert to int value in semantic check
	}

	@Override
	public Type getType() {
		return Type.INT;
	}

	public String getStringValue() {
		return stringValue;
	}

	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return stringValue;
	}
}
