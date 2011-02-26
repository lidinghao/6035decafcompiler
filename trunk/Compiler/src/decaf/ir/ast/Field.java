package decaf.ir.ast;

public class Field {
	private String id;
	private boolean isArray;
	private int arraySize;
	
	public Field(String i) {
		id = i;
		isArray = false;
		arraySize = -1;
	}
	
	public Field(String i, String arrSize) {
		id = i;
		isArray = true;
		arraySize = Integer.parseInt(arrSize);	
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
		return arraySize;
	}
	
	@Override
	public String toString() {
		if (isArray) {
			return id + "[" + arraySize + "]";
		}
		else {
			return id;
		}
	}
}
