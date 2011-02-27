package decaf.ir.desc;

import decaf.ir.ast.Type;

public class FieldDescriptor extends GenericDescriptor {
	int arrayLength;
	
	public FieldDescriptor(String id, Type type, boolean isArray, int arrayLength) {
		this.type = type;
		this.id = id;
		this.isArray = isArray;
		this.arrayLength = arrayLength;
	}
		
	public FieldDescriptor(String id, Type type, boolean isArray) {
		this(id, type, true, -1);
	}
	
	public FieldDescriptor(String id, Type type) {
		this(id, type, false, -1);
	}

	public int getArrayLength() {
		return arrayLength;
	}

	public void setArrayLength(int arrayLength) {
		this.arrayLength = arrayLength;
	}
}
