package decaf.codegen.flatir;

public class ArrayName extends Name {
	private Name index;
	private String id;
	
	public ArrayName(String id, Name index) {
		this.setIndex(index);
		this.setId(id);
	}

	public void setIndex(Name index) {
		this.index = index;
	}

	public Name getIndex() {
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

	@Override
	public boolean isArray() {
		return true;
	}
	
	public void setOffsetRegister(Register r) {
		GlobalLocation loc = (GlobalLocation) this.getLocation();
		loc.setOffsetRegister(new RegisterLocation(r));
	}
	
	@Override
	public int hashCode() {
		return hashString().hashCode() + 13 * this.index.hashCode(); // Using forbidden chars
	}
	
	public String hashString() {
		return ("Array#" + id + "[" + index + "]");
	}
	
	@Override 
	public boolean equals(Object name) {
		if (name == null) return false;
		if (this == name) return true;
		if (!name.getClass().equals(ArrayName.class)) return false;
		
		ArrayName aName = (ArrayName)name;
		
		if (!this.id.equals(aName.id)) return false;
		if (!this.index.equals(aName.index)) return false;
		
		return true;
	}
	
	@Override
	public Object clone() {
		ArrayName a = new ArrayName(this.id, (Name)this.index.clone());
		a.setLocation(this.getLocation());
		return a;
	}
	
	@Override
	public boolean isGlobal() {
		return true;
	}
	
	@Override
	public boolean needsLoad() {
		return true;
	}
}
