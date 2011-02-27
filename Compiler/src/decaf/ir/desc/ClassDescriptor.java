package decaf.ir.desc;

public class ClassDescriptor extends Descriptor {
	private GenericSymbolTable fieldSymbolTable;
	private MethodSymbolTable methodSymbolTable;
	
	public ClassDescriptor() {
		fieldSymbolTable = new GenericSymbolTable();
		methodSymbolTable = new MethodSymbolTable();
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
}
