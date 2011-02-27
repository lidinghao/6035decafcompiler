package decaf.ir.desc;

import decaf.ir.ast.Type;

public class VariableDescriptor extends GenericDescriptor {
	Object value; //?

	public VariableDescriptor(String id, Type type) {
		this.id = id;
		this.type = type;
		this.isArray = false;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}
}
