package decaf.ir.semcheck;

import java.util.ArrayList;

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
import decaf.test.Error;

public class ArraySizeCheckVisitor implements ASTVisitor<Integer> {
	private ClassDecl cd;
	private ArrayList<Error> errors;
	
	public ArraySizeCheckVisitor() {
		this.errors = new ArrayList<Error>();
	}
	
	@Override
	public Integer visit(ArrayLocation loc) {
		// Set size
		for (FieldDecl fd: cd.getFieldDeclarations()) {
			for (Field f: fd.getFields()) {
				if (f.getId().equals(loc.getId())) {
					loc.setSize(f.getArrayLength().getValue());
				}
			}
		}
		
		loc.getExpr().accept(this);
		return 0;
	}

	@Override
	public Integer visit(AssignStmt stmt) {
		stmt.getLocation().accept(this);
		stmt.getExpression().accept(this);
		return 0;
	}

	@Override
	public Integer visit(BinOpExpr expr) {
		expr.getLeftOperand().accept(this);
		expr.getRightOperand().accept(this);
		return 0;
	}

	@Override
	public Integer visit(Block block) {
		for (VarDecl vd: block.getVarDeclarations()) {
			vd.accept(this);
		}
		
		for (Statement s: block.getStatements()) {
			s.accept(this);
		}
		
		return 0;
	}

	@Override
	public Integer visit(BooleanLiteral lit) {
		return 0;
	}

	@Override
	public Integer visit(BreakStmt stmt) {
		return 0;
	}

	@Override
	public Integer visit(CalloutArg arg) {
		if (!arg.isString()) {
			arg.getExpression().accept(this);
		}
		return 0;
	}

	@Override
	public Integer visit(CalloutExpr expr) {
		for (CalloutArg arg: expr.getArguments()) {
			arg.accept(this);
		}
		
		return 0;
	}

	@Override
	public Integer visit(CharLiteral lit) {
		return 0;
	}

	@Override
	public Integer visit(ClassDecl cd) {
		this.cd = cd;
		
		for (FieldDecl fd: cd.getFieldDeclarations()) {
			fd.accept(this);
		}
		
		for (MethodDecl md: cd.getMethodDeclarations()) {
			md.accept(this);
		}
		return 0;
	}

	@Override
	public Integer visit(ContinueStmt stmt) {
		return 0;
	}

	@Override
	public Integer visit(Field f) {		
		// checking the size of the array
		if (f.getType().isArray()) {
			if (f.getArrayLength().getValue() < 1) {
				int ln = f.getLineNumber();
				int cn = f.getColumnNumber();
				String msg = "Size of array '" + f.getId() + "' is less than 1";
				Error err = new Error(ln, cn, msg);
				this.errors.add(err);
			}
		}
		return 0;
	}

	@Override
	public Integer visit(FieldDecl fd) {
		for (Field f: fd.getFields()) {
			f.accept(this);
		}
		return 0;
	}

	@Override
	public Integer visit(ForStmt stmt) {
		stmt.getBlock().accept(this);
		return 0;
	}

	@Override
	public Integer visit(IfStmt stmt) {
		stmt.getCondition().accept(this);
		stmt.getIfBlock().accept(this);
		if (stmt.getElseBlock() != null) {
			stmt.getElseBlock().accept(this);
		}
		
		return 0;
	}

	@Override
	public Integer visit(IntLiteral lit) {
		return 0;
	}

	@Override
	public Integer visit(InvokeStmt stmt) {
		stmt.getMethodCall().accept(this);
		return 0;
	}

	@Override
	public Integer visit(MethodCallExpr expr) {
		for (Expression arg: expr.getArguments()) {
			arg.accept(this);
		}
		return 0;
	}

	@Override
	public Integer visit(MethodDecl md) {
		for (Parameter p: md.getParameters()) {
			p.accept(this);
		}
		md.getBlock().accept(this);
		
		return 0;
	}

	@Override
	public Integer visit(Parameter param) {
		return 0;
	}

	@Override
	public Integer visit(ReturnStmt stmt) {
		if (stmt.getExpression() != null) {
			stmt.getExpression().accept(this);
		}
		
		return 0;
	}

	@Override
	public Integer visit(UnaryOpExpr expr) {
		expr.getExpression().accept(this);
		return 0;
	}

	@Override
	public Integer visit(VarDecl vd) {
		return 0;
	}

	@Override
	public Integer visit(VarLocation loc) {
		return 0;
	}

	public ArrayList<Error> getErrors() {
		return errors;
	}

}

