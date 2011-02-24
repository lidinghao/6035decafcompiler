package ir.ast;

public class Field {
	private Type type;
	private String id;
	private boolean isArray;
	private int arraySize;
	
	public Field(Type t, String i) {
		type = t;
		id = i;
		isArray = false;
		arraySize = -1;
	}
	
	public Field(Type t, String i, boolean array, int arrSize) {
		type = t;
		id = i;
		isArray = array;
		arraySize = arrSize;
	}
	
	public void setType(Type t) {
		type = t;
	}
	
	public Type getType() {
		return type;
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
	
	public void setArraySize(int size) {
		arraySize = size;
	}
	
	public int getArraySize() {
		if (isArray) {
			return arraySize;
		}
		return -1;
	}
}
