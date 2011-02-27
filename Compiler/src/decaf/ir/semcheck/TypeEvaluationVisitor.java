package decaf.ir.semcheck;

import java.util.ArrayList;
import java.util.List;

import decaf.ir.ASTVisitor;
import decaf.ir.ast.AST;
import decaf.ir.ast.ArrayLocation;
import decaf.ir.ast.AssignOpType;
import decaf.ir.ast.AssignStmt;
import decaf.ir.ast.BinOpExpr;
import decaf.ir.ast.BinOpType;
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
import decaf.ir.ast.UnaryOpType;
import decaf.ir.ast.VarDecl;
import decaf.ir.ast.VarLocation;
import decaf.ir.desc.ClassDescriptor;
import decaf.ir.desc.GenericDescriptor;
import decaf.ir.desc.GenericSymbolTable;
import decaf.test.Error;

public class TypeEvaluationVisitor implements ASTVisitor<Type> {
	private GenericSymbolTable currentScope;
	private ClassDescriptor classDesc;
	private List<Error> errors;

	public TypeEvaluationVisitor(ClassDescriptor cd) {
		currentScope = cd.getFieldSymbolTable();
		classDesc = cd;
		errors = new ArrayList<Error>();
	}

	@Override
	public Type visit(ArrayLocation loc) {
		if (loc.getExpr().accept(this) != Type.INT) {
			addError(loc, "'" + loc.getExpr()
					+ "' must be of int type (array index)");
		}

		GenericDescriptor desc = getDescriptorFromScope(loc.getId());

		Type myType = Type.UNDEFINED;

		if (desc != null) {
			if (desc.getType() == Type.INTARRAY) {
				myType = Type.INT;
			}
			else if (desc.getType() == Type.BOOLEANARRAY) {
				myType = Type.BOOLEAN;
			}
			else {
				addError(loc, "'" + loc.getId()
						+ "' must be of an array");
			}
		}

		loc.setType(myType);

		return myType;
	}

	@Override
	public Type visit(AssignStmt stmt) {
		Type lhs = stmt.getLocation().accept(this);
		Type rhs = stmt.getExpression().accept(this);

		if (lhs != Type.UNDEFINED && rhs != Type.UNDEFINED) {
			if (stmt.getOperator() == AssignOpType.ASSIGN) {
				if (lhs != rhs) {
					addError(stmt, "'" + stmt.getLocation() + "' is of " + lhs
							+ " type, but is being assigned '" + stmt.getExpression()
							+ "' of " + rhs + "' type");
				}
			}
			else {
				if (lhs != Type.INT) {
					addError(stmt, "'" + stmt.getLocation()
							+ "' must be of int type");
				}
				if (rhs != Type.INT) {
					addError(stmt, "'" + stmt.getExpression()
							+ " must be of int type");
				}
			}
		}

		return Type.UNDEFINED;
	}

	@Override
	public Type visit(BinOpExpr expr) {
		Type lhs = expr.getLeftOperand().accept(this);
		Type rhs = expr.getRightOperand().accept(this);
		Type myType = Type.UNDEFINED;

		BinOpType op = expr.getOperator();

		// Already produced error somewhere in the child subtree (avoid multiple
		// errors)
		if (lhs != Type.UNDEFINED && rhs != Type.UNDEFINED) {
			switch (op) {
				case AND: // boolean only
				case OR:
					if (lhs == Type.BOOLEAN && rhs == Type.BOOLEAN) {
						myType = Type.BOOLEAN;
					}

					if (lhs != Type.BOOLEAN) {
						addError(expr.getLeftOperand(), "'"
								+ expr.getLeftOperand()
								+ "' must be of boolean type");
					}

					if (rhs != Type.BOOLEAN) {
						addError(expr.getRightOperand(), "'"
								+ expr.getRightOperand()
								+ "' must be of boolean type");
					}

					break;
				case PLUS: // int only
				case MINUS:
				case MULTIPLY:
				case DIVIDE:
				case MOD:
					if (lhs == Type.INT && rhs == Type.INT) {
						myType = Type.INT;
					}

					if (lhs != Type.INT) {
						addError(expr.getLeftOperand(), "'"
								+ expr.getLeftOperand() + "' must be of int type");
					}

					if (rhs != Type.INT) {
						addError(expr.getRightOperand(), "'"
								+ expr.getRightOperand() + "' must be of int type");
					}

					break;
				case LE:
				case LEQ:
				case GE:
				case GEQ:
					if (lhs == Type.INT && rhs == Type.INT) {
						myType = Type.BOOLEAN;
					}

					if (lhs != Type.INT) {
						addError(expr.getLeftOperand(), "'"
								+ expr.getLeftOperand() + "' must be of int type");
					}

					if (rhs != Type.INT) {
						addError(expr.getRightOperand(), "'"
								+ expr.getRightOperand() + "' must be of int type");
					}

					break;
				case NEQ: // int or boolean (same type though)
				case CEQ:
					if (lhs != rhs) {
						addError(expr.getLeftOperand(), "'"
								+ expr.getLeftOperand() + "' and '"
								+ expr.getRightOperand() + "' must be of same type");
					}
					else if (lhs.isArray()) {
						addError(expr.getLeftOperand(), "'"
								+ expr.getLeftOperand() + "' cant be an array");
					}
					else if (rhs.isArray()) {
						addError(expr.getRightOperand(), "'"
								+ expr.getRightOperand() + "' cant be an array");
					}
					else {
						myType = Type.BOOLEAN;
					}
					break;
			}
		}

		expr.setType(myType);

		return myType;
	}

