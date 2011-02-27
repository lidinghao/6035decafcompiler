package decaf.ir.desc;

import java.util.ArrayList;
import java.util.List;

import decaf.ir.ast.Parameter;
import decaf.ir.ast.Type;

public class MethodDescriptor extends Descriptor {
	private GenericSymbolTable parameterSymbolTable;
	private GenericSymbolTable localSymbolTable;
	private List<Type> parameterTypes;
	private Type returnType;
	private String id;
	private Object body;
	
	public MethodDescriptor(String id, Type returnType, List<Parameter> params) {
		parameterSymbolTable = null;
		localSymbolTable = null;
		parameterTypes = new ArrayList<Type>();
		for (Parameter p : params) {
			parameterTypes.add(p.getType());
		}
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
	
	public List<Type> getParameterTypes() {
		return parameterTypes;
	}

	public void setParameterTypes(List<Type> parameterTypes) {
		this.parameterTypes = parameterTypes;
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
