package decaf.test;

import java.io.PrintStream;

import decaf.ir.ASTVisitor;
import decaf.ir.ast.*;

public class PrettyPrintVisitor implements ASTVisitor<Void> {
	int tabSize;
	PrintStream out;
	
	public PrettyPrintVisitor() {
		tabSize = 0;
		out = System.out;
	}
	
	public PrettyPrintVisitor(PrintStream ps) {
		tabSize = 0;
		out = ps;
	}

	@Override
	public Void visit(ArrayLocation loc) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(AssignStmt stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(BinOpExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(Block block) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(BooleanLiteral lit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(BreakStmt stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(CalloutArg arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(CalloutExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(CharLiteral lit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(ClassDecl cd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(ContinueStmt stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(Field f) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(FieldDecl fd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(ForStmt stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(IfStmt stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(IntLiteral lit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(InvokeStmt stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(MethodCallExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(MethodDecl md) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(Parameter param) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(ReturnStmt stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(UnaryOpExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(VarDecl vd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(VarLocation loc) {
		// TODO Auto-generated method stub
		return null;
	}

}
