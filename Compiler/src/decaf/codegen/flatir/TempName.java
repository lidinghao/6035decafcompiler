package decaf.codegen.flatir;

public class TempName extends Name {
	private int id;
	
	public TempName(int id) {
		this.setId(id);
	}
	
	public TempName() {
		
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
