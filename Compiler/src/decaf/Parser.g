header {
  	package decaf;
  	import decaf.ir.ast.*;
  	import java.util.List;
  	import java.util.ArrayList;
}

options
{
  mangleLiteralPrefix = "TK_";
  language = "Java";
}

{ 
	class StringToken {
		private String str;
		private int lineNumber;
		private int colNumber;
		
		public StringToken(String s) {
			str = s;
		}
		
		public String getString() {
			return str;
		}
		
		public void setString(String str) {
			this.str = str;
		}
		
		public int getLineNumber() {
			return lineNumber;
		}
		
		public void setLineNumber(int ln) {
			lineNumber = ln;
		}
		
		public int getColumnNumber() {
			return colNumber;
		}
		
		public void setColumnNumber(int cn) {
			colNumber = cn;
		}
	}
	
	class BlockId {
		public static int blockId = 0;
	}
}

class DecafParser extends Parser;

options {
  importVocab = DecafScanner;
  k = 3;
  buildAST = true;
  defaultErrorHandler=false;
}

// Rename Tokens
id returns [StringToken s] { 
	s = null; 
} : 
(myid:ID 
{ 
	s = new StringToken(myid.getText()); 
	s.setLineNumber(myid.getLine());
	s.setColumnNumber(myid.getColumn());
}) ;

intLit returns [StringToken s] {
	s = null;	
} : 
(myint:INTLIT 
{ 
	s = new StringToken(myint.getText()); 
	s.setLineNumber(myint.getLine());
	s.setColumnNumber(myint.getColumn());
}) ;

charLit returns [StringToken s] {
	s = null;	
} : 
(c:CHAR 
{ 
	s = new StringToken(c.getText()); 
	s.setLineNumber(c.getLine());
	s.setColumnNumber(c.getColumn());
}) ;

str returns [StringToken s] {
	s = null;
} : 
(st:STRING 
{ 
	s = new StringToken(st.getText());
	s.setLineNumber(st.getLine());
	s.setColumnNumber(st.getColumn());
}) ;

// Program
program returns [ClassDecl classDecl] { 
	List<FieldDecl> fieldDecls = new ArrayList<FieldDecl>();
	List<MethodDecl> methodDecls = new ArrayList<MethodDecl>();
	classDecl = new ClassDecl(fieldDecls, methodDecls);
	FieldDecl f;
	MethodDecl m;
	StringToken className;
} : 
(cl:TK_class className=id LCURLY (f=field_decl { fieldDecls.add(f); })* (m=method_decl { methodDecls.add(m); })* RCURLY EOF) 
{
	classDecl.setLineNumber(cl.getLine());
	classDecl.setColumnNumber(cl.getColumn());
	System.out.println(className.getString());
	if (!className.getString().equals("Program")) {
		classDecl = null;
	}
} ;

// Field declaration group
field_decl_group returns [Field f] {
	StringToken i;
	StringToken l;
	f = null;
	IntLiteral il;
} : 
(i=id { 
	f = new Field(i.getString()); 
	f.setLineNumber(i.getLineNumber());
	f.setColumnNumber(i.getColumnNumber());
}) |
(i=id LSQUARE l=intLit RSQUARE { 
	il = new IntLiteral(l.getString());
	il.setLineNumber(l.getLineNumber());
	il.setColumnNumber(l.getColumnNumber());	
	f = new Field(i.getString(),il); 
	f.setLineNumber(i.getLineNumber());
	f.setColumnNumber(i.getColumnNumber());
}) ;

// Field declaration group (CSL)
field_decl_group_list returns [List<Field> fields] { 
	fields = new ArrayList<Field>(); 
	Field f;
} : 
(f=field_decl_group { fields.add(f); } (COMMA f=field_decl_group { fields.add(f); })*) ;

// Field declaration
field_decl returns [FieldDecl fDecl] { 
	Type t; 
	List<Field> fList;
	fDecl = null;
} :
(t=type fList=field_decl_group_list { fDecl = new FieldDecl(fList,t); } s:SEMI) 
{
	fDecl.setLineNumber(s.getLine());
	fDecl.setLineNumber(s.getColumn());
} ;

