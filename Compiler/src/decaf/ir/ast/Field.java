package decaf.ir.ast;

import decaf.ir.ASTVisitor;

public class Field extends AST {
	private String id;
	private boolean isArray;
	private IntLiteral arrayLength;
	private Type type;
	
	public Field(String i) {
		id = i;
		isArray = false;
		arrayLength = null;
	}
	
	public Field(String i, String arrSize) {
		id = i;
		isArray = true;
		arrayLength = new IntLiteral(arrSize);
	}
	
	public void setId(String i) {
		id = i;
	}
	
	public String getId() {
		return id;
	}
	
	public void setIsArray(boolean array) {
		isArray = array;
	}
	
	public boolean isArray() {
		return isArray;
	}
	
	public void setArrayLength(IntLiteral len) {
		arrayLength = len;
	}

	public IntLiteral getArrayLength() {
		return arrayLength;
	}

	@Override
	public String toString() {
		if (isArray) {
			return id + "[" + arrayLength.toString() + "]";
		}
		else {
			return id;
		}
	}

	@Override
	public <T> T accept(ASTVisitor<T> v) {
		return v.visit(this);
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Type getType() {
		return type;
	}
}
