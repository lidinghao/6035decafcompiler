package decaf.codegen.flatir;

/**
 * Used for optimizations. Not reused; a name is declared at instantiation time.
 * @author usmanm
 *
 */

public class DynamicVarName extends Name {
	public static int ID = 0;
	public static String NAME = "$tmp";
	private boolean forGlobal;

	private int myId;
	
	public DynamicVarName() {
		this.myId = ID;
		this.forGlobal = false;
		ID++;
	}
	
	public DynamicVarName(boolean forGlobal) {
		this.myId = ID;
		this.forGlobal = forGlobal;
		ID++;
	}
	
	public static void reset() {
		ID = 0;
	}

	public boolean isForGlobal() {
		return forGlobal;
	}

	public void setForGlobal(boolean forGlobal) {
		this.forGlobal = forGlobal;
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
		return NAME + ((forGlobal) ? "_global" : "") + myId;
	}
	
	@Override
	public int hashCode() {
		return hashString().hashCode(); // Using forbidden chars
	}
	
	public String hashString() {
		return ((forGlobal) ? "Global" : "" + "DynamicTemporary#" + myId);
	}
	
	@Override 
	public boolean equals(Object name) {
		if (this == name) return true;
		if (!name.getClass().equals(DynamicVarName.class)) return false;
		
		DynamicVarName vName = (DynamicVarName)name;
		
		return this.hashString().equals(vName.hashString());
	}


}
