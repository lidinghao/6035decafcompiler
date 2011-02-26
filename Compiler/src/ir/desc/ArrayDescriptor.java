package ir.desc;

import ir.ast.Type;

public class ArrayDescriptor extends FieldDescriptor {
	private Type type;
	private int size;
	
	public ArrayDescriptor(String name, int objectOffset, Type type, int size) {
		this.setName(name);
		this.setObjectOffset(objectOffset);
		this.setType(type);
		this.setSize(size);
	}
	
	public void setType(Type type) {
		this.type = type;
	}

	@Override
	public Type getType() {
		return this.type;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getSize() {
		return size;
	}
	@Override
	public String toString() {
		String str = this.type.toString() + " " + this.name;
		return str;
	}


}
