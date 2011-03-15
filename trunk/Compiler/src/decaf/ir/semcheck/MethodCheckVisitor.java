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

public class MethodCheckVisitor implements ASTVisitor<Boolean> {
	private ArrayList<Error> errors;
	private ClassDescriptor classDescriptor;
	private Type currentReturnType;

	public MethodCheckVisitor(ClassDescriptor cd) {
		this.errors = new ArrayList<Error>();
		this.classDescriptor = cd;
		this.currentReturnType = Type.UNDEFINED;
	}

	@Override
	public Boolean visit(ArrayLocation loc) {
		loc.getExpr().accept(this);

		return false;
	}

	@Override
	public Boolean visit(AssignStmt stmt) {
		stmt.getLocation().accept(this);
		stmt.getExpression().accept(this);

		return false;
	}

	@Override
	public Boolean visit(BinOpExpr expr) {
		expr.getLeftOperand().accept(this);
		expr.getRightOperand().accept(this);

		return false;
	}

	@Override
	public Boolean visit(Block block) {

		for (VarDecl vd : block.getVarDeclarations()) {
			vd.accept(this);
		}

		for (Statement s : block.getStatements()) {
			s.accept(this);
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
			arg.getExpression().accept(this);
		}

		return false;
	}

	@Override
	public Boolean visit(CalloutExpr expr) {
		for (CalloutArg arg : expr.getArguments()) {
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
		for (FieldDecl fd : cd.getFieldDeclarations()) {
			fd.accept(this);
		}

		for (MethodDecl md : cd.getMethodDeclarations()) {
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
		return false;
	}

	@Override
	public Boolean visit(FieldDecl fd) {
		for (Field f : fd.getFields()) {
			f.accept(this);
		}

		return false;
	}

	@Override
	public Boolean visit(ForStmt stmt) {
		stmt.getInitialValue().accept(this);
		stmt.getFinalValue().accept(this);
		stmt.getBlock().accept(this);

		return false; // Can't ensure if for loop executes at least once
	}

	@Override
	public Boolean visit(IfStmt stmt) {
		stmt.getCondition().accept(this);

		stmt.getIfBlock().accept(this);
		if (stmt.getElseBlock() != null) {
			stmt.getElseBlock().accept(this);
		}

		return false;
	}

	@Override
	public Boolean visit(IntLiteral lit) {
		return false;
	}

	@Override
	public Boolean visit(InvokeStmt stmt) {
		stmt.getMethodCall().accept(this);

		return false;
	}

	@Override
	public Boolean visit(MethodCallExpr expr) {
		for (Expression arg : expr.getArguments()) {
			arg.accept(this);
		}

		// Check appropriate return type for each argument
		MethodSymbolTable methodTable = classDescriptor.getMethodSymbolTable();
		MethodDescriptor methodDesc = methodTable.get(expr.getName());

		if (methodDesc != null) {
			List<Type> methodParamTypes = methodDesc.getParameterTypes();

			// Too few arguments error
			if (methodParamTypes.size() != expr.getArguments().size()) {
				addError(expr, "Expecting "
						+ Integer.toString(methodParamTypes.size())
						+ " arguments but found " + expr.getArguments().size());

				return false;
			}

			// Otherwise, type check the arguments
			for (int i = 0; i < methodParamTypes.size(); i++) {
				Type desiredType = methodParamTypes.get(i);
				Type argType = expr.getArguments().get(i).getType();
				if (desiredType != argType) {
					addError(expr, "Expecting " + desiredType.toString()
							+ " but found " + argType.toString() + " for argument "
							+ Integer.toString(i + 1));
				}
			}
		}

		return false;
	}

	@Override
	public Boolean visit(MethodDecl md) {
		for (Parameter p : md.getParameters()) {
			p.accept(this);
		}

		currentReturnType = md.getReturnType();
		
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
			Type retType = stmt.getExpression().getType();
			if (currentReturnType != retType) {
				// Returning wrong type
				addError(stmt, "Returning " + retType + " when expecting "
						+ currentReturnType);
			}
			stmt.getExpression().accept(this);
		} else {
			if (!currentReturnType.equals(Type.VOID)) {
				// Trying to return void when there is a current return type
				addError(stmt, "Returning void when expecting " + currentReturnType);
			}
		}

		return true;
	}

	@Override
	public Boolean visit(UnaryOpExpr expr) {
		expr.getExpression().accept(this);
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
	
	public ArrayList<Error> getErrors() {
		return errors;
	}

	private void addError(AST a, String desc) {
		errors.add(new Error(a.getLineNumber(), a.getColumnNumber(), desc));
	}
}
