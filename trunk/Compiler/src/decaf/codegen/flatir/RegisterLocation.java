package decaf.codegen.flatir;

public class RegisterLocation extends Location {
	private Register register;
	
	public RegisterLocation(Register register) {
		this.setRegister(register);
	}
	
	public RegisterLocation(RegisterName register) {
		this.setRegister(register.getRegister());
	}

	public void setRegister(Register register) {
		this.register = register;
	}

	public Register getRegister() {
		return register;
	}
	
	@Override
	public String toString() {
		return "%" + register.toString().toLowerCase();
	}

	@Override
	public String getASMRepresentation() {
		return this.toString();
	}
}
