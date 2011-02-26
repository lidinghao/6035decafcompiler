package decaf.ir.ast;

import java.util.ArrayList;
import java.util.List;
import decaf.ir.ASTVisitor;

public class Block extends Statement {
	private List<Statement> statements;
	private List<VarDecl> variableDeclarations;
	
	public Block() {
		statements = new ArrayList<Statement>();
		variableDeclarations = new ArrayList<VarDecl>();
	}
	
	public Block(List<Statement> s, List<VarDecl> f) {
		statements = s;
		variableDeclarations = f;
	}
	
	public void addStatement(Statement s) {
		this.statements.add(s);
	}
	
	public void addFieldDeclaration(VarDecl f) {
		this.variableDeclarations.add(f);
	}
	
	public List<Statement> getStatements() {
		return this.statements;
	} 
	
	public List<VarDecl> getVarDeclarations() {
		return this.variableDeclarations;
	}
	
	@Override
	public String toString() {
		String rtn = "";
		
		for (VarDecl f: variableDeclarations) {
			rtn += f.toString() + '\n';
		}
		
		for (Statement s: statements) {
			rtn += s.toString() + '\n';
		}
		
		if (rtn.length() > 0) return rtn.substring(0, rtn.length() - 1); // remove last new line char
		
		return rtn; 
	}

	@Override
	public <T> T accept(ASTVisitor<T> v) {
		return v.visit(this);
	}
	
}
