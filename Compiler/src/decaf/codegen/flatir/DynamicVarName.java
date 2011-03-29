package decaf.codegen.flatir;

/**
 * Used for optimizations. Not reused; a name is declared at instantiation time.
 * @author usmanm
 *
 */

public class DynamicVarName extends Name {
	public static int ID = 0;
	public static String NAME = "$tmp";
	private int myId;
	
	public DynamicVarName() {
		this.myId = ID;
		ID++;
	}
	
	public static void reset() {
		ID = 0;
	}

	public int getMyId() {
		return myId;
	}

	public void setMyId(int myId) {
		this.myId = myId;
	}

	@Override
	public boolean isArray() {
		return false;
	}
	
	@Override 
	public String toString() {
		return NAME + myId;
	}
	
	@Override
	public int hashCode() {
		return hashString().hashCode(); // Using forbidden chars
	}
	
	public String hashString() {
		return ("DynamicTemporary#" + myId);
	}
	
	@Override 
	public boolean equals(Object name) {
		if (this == name) return true;
		if (!name.getClass().equals(DynamicVarName.class)) return false;
		
		DynamicVarName vName = (DynamicVarName)name;
		
		return this.hashString().equals(vName.hashString());
	}


}
