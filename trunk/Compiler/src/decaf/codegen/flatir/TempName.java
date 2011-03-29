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
		return "$t" + id;
	}
	
	@Override
	public int hashCode() {
		return hashString().hashCode(); // Using forbidden chars
	}
	
	public String hashString() {
		return ("Temporary#" + id);
	}
	
	@Override 
	public boolean equals(Object name) {
		if (this == name) return true;
		if (!name.getClass().equals(TempName.class)) return false;
		
		TempName vName = (TempName)name;
		
		return this.hashString().equals(vName.hashString());
	}

	@Override
	public boolean isArray() {
		return false;
	}
}
