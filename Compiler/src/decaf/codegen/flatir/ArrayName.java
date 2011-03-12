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
}
