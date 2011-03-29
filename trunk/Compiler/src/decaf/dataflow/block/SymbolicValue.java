package decaf.dataflow.block;

public class SymbolicValue {
	public static int ID = 0;
	public int myId;
	
	public SymbolicValue() {
		myId = ID;
		ID++;
	}
	
	public int getMyId() {
		return myId;
	}

	public void setMyId(int myId) {
		this.myId = myId;
	}

	@Override 
	public int hashCode() {
		return myId;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!o.getClass().equals(SymbolicValue.class)) return false;
		
		SymbolicValue val = (SymbolicValue) o;
		
		return (this.getMyId() == val.getMyId());
	}
}
