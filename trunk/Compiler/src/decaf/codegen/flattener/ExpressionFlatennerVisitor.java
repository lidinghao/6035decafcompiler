package decaf.codegen.flattener;

import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.Constant;
import decaf.codegen.flatir.JumpCondOp;
import decaf.codegen.flatir.JumpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
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
	private String methodName;
	private int andCount;
	private int orCount;
	private int currentAndId;
	private int currentOrId;
	
	
	public ExpressionFlatennerVisitor(List<LIRStatement> statements, String methodName) {
		this.statements = statements;
		this.methodName = methodName;
		this.andCount = 0;
		this.orCount = 0;
		this.currentAndId = 0;
		this.currentOrId = 0;
	}

	@Override
	public Name visit(ArrayLocation loc) {
		String id = loc.getId();
		Name index = loc.getExpr().accept(this);
		ArrayName arrayName = new ArrayName(id, index);
		return arrayName;
	}

	@Override
	public Name visit(AssignStmt stmt) {
		return null;
	}

	@Override
	public Name visit(BinOpExpr expr) {
		BinOpType op = expr.getOperator();
		
		if (op == BinOpType.AND) {
			return shortCircuitAnd(expr);
		}
		
		if (op == BinOpType.OR) {
			return shortCircuitOr(expr);
		}
		
		Name arg1 = expr.getLeftOperand().accept(this);
		Name arg2 = expr.getRightOperand().accept(this);
		
		QuadrupletOp qOp = null;
		switch(op) {
			case PLUS:
				qOp = QuadrupletOp.ADD;
				break;
			case MINUS:
				qOp = QuadrupletOp.SUB;
				break;
			case MULTIPLY:
				qOp = QuadrupletOp.MUL;
				break;
			case DIVIDE:
				qOp = QuadrupletOp.DIV;
				break;
			case MOD:
				qOp = QuadrupletOp.MOD;
				break;
			case LE:
				qOp = QuadrupletOp.LT;
				break;
			case LEQ:
				qOp = QuadrupletOp.LTE;
				break;
			case GE:
				qOp = QuadrupletOp.GT;
				break;
			case GEQ:
				qOp = QuadrupletOp.GTE;
				break;
			case CEQ:
				qOp = QuadrupletOp.EQ;
				break;
			case NEQ:
				qOp = QuadrupletOp.NEQ;
				break;
		}
		
		TempName dest = new TempName();
		QuadrupletStmt qStmt = new QuadrupletStmt(qOp, dest, arg1, arg2);
		this.statements.add(qStmt);
		
		return dest;
	}

	@Override
	public Name visit(Block block) {
		return null;
	}

	@Override
	public Name visit(BooleanLiteral lit) {
		return new Constant(lit.getValue());
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
		return new Constant(lit.getValue());
	}

	@Override
	public Name visit(InvokeStmt stmt) {
		return null;
	}

	@Override
	public Name visit(MethodCallExpr expr) {
		// TODO: Implement Method Call Expr
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
	
	private Name shortCircuitAnd(BinOpExpr expr) {
		int oldAndId = currentAndId;
		currentAndId = ++andCount;
		
		Name dest = new TempName();
		
		// Test LHS
		this.statements.add(new LabelStmt(getAndTestLHS()));
		Name lhs = expr.getLeftOperand().accept(this);
		this.statements.add(new QuadrupletStmt(QuadrupletOp.CMP, null, lhs, new Constant(0)));
		this.statements.add(new JumpStmt(JumpCondOp.NEQ, new LabelStmt(getAndTestRHS())));
		this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, dest, new Constant(0), null));
		this.statements.add(new JumpStmt(JumpCondOp.NONE, new LabelStmt(getAndEnd())));
		
		// Test RHS
		this.statements.add(new LabelStmt(getAndTestRHS()));
		Name rhs = expr.getRightOperand().accept(this);
		this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, dest, rhs, null));
		
		// End block
		this.statements.add(new LabelStmt(getAndEnd()));
		
		currentAndId = oldAndId;
		
		return dest;
	}

	private Name shortCircuitOr(BinOpExpr expr) {
		int oldOrId = currentOrId;
		currentOrId = ++orCount;
		
		Name dest = new TempName();
		
		// Test LHS
		this.statements.add(new LabelStmt(getOrTestLHS()));
		Name lhs = expr.getLeftOperand().accept(this);
		this.statements.add(new QuadrupletStmt(QuadrupletOp.CMP, null, lhs, new Constant(0)));
		this.statements.add(new JumpStmt(JumpCondOp.EQ, new LabelStmt(getOrTestRHS())));
		this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, dest, new Constant(1), null));
		this.statements.add(new JumpStmt(JumpCondOp.NONE, new LabelStmt(getOrEnd())));
		
		// Test RHS
		this.statements.add(new LabelStmt(getOrTestRHS()));
		Name rhs = expr.getRightOperand().accept(this);
		this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, dest, rhs, null));
		
		// End block
		this.statements.add(new LabelStmt(getOrEnd()));
		
		currentOrId = oldOrId;
		
		return dest;
	}
	
	private String getAndTestLHS() {
		return methodName + "_and" + currentAndId + "_testLHS";
	}
	
	private String getAndTestRHS() {
		return methodName + "_and" + currentAndId + "_testRHS";
	}
	
	private String getAndEnd() {
		return methodName + "_and" + currentAndId + "_end";
	}
	
	private String getOrTestLHS() {
		return methodName + "_or" + currentOrId + "_testLHS";
	}
	
	private String getOrTestRHS() {
		return methodName + "_or" + currentOrId + "_testRHS";
	}
	
	private String getOrEnd() {
		return methodName + "_or" + currentOrId + "_end";
	}
}
