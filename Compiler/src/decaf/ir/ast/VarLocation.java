package decaf.ir.ast;

import decaf.ir.semcheck.ASTVisitor;

public class VarLocation extends Location {
	public VarLocation(String id) {
		this.id = id;
	}
	
	@Override
	public String toString() {
		return id;
	}

	@Override
	public <T> T accept(ASTVisitor<T> v) {
		// TODO Auto-generated method stub
		return null;
	}
}