package decaf.codegen.flatir;

public class VarAddress extends Name {
	private String id;
	
	public VarAddress(String id) {
		this.setId(id);
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}
	
	@Override
	public String toString() {
		return id;
	}
}
