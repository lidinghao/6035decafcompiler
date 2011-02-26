package decaf.ir.ast;

import java.util.ArrayList;
import java.util.List;

public class Block extends Statement {
	private List<Statement> statements;
	private List<FieldDecl> fieldDeclarations;
	
	public Block() {
		statements = new ArrayList<Statement>();
		fieldDeclarations = new ArrayList<FieldDecl>();
	}
	
	public Block(List<Statement> s, List<FieldDecl> f) {
		statements = s;
		fieldDeclarations = f;
	}
	
	public void addStatement(Statement s) {
		this.statements.add(s);
	}
	
	public void addFieldDeclaration(FieldDecl f) {
		this.fieldDeclarations.add(f);
	}
	
	public List<Statement> getStatements() {
		return this.statements;
	} 
	
	public List<FieldDecl> getFieldDeclarations() {
		return this.fieldDeclarations;
	}
	
	@Override
	public String toString() {
		String rtn = "";
		for (FieldDecl f: fieldDeclarations) {
			rtn += f.toString() + '\n';
		}
		
		for (Statement s: statements) {
			rtn += s.toString() + '\n';
		}
		
		return rtn.substring(0, rtn.length() - 1); // remove last new line char
	}
	
}
