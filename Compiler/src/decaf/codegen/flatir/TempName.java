package decaf.codegen.flatir;

public class TempName extends Name {
	private int id;
	
	public TempName(int id) {
		this.setId(id);
	}
	
	public TempName() {
		this.setId(-1);
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}
	
	@Override
	public String toString() {
		if (id == -1) {
			return "t";
		}
		
		return "t" + id;
	}
}
