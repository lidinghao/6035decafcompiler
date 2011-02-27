package decaf.ir.semcheck;

import decaf.ir.ASTVisitor;
import decaf.ir.ast.ArrayLocation;
import decaf.ir.ast.AssignStmt;
import decaf.ir.ast.BinOpExpr;
import decaf.ir.ast.Block;
import decaf.ir.ast.BooleanLiteral;
import decaf.ir.ast.BreakStmt;
import decaf.ir.ast.CalloutArg;
import decaf.ir.ast.CalloutExpr;
import decaf.ir.ast.CharLiteral;
import decaf.ir.ast.ClassDecl;
import decaf.ir.ast.ContinueStmt;
import decaf.ir.ast.Field;
import decaf.ir.ast.FieldDecl;
import decaf.ir.ast.ForStmt;
import decaf.ir.ast.IfStmt;
import decaf.ir.ast.IntLiteral;
import decaf.ir.ast.InvokeStmt;
import decaf.ir.ast.MethodCallExpr;
import decaf.ir.ast.MethodDecl;
import decaf.ir.ast.Parameter;
import decaf.ir.ast.ReturnStmt;
import decaf.ir.ast.UnaryOpExpr;
import decaf.ir.ast.VarDecl;
import decaf.ir.ast.VarLocation;
import decaf.ir.desc.ClassDescriptor;
import decaf.ir.desc.GenericSymbolTable;

public class SymbolTableGenerationVisitor implements ASTVisitor<Integer> {
	private ClassDescriptor classDescriptor;
	private GenericSymbolTable currentScope;
	
	public SymbolTableGenerationVisitor() {
		setClassDescriptor(new ClassDescriptor());
	}

	@Override
	public Integer visit(ArrayLocation loc) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visit(AssignStmt stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visit(BinOpExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visit(Block block) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visit(BooleanLiteral lit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visit(BreakStmt stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visit(CalloutArg arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visit(CalloutExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visit(CharLiteral lit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visit(ClassDecl cd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visit(ContinueStmt stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visit(Field f) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visit(FieldDecl fd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visit(ForStmt stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visit(IfStmt stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visit(IntLiteral lit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visit(InvokeStmt stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visit(MethodCallExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visit(MethodDecl md) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visit(Parameter param) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visit(ReturnStmt stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visit(UnaryOpExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visit(VarDecl vd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visit(VarLocation loc) {
		// TODO Auto-generated method stub
		return null;
	}

	public void setClassDescriptor(ClassDescriptor classDescriptor) {
		this.classDescriptor = classDescriptor;
	}

	public ClassDescriptor getClassDescriptor() {
		return classDescriptor;
	}

}
