package ir.ast;

import java.util.ArrayList;
import java.util.List;

public class ClassDecl extends AST {
	private List<FieldDecl> fieldDeclarations;
	private List<MethodDecl> methodDeclarations;
	
	public ClassDecl() {
		fieldDeclarations = new ArrayList<FieldDecl>();
		methodDeclarations = new ArrayList<MethodDecl>();
	}
	
	public void addFieldDecl(FieldDecl f) {
		fieldDeclarations.add(f);
	}
	
	public void addMethodDecl(MethodDecl m) {
		methodDeclarations.add(m);
	}
	
	public List<FieldDecl> getFieldDeclarations() {
		return fieldDeclarations;
	}
	
	public List<MethodDecl> getMethodDeclarations() {
		return methodDeclarations;
	}
}
