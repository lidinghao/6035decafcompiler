package decaf.codegen.flatir;

public class PushStmt extends LIRStatement {
	private Address address;
	
	public PopStmt(Address address) {
		this.setAddress(address);
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public Address getAddress() {
		return address;
	}
}
