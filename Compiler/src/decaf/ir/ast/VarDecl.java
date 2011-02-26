package decaf.ir.ast;

import java.util.ArrayList;
import java.util.List;

public class VarDecl extends AST {
	private Type type;
	private List<String> ids;
	
	public VarDecl() {
		type = Type.VOID;
		ids = new ArrayList<String>();
	}
	
	public VarDecl(Type t) {
		type = t;
		ids = new ArrayList<String>();
	}
	
	public void addVariable(String id) {
		ids.add(id);
	}
	
	public List<String> getVariables() {
		return ids;
	}
	
	public void setType(Type t) {
		type = t;
	}
	
	public Type getType() {
		return type;
	}
	
	@Override
	public String toString() {
		return type + ids.toString();
	}
}
