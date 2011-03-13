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
}
