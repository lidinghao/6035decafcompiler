package ir.ast;

import java.util.ArrayList;

public class Block extends Statement {
	private ArrayList<Statement> statements;
	private ArrayList<FieldDecl> fieldDeclarations;
	
	public Block() {
		statements = new ArrayList<Statement>();
		fieldDeclarations = new ArrayList<FieldDecl>();
	}
	
	public Block(ArrayList<Statement> s, ArrayList<FieldDecl> f) {
		if (s == null) {
			this.statements = new ArrayList<Statement>();
		} else {
			this.statements = s;
		}
		if (f == null) {
			this.fieldDeclarations = new ArrayList<FieldDecl>();
		} else {
			this.fieldDeclarations = f;
		}
	}

	public void addStatement(Statement s) {
		this.statements.add(s);
	}
	
	public void addFieldDecl(FieldDecl f) {
		this.fieldDeclarations.add(f);
	}
	
	public ArrayList<Statement> getStatements() {
		return this.statements;
	} 
	
	public ArrayList<FieldDecl> getFieldDeclarations() {
		return this.fieldDeclarations;
	}
	
}
