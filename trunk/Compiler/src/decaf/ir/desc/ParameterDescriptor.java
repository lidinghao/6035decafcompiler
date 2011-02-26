package decaf.ir.desc;

import decaf.ir.ast.Type;

public class ParameterDescriptor extends Descriptor {
	private String name;
	private Type type;
	
	public ParameterDescriptor(String name, Type type) {
		this.setName(name);
		this.setType(type);

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
		String str = this.type.toString() + " " + this.name;
		return str;
	}

}
