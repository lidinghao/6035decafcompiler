package decaf.ir.semcheck;

import java.util.ArrayList;
import java.util.List;

import decaf.ir.ASTVisitor;
import decaf.ir.ast.AST;
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
import decaf.ir.ast.Type;
import decaf.ir.ast.UnaryOpExpr;
import decaf.ir.ast.VarDecl;
import decaf.ir.ast.VarLocation;
import decaf.ir.desc.ClassDescriptor;
import decaf.ir.desc.MethodDescriptor;
import decaf.ir.desc.MethodSymbolTable;
import decaf.test.Error;

public class ProperMethodCallCheckVisitor implements ASTVisitor<Integer> {
	private ArrayList<Error> errors;
	private ClassDescriptor classDescriptor;
	private Type currentReturnType;

	public ProperMethodCallCheckVisitor(ClassDescriptor cd) {
		this.errors = new ArrayList<Error>();
		this.classDescriptor = cd;
	}

	@Override
	public Integer visit(ArrayLocation loc) {
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
		for (VarDecl vd : block.getVarDeclarations()) {
			vd.accept(this);
		}

		for (Statement s : block.getStatements()) {
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
		for (CalloutArg arg : expr.getArguments()) {
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
		for (FieldDecl fd : cd.getFieldDeclarations()) {
			fd.accept(this);
		}

		for (MethodDecl md : cd.getMethodDeclarations()) {
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
		return 0;
	}

	@Override
	public Integer visit(FieldDecl fd) {
		for (Field f : fd.getFields()) {
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
		for (Expression arg : expr.getArguments()) {
			arg.accept(this);
		}

		// Check appropriate return type for each argument
		MethodSymbolTable methodTable = classDescriptor.getMethodSymbolTable();
		MethodDescriptor methodDesc = methodTable.get(expr.getName());
		List<Type> methodParamTypes = methodDesc.getParameterTypes();

		// Too few arguments error
		if (methodParamTypes.size() != expr.getArguments().size()) {
			addError(expr,
					"Expecting " + Integer.toString(methodParamTypes.size())
							+ " arguments but found " + expr.getArguments().size());
			return 0;
		}

		// Otherwise, type check the arguments
		for (int i = 0; i < methodParamTypes.size(); i++) {
			Type desiredType = methodParamTypes.get(i);
			Type argType = expr.getArguments().get(i).getType();
			if (!desiredType.equals(argType)) {
				addError(expr, "Expecting " + desiredType.toString()
						+ " but found " + argType.toString() + " for argument "
						+ Integer.toString(i + 1));
			}
		}
		return 0;
	}

	@Override
	public Integer visit(MethodDecl md) {
		// Get return type
		currentReturnType = md.getReturnType();
		md.getBlock().accept(this);
		currentReturnType = null;

		return 0;
	}

	@Override
	public Integer visit(Parameter param) {
		return 0;
	}

	@Override
	public Integer visit(ReturnStmt stmt) {
		if (stmt.getExpression() != null) {
			Type retType = stmt.getExpression().getType();
			if (!currentReturnType.equals(retType)) {
				// Returning wrong type
				addError(stmt, "Returning " + retType.toString()
						+ " when expecting " + currentReturnType.toString());
			}
			stmt.getExpression().accept(this);
		} else {
			if (!currentReturnType.equals(Type.VOID)) {
				// Trying to return void when there is a current return type
				addError(
						stmt,
						"Returning void when expecting "
								+ currentReturnType.toString());
			}
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

	private void addError(AST a, String desc) {
		errors.add(new Error(a.getLineNumber(), a.getColumnNumber(), desc));
	}
}
