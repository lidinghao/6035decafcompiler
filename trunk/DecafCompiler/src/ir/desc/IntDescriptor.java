package ir.desc;

import ir.ast.Type;

public class IntDescriptor extends FieldDescriptor {
	public IntDescriptor(String name, int objectOffset) {
		this.setName(name);
		this.setObjectOffset(objectOffset);
	}

	@Override
	public Type getType() {
		return Type.INT;
	}
	
	@Override
	public String toString() {
		String str = "int" + " " + this.name;
		return str;
	}
}
