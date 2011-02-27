package decaf.ir.desc;

import java.util.HashMap;

public class ClassDescriptor extends Descriptor {
	private GenericSymbolTable fieldSymbolTable;
	private MethodSymbolTable methodSymbolTable;
	private HashMap<Integer, GenericSymbolTable> scopeTable;
	
	public ClassDescriptor() {
		fieldSymbolTable = new GenericSymbolTable();
		methodSymbolTable = new MethodSymbolTable();
		scopeTable = new HashMap<Integer, GenericSymbolTable>();
	}

	public GenericSymbolTable getFieldSymbolTable() {
		return fieldSymbolTable;
	}

	public void setFieldSymbolTable(GenericSymbolTable fieldSymbolTable) {
		this.fieldSymbolTable = fieldSymbolTable;
	}

	public MethodSymbolTable getMethodSymbolTable() {
		return methodSymbolTable;
	}

	public void setMethodSymbolTable(MethodSymbolTable methodSymbolTable) {
		this.methodSymbolTable = methodSymbolTable;
	}

	public void setScopeTable(HashMap<Integer, GenericSymbolTable> scopeTable) {
		this.scopeTable = scopeTable;
	}

	public HashMap<Integer, GenericSymbolTable> getScopeTable() {
		return scopeTable;
	}
}
