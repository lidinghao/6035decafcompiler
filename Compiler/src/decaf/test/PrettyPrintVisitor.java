package decaf.test;

import java.io.PrintStream;
import java.util.List;

import decaf.ir.ASTVisitor;
import decaf.ir.ast.*;

public class PrettyPrintVisitor implements ASTVisitor<Integer> {
	public static String INDENT = "   ";
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
		dedent();
		
		return 0;
	}

	@Override
	public Integer visit(AssignStmt stmt) {
		indent();
		stmt.getLocation().accept(this);
		dedent();
		
		newLineAndIndent();
		out.print(stmt.getOperator().toString());
		
		indent();	
		stmt.getExrpression().accept(this);
		dedent();
		
		return 0;
	}

	@Override
	public Integer visit(BinOpExpr expr) {
		indent();
		expr.getLeftOperand().accept(this);
		dedent();
		
		newLineAndIndent();
		out.print(expr.getOperator().toString());
		
		indent();		
		expr.getRightOperand().accept(this);
		dedent();
		
		return 0;
	}

	@Override
	public Integer visit(Block block) {
		indent();
		
		List<FieldDecl> fDecls = block.getFieldDeclarations();
		
		for (int i = 0; i < fDecls.size(); i++) {
			fDecls.get(i).accept(this);
			if (i < fDecls.size() - 1) {
				newLine();
			}
		}
		
		List<Statement> stmts = block.getStatements();
		
		if (fDecls.size() > 0 && stmts.size() > 0) {
			newLine();
		}
		
		for (int i = 0; i < stmts.size(); i++) {
			stmts.get(i).accept(this);
			if (i < stmts.size() - 1) {
				newLine();
			}
		}
		
		dedent();
		
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
		
		dedent();
		
		newLineAndIndent();
		out.print("callout");
		
		indent();
		newLineAndIndent();
		out.print(expr.getMethodName());
		dedent();
		
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
			newLine();
		}
		
		dedent();
		
		indent();
		for (MethodDecl md: cd.getMethodDeclarations()) {
			md.accept(this);
			newLine();
		}
		
		dedent();
		
		newLine();
		
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
			out.print(f.getId() + "[" + f.getArrayLength() + "]");
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
		
		dedent();
		
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
		dedent();
		
		newLineAndIndent();
		out.print("for");
		
		stmt.getBlock().accept(this); // Block auto indents
		
		return 0;
		
	}

	@Override
	public Integer visit(IfStmt stmt) {
		indent();
		stmt.getCondition().accept(this);
		dedent();
		
		newLineAndIndent();
		out.print("if");
		
		stmt.getIfBlock().accept(this);
		
		if (stmt.getElseBlock() != null) {
			indent();
			newLineAndIndent();
			out.print("else");
			stmt.getElseBlock().accept(this);
			dedent();
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
		out.print(expr.getName() + "()");
		
		indent();
		for (Expression arg: expr.getArgs()) {
			arg.accept(this);
		}
		
		dedent();
		
		return 0;
	}

	@Override
	public Integer visit(MethodDecl md) {
		indent();
		for (Parameter p: md.getParamters()) {
			p.accept(this);
		}
		dedent();
		
		newLineAndIndent();
		out.print(md.getId() + "() [" + md.getReturnType() + "]");
		
		md.getBlock().accept(this);
		
		return 0;
	}

	@Override
	public Integer visit(Parameter param) {
		newLineAndIndent();
		out.print(param.getType().toString());
		
		indent();
		newLineAndIndent();
		out.print(param.getId());
		dedent();
		
		return 0;
	}

	@Override
	public Integer visit(ReturnStmt stmt) {
		newLineAndIndent();
		out.print("return");			

		if (stmt.getExpression() != null) {
			indent();
			stmt.getExpression().accept(this);
			dedent();
		}
		
		return 0;
	}

	@Override
	public Integer visit(UnaryOpExpr expr) {
		newLineAndIndent();
		out.print(expr.getOperator().toString());
		
		indent();
		expr.getExpression().accept(this);
		dedent();
		
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
		
		dedent();
		
		return 0;
	}

	@Override
	public Integer visit(VarLocation loc) {
		newLineAndIndent();
		out.print(loc.getId());
		
		return 0;
	}
	
	private String getIndent() {
		String rtn = "";
		
		for (int i = 0; i < tabSize; i++) {
			rtn += PrettyPrintVisitor.INDENT;
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
	
	private void dedent() {
		tabSize--;
	}

}
