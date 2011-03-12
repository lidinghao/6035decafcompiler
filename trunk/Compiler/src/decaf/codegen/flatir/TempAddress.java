package decaf.codegen.flatir;

public class TempAddress extends Address {
	private int id;
	
	public TempAddress(int id) {
		this.setId(id);
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}
	
	@Override
	public String toString() {
		return "t" + id;
	}
}
