package ir.desc;

import ir.ast.Type;

public class BooleanDescriptor extends FieldDescriptor {
	public BooleanDescriptor(String name, int objectOffset) {
		this.setName(name);
		this.setObjectOffset(objectOffset);
	}

	@Override
	public Type getType() {
		return Type.BOOLEAN;
	}
	
	@Override
	public String toString() {
		String str = "bool" + " " + this.name;
		return str;
	}

}
