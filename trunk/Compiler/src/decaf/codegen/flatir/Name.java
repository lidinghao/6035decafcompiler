package decaf.codegen.flatir;

public abstract class Name {
	private Location location;
	private Register register = null;

	public void setLocation(Location location) {
		this.location = location;
	}

	public Location getLocation() {
		return location;
	}
	
	public abstract boolean isArray();
	
	public abstract Object clone(); // Don't copy register!

	public void setRegister(Register register) {
		this.register = register;
	}
	
	public Register getMyRegister() {
		return this.register;
	}

	public String getRegister() {
		if (register != null) {
			return register.toString();
		}
		else {
			return "f*ck";
		}
	}
	
	public boolean isGlobal() {
		return false;
	}
	
	public boolean needsLoad() {
		return false;
	}
}
