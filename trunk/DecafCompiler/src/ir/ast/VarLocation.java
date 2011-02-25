package ir.ast;

public class VarLocation extends Location {
	public VarLocation() {
		
	}
	
	public VarLocation(String id) {
		this.id = id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
}