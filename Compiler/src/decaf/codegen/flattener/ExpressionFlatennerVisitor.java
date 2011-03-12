package decaf.codegen.flattener;

import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.Constant;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.TempName;
import decaf.codegen.flatir.VarName;
import decaf.ir.ASTVisitor;
import decaf.ir.ast.ArrayLocation;
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
import decaf.ir.ast.UnaryOpType;
import decaf.ir.ast.VarDecl;
import decaf.ir.ast.VarLocation;

public class ExpressionFlatennerVisitor implements ASTVisitor<Name> {
	private List<LIRStatement> statements;
	
	
	public ExpressionFlatennerVisitor(List<LIRStatement> statements) {
		this.statements = statements;
	}

	@Override
	public Name visit(ArrayLocation loc) {
		String id = loc.getId();
		Name index = loc.getExpr().accept(this);
		ArrayName arrayName = new ArrayName(index, id);
		return arrayName;
	}

	@Override
	public Name visit(AssignStmt stmt) {
		return null;
	}

	@Override
	public Name visit(BinOpExpr expr) {
		Name arg1 = expr.getLeftOperand().accept(this);
		BinOpType op = expr.getOperator();
		// shortcircuit hack
		if (op == BinOpType.AND) {
			if(((Constant) arg1).getValue() == 0 ) {
				Constant constant = new Constant(0);
				return constant;
			} else {
				Name arg2 = expr.getRightOperand().accept(this);
				if(((Constant) arg2).getValue() == 1) {
					Constant constant = new Constant(1);
					return constant;
				} else {
					Constant constant = new Constant(0);
					return constant;
				}
			}
		} else if (op == BinOpType.OR) {
			if(((Constant) arg1).getValue() == 1) {
				Constant constant = new Constant(1);
				return constant;
			} else {
				Name arg2 = expr.getRightOperand().accept(this);
				if(((Constant) arg2).getValue() == 1) {
					Constant constant = new Constant(1);
					return constant;
				} else {
					Constant constant = new Constant(0);
					return constant;
				}
			}
			
		} else {
			Name arg2 = expr.getRightOperand().accept(this);
			QuadrupletOp qOp = null;
			switch(op) {
				case PLUS:
					qOp = QuadrupletOp.ADD;
				case MINUS:
					qOp = QuadrupletOp.SUB;
				case MULTIPLY:
					qOp = QuadrupletOp.MUL;
				case DIVIDE:
					qOp = QuadrupletOp.DIV;
				case MOD:
					qOp = QuadrupletOp.MOD;
				case LE:
					qOp = QuadrupletOp.LT;
				case LEQ:
					qOp = QuadrupletOp.LTE;
				case GE:
					qOp = QuadrupletOp.GT;
				case GEQ:
					qOp = QuadrupletOp.GTE;
				case CEQ:
					qOp = QuadrupletOp.EQ;
				case NEQ:
					qOp = QuadrupletOp.NEQ;
			}
			TempName dest = new TempName();
			QuadrupletStmt qStmt = new QuadrupletStmt(qOp, dest, arg1, arg2);
			this.statements.add(qStmt);
			return dest;
		}
	}

	@Override
	public Name visit(Block block) {
		return null;
	}

	@Override
	public Name visit(BooleanLiteral lit) {
		int value = lit.getValue();
		Constant constant = new Constant(value);
		return constant;
	}

	@Override
	public Name visit(BreakStmt stmt) {
		return null;
	}

	@Override
	public Name visit(CalloutArg arg) {
		return null;
	}

	@Override
	public Name visit(CalloutExpr expr) {
		return null;
	}

	@Override
	public Name visit(CharLiteral lit) {
		int value = (int) lit.getValue().charAt(0);
		Constant constant = new Constant(value);
		return constant;
	}

	@Override
	public Name visit(ClassDecl cd) {
		return null;
	}

	@Override
	public Name visit(ContinueStmt stmt) {
		return null;
	}

	@Override
	public Name visit(Field f) {
		return null;
	}

	@Override
	public Name visit(FieldDecl fd) {
		return null;
	}

	@Override
	public Name visit(ForStmt stmt) {
		return null;
	}

	@Override
	public Name visit(IfStmt stmt) {
		return null;
	}

	@Override
	public Name visit(IntLiteral lit) {
		int value = lit.getValue();
		Constant constant = new Constant(value);
		return constant;
	}

	@Override
	public Name visit(InvokeStmt stmt) {
		return null;
	}

	@Override
	public Name visit(MethodCallExpr expr) {
		return null;
	}

	@Override
	public Name visit(MethodDecl md) {
		return null;
	}

	@Override
	public Name visit(Parameter param) {
		return null;
	}

	@Override
	public Name visit(ReturnStmt stmt) {
		return null;
	}

	@Override
	public Name visit(UnaryOpExpr expr) {
		Name arg1 = expr.getExpression().accept(this);
		UnaryOpType op = expr.getOperator();
		QuadrupletOp qOp = null;
		switch(op) {
			case NOT:
				qOp = QuadrupletOp.NOT;
			case MINUS:
				qOp = QuadrupletOp.MINUS;
		}
		TempName dest = new TempName();
		QuadrupletStmt qStmt = new QuadrupletStmt(qOp, dest, arg1, null);
		this.statements.add(qStmt);
		return dest;
	}

	@Override
	public Name visit(VarDecl vd) {
		return null;
	}

	@Override
	public Name visit(VarLocation loc) {
		String id = loc.getId();
		VarName varName = new VarName(id);
		return varName;
	}
}
