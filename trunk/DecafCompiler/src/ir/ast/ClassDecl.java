package ir.ast;

import java.util.ArrayList;

public class ClassDecl extends AST {
	private ArrayList<FieldDecl> fieldDeclarations;
	private ArrayList<MethodDecl> methodDeclarations;
	
	public ClassDecl() {
		fieldDeclarations = new ArrayList<FieldDecl>();
		methodDeclarations = new ArrayList<MethodDecl>();
	}
	
	public ClassDecl(ArrayList<FieldDecl> f, ArrayList<MethodDecl> m) {
		if (f != null)
			fieldDeclarations = f;
		else
			fieldDeclarations = new ArrayList<FieldDecl>();
		if (m != null)
			methodDeclarations = m;
		else
			methodDeclarations = new ArrayList<MethodDecl>();
	}
	
	public void addFieldDecl(FieldDecl f) {
		fieldDeclarations.add(f);
	}
	
	public void addMethodDecl(MethodDecl m) {
		methodDeclarations.add(m);
	}
	
	public void setFieldDeclarations(ArrayList<FieldDecl> f) {
		fieldDeclarations = f;
	}
	
	public void setMethodDeclarations(ArrayList<MethodDecl> m) {
		methodDeclarations = m;
	}
	
	public ArrayList<FieldDecl> getFieldDeclarations() {
		return fieldDeclarations;
	}
	
	public ArrayList<MethodDecl> getMethodDeclarations() {
		return methodDeclarations;
	}
}