	@Override
	public Type visit(Block block) {
		currentScope = classDesc.getScopeTable().get(block.getBlockId());

		for (VarDecl vd : block.getVarDeclarations()) {
			vd.accept(this);
		}
		for (Statement s : block.getStatements()) {
			s.accept(this);
		}

		return Type.UNDEFINED;
	}

	@Override
	public Type visit(BooleanLiteral lit) {
		return Type.BOOLEAN;
	}

	@Override
	public Type visit(BreakStmt stmt) {
		return Type.UNDEFINED;
	}

	@Override
	public Type visit(CalloutArg arg) {
		if (!arg.isString()) {
			return arg.getExpression().accept(this);
		}

		return Type.UNDEFINED;
	}

	@Override
	public Type visit(CalloutExpr expr) {
		for (CalloutArg arg : expr.getArguments()) {
			arg.accept(this);
		}

		expr.setType(Type.UNDEFINED);

		return Type.UNDEFINED;
	}

	@Override
	public Type visit(CharLiteral lit) {
		return Type.CHAR;
	}

	@Override
	public Type visit(ClassDecl cd) {
		for (FieldDecl fd : cd.getFieldDeclarations()) {
			fd.accept(this);
		}

		for (MethodDecl md : cd.getMethodDeclarations()) {
			md.accept(this);
		}

		return Type.UNDEFINED;
	}

	@Override
	public Type visit(ContinueStmt stmt) {
		return Type.UNDEFINED;
	}

	@Override
	public Type visit(Field f) {
		return f.getType();
	}

	@Override
	public Type visit(FieldDecl fd) {
		for (Field f : fd.getFields()) {
			f.accept(this);
		}

		return fd.getType();
	}

	@Override
	public Type visit(ForStmt stmt) {
		GenericDescriptor desc = getDescriptorFromScope(stmt.getId());

		Type idType = Type.UNDEFINED;

		if (desc != null) {
			idType = desc.getType();
		}

		if (idType != Type.INT && idType != Type.UNDEFINED) {
			addError(stmt, "Loop variable '" + stmt.getId()
					+ "' must be of type int");
		}

		if (stmt.getInitialValue().accept(this) != Type.INT) {
			addError(stmt, "'" + stmt.getInitialValue() + "' must be of int type");
		}
		if (stmt.getFinalValue().accept(this) != Type.INT) {
			addError(stmt, "'" + stmt.getInitialValue() + "' must be of int type");
		}

		stmt.getBlock().accept(this);

		return Type.UNDEFINED;
	}

	@Override
	public Type visit(IfStmt stmt) {
		Type exprType = stmt.getCondition().accept(this);

		if (exprType != Type.BOOLEAN) {
			addError(stmt, "'" + stmt.getCondition() + "' must be of boolean type");
		}

		stmt.getIfBlock().accept(this);

		if (stmt.getElseBlock() != null) {
			stmt.getElseBlock().accept(this);
		}

		return Type.UNDEFINED;
	}

	@Override
	public Type visit(IntLiteral lit) {
		return Type.INT;
	}

	@Override
	public Type visit(InvokeStmt stmt) {
		stmt.getMethodCall().accept(this);

		return Type.UNDEFINED;
	}

	@Override
	public Type visit(MethodCallExpr expr) {
		for (Expression e : expr.getArguments()) {
			e.accept(this);
		}

		expr.setType(classDesc.getMethodSymbolTable().get(expr.getName())
				.getReturnType());

		return expr.getType();
	}

	@Override
	public Type visit(MethodDecl md) {
		md.getBlock().accept(this);

		for (Parameter p : md.getParameters()) {
			p.accept(this);
		}

		return md.getReturnType();
	}

	@Override
	public Type visit(Parameter param) {
		return param.getType();
	}

	@Override
	public Type visit(ReturnStmt stmt) {
		if (stmt.getExpression() == null) {
			return Type.VOID;
		} else {
			return stmt.getExpression().accept(this);
		}
	}

	@Override
	public Type visit(UnaryOpExpr expr) {
		Type t = expr.getExpression().accept(this);
		Type myType = Type.UNDEFINED;

		if (t != Type.UNDEFINED) {
			if (t.isArray()) {
				addError(expr.getExpression(), "'" + expr.getExpression()
						+ "' cannot be an array");
			}
			else if (expr.getOperator() == UnaryOpType.NOT) {
				if (t != Type.BOOLEAN) {
					addError(expr, "'" + expr.getExpression()
							+ "' must be of boolean type");
				} else {
					myType = Type.BOOLEAN;
				}
			} else {
				if (t != Type.INT) {
					addError(expr, "'" + expr.getExpression()
							+ "' must be of int type");
				} else {
					myType = Type.INT;
				}
			}
		}

		expr.setType(myType);

		return myType;
	}

	@Override
	public Type visit(VarDecl vd) {
		return vd.getType();
	}

	@Override
	public Type visit(VarLocation loc) {
		GenericDescriptor desc = getDescriptorFromScope(loc.getId());

		Type myType = Type.UNDEFINED;

		if (desc != null) {
			myType = desc.getType();
		}

		loc.setType(myType);

		return myType;
	}

	private GenericDescriptor getDescriptorFromScope(String id) {
		GenericSymbolTable scope = currentScope;

		while (scope != null) {
			if (scope.containsKey(id)) {
				return scope.get(id);
			}

			scope = scope.getParent();
		}

		return null;
	}

	private void addError(AST a, String desc) {
		errors.add(new Error(a.getLineNumber(), a.getColumnNumber(), desc));
	}

	public List<Error> getErrors() {
		return errors;
	}

	public void setErrors(List<Error> errors) {
		this.errors = errors;
	}
}
