package decaf.codegen.flatir;

public class GlobalLocation extends Location {
	private String name;
	
	public GlobalLocation(String name) {
		this.setName(name);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
