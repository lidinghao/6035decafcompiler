package decaf.ir.ast;

import decaf.ir.ASTVisitor;

public class Field extends AST {
	private String id;
	private boolean isArray;
	private String rawLength;
	private int length;
	
	public Field(String i) {
		id = i;
		isArray = false;
		rawLength = "-1";
	}
	
	public Field(String i, String arrSize) {
		id = i;
		isArray = true;
		rawLength = arrSize;	
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
	
	public void setArrayLength(int size) {
		length = size;
	}
	
	public int getArrayLength() {
		return length;
	}
	
	@Override
	public String toString() {
		if (isArray) {
			return id + "[" + rawLength + "]";
		}
		else {
			return id;
		}
	}

	@Override
	public <T> T accept(ASTVisitor<T> v) {
		return v.visit(this);
	}
}
