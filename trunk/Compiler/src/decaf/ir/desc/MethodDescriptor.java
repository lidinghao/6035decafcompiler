package decaf.ir.desc;

import decaf.ir.ast.Type;

public class MethodDescriptor extends Descriptor {
	private GenericSymbolTable parameterSymbolTable;
	private GenericSymbolTable localSymbolTable;
	private Type returnType;
	private String id;
	private Object body;
	
	public MethodDescriptor(String id, Type returnType) {
		parameterSymbolTable = null;
		localSymbolTable = null;
		this.id = id;
		this.returnType = returnType;
	}

	public GenericSymbolTable getParameterSymbolTable() {
		return parameterSymbolTable;
	}

	public void setParameterSymbolTable(GenericSymbolTable parameterSymbolTable) {
		this.parameterSymbolTable = parameterSymbolTable;
	}

	public GenericSymbolTable getLocalSymbolTable() {
		return localSymbolTable;
	}

	public void setLocalSymbolTable(GenericSymbolTable localSymbolTable) {
		this.localSymbolTable = localSymbolTable;
	}

	public Type getReturnType() {
		return returnType;
	}

	public void setReturnType(Type returnType) {
		this.returnType = returnType;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Object getBody() {
		return body;
	}

	public void setBody(Object body) {
		this.body = body;
	}
}
