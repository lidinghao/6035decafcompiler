package decaf.ir.desc;

public class GenericSymbolTable extends SymbolTable<String, GenericDescriptor> {
	private static final long serialVersionUID = 1L;
	private GenericSymbolTable parent;
	private int scopeId;

	public GenericSymbolTable() {
		this(null);
		scopeId = -1;
	}
	
	public GenericSymbolTable(GenericSymbolTable parent) {
		super();
		this.parent = parent;
	}

	public GenericSymbolTable getParent() {
		return parent;
	}

	public void setParent(GenericSymbolTable parent) {
		this.parent = parent;
	}
	
	public int getScopeId() {
		return scopeId;
	}

	public void setScopeId(int scopeId) {
		this.scopeId = scopeId;
	}
}