// Variable declaration
var_decl returns [VarDecl vDecl] {
	Type t;
	List<String> vars = new ArrayList<String>();
	vDecl = null;
	StringToken i1, i2;
} : 
(t=type { vDecl = new VarDecl(t, vars); } ((i1=id { vars.add(i1.getString()); } COMMA)* i2=id { 
	vars.add(i2.getString()); 
	vDecl.setLineNumber(i2.getLineNumber());
	vDecl.setColumnNumber(i2.getColumnNumber());
}) SEMI) ;

// Method declaration group
method_decl_group returns [Parameter p] {
	Type t;
	StringToken i;
	p = null;
} : 
(t=type i=id 
{ 
	p = new Parameter(t,i.getString());
	p.setLineNumber(i.getLineNumber());
	p.setColumnNumber(i.getColumnNumber());
}) ;

// Method declaration group CSL
method_decl_group_list returns [List<Parameter> parameters] { 
	parameters = new ArrayList<Parameter>(); 
	Parameter p;
} : 
(p=method_decl_group { parameters.add(p); } (COMMA p=method_decl_group { parameters.add(p); })*)? ;

// Method declaration
method_decl returns [MethodDecl mDecl] { 
	mDecl = new MethodDecl(); 
	Type rt;
	List<Parameter> params;
	Block b;
	StringToken i;
} :
((rt=type { mDecl.setReturnType(rt); }) | (TK_void { mDecl.setReturnType(Type.VOID); })) (i=id lp:LPAREN params=method_decl_group_list RPAREN b=block 
{
	mDecl.setId(i.getString());
	mDecl.setParameters(params);
	mDecl.setBlock(b);	
	mDecl.setLineNumber(lp.getLine());
	mDecl.setColumnNumber(lp.getColumn());
}) ;

// Type
type returns [Type t] {
	t = null;	
} : 
(TK_int { t = Type.INT; }) | 
(TK_boolean { t = Type.BOOLEAN; }) ;

// Boolean literal (true or false)
boolLiteral returns [BooleanLiteral bl] {
	bl = null;	
} : 
(t1:TK_true { 
	bl = new BooleanLiteral(t1.getText()); 
	bl.setLineNumber(t1.getLine());
	bl.setColumnNumber(t1.getColumn());
}) | 
(t2:TK_false { 
	bl = new BooleanLiteral(t2.getText()); 
	bl.setLineNumber(t2.getLine());
	bl.setColumnNumber(t2.getColumn());	
});

// Literal can be either int, char or boolean
// TODO add line numbers
literal returns [Literal lit] {
	StringToken l;	
	StringToken c;
	lit = null;
} : 
(l=intLit { 
	lit = new IntLiteral(l.getString()); 
	lit.setLineNumber(l.getLineNumber());
	lit.setColumnNumber(l.getColumnNumber());
}) | 
(c=charLit { 
	lit = new CharLiteral(c.getString()); 
	lit.setLineNumber(c.getLineNumber());
	lit.setColumnNumber(c.getColumnNumber()); 
}) | 
(lit=boolLiteral);

// Binary operator can be either add, sub, mul, div, mod
add_op returns [BinOpType bt] {
	bt = null;
} : 
(PLUS { bt = BinOpType.PLUS; } | 
MINUS { bt = BinOpType.MINUS; }) ; 

mul_op returns [BinOpType bt] {
	bt = null;
} : (m:MULDIV 
{ 
	if (m.getText().equals("*")) bt = BinOpType.MULTIPLY;
	else bt=BinOpType.DIVIDE;
} | MOD { bt = BinOpType.MOD; }) ;

// Relation operators
rel_op returns [BinOpType bt] {
	bt = null;	
} : 
(LESS { bt = BinOpType.LE; } | 
MORE { bt = BinOpType.GE; } | 
LEQ { bt = BinOpType.LEQ; } | 
GEQ { bt = BinOpType.GEQ; }) ;

