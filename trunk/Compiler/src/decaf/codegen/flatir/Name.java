package decaf.codegen.flatir;

public abstract class Name {
	private Location location;

	public void setLocation(Location location) {
		this.location = location;
	}

	public Location getLocation() {
		return location;
	}
}
