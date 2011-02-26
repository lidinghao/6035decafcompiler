package decaf.ir;

import decaf.ir.ast.*;

public interface ASTVisitor<T> {
	T visit(ArrayLocation loc);
	T visit(AssignStmt stmt);
	T visit(BinOpExpr expr);
	T visit(Block block);
	T visit(BooleanLiteral lit);
	T visit(BreakStmt stmt);
	T visit(CalloutArg arg);
	T visit(CalloutExpr expr);
	T visit(CharLiteral lit);
	T visit(ClassDecl cd);
	T visit(ContinueStmt stmt);
	T visit(Field f);
	T visit(FieldDecl fd);
	T visit(ForStmt stmt);
	T visit(IfStmt stmt);
	T visit(IntLiteral lit);
	T visit(InvokeStmt stmt);
	T visit(MethodCallExpr expr);
	T visit(MethodDecl md);
	T visit(Parameter param);
	T visit(ReturnStmt stmt);
	T visit(UnaryOpExpr expr);
	T visit(VarDecl vd);
	T visit(VarLocation loc);
}