// Equality operators
eq_op returns [BinOpType bt] {
	bt = null;	
} : 
(CEQ { bt = BinOpType.CEQ; } | 
NEQ { bt = BinOpType.NEQ; }) ;
	
// Assignment operators
assign_op returns [AssignOpType a] {
	a = null;
} : 
(ASSIGNPLUSEQ { a = AssignOpType.INCREMENT; } | 
ASSIGNMINUSEQ { a = AssignOpType.DECREMENT; } | 
ASSIGNEQ { a = AssignOpType.ASSIGN; });

// Location
location returns [Location loc] {
	Expression e;
	StringToken i;
	loc = null;
} : 
(i=id { 
	loc = new VarLocation(i.getString()); 
	loc.setLineNumber(i.getLineNumber());
	loc.setColumnNumber(i.getColumnNumber());
} | 
i=id LSQUARE e=expr RSQUARE { 
	loc = new ArrayLocation(i.getString(),e); 
	loc.setLineNumber(i.getLineNumber());
	loc.setColumnNumber(i.getColumnNumber());
});

// Expression argument CSL
expr_argument_list returns [List<Expression> exprs] { 
	exprs = new ArrayList<Expression>(); 
	Expression e;
} : 
(e=expr { exprs.add(e); } (COMMA e=expr { exprs.add(e); })*)? ;

// Callout argument
callout_argument returns [CalloutArg arg] {
	Expression e;
	StringToken s;
	arg = null;
} : 
(e=expr 
{ 
	arg = new CalloutArg(e); 
	arg.setLineNumber(e.getLineNumber());
	arg.setColumnNumber(e.getColumnNumber());
} | 
s=str 
{ 
	arg = new CalloutArg(s.getString()); 
	arg.setLineNumber(s.getLineNumber());
	arg.setColumnNumber(s.getColumnNumber());
}) ;

// Callout argument CSL
callout_argument_list returns [List<CalloutArg> args] { 
	args = new ArrayList<CalloutArg>(); 
	CalloutArg a;
} : 
(COMMA a=callout_argument { args.add(a); } (COMMA a=callout_argument { args.add(a); })*)? ;

// Method call
method_call returns [CallExpr callExpr] {
	List<CalloutArg> args;
	List<Expression> exprs;
	StringToken i;
	StringToken s;
	callExpr = null;
} : 
(i=id lp:LPAREN exprs=expr_argument_list RPAREN 
{ 
	callExpr = new MethodCallExpr(i.getString(),exprs); 
	callExpr.setLineNumber(lp.getLine());
	callExpr.setColumnNumber(lp.getColumn());
}) |
(co:TK_callout LPAREN s=str args=callout_argument_list RPAREN 
{ 
	callExpr = new CalloutExpr(s.getString(),args); 
	callExpr.setLineNumber(co.getLine());
	callExpr.setColumnNumber(co.getColumn());
});

// Block
block returns [Block b] {
	List<Statement> stmts = new ArrayList<Statement>();
	List<VarDecl> fields = new ArrayList<VarDecl>();
	VarDecl f;
	Statement s;	
	b = null;
} : 
(lc:LCURLY (f=var_decl { fields.add(f); })* (s=statement { stmts.add(s); })* RCURLY 
{ 
	b=new Block(BlockId.blockId,stmts,fields); 
	BlockId.blockId++;
	b.setLineNumber(lc.getLine());
	b.setColumnNumber(lc.getColumn());
}) ;

