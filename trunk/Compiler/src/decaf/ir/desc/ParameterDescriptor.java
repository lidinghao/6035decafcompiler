package decaf.ir.desc;

import decaf.ir.ast.Type;

public class ParameterDescriptor extends GenericDescriptor {
	public ParameterDescriptor(String id, Type type) {
		this.id = id;
		this.type = type;
	}
}
