package decaf.ir.ast;

import java.util.ArrayList;
import java.util.List;

public class MethodDecl extends MemberDecl {
	private Type returnType;
	private String id;
	private List<Parameter> parameters;
	private Block block;
	
	public MethodDecl() {
	}
	
	public MethodDecl(Type t, String i) {
		returnType = t;
		id = i;
		parameters = new ArrayList<Parameter>();
	}
	
	public MethodDecl(Type t, String i, Parameter p) {
		returnType = t;
		id = i;
		parameters = new ArrayList<Parameter>();
		addParameter(p);
	}
	
	public MethodDecl(Type t, String i, List<Parameter> p) {
		returnType = t;
		id = i;
		if (p != null)
			parameters = p;
		else
			parameters = new ArrayList<Parameter>();
	}
	
	public void setType(Type t) {
		returnType = t;
	}
	
	public Type getType() {
		return returnType;
	}
	
	public void setId(String i) {
		id = i;
	}
	
	public String getId() {
		return id;
	}
	
	public void addParameter(Parameter param) {
		parameters.add(param);
	}
	
	public void setParameters(List<Parameter> p) {
		parameters = p;
	}
	
	public List<Parameter> getParamters() {
		return parameters;
	}
	
	public Block getBlock() {
		return block;
	}

	public void setBlock(Block block) {
		this.block = block;
	}

	@Override
	public String toString() {
		String rtn = returnType + " " + id + "(" + parameters + ")\n";
		rtn += block.toString();
		
		return rtn;
	}
}
