package decaf.ir.ast;

public enum Type {
	INT,
	BOOLEAN,
	CHAR,
	VOID;
	
	@Override
	public String toString() {
		switch(this) {
			case INT:
				return "int";
			case CHAR:
				return "char";
			case BOOLEAN:
				return "bool";
			case VOID:
				return "void";
		}
		
		return null;
	}
}