// Statement
statement returns [Statement s] {
	Location l;
	AssignOpType a;
	Expression e, e1, e2;
	CallExpr m;
	Block b, b1, b2;
	StringToken i;
	s = null;
} : 
(l=location a=assign_op e=expr semi1:SEMI 
{ 
	s = new AssignStmt(l,a,e); 
	s.setLineNumber(semi1.getLine());
	s.setColumnNumber(semi1.getColumn());	
}) | 
(m=method_call semi2:SEMI 
{ 
	s = new InvokeStmt((CallExpr)m); 
	s.setLineNumber(semi2.getLine());
	s.setColumnNumber(semi2.getColumn());
}) | 
(ift:TK_if LPAREN e=expr RPAREN b1=block 
{ 
	s = new IfStmt(e,b1); 
	s.setLineNumber(ift.getLine());
	s.setColumnNumber(ift.getColumn());
} (TK_else b2=block { ((IfStmt)s).setElseBlock(b2); })?) | 
(fort:TK_for i=id ASSIGNEQ e1=expr COMMA e2=expr b=block 
{ 
	s = new ForStmt(i.getString(),e1,e2,b);
	s.setLineNumber(fort.getLine());
	s.setColumnNumber(fort.getColumn());
}) | 
((rett:TK_return 
{ 
	s = new ReturnStmt(); 
	s.setLineNumber(rett.getLine());
	s.setColumnNumber(rett.getColumn());
} (e=expr { ((ReturnStmt)s).setExpression(e); })?) SEMI) | 
(breakt:TK_break { 
	s = new BreakStmt(); 
	s.setLineNumber(breakt.getLine());
	s.setColumnNumber(breakt.getColumn());
} SEMI) | 
(cont:TK_continue 
{ 
	s = new ContinueStmt(); 
	s.setLineNumber(cont.getLine());
	s.setColumnNumber(cont.getColumn());
} SEMI) |
s=block ;
		   
// Expression
// Unary minus
unary_minus_term returns [Expression e] {
	e = null;	
} : 
(mt:MINUS e=unary_minus_term { 
	e = new UnaryOpExpr(UnaryOpType.MINUS,e); 
	e.setLineNumber(mt.getLine());
	e.setColumnNumber(mt.getColumn());
}) | e=expr_static;

// Unary negation
not_term returns [Expression e] {
	e = null;
} : 
(nt:NOT e=not_term {
	e = new UnaryOpExpr(UnaryOpType.NOT, e); 
	e.setLineNumber(nt.getLine());
	e.setColumnNumber(nt.getColumn());
}) | e=unary_minus_term ;

// Multiplication
mul_term_temp returns [TempExpression e] {
	BinOpType bt;
	TempExpression te;
	Expression se;	
	e = null;
} : (bt=mul_op se=not_term te=mul_term_temp 
{ 
	if (te == null) e = new TempExpression(bt, se);
	else {
		e = new TempExpression(bt, se);
		e.setRightDeepChild(te);
	}
	e.setLineNumber(se.getLineNumber());
	e.setColumnNumber(se.getColumnNumber()); 
}) | 
({ e = null; }) ;

mul_term returns [Expression e] {
	Expression se;
	Expression temp;
	TempExpression te;
	e = null;
} : se=not_term te=mul_term_temp 
{ 
	if (te == null)	e = se; 
	else {
		temp = se;
		while (te.isMakeLeftDeep()) {
			temp = new BinOpExpr(temp, te);
			te = te.getRightDeepChild();
		}
		
		e = new BinOpExpr(temp,te);
	}
	
	e.setLineNumber(se.getLineNumber());
	e.setColumnNumber(se.getColumnNumber()); 
} ;

// Addition
add_term_temp returns [TempExpression e] {
	BinOpType bt;
	TempExpression te;
	Expression se;	
	e = null;
} : (bt=add_op se=mul_term te=add_term_temp 
{ 
	if (te == null) e = new TempExpression(bt, se);
	else {
		e = new TempExpression(bt, se);
		e.setRightDeepChild(te);
	}
	
	e.setLineNumber(se.getLineNumber());
	e.setColumnNumber(se.getColumnNumber()); 
}) 
| ({ e=null; }) ;

add_term returns [Expression e] {
	Expression se;
	Expression temp;
	TempExpression te;
	e = null;
} : se=mul_term te=add_term_temp 
{ 
	if (te == null)	e = se; 
	else {
		temp = se;
		while (te.isMakeLeftDeep()) {
			temp = new BinOpExpr(temp, te);
			te = te.getRightDeepChild();
		}
		
		e = new BinOpExpr(temp,te);
	}
	
	e.setLineNumber(se.getLineNumber());
	e.setColumnNumber(se.getColumnNumber()); 
} ;

