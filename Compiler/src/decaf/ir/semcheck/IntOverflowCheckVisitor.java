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
import decaf.ir.ast.Type;
import decaf.ir.ast.UnaryOpExpr;
import decaf.ir.ast.UnaryOpType;
import decaf.ir.ast.VarDecl;
import decaf.ir.ast.VarLocation;


/*
 * This Visitor class checks if input int is valid i.e. in the right range and has a right value
 */
public class IntOverflowCheckVisitor implements ASTVisitor<Integer>{
	private ArrayList<Error> errors;
	
	public IntOverflowCheckVisitor() {
		this.errors = new ArrayList<Error>();
	}

	@Override
	public Integer visit(ArrayLocation loc) {
		loc.getExpr().accept(this); // Print index expression
		return 0;
	}

	@Override
	public Integer visit(AssignStmt stmt) {
		stmt.getLocation().accept(this);
		stmt.getExpression().accept(this);
		return null;
	}

	@Override
	public Integer visit(BinOpExpr expr) {
		expr.getLeftOperand().accept(this);
		expr.getRightOperand().accept(this);	
		return 0;
}

	@Override
	public Integer visit(Block block) {
		List<Statement> stmts = block.getStatements();
		
		for (int i = 0; i < stmts.size(); i++) {
			stmts.get(i).accept(this);
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
		return 0;
		}

	@Override
	public Integer visit(CalloutExpr expr) {
		return 0;
	}

	@Override
	public Integer visit(CharLiteral lit) {
		return 0;
		}

	@Override
	public Integer visit(ClassDecl cd) {
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
		stmt.getInitialValue().accept(this);
		stmt.getFinalValue().accept(this);
		stmt.getBlock().accept(this); // Block auto indents
		
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

	private Boolean isHex(String intStr){
		if (intStr.length() < 2)
			return false;
		return intStr.startsWith("0x");
	}
	
	 public double hex2decimal(String s) {
	        String digits = "0123456789ABCDEF";
	        s = s.toUpperCase();
	        double val = 0 ;
	        for (int i = 0; i < s.length(); i++) {
	            char c = s.charAt(i);
	            int d = digits.indexOf(c);
	            val = 16*val + d;
	        }
	        return val;
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
	
	private double getDoubleValue(String lit){
		double interm;
		String rawValue = lit;
		if(isHex(rawValue)){
			 interm = hex2decimal(rawValue.substring(2, rawValue.length()-1));
		}
		else{
			interm = Double.parseDouble(rawValue);
		}
		return interm;
	}
	
	@Override
	public Integer visit(IntLiteral lit) {
		double interm = getDoubleValue(lit.getRawValue());
		if(interm <= 2147483647){ //does not cover the case of -2147483648	
			lit.setValue((int) interm);
		}else{
			String msg = "Int is out of range";
			Error err = new Error(lit.getLineNumber(), lit.getColumnNumber(), msg );
			this.errors.add(err);
		}
		return 0;
	}

	@Override
	public Integer visit(UnaryOpExpr expr) {
		if(expr.getExpression().getClass() == IntLiteral.class && expr.getOperator() == UnaryOpType.MINUS){			
			IntLiteral intLit = (IntLiteral) expr.getExpression();
			double interm = getDoubleValue(intLit.getRawValue());
			double range = 2147483647;
			//System.out.println("SHIT");
			range++; //make it 2147483648
			if(interm <= range){ 
				intLit.setValue((int) interm);
			}else{
				String msg = "Int is out of range";
				Error err = new Error(intLit.getLineNumber(), intLit.getColumnNumber(), msg );
				this.errors.add(err);
			}
		}else{
			expr.getExpression().accept(this);
		}
		return null;
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
