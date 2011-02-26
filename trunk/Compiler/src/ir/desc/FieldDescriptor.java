package ir.desc;

import ir.ast.Type;

abstract class FieldDescriptor extends Descriptor {
	protected String name;
	protected int objectOffset;
	public abstract Type getType();

	public void setObjectOffset(int objectOffset) {
		this.objectOffset = objectOffset;
	}

	public int getObjectOffset() {
		return objectOffset;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
