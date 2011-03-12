package decaf.codegen.flattener;

import java.util.List;

import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
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

public class ExpressionFlatennerVisitor implements ASTVisitor<Name> {
	private List<LIRStatement> statements;
	
	public ExpressionFlatennerVisitor(List<LIRStatement> statements) {
		this.statements = statements;
	}

	@Override
	public Name visit(ArrayLocation loc) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Name visit(AssignStmt stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Name visit(BinOpExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Name visit(Block block) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Name visit(BooleanLiteral lit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Name visit(BreakStmt stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Name visit(CalloutArg arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Name visit(CalloutExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Name visit(CharLiteral lit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Name visit(ClassDecl cd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Name visit(ContinueStmt stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Name visit(Field f) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Name visit(FieldDecl fd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Name visit(ForStmt stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Name visit(IfStmt stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Name visit(IntLiteral lit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Name visit(InvokeStmt stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Name visit(MethodCallExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Name visit(MethodDecl md) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Name visit(Parameter param) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Name visit(ReturnStmt stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Name visit(UnaryOpExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Name visit(VarDecl vd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Name visit(VarLocation loc) {
		// TODO Auto-generated method stub
		return null;
	}
}
