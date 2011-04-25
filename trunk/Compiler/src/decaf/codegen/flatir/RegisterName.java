package decaf.codegen.flatir;

public class RegisterName extends Name {
	private Register register;
	
	public RegisterName(Register register) {
		this.setRegister(register);
	}

	public void setRegister(Register register) {
		this.register = register;
	}

	public Register getRegister() {
		return register;
	}
	
	@Override
	public String toString() {
		return register.toString();
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
		return ("Register#" + toString());
	}
	
	@Override 
	public boolean equals(Object name) {
		if (this == name) return true;
		if (!name.getClass().equals(RegisterName.class)) return false;
		
		RegisterName rName = (RegisterName)name;
		
		return this.hashString().equals(rName.hashString());
	}

	@Override
	public Object clone() {
		RegisterName r = new RegisterName(this.register);
		r.setLocation(this.getLocation());
		return r;
	}
}
