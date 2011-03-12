package decaf.codegen.flatir;

public class PushStmt extends LIRStatement {
	private Name address;
	
	public PopStmt(Name address) {
		this.setAddress(address);
	}

	public void setAddress(Name address) {
		this.address = address;
	}

	public Name getAddress() {
		return address;
	}
}
