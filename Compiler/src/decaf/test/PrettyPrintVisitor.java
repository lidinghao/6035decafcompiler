package decaf.test;

import java.io.PrintStream;

import decaf.ir.ASTVisitor;
import decaf.ir.ast.*;

public class PrettyPrintVisitor implements ASTVisitor<Integer> {
	int tabSize;
	PrintStream out;
	
	public PrettyPrintVisitor() {
		tabSize = 0;
		out = System.out;
	}
	
	public PrettyPrintVisitor(PrintStream ps) {
		tabSize = 0;
		out = ps;
	}

	@Override
	public Integer visit(ArrayLocation loc) {
		newLineAndIndent();
		out.print(loc.getId() + "[]");
		
		indent();
		loc.getExpr().accept(this); // Print index expression
		unindent();
		
		return 0;
	}

	@Override
	public Integer visit(AssignStmt stmt) {
		indent();
		stmt.getLocation().accept(this);
		unindent();
		
		newLineAndIndent();
		out.print(stmt.getOperator().toString());
		
		indent();	
		stmt.getExrpression().accept(this);
		unindent();
		
		return 0;
	}

	@Override
	public Integer visit(BinOpExpr expr) {
		indent();
		expr.getLeftOperand().accept(this);
		unindent();
		
		newLineAndIndent();
		out.print(expr.getOperator().toString());
		
		indent();		
		expr.getRightOperand().accept(this);
		unindent();
		
		return 0;
	}

	@Override
	public Integer visit(Block block) {
		indent();
		
		for (Statement s: block.getStatements()) {
			s.accept(this);
		}
		
		unindent();
		
		return 0;
	}

	@Override
	public Integer visit(BooleanLiteral lit) {
		newLineAndIndent();
		out.print(lit.toString());
		
		return 0;
	}

	@Override
	public Integer visit(BreakStmt stmt) {
		newLineAndIndent();
		out.print("break");
		
		return 0;
		
	}

	@Override
	public Integer visit(CalloutArg arg) {
		if (arg.isString()) {
			newLineAndIndent();
			out.print(arg.getStringArg());
		}
		else {
			arg.getExpression().accept(this);
		}
		
		return 0;
	}

	@Override
	public Integer visit(CalloutExpr expr) {
		// Print args
		indent();
		
		for (CalloutArg arg: expr.getArgs()) {
			arg.accept(this);
		}
		
		unindent();
		
		newLineAndIndent();
		out.print("callout");
		
		indent();
		newLineAndIndent();
		out.print(expr.getMethodName());
		unindent();
		
		return 0;
	}

	@Override
	public Integer visit(CharLiteral lit) {
		newLineAndIndent();
		out.print(lit.getValue());
		
		return 0;
	}

	@Override
	public Integer visit(ClassDecl cd) {
		out.print("CLASS");
		
		indent();
		for (FieldDecl fd: cd.getFieldDeclarations()) {
			fd.accept(this);
		}
		
		unindent();
		
		indent();
		for (MethodDecl md: cd.getMethodDeclarations()) {
			md.accept(this);
		}
		
		unindent();
		
		return 0;
	}

	@Override
	public Integer visit(ContinueStmt stmt) {
		newLineAndIndent();
		out.print("continue");
		
		return 0;
	}

	@Override
	public Integer visit(Field f) {
		newLineAndIndent();
		
		if (f.isArray()) {
			out.print(f.getId() + "[" + f.getRawLength() + "]");
		}
		else {
			out.print(f.getId());
		}
		
		return 0;
	}

	@Override
	public Integer visit(FieldDecl fd) {
		newLineAndIndent();
		out.print(fd.getType().toString());
		
		indent();
		
		for (Field f: fd.getFields()) {
			f.accept(this);
		}
		
		unindent();
		
		return 0;
	}

	@Override
	public Integer visit(ForStmt stmt) {
		indent();
		
		newLineAndIndent();
		out.print(stmt.getId());
		
		indent();
		stmt.getInitialValue().accept(this);
		stmt.getFinalValue().accept(this);
		unindent();
		
		newLineAndIndent();
		out.print("for");
		
		stmt.getBlock().accept(this); // Block auto indents
		
		return 0;
		
	}

	@Override
	public Integer visit(IfStmt stmt) {
		indent();
		stmt.getCondition().accept(this);
		unindent();
		
		newLineAndIndent();
		out.print("if");
		
		stmt.getIfBlock().accept(this);
		
		if (stmt.getElseBlock() != null) {
			indent();
			out.print("else");
			stmt.getElseBlock().accept(this);
			unindent();
		}
		
		return 0;
	}

	@Override
	public Integer visit(IntLiteral lit) {
		newLineAndIndent();
		out.print(lit.getRawValue());
		
		return 0;
	}

	@Override
	public Integer visit(InvokeStmt stmt) {
		stmt.getMethodCall().accept(this);
		
		return 0;
	}

	@Override
	public Integer visit(MethodCallExpr expr) {
		newLineAndIndent();
		out.print(expr.getName());
		
		indent();
		for (Expression arg: expr.getArgs()) {
			arg.accept(this);
		}
		
		unindent();
		
		return 0;
	}

	@Override
	public Integer visit(MethodDecl md) {
		newLineAndIndent();
		out.print(md.getId());
		
		indent();
		for (Parameter p: md.getParamters()) {
			p.accept(this);
		}
		
		return 0;
	}

	@Override
	public Integer visit(Parameter param) {
		newLineAndIndent();
		out.print(param.getType().toString());
		
		indent();
		out.print(param.getId());
		unindent();
		
		return 0;
	}

	@Override
	public Integer visit(ReturnStmt stmt) {
		newLineAndIndent();
		out.print("return");			

		if (stmt.getExpression() != null) {
			indent();
			stmt.getExpression().accept(this);
			unindent();
		}
		
		return 0;
	}

	@Override
	public Integer visit(UnaryOpExpr expr) {
		newLineAndIndent();
		out.print(expr.getOperator().toString());
		
		indent();
		expr.getExpression().accept(this);
		unindent();
		
		return 0;
	}

	@Override
	public Integer visit(VarDecl vd) {
		newLineAndIndent();
		out.print(vd.getType().toString());
		
		indent();
		for (String v: vd.getVariables()) {
			newLineAndIndent();
			out.print(v);
		}
		
		unindent();
		
		return 0;
	}

	@Override
	public Integer visit(VarLocation loc) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private String getIndent() {
		String rtn = "";
		
		for (int i = 0; i < tabSize; i++) {
			rtn += "  ";
		}
		
		return rtn;
	}
	
	private void newLine() {
		out.print('\n');
	}
	
	private void newLineAndIndent() {
		newLine();
		out.print(getIndent());
	}
	
	private void indent() {
		tabSize++;
	}
	
	private void unindent() {
		tabSize--;
	}

}
