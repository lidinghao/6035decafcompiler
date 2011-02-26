package ir.desc;

import ir.ast.Type;

public class LocalDescriptor extends Descriptor {
	private String name;
	private Type type;
	private int stackOffset;
	
	public LocalDescriptor(String name, Type type, int stackOffset) {
		this.setName(name);
		this.setType(type);
		this.setStackOffset(stackOffset);
	}

	public void setStackOffset(int stackOffset) {
		this.stackOffset = stackOffset;
	}

	public int getStackOffset() {
		return stackOffset;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		String str = this.type.toString() + " "+ this.name;
		return str;
	}
	
	


}