// Relationals
relation_term_temp returns [TempExpression e] {
	BinOpType bt;
	TempExpression te;
	Expression se;	
	e = null;
} : (bt=rel_op se=add_term te=relation_term_temp 
{ 
	if (te == null) e = new TempExpression(bt, se);
	else e = new TempExpression(bt, new BinOpExpr(se, te));
	e.setLineNumber(se.getLineNumber());
	e.setColumnNumber(se.getColumnNumber()); 
}) | 
({ e=null; }) ;

relation_term returns [Expression e] {
	Expression se;
	TempExpression te;
	e = null;
} : se=add_term te=relation_term_temp 
{ 
	if (te == null)	e = se; 
	else e = new BinOpExpr(se,te);
	e.setLineNumber(se.getLineNumber());
	e.setColumnNumber(se.getColumnNumber()); 
} ;

// Equalities
equality_term_temp returns [TempExpression e] {
	BinOpType bt;
	TempExpression te;
	Expression se;	
	e = null;
} : (bt=eq_op se=relation_term te=equality_term_temp 
{ 
	if (te == null) e = new TempExpression(bt, se);
	else e = new TempExpression(bt, new BinOpExpr(se, te));
	e.setLineNumber(se.getLineNumber());
	e.setColumnNumber(se.getColumnNumber()); 
}) | 
({ e=null; }) ;

equality_term returns [Expression e] {
	Expression se;
	TempExpression te;
	e = null;
} : se=relation_term te=equality_term_temp 
{ 
	if (te == null)	e = se; 
	else e = new BinOpExpr(se, te);
	e.setLineNumber(se.getLineNumber());
	e.setColumnNumber(se.getColumnNumber()); 	
} ;

// Conditional AND
cond_and_term_temp returns [TempExpression e] {
	BinOpType bt;
	TempExpression te;
	Expression se;	
	e = null;
} : (at:AND se=equality_term te=cond_and_term_temp 
{ 
	if (te == null) e = new TempExpression(BinOpType.AND, se);
	else e = new TempExpression(BinOpType.AND, new BinOpExpr(se, te)); 
	e.setLineNumber(at.getLine());
	e.setColumnNumber(at.getColumn()); 
}) | 
({ e=null; }) ;

cond_and_term returns [Expression e] {
	Expression se;
	TempExpression te;
	e = null;
} : se=equality_term te=cond_and_term_temp 
{ 
	if (te == null) e = se; 
	else e = new BinOpExpr(se,te);	
	e.setLineNumber(se.getLineNumber());
	e.setColumnNumber(se.getColumnNumber()); 	
} ;

// Conditional OR
cond_or_term_temp returns [TempExpression e] {
	BinOpType bt;
	TempExpression te;
	Expression se;	
	e = null;
} : (ot:OR se=cond_and_term te=cond_or_term_temp 
{ 
	if (te == null) e = new TempExpression(BinOpType.OR, se);
	else e = new TempExpression(BinOpType.OR, new BinOpExpr(se, te)); 
	e.setLineNumber(ot.getLine());
	e.setColumnNumber(ot.getColumn()); 
}) | 
({ e=null; }) ;

cond_or_term returns [Expression e] {
	Expression se;
	TempExpression te;
	e = null;
} : se=cond_and_term te=cond_or_term_temp 
{ 
	if (te == null)	e = se; 
	else e = new BinOpExpr(se, te);
	e.setLineNumber(se.getLineNumber());
	e.setColumnNumber(se.getColumnNumber()); 
} ;

// Leaf Expressions
expr_static returns [Expression e] {
	e = null;
} : (e=location | e=literal | (LPAREN e=expr RPAREN) | e=method_call) ;

expr returns [Expression e]
{
	e = null;	
}: e=cond_or_term ;
