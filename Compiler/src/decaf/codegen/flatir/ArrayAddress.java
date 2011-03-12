package decaf.codegen.flatir;

public class ArrayAddress extends Address {
	private Address index;
	private String id;
	
	public ArrayAddress(int index, String id) {
		this.setIndex(new Constant(index));
		this.setId(id);
	}
	
	public ArrayAddress(Address index, String id) {
		this.setIndex(index);
		this.setId(id);
	}

	public void setIndex(Address index) {
		this.index = index;
	}

	public Address getIndex() {
		return index;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}
	
	@Override
	public String toString() {
		return id + "[" + index + "]";
	}
}
