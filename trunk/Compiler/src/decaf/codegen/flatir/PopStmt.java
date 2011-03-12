package decaf.codegen.flatir;

public class PopStmt extends LIRStatement {
	private Name address; // Can be register or memory
	
	public PopStmt(Name address) {
		this.setAddress(address);
	}

	public void setAddress(Name address) {
		this.address = address;
	}

	public Name getAddress() {
		return address;
	}
	
	@Override
	public String toString() {
		return "pop " + address;
	}
}
