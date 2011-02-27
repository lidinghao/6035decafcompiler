package decaf.ir.ast;

public enum Type {
	INT,
	BOOLEAN,
	CHAR,
	VOID,
	ERROR;
	
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
			case ERROR:
				return "error";
		}
		
		return null;
	}
}
