package ir.desc;

import ir.SymbolTable;

public class ClassDescriptor extends Descriptor {
	private SymbolTable methodsSymbolTable;
	private SymbolTable fieldsSymbolTable;
	
	public ClassDescriptor() {
		this.setFieldsSymbolTable(null);
		this.setMethodsSymbolTable(null);
	}
	
	public ClassDescriptor(SymbolTable fieldsSymbolTable, SymbolTable methodsSymbolTable) {
		this.setFieldsSymbolTable(fieldsSymbolTable);
		this.setMethodsSymbolTable(methodsSymbolTable);
	}

	public void setMethodsSymbolTable(SymbolTable methodsSymbolTable) {
		this.methodsSymbolTable = methodsSymbolTable;
	}

	public SymbolTable getMethodsSymbolTable() {
		return methodsSymbolTable;
	}

	public void setFieldsSymbolTable(SymbolTable fieldsSymbolTable) {
		this.fieldsSymbolTable = fieldsSymbolTable;
	}

	public SymbolTable getFieldsSymbolTable() {
		return fieldsSymbolTable;
	}
	
	@Override
	public String toString() {
		return null;
	}

}
