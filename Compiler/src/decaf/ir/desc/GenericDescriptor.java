package decaf.ir.desc;

import decaf.ir.ast.Type;

public abstract class GenericDescriptor {
	String id;
	Type type;	
	boolean isArray;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public boolean isArray() {
		return isArray;
	}

	public void setIsArray(boolean isArray) {
		this.isArray = isArray;
	}
}
