package decaf.codegen.flatir;

public class ConstantName extends Name { // Hack for array index
	private String value;
	
	public ConstantName(int value) {
		this.setValue(value);
	}
	
	public ConstantName(String value) {
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
	
	@Override
	public int hashCode() {
		return hashString().hashCode(); // Using forbidden chars
	}
	
	public String hashString() {
		return ("Constant#" + value);
	}
	
	@Override 
	public boolean equals(Object name) {
		if (this == name) return true;
		if (!name.getClass().equals(ConstantName.class)) return false;
		
		ConstantName cName = (ConstantName)name;
		
		return this.hashString().equals(cName.hashString());
	}

	@Override
	public Object clone() {
		ConstantName c = new ConstantName(this.value);
		c.setLocation(this.getLocation());
		return c;
	}
	
	@Override
	public String getRegister() {
		return "$" + this.value;
	}
}
