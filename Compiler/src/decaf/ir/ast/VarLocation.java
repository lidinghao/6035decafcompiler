package decaf.ir.ast;

public class VarLocation extends Location {
	public VarLocation(String id) {
		this.id = id;
	}
	
	@Override
	public String toString() {
		return id;
	}
}