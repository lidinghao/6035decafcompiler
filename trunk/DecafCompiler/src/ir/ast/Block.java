package ir.ast;

import java.util.ArrayList;
import java.util.List;

public class Block extends Statement {
	private List<Statement> statements;
	private List<FieldDecl> fieldDeclarations;
	
	public Block() {
		statements = new ArrayList<Statement>();
		fieldDeclarations = new ArrayList<FieldDecl>();
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
	
}
