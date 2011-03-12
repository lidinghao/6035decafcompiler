package decaf.codegen.flatir;

public class RegisterAddress extends Address {
	private Register register;
	
	public RegisterAddress(Register register) {
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
		return null;
	}
}
