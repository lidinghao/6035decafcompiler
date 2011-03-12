package decaf.codegen.flattener;

import java.util.ArrayList;
import java.util.List;

import decaf.codegen.flatir.JumpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
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
import decaf.ir.ast.Statement;
import decaf.ir.ast.UnaryOpExpr;
import decaf.ir.ast.VarDecl;
import decaf.ir.ast.VarLocation;

public class StatementFlatennerVisitor implements ASTVisitor<Integer> {
	private class StmtFlatennerState {
		private int ifCount;
		private int forCount;
		private String methodName;
		
		public StmtFlatennerState(String methodName) {
			this.ifCount = 0;
			this.forCount = 0;
			this.methodName = methodName;
		}
		
		public void incrementIf() {
			this.ifCount++;
		}
		
		public void incrementFor() {
			this.forCount++;
		}

		public int getIfCount() {
			return ifCount;
		}

		public int getForCount() {
			return forCount;
		}
	}
	private List<LIRStatement> statements;
	private ExpressionFlatennerVisitor exprFlatenner;
	private StmtFlatennerState currentState;
	
	public StatementFlatennerVisitor(String methodName) {
		this.statements = new ArrayList<LIRStatement>();
		this.exprFlatenner = new ExpressionFlatennerVisitor(statements);
		this.currentState = new StmtFlatennerState(methodName);
	}

	@Override
	public Integer visit(ArrayLocation loc) {
		return 0;
	}

	@Override
	public Integer visit(AssignStmt stmt) {
		Name dest = stmt.getLocation().accept(exprFlatenner);
		Name src = stmt.getExpression().accept(exprFlatenner);
		
		this.statements.add(new QuadrupletStmt(QuadrupletOp.EQ, dest, src, null));
		
		return 0;
	}

	@Override
	public Integer visit(BinOpExpr expr) {
		return 0;
	}

	@Override
	public Integer visit(Block block) {
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
		// Can only occur in for
		
		statements.add(new JumpStmt(null, null));
		
		return 0;
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

}
