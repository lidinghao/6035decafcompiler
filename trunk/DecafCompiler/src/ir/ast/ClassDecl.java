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
	
	public ClassDecl(List<FieldDecl> f, List<MethodDecl> m) {
		fieldDeclarations = f;
		methodDeclarations = m;
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
	
	@Override
	public String toString() {
		String rtn = "CLASS \n";
		for (FieldDecl f: fieldDeclarations) {
			rtn += f.toString() + '\n';
		}
		
		for (MethodDecl m: methodDeclarations) {
			rtn += m.toString() + '\n';
		}
		
		if (fieldDeclarations.size() > 0 || methodDeclarations.size() > 0) {
			rtn = rtn.substring(0, rtn.length() - 1); // truncate last new line char
		}
		
		return rtn;
	}
}
