package ir.ast;

import java.util.ArrayList;

public class MethodDecl extends MemberDecl {
	private Type returnType;
	private String id;
	private ArrayList<Parameter> parameters;
	
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
	
	public MethodDecl(Type t, String i, ArrayList<Parameter> p) {
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
	
	public void setParameters(ArrayList<Parameter> p) {
		parameters = p;
	}
	
	public ArrayList<Parameter> getParamters() {
		return parameters;
	}
	
	@Override
	public String toString() {
		return returnType + " " + id + "(" + parameters + ")";
	}
}
