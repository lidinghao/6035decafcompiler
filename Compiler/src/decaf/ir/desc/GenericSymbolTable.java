package decaf.ir.desc;

public class GenericSymbolTable extends SymbolTable<String, GenericDescriptor> {
	private static final long serialVersionUID = 1L;
	private GenericSymbolTable parent;
	
	public GenericSymbolTable() {
		this(null);
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
}
