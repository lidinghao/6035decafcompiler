package decaf.ir.semcheck;

import java.util.ArrayList;
import java.util.List;
import decaf.test.Error;

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
import decaf.ir.ast.UnaryOpType;
import decaf.ir.ast.VarDecl;
import decaf.ir.ast.VarLocation;


/*
 * This Visitor class checks if input int is valid i.e. in the right range and has a right value
 * Returns true if expression is unary and needs to be replaced
 */
public class IntOverflowCheckVisitor implements ASTVisitor<Boolean>{
	private ArrayList<Error> errors;
	private boolean inUnaryMinus;
	
	public IntOverflowCheckVisitor() {
		this.errors = new ArrayList<Error>();
		inUnaryMinus = false;
	}

	@Override
	public Boolean visit(ArrayLocation loc) {
		if (loc.getExpr().accept(this)) {
			loc.setExpr(getNegativeIntLiteral(loc.getExpr()));
		}
		return false;
	}

	@Override
	public Boolean visit(AssignStmt stmt) {
		stmt.getLocation().accept(this);
		if (stmt.getExpression().accept(this)) {
			stmt.setExpression(getNegativeIntLiteral(stmt.getExpression()));
		}
		return false;
	}

	@Override
	public Boolean visit(BinOpExpr expr) {
		if (expr.getLeftOperand().accept(this)) {
			expr.setLeftOperand(getNegativeIntLiteral(expr.getLeftOperand()));
		}
		if (expr.getRightOperand().accept(this)) {
			expr.setRightOperand(getNegativeIntLiteral(expr.getRightOperand()));;	
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
				arg.setExpression(getNegativeIntLiteral(arg.getExpression()));
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
			stmt.setInitialValue(getNegativeIntLiteral(stmt.getInitialValue()));
		}
		
		if (stmt.getFinalValue().accept(this)) {
			stmt.setFinalValue(getNegativeIntLiteral(stmt.getFinalValue()));
		}
		
		stmt.getBlock().accept(this); // Block auto indents
		
		return false;
	}

	@Override
	public Boolean visit(IfStmt stmt) {
		if (stmt.getCondition().accept(this)) {
			stmt.setCondition(getNegativeIntLiteral(stmt.getCondition()));
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
				expr.getArguments().set(i, getNegativeIntLiteral(expr.getArguments().get(i)));
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
				stmt.setExpression(getNegativeIntLiteral(stmt.getExpression()));
			}
		}
		return false;
	}
	
	@Override
	public Boolean visit(IntLiteral lit) {
		String rawValue = lit.getRawValue();
		
		if (lit.getValue() == null) { // Checking int literal for first time
			int value = -1;
			boolean isHex = false;
			
			if (isHex(rawValue)) { // Check for hex string
				rawValue = rawValue.substring(2); // Remove '0x'
				isHex = true;
			}
			
			if (inUnaryMinus) {
					rawValue = "-" + rawValue;
					lit.setRawValue("-" + lit.getRawValue());
			}
			
			try {
				if (isHex) {
					value = Integer.parseInt(rawValue, 16);
				}
				else {
					value = Integer.parseInt(rawValue);
				}
			}
			catch (Exception e) {
				String msg = "Int literal " + lit.getRawValue() + " is out of range";
				Error err = new Error(lit.getLineNumber(), lit.getColumnNumber(), msg);
				this.errors.add(err);
			}
			
			lit.setValue(value);
			lit.setRawValue(Integer.toString(value));
		}
		else {
			if (inUnaryMinus) {
				if (lit.getValue() < 0) { // Fix sign in raw format
					lit.setRawValue(rawValue.substring(1));
				}
				else {
					lit.setRawValue("-" + rawValue);
				}
				
				if (lit.getValue() == -2147483648) {
					String msg = "Int literal " + lit.getRawValue() + " is out of range";
					Error err = new Error(lit.getLineNumber(), lit.getColumnNumber(), msg);
					this.errors.add(err);
				}
				else {
					lit.setValue(lit.getValue() * -1);
				}
			}
		}
		
		return false;
	}

	@Override
	public Boolean visit(UnaryOpExpr expr) {
		if (expr.getOperator() == UnaryOpType.MINUS && expr.getExpression().getClass() == IntLiteral.class) {
			inUnaryMinus = true;
			expr.getExpression().accept(this);
			inUnaryMinus = false;
			return true;
		}
		else {
			if (expr.getExpression().accept(this)) {
				expr.setExpression(getNegativeIntLiteral(expr.getExpression()));
				return expr.accept(this);
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
	
	public ArrayList<Error> getErrors() {
		return errors;
	}
	
	private Boolean isHex(String intStr){
		if (intStr.length() < 2)
			return false;
		return intStr.startsWith("0x");
	}
	 
	 private Expression getNegativeIntLiteral(Expression e) {
		 UnaryOpExpr expr = (UnaryOpExpr) e;
		 return expr.getExpression();
	 }
}
