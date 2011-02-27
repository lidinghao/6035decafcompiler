package decaf.ir.desc;

import decaf.ir.desc.MethodDescriptor;

public class MethodSymbolTable extends SymbolTable<String, MethodDescriptor> {
	private static final long serialVersionUID = 1L;

	public MethodSymbolTable() {
		super();
	}
	
	@Override
	public String toString() {
		String rtn = "";
		for (String key: this.keySet()) {
			rtn += "" + key + "=" + this.get(key) + "\n";
		}
		
		return rtn;
	}
}
