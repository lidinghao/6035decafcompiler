package decaf.ir.desc;

import decaf.ir.ast.Type;

public class FieldDescriptor extends GenericDescriptor {
	int arrayLength;
	
	public FieldDescriptor(String id, Type type, int arrayLength) {
		this.type = type;
		this.id = id;
		this.arrayLength = arrayLength;
	}
		
	public FieldDescriptor(String id, Type type) {
		this(id, type, -1);
	}

	public int getArrayLength() {
		return arrayLength;
	}

	public void setArrayLength(int arrayLength) {
		this.arrayLength = arrayLength;
	}
}
