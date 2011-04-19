package decaf.ir.semcheck;

import java.util.List;

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
import decaf.ir.ast.Expression;
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
import decaf.ir.ast.Statement;
import decaf.ir.ast.UnaryOpExpr;
import decaf.ir.ast.VarDecl;
import decaf.ir.ast.VarLocation;

public class UnaryCollapseVisitor implements ASTVisitor<Boolean> {
	private Expression exprToReplace;
	
	public UnaryCollapseVisitor() { 
		exprToReplace = null;
	}

	@Override
	public Boolean visit(ArrayLocation loc) {
		if (loc.getExpr().accept(this)) {
			loc.setExpr(exprToReplace);
			exprToReplace = null;
			loc.accept(this);
		}
		
		return false;
	}

	@Override
	public Boolean visit(AssignStmt stmt) {
		stmt.getLocation().accept(this);
		
		if (stmt.getExpression().accept(this)) {
			stmt.setExpression(exprToReplace);
			exprToReplace = null;
			stmt.accept(this);
		}
		
		return false;
	}

	@Override
	public Boolean visit(BinOpExpr expr) {
		if (expr.getLeftOperand().accept(this)) {
			expr.setLeftOperand(exprToReplace);
			exprToReplace = null;
			expr.accept(this);
		}
		
		if (expr.getRightOperand().accept(this)) {
			expr.setRightOperand(exprToReplace);
			exprToReplace = null;
			expr.accept(this);
		}
		
		return false;
}

	@Override
	public Boolean visit(Block block) {
		List<Statement> stmts = block.getStatements();
		
		for (int i = 0; i < stmts.size(); i++) {
			stmts.get(i).accept(this);
		}		
		return false;
	
	}

	@Override
	public Boolean visit(BooleanLiteral lit) {
		return false;
	}

	@Override
	public Boolean visit(BreakStmt stmt) {
		return false;
	}

	@Override
	public Boolean visit(CalloutArg arg) {
		if (!arg.isString()) {
			if (arg.getExpression().accept(this)) {
				arg.setExpression(exprToReplace);
				exprToReplace = null;
				arg.accept(this);
			}
		}
		return false;
	}

	@Override
	public Boolean visit(CalloutExpr expr) {
		for (CalloutArg arg: expr.getArguments()) {
			arg.accept(this);
		}
		return false;
	}

	@Override
	public Boolean visit(CharLiteral lit) {
		return false;
		}

	@Override
	public Boolean visit(ClassDecl cd) {
		for (FieldDecl fd: cd.getFieldDeclarations()) {
			fd.accept(this);
		}
		
		for (MethodDecl md: cd.getMethodDeclarations()) {
			md.accept(this);
		}
		
		return false;
	}

	@Override
	public Boolean visit(ContinueStmt stmt) {
		return false;
	}
	
	@Override
	public Boolean visit(Field f) {
		if (f.getArrayLength() != null) {
			f.getArrayLength().accept(this);
		}
		return false;
	}

	@Override
	public Boolean visit(FieldDecl fd) {
		for (Field f: fd.getFields()) {
			f.accept(this);
		}
		return false;
	}

	@Override
	public Boolean visit(ForStmt stmt) {
		if (stmt.getInitialValue().accept(this)) {
			stmt.setInitialValue(exprToReplace);
			exprToReplace = null;
			stmt.accept(this);
		}
		
		if (stmt.getFinalValue().accept(this)) {
			stmt.setFinalValue(exprToReplace);
			exprToReplace = null;
			stmt.accept(this);
		}
		
		stmt.getBlock().accept(this); // Block auto indents
		
		return false;
	}

	@Override
	public Boolean visit(IfStmt stmt) {
		if (stmt.getCondition().accept(this)) {
			stmt.setCondition(exprToReplace);
			exprToReplace = null;
			stmt.accept(this);
		}
		
		stmt.getIfBlock().accept(this);
		
		if (stmt.getElseBlock() != null) {
			stmt.getElseBlock().accept(this);
		}
		return false;
	}

	@Override
	public Boolean visit(InvokeStmt stmt) {
		stmt.getMethodCall().accept(this);
		return false;
	}

	@Override
	public Boolean visit(MethodCallExpr expr) {
		for (int i = 0; i < expr.getArguments().size(); i++) {
			if (expr.getArguments().get(i).accept(this)) {
				expr.getArguments().set(i, exprToReplace);
				exprToReplace = null;
				expr.accept(this);
			}
		}
		return false;
	}

	@Override
	public Boolean visit(MethodDecl md) {		
		md.getBlock().accept(this);
		return false;
	}

	@Override
	public Boolean visit(Parameter param) {
		return false;
	}

	@Override
	public Boolean visit(ReturnStmt stmt) {
		if (stmt.getExpression() != null) {
			if (stmt.getExpression().accept(this)) {
				stmt.setExpression(exprToReplace);
				exprToReplace = null;
				stmt.accept(this);
			}
		}
		return false;
	}
	
	@Override
	public Boolean visit(IntLiteral lit) {
		return false;
	}

	@Override
	public Boolean visit(UnaryOpExpr expr) {
		if (expr.getExpression().getClass().equals(UnaryOpExpr.class)) { // Check if nested UnaryOps
			UnaryOpExpr e = (UnaryOpExpr)expr.getExpression();
			exprToReplace = e.getExpression();
			return true;
		}
		else {
			if (expr.getExpression().accept(this)) {
				expr.setExpression(exprToReplace);
				exprToReplace = null;
				expr.accept(this);
			}
		}
		
		return false;
	}

	@Override
	public Boolean visit(VarDecl vd) {
		return false;
	}

	@Override
	public Boolean visit(VarLocation loc) {
		return false;
	}
}