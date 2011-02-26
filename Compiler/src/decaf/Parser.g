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

class DecafParser extends Parser;

options {
  importVocab = DecafScanner;
  k = 3;	
  buildAST = true;
  defaultErrorHandler=false;
}

// Rename Tokens
id returns [String s] {
	s = null;	
} : 
(myid:ID { s = myid.getText(); })  | 
(TK_Program { s = "Program"; }) ;

intLit returns [String s] {
	s = null;	
} : 
(myint:INTLIT { s = myint.getText(); }) ;

charLit returns [String s] {
	s = null;	
} : 
(c:CHAR { s = c.getText(); }) ;

str returns [String s] {
	s = null;
} : 
(st:STRING { s=st.getText(); }) ;

// Program
program returns [ClassDecl classDecl] { 
	List<FieldDecl> fieldDecls = new ArrayList<FieldDecl>();
	List<MethodDecl> methodDecls = new ArrayList<MethodDecl>();
	classDecl = new ClassDecl(fieldDecls, methodDecls);
	FieldDecl f;
	MethodDecl m;
} : 
(TK_class TK_Program LCURLY (f=field_decl { fieldDecls.add(f); })* (m=method_decl { methodDecls.add(m); })* RCURLY EOF) ;

// Field declaration group
field_decl_group returns [Field f] {
	String i;
	String l;
	f = null;
} : 
(i=id { f = new Field(i); }) | 
(i=id LSQUARE l=intLit RSQUARE { f = new Field(i,l); }) ;

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
(t=type fList=field_decl_group_list { fDecl = new FieldDecl(fList,t); } SEMI) ;

// Method declaration group
method_decl_group returns [Parameter p] {
	Type t;
	String i;
	p = null;
} : 
(t=type i=id { p = new Parameter(t,i); }) ;

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
	String i;
} :
((rt=type { mDecl.setType(rt); }) | (TK_void { mDecl.setType(Type.VOID); })) (i=id LPAREN params=method_decl_group_list RPAREN b=block 
{
	mDecl.setId(i);
	mDecl.setParameters(params);
	mDecl.setBlock(b);	
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
(t1:TK_true { bl = new BooleanLiteral(t1.getText()); }) | 
(t2:TK_false { bl = new BooleanLiteral(t2.getText()); });

// Literal can be either int, char or boolean
literal returns [Literal lit] {
	String l;	
	String c;
	lit = null;
} : 
(l=intLit { lit = new IntLiteral(l); }) | 
(c=charLit { lit = new CharLiteral(c); }) | 
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
ASSIGNPLUSNEQ { a = AssignOpType.DECREMENT; } | 
ASSIGNEQ { a = AssignOpType.ASSIGN; });

// Location
location returns [Location loc] {
	Expression e;
	String i;
	loc = null;
} : 
(i=id { loc = new VarLocation(i); } | 
i=id LSQUARE e=expr RSQUARE { loc = new ArrayLocation(i,e); });

// Expression argument CSL
expr_argument_list returns [List<Expression> exprs] { 
	exprs = new ArrayList<Expression>(); 
	Expression e;
} : 
(e=expr { exprs.add(e); } (COMMA e=expr { exprs.add(e); })*)? ;

// Callout argument
callout_argument returns [CalloutArg arg] {
	Expression e;
	String s;
	arg = null;
} : 
(e=expr { arg=new CalloutArg(e); } | s=str { arg=new CalloutArg(s); }) ;

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
	String i;
	String s;
	callExpr = null;
} : 
(i=id LPAREN exprs=expr_argument_list RPAREN { callExpr = new MethodCallExpr(i,exprs); } |
TK_callout LPAREN s=str args=callout_argument_list RPAREN { callExpr = new CalloutExpr(s,args); });

// Block
block returns [Block b] {
	List<Statement> stmts = new ArrayList<Statement>();
	List<FieldDecl> fields = new ArrayList<FieldDecl>();
	FieldDecl f;
	Statement s;	
	b = null;
} : 
LCURLY (f=field_decl { fields.add(f); })* (s=statement { stmts.add(s); })* RCURLY { b=new Block(stmts,fields); } ;

// Statement
statement returns [Statement s] {
	Location l;
	AssignOpType a;
	Expression e, e1, e2;
	CallExpr m;
	Block b, b1, b2;
	String i;
	s = null;
} : 
(l=location a=assign_op e=expr { s = new AssignStmt(l,a,e); } SEMI) | 
(m=method_call { s = new InvokeStmt((CallExpr)m); } SEMI) | 
(TK_if LPAREN e=expr RPAREN b1=block { s = new IfStmt(e,b1); } (TK_else b2=block { ((IfStmt)s).setElseBlock(b2); })?) | 
(TK_for i=id ASSIGNEQ e1=expr COMMA e2=expr b=block { s = new ForStmt(i,e1,e2,b); }) | 
((TK_return { s = new ReturnStmt(); } (e=expr { ((ReturnStmt)s).setExpression(e); })?) SEMI) | 
(TK_break { s = new BreakStmt(); } SEMI) | 
(TK_continue { s = new ContinueStmt(); } SEMI) | 
s=block ;
		   
// Expression
// Unary minus
unary_minus_term returns [Expression e] {
	e = null;	
} : 
(MINUS e=expr_static { e = new UnaryOpExpr(UnaryOpType.MINUS,e); }) | e=expr_static;

// Unary negation
not_term returns [Expression e] {
	e = null;
} : 
(NOT e=unary_minus_term { e = new UnaryOpExpr(UnaryOpType.NOT, e); }) | e=unary_minus_term ;

// Multiplication
mul_term_temp returns [TempExpression e] {
	BinOpType bt;
	TempExpression te;
	Expression se;	
	e = null;
} : (bt=mul_op se=not_term te=mul_term_temp 
{ 
	if (te == null) e = new TempExpression(bt, se);
	else 	e = new TempExpression(bt, new BinOpExpr(se, te)); 
}) | 
({ e = null; }) ;

mul_term returns [Expression e] {
	Expression se;
	TempExpression te;
	e = null;
} : se=not_term te=mul_term_temp 
{ 
	if (te == null)	e = se; 
	else e = new BinOpExpr(se, te);		
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
	else e = new TempExpression(bt, new BinOpExpr(se, te)); 
}) 
| ({ e=null; }) ;

add_term returns [Expression e] {
	Expression se;
	TempExpression te;
	e = null;
} : se=mul_term te=add_term_temp 
{ 
	if (te == null)	e = se; 
	else e = new BinOpExpr(se,te);		
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
} ;

// Conditional AND
cond_and_term_temp returns [TempExpression e] {
	BinOpType bt;
	TempExpression te;
	Expression se;	
	e = null;
} : (AND se=equality_term te=cond_and_term_temp 
{ 
	if (te == null) e = new TempExpression(BinOpType.AND, se);
	else e = new TempExpression(BinOpType.AND, new BinOpExpr(se, te)); 
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
} ;

// Conditional OR
cond_or_term_temp returns [TempExpression e] {
	BinOpType bt;
	TempExpression te;
	Expression se;	
	e = null;
} : (OR se=cond_and_term te=cond_or_term_temp 
{ 
	if (te == null) e = new TempExpression(BinOpType.OR, se);
	else e = new TempExpression(BinOpType.OR, new BinOpExpr(se, te)); 
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
} ;

// Leaf Expressions
expr_static returns [Expression e] {
	e = null;
} : (e=location | e=literal | (LPAREN e=expr RPAREN) | e=method_call) ;

expr returns [Expression e]
{
	e = null;	
}: e=cond_or_term ;
