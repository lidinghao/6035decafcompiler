// $ANTLR 2.7.7 (2006-11-01): "Parser.g" -> "DecafParser.java"$

  	package decaf;
  	import decaf.ir.ast.*;
  	import java.util.List;
  	import java.util.ArrayList;

import antlr.TokenBuffer;
import antlr.TokenStreamException;
import antlr.TokenStreamIOException;
import antlr.ANTLRException;
import antlr.LLkParser;
import antlr.Token;
import antlr.TokenStream;
import antlr.RecognitionException;
import antlr.NoViableAltException;
import antlr.MismatchedTokenException;
import antlr.SemanticException;
import antlr.ParserSharedInputState;
import antlr.collections.impl.BitSet;
import antlr.collections.AST;
import java.util.Hashtable;
import antlr.ASTFactory;
import antlr.ASTPair;
import antlr.collections.impl.ASTArray;
 
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

public class DecafParser extends antlr.LLkParser       implements DecafParserTokenTypes
 {

protected DecafParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
  buildTokenTypeASTClassMap();
  astFactory = new ASTFactory(getTokenTypeToASTClassMap());
}

public DecafParser(TokenBuffer tokenBuf) {
  this(tokenBuf,3);
}

protected DecafParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
  buildTokenTypeASTClassMap();
  astFactory = new ASTFactory(getTokenTypeToASTClassMap());
}

public DecafParser(TokenStream lexer) {
  this(lexer,3);
}

public DecafParser(ParserSharedInputState state) {
  super(state,3);
  tokenNames = _tokenNames;
  buildTokenTypeASTClassMap();
  astFactory = new ASTFactory(getTokenTypeToASTClassMap());
}

	public final StringToken  id() throws RecognitionException, TokenStreamException {
		StringToken s;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST id_AST = null;
		Token  myid = null;
		AST myid_AST = null;
		
			s = null; 
		
		
		{
		myid = LT(1);
		myid_AST = astFactory.create(myid);
		astFactory.addASTChild(currentAST, myid_AST);
		match(ID);
		
			s = new StringToken(myid.getText()); 
			s.setLineNumber(myid.getLine());
			s.setColumnNumber(myid.getColumn());
		
		}
		id_AST = (AST)currentAST.root;
		returnAST = id_AST;
		return s;
	}
	
	public final StringToken  intLit() throws RecognitionException, TokenStreamException {
		StringToken s;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST intLit_AST = null;
		Token  myint = null;
		AST myint_AST = null;
		
			s = null;	
		
		
		{
		myint = LT(1);
		myint_AST = astFactory.create(myint);
		astFactory.addASTChild(currentAST, myint_AST);
		match(INTLIT);
		
			s = new StringToken(myint.getText()); 
			s.setLineNumber(myint.getLine());
			s.setColumnNumber(myint.getColumn());
		
		}
		intLit_AST = (AST)currentAST.root;
		returnAST = intLit_AST;
		return s;
	}
	
	public final StringToken  charLit() throws RecognitionException, TokenStreamException {
		StringToken s;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST charLit_AST = null;
		Token  c = null;
		AST c_AST = null;
		
			s = null;	
		
		
		{
		c = LT(1);
		c_AST = astFactory.create(c);
		astFactory.addASTChild(currentAST, c_AST);
		match(CHAR);
		
			s = new StringToken(c.getText()); 
			s.setLineNumber(c.getLine());
			s.setColumnNumber(c.getColumn());
		
		}
		charLit_AST = (AST)currentAST.root;
		returnAST = charLit_AST;
		return s;
	}
	
	public final StringToken  str() throws RecognitionException, TokenStreamException {
		StringToken s;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST str_AST = null;
		Token  st = null;
		AST st_AST = null;
		
			s = null;
		
		
		{
		st = LT(1);
		st_AST = astFactory.create(st);
		astFactory.addASTChild(currentAST, st_AST);
		match(STRING);
		
			s = new StringToken(st.getText());
			s.setLineNumber(st.getLine());
			s.setColumnNumber(st.getColumn());
		
		}
		str_AST = (AST)currentAST.root;
		returnAST = str_AST;
		return s;
	}
	
	public final ClassDecl  program() throws RecognitionException, TokenStreamException {
		ClassDecl classDecl;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST program_AST = null;
		Token  cl = null;
		AST cl_AST = null;
		
			List<FieldDecl> fieldDecls = new ArrayList<FieldDecl>();
			List<MethodDecl> methodDecls = new ArrayList<MethodDecl>();
			classDecl = new ClassDecl(fieldDecls, methodDecls);
			FieldDecl f;
			MethodDecl m;
			StringToken className;
		
		
		{
		cl = LT(1);
		cl_AST = astFactory.create(cl);
		astFactory.addASTChild(currentAST, cl_AST);
		match(TK_class);
		className=id();
		astFactory.addASTChild(currentAST, returnAST);
		AST tmp1_AST = null;
		tmp1_AST = astFactory.create(LT(1));
		astFactory.addASTChild(currentAST, tmp1_AST);
		match(LCURLY);
		{
		_loop12:
		do {
			if ((LA(1)==TK_int||LA(1)==TK_boolean) && (LA(2)==ID) && (LA(3)==LSQUARE||LA(3)==COMMA||LA(3)==SEMI)) {
				f=field_decl();
				astFactory.addASTChild(currentAST, returnAST);
				fieldDecls.add(f);
			}
			else {
				break _loop12;
			}
			
		} while (true);
		}
		{
		_loop14:
		do {
			if (((LA(1) >= TK_int && LA(1) <= TK_void))) {
				m=method_decl();
				astFactory.addASTChild(currentAST, returnAST);
				methodDecls.add(m);
			}
			else {
				break _loop14;
			}
			
		} while (true);
		}
		AST tmp2_AST = null;
		tmp2_AST = astFactory.create(LT(1));
		astFactory.addASTChild(currentAST, tmp2_AST);
		match(RCURLY);
		AST tmp3_AST = null;
		tmp3_AST = astFactory.create(LT(1));
		astFactory.addASTChild(currentAST, tmp3_AST);
		match(Token.EOF_TYPE);
		}
		
			classDecl.setLineNumber(cl.getLine());
			classDecl.setColumnNumber(cl.getColumn());
			if (!className.getString().equals("Program")) {
				classDecl = null;
			}
		
		program_AST = (AST)currentAST.root;
		returnAST = program_AST;
		return classDecl;
	}
	
	public final FieldDecl  field_decl() throws RecognitionException, TokenStreamException {
		FieldDecl fDecl;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST field_decl_AST = null;
		Token  s = null;
		AST s_AST = null;
		
			Type t; 
			List<Field> fList;
			fDecl = null;
		
		
		{
		t=type();
		astFactory.addASTChild(currentAST, returnAST);
		fList=field_decl_group_list();
		astFactory.addASTChild(currentAST, returnAST);
		fDecl = new FieldDecl(fList,t);
		s = LT(1);
		s_AST = astFactory.create(s);
		astFactory.addASTChild(currentAST, s_AST);
		match(SEMI);
		}
		
			fDecl.setLineNumber(s.getLine());
			fDecl.setLineNumber(s.getColumn());
		
		field_decl_AST = (AST)currentAST.root;
		returnAST = field_decl_AST;
		return fDecl;
	}
	
	public final MethodDecl  method_decl() throws RecognitionException, TokenStreamException {
		MethodDecl mDecl;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST method_decl_AST = null;
		Token  lp = null;
		AST lp_AST = null;
		
			mDecl = new MethodDecl(); 
			Type rt;
			List<Parameter> params;
			Block b;
			StringToken i;
		
		
		{
		switch ( LA(1)) {
		case TK_int:
		case TK_boolean:
		{
			{
			rt=type();
			astFactory.addASTChild(currentAST, returnAST);
			mDecl.setReturnType(rt);
			}
			break;
		}
		case TK_void:
		{
			{
			AST tmp4_AST = null;
			tmp4_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp4_AST);
			match(TK_void);
			mDecl.setReturnType(Type.VOID);
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		i=id();
		astFactory.addASTChild(currentAST, returnAST);
		lp = LT(1);
		lp_AST = astFactory.create(lp);
		astFactory.addASTChild(currentAST, lp_AST);
		match(LPAREN);
		params=method_decl_group_list();
		astFactory.addASTChild(currentAST, returnAST);
		AST tmp5_AST = null;
		tmp5_AST = astFactory.create(LT(1));
		astFactory.addASTChild(currentAST, tmp5_AST);
		match(RPAREN);
		b=block();
		astFactory.addASTChild(currentAST, returnAST);
		
			mDecl.setId(i.getString());
			mDecl.setParameters(params);
			mDecl.setBlock(b);	
			mDecl.setLineNumber(lp.getLine());
			mDecl.setColumnNumber(lp.getColumn());
		
		}
		method_decl_AST = (AST)currentAST.root;
		returnAST = method_decl_AST;
		return mDecl;
	}
	
	public final Field  field_decl_group() throws RecognitionException, TokenStreamException {
		Field f;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST field_decl_group_AST = null;
		
			StringToken i;
			StringToken l;
			f = null;
			IntLiteral il;
		
		
		if ((LA(1)==ID) && (LA(2)==COMMA||LA(2)==SEMI)) {
			{
			i=id();
			astFactory.addASTChild(currentAST, returnAST);
			
				f = new Field(i.getString()); 
				f.setLineNumber(i.getLineNumber());
				f.setColumnNumber(i.getColumnNumber());
			
			}
			field_decl_group_AST = (AST)currentAST.root;
		}
		else if ((LA(1)==ID) && (LA(2)==LSQUARE)) {
			{
			i=id();
			astFactory.addASTChild(currentAST, returnAST);
			AST tmp6_AST = null;
			tmp6_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp6_AST);
			match(LSQUARE);
			l=intLit();
			astFactory.addASTChild(currentAST, returnAST);
			AST tmp7_AST = null;
			tmp7_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp7_AST);
			match(RSQUARE);
			
				il = new IntLiteral(l.getString());
				il.setLineNumber(l.getLineNumber());
				il.setColumnNumber(l.getColumnNumber());	
				f = new Field(i.getString(),il); 
				f.setLineNumber(i.getLineNumber());
				f.setColumnNumber(i.getColumnNumber());
			
			}
			field_decl_group_AST = (AST)currentAST.root;
		}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		
		returnAST = field_decl_group_AST;
		return f;
	}
	
	public final List<Field>  field_decl_group_list() throws RecognitionException, TokenStreamException {
		List<Field> fields;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST field_decl_group_list_AST = null;
		
			fields = new ArrayList<Field>(); 
			Field f;
		
		
		{
		f=field_decl_group();
		astFactory.addASTChild(currentAST, returnAST);
		fields.add(f);
		{
		_loop21:
		do {
			if ((LA(1)==COMMA)) {
				AST tmp8_AST = null;
				tmp8_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp8_AST);
				match(COMMA);
				f=field_decl_group();
				astFactory.addASTChild(currentAST, returnAST);
				fields.add(f);
			}
			else {
				break _loop21;
			}
			
		} while (true);
		}
		}
		field_decl_group_list_AST = (AST)currentAST.root;
		returnAST = field_decl_group_list_AST;
		return fields;
	}
	
	public final Type  type() throws RecognitionException, TokenStreamException {
		Type t;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST type_AST = null;
		
			t = null;	
		
		
		switch ( LA(1)) {
		case TK_int:
		{
			{
			AST tmp9_AST = null;
			tmp9_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp9_AST);
			match(TK_int);
			t = Type.INT;
			}
			type_AST = (AST)currentAST.root;
			break;
		}
		case TK_boolean:
		{
			{
			AST tmp10_AST = null;
			tmp10_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp10_AST);
			match(TK_boolean);
			t = Type.BOOLEAN;
			}
			type_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = type_AST;
		return t;
	}
	
	public final VarDecl  var_decl() throws RecognitionException, TokenStreamException {
		VarDecl vDecl;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST var_decl_AST = null;
		
			Type t;
			List<String> vars = new ArrayList<String>();
			vDecl = null;
			StringToken i1, i2;
		
		
		{
		t=type();
		astFactory.addASTChild(currentAST, returnAST);
		vDecl = new VarDecl(t, vars);
		{
		{
		_loop28:
		do {
			if ((LA(1)==ID) && (LA(2)==COMMA)) {
				i1=id();
				astFactory.addASTChild(currentAST, returnAST);
				vars.add(i1.getString());
				AST tmp11_AST = null;
				tmp11_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp11_AST);
				match(COMMA);
			}
			else {
				break _loop28;
			}
			
		} while (true);
		}
		i2=id();
		astFactory.addASTChild(currentAST, returnAST);
		
			vars.add(i2.getString()); 
			vDecl.setLineNumber(i2.getLineNumber());
			vDecl.setColumnNumber(i2.getColumnNumber());
		
		}
		AST tmp12_AST = null;
		tmp12_AST = astFactory.create(LT(1));
		astFactory.addASTChild(currentAST, tmp12_AST);
		match(SEMI);
		}
		var_decl_AST = (AST)currentAST.root;
		returnAST = var_decl_AST;
		return vDecl;
	}
	
	public final Parameter  method_decl_group() throws RecognitionException, TokenStreamException {
		Parameter p;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST method_decl_group_AST = null;
		
			Type t;
			StringToken i;
			p = null;
		
		
		{
		t=type();
		astFactory.addASTChild(currentAST, returnAST);
		i=id();
		astFactory.addASTChild(currentAST, returnAST);
		
			p = new Parameter(t,i.getString());
			p.setLineNumber(i.getLineNumber());
			p.setColumnNumber(i.getColumnNumber());
		
		}
		method_decl_group_AST = (AST)currentAST.root;
		returnAST = method_decl_group_AST;
		return p;
	}
	
	public final List<Parameter>  method_decl_group_list() throws RecognitionException, TokenStreamException {
		List<Parameter> parameters;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST method_decl_group_list_AST = null;
		
			parameters = new ArrayList<Parameter>(); 
			Parameter p;
		
		
		{
		switch ( LA(1)) {
		case TK_int:
		case TK_boolean:
		{
			p=method_decl_group();
			astFactory.addASTChild(currentAST, returnAST);
			parameters.add(p);
			{
			_loop34:
			do {
				if ((LA(1)==COMMA)) {
					AST tmp13_AST = null;
					tmp13_AST = astFactory.create(LT(1));
					astFactory.addASTChild(currentAST, tmp13_AST);
					match(COMMA);
					p=method_decl_group();
					astFactory.addASTChild(currentAST, returnAST);
					parameters.add(p);
				}
				else {
					break _loop34;
				}
				
			} while (true);
			}
			break;
		}
		case RPAREN:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		method_decl_group_list_AST = (AST)currentAST.root;
		returnAST = method_decl_group_list_AST;
		return parameters;
	}
	
	public final Block  block() throws RecognitionException, TokenStreamException {
		Block b;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST block_AST = null;
		Token  lc = null;
		AST lc_AST = null;
		
			List<Statement> stmts = new ArrayList<Statement>();
			List<VarDecl> fields = new ArrayList<VarDecl>();
			VarDecl f;
			Statement s;	
			b = null;
		
		
		{
		lc = LT(1);
		lc_AST = astFactory.create(lc);
		astFactory.addASTChild(currentAST, lc_AST);
		match(LCURLY);
		{
		_loop78:
		do {
			if ((LA(1)==TK_int||LA(1)==TK_boolean)) {
				f=var_decl();
				astFactory.addASTChild(currentAST, returnAST);
				fields.add(f);
			}
			else {
				break _loop78;
			}
			
		} while (true);
		}
		{
		_loop80:
		do {
			if ((_tokenSet_0.member(LA(1)))) {
				s=statement();
				astFactory.addASTChild(currentAST, returnAST);
				stmts.add(s);
			}
			else {
				break _loop80;
			}
			
		} while (true);
		}
		AST tmp14_AST = null;
		tmp14_AST = astFactory.create(LT(1));
		astFactory.addASTChild(currentAST, tmp14_AST);
		match(RCURLY);
		
			b=new Block(BlockId.blockId,stmts,fields); 
			BlockId.blockId++;
			b.setLineNumber(lc.getLine());
			b.setColumnNumber(lc.getColumn());
		
		}
		block_AST = (AST)currentAST.root;
		returnAST = block_AST;
		return b;
	}
	
	public final BooleanLiteral  boolLiteral() throws RecognitionException, TokenStreamException {
		BooleanLiteral bl;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST boolLiteral_AST = null;
		Token  t1 = null;
		AST t1_AST = null;
		Token  t2 = null;
		AST t2_AST = null;
		
			bl = null;	
		
		
		switch ( LA(1)) {
		case TK_true:
		{
			{
			t1 = LT(1);
			t1_AST = astFactory.create(t1);
			astFactory.addASTChild(currentAST, t1_AST);
			match(TK_true);
			
				bl = new BooleanLiteral(t1.getText()); 
				bl.setLineNumber(t1.getLine());
				bl.setColumnNumber(t1.getColumn());
			
			}
			boolLiteral_AST = (AST)currentAST.root;
			break;
		}
		case TK_false:
		{
			{
			t2 = LT(1);
			t2_AST = astFactory.create(t2);
			astFactory.addASTChild(currentAST, t2_AST);
			match(TK_false);
			
				bl = new BooleanLiteral(t2.getText()); 
				bl.setLineNumber(t2.getLine());
				bl.setColumnNumber(t2.getColumn());	
			
			}
			boolLiteral_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = boolLiteral_AST;
		return bl;
	}
	
	public final Literal  literal() throws RecognitionException, TokenStreamException {
		Literal lit;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST literal_AST = null;
		
			StringToken l;	
			StringToken c;
			lit = null;
		
		
		switch ( LA(1)) {
		case INTLIT:
		{
			{
			l=intLit();
			astFactory.addASTChild(currentAST, returnAST);
			
				lit = new IntLiteral(l.getString()); 
				lit.setLineNumber(l.getLineNumber());
				lit.setColumnNumber(l.getColumnNumber());
			
			}
			literal_AST = (AST)currentAST.root;
			break;
		}
		case CHAR:
		{
			{
			c=charLit();
			astFactory.addASTChild(currentAST, returnAST);
			
				lit = new CharLiteral(c.getString()); 
				lit.setLineNumber(c.getLineNumber());
				lit.setColumnNumber(c.getColumnNumber()); 
			
			}
			literal_AST = (AST)currentAST.root;
			break;
		}
		case TK_true:
		case TK_false:
		{
			{
			lit=boolLiteral();
			astFactory.addASTChild(currentAST, returnAST);
			}
			literal_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = literal_AST;
		return lit;
	}
	
	public final BinOpType  add_op() throws RecognitionException, TokenStreamException {
		BinOpType bt;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST add_op_AST = null;
		
			bt = null;
		
		
		{
		switch ( LA(1)) {
		case PLUS:
		{
			AST tmp15_AST = null;
			tmp15_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp15_AST);
			match(PLUS);
			bt = BinOpType.PLUS;
			break;
		}
		case MINUS:
		{
			AST tmp16_AST = null;
			tmp16_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp16_AST);
			match(MINUS);
			bt = BinOpType.MINUS;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		add_op_AST = (AST)currentAST.root;
		returnAST = add_op_AST;
		return bt;
	}
	
	public final BinOpType  mul_op() throws RecognitionException, TokenStreamException {
		BinOpType bt;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST mul_op_AST = null;
		Token  m = null;
		AST m_AST = null;
		
			bt = null;
		
		
		{
		switch ( LA(1)) {
		case MULDIV:
		{
			m = LT(1);
			m_AST = astFactory.create(m);
			astFactory.addASTChild(currentAST, m_AST);
			match(MULDIV);
			
				if (m.getText().equals("*")) bt = BinOpType.MULTIPLY;
				else bt=BinOpType.DIVIDE;
			
			break;
		}
		case MOD:
		{
			AST tmp17_AST = null;
			tmp17_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp17_AST);
			match(MOD);
			bt = BinOpType.MOD;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		mul_op_AST = (AST)currentAST.root;
		returnAST = mul_op_AST;
		return bt;
	}
	
	public final BinOpType  rel_op() throws RecognitionException, TokenStreamException {
		BinOpType bt;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST rel_op_AST = null;
		
			bt = null;	
		
		
		{
		switch ( LA(1)) {
		case LESS:
		{
			AST tmp18_AST = null;
			tmp18_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp18_AST);
			match(LESS);
			bt = BinOpType.LE;
			break;
		}
		case MORE:
		{
			AST tmp19_AST = null;
			tmp19_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp19_AST);
			match(MORE);
			bt = BinOpType.GE;
			break;
		}
		case LEQ:
		{
			AST tmp20_AST = null;
			tmp20_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp20_AST);
			match(LEQ);
			bt = BinOpType.LEQ;
			break;
		}
		case GEQ:
		{
			AST tmp21_AST = null;
			tmp21_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp21_AST);
			match(GEQ);
			bt = BinOpType.GEQ;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		rel_op_AST = (AST)currentAST.root;
		returnAST = rel_op_AST;
		return bt;
	}
	
	public final BinOpType  eq_op() throws RecognitionException, TokenStreamException {
		BinOpType bt;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST eq_op_AST = null;
		
			bt = null;	
		
		
		{
		switch ( LA(1)) {
		case CEQ:
		{
			AST tmp22_AST = null;
			tmp22_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp22_AST);
			match(CEQ);
			bt = BinOpType.CEQ;
			break;
		}
		case NEQ:
		{
			AST tmp23_AST = null;
			tmp23_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp23_AST);
			match(NEQ);
			bt = BinOpType.NEQ;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		eq_op_AST = (AST)currentAST.root;
		returnAST = eq_op_AST;
		return bt;
	}
	
	public final AssignOpType  assign_op() throws RecognitionException, TokenStreamException {
		AssignOpType a;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST assign_op_AST = null;
		
			a = null;
		
		
		{
		switch ( LA(1)) {
		case ASSIGNPLUSEQ:
		{
			AST tmp24_AST = null;
			tmp24_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp24_AST);
			match(ASSIGNPLUSEQ);
			a = AssignOpType.INCREMENT;
			break;
		}
		case ASSIGNMINUSEQ:
		{
			AST tmp25_AST = null;
			tmp25_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp25_AST);
			match(ASSIGNMINUSEQ);
			a = AssignOpType.DECREMENT;
			break;
		}
		case ASSIGNEQ:
		{
			AST tmp26_AST = null;
			tmp26_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp26_AST);
			match(ASSIGNEQ);
			a = AssignOpType.ASSIGN;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		assign_op_AST = (AST)currentAST.root;
		returnAST = assign_op_AST;
		return a;
	}
	
	public final Location  location() throws RecognitionException, TokenStreamException {
		Location loc;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST location_AST = null;
		
			Expression e;
			StringToken i;
			loc = null;
		
		
		{
		if ((LA(1)==ID) && (_tokenSet_1.member(LA(2)))) {
			i=id();
			astFactory.addASTChild(currentAST, returnAST);
			
				loc = new VarLocation(i.getString()); 
				loc.setLineNumber(i.getLineNumber());
				loc.setColumnNumber(i.getColumnNumber());
			
		}
		else if ((LA(1)==ID) && (LA(2)==LSQUARE)) {
			i=id();
			astFactory.addASTChild(currentAST, returnAST);
			AST tmp27_AST = null;
			tmp27_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp27_AST);
			match(LSQUARE);
			e=expr();
			astFactory.addASTChild(currentAST, returnAST);
			AST tmp28_AST = null;
			tmp28_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp28_AST);
			match(RSQUARE);
			
				loc = new ArrayLocation(i.getString(),e); 
				loc.setLineNumber(i.getLineNumber());
				loc.setColumnNumber(i.getColumnNumber());
			
		}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		
		}
		location_AST = (AST)currentAST.root;
		returnAST = location_AST;
		return loc;
	}
	
	public final Expression  expr() throws RecognitionException, TokenStreamException {
		Expression e;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST expr_AST = null;
		
			e = null;	
		
		
		e=cond_or_term();
		astFactory.addASTChild(currentAST, returnAST);
		expr_AST = (AST)currentAST.root;
		returnAST = expr_AST;
		return e;
	}
	
	public final List<Expression>  expr_argument_list() throws RecognitionException, TokenStreamException {
		List<Expression> exprs;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST expr_argument_list_AST = null;
		
			exprs = new ArrayList<Expression>(); 
			Expression e;
		
		
		{
		switch ( LA(1)) {
		case TK_callout:
		case TK_true:
		case TK_false:
		case LPAREN:
		case CHAR:
		case MINUS:
		case NOT:
		case ID:
		case INTLIT:
		{
			e=expr();
			astFactory.addASTChild(currentAST, returnAST);
			exprs.add(e);
			{
			_loop65:
			do {
				if ((LA(1)==COMMA)) {
					AST tmp29_AST = null;
					tmp29_AST = astFactory.create(LT(1));
					astFactory.addASTChild(currentAST, tmp29_AST);
					match(COMMA);
					e=expr();
					astFactory.addASTChild(currentAST, returnAST);
					exprs.add(e);
				}
				else {
					break _loop65;
				}
				
			} while (true);
			}
			break;
		}
		case RPAREN:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		expr_argument_list_AST = (AST)currentAST.root;
		returnAST = expr_argument_list_AST;
		return exprs;
	}
	
	public final CalloutArg  callout_argument() throws RecognitionException, TokenStreamException {
		CalloutArg arg;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST callout_argument_AST = null;
		
			Expression e;
			StringToken s;
			arg = null;
		
		
		{
		switch ( LA(1)) {
		case TK_callout:
		case TK_true:
		case TK_false:
		case LPAREN:
		case CHAR:
		case MINUS:
		case NOT:
		case ID:
		case INTLIT:
		{
			e=expr();
			astFactory.addASTChild(currentAST, returnAST);
			
				arg = new CalloutArg(e); 
				arg.setLineNumber(e.getLineNumber());
				arg.setColumnNumber(e.getColumnNumber());
			
			break;
		}
		case STRING:
		{
			s=str();
			astFactory.addASTChild(currentAST, returnAST);
			
				arg = new CalloutArg(s.getString()); 
				arg.setLineNumber(s.getLineNumber());
				arg.setColumnNumber(s.getColumnNumber());
			
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		callout_argument_AST = (AST)currentAST.root;
		returnAST = callout_argument_AST;
		return arg;
	}
	
	public final List<CalloutArg>  callout_argument_list() throws RecognitionException, TokenStreamException {
		List<CalloutArg> args;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST callout_argument_list_AST = null;
		
			args = new ArrayList<CalloutArg>(); 
			CalloutArg a;
		
		
		{
		switch ( LA(1)) {
		case COMMA:
		{
			AST tmp30_AST = null;
			tmp30_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp30_AST);
			match(COMMA);
			a=callout_argument();
			astFactory.addASTChild(currentAST, returnAST);
			args.add(a);
			{
			_loop71:
			do {
				if ((LA(1)==COMMA)) {
					AST tmp31_AST = null;
					tmp31_AST = astFactory.create(LT(1));
					astFactory.addASTChild(currentAST, tmp31_AST);
					match(COMMA);
					a=callout_argument();
					astFactory.addASTChild(currentAST, returnAST);
					args.add(a);
				}
				else {
					break _loop71;
				}
				
			} while (true);
			}
			break;
		}
		case RPAREN:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		callout_argument_list_AST = (AST)currentAST.root;
		returnAST = callout_argument_list_AST;
		return args;
	}
	
	public final CallExpr  method_call() throws RecognitionException, TokenStreamException {
		CallExpr callExpr;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST method_call_AST = null;
		Token  lp = null;
		AST lp_AST = null;
		Token  co = null;
		AST co_AST = null;
		
			List<CalloutArg> args;
			List<Expression> exprs;
			StringToken i;
			StringToken s;
			callExpr = null;
		
		
		switch ( LA(1)) {
		case ID:
		{
			{
			i=id();
			astFactory.addASTChild(currentAST, returnAST);
			lp = LT(1);
			lp_AST = astFactory.create(lp);
			astFactory.addASTChild(currentAST, lp_AST);
			match(LPAREN);
			exprs=expr_argument_list();
			astFactory.addASTChild(currentAST, returnAST);
			AST tmp32_AST = null;
			tmp32_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp32_AST);
			match(RPAREN);
			
				callExpr = new MethodCallExpr(i.getString(),exprs); 
				callExpr.setLineNumber(lp.getLine());
				callExpr.setColumnNumber(lp.getColumn());
			
			}
			method_call_AST = (AST)currentAST.root;
			break;
		}
		case TK_callout:
		{
			{
			co = LT(1);
			co_AST = astFactory.create(co);
			astFactory.addASTChild(currentAST, co_AST);
			match(TK_callout);
			AST tmp33_AST = null;
			tmp33_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp33_AST);
			match(LPAREN);
			s=str();
			astFactory.addASTChild(currentAST, returnAST);
			args=callout_argument_list();
			astFactory.addASTChild(currentAST, returnAST);
			AST tmp34_AST = null;
			tmp34_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp34_AST);
			match(RPAREN);
			
				callExpr = new CalloutExpr(s.getString(),args); 
				callExpr.setLineNumber(co.getLine());
				callExpr.setColumnNumber(co.getColumn());
			
			}
			method_call_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = method_call_AST;
		return callExpr;
	}
	
	public final Statement  statement() throws RecognitionException, TokenStreamException {
		Statement s;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST statement_AST = null;
		Token  semi1 = null;
		AST semi1_AST = null;
		Token  semi2 = null;
		AST semi2_AST = null;
		Token  ift = null;
		AST ift_AST = null;
		Token  fort = null;
		AST fort_AST = null;
		Token  rett = null;
		AST rett_AST = null;
		Token  breakt = null;
		AST breakt_AST = null;
		Token  cont = null;
		AST cont_AST = null;
		
			Location l;
			AssignOpType a;
			Expression e, e1, e2;
			CallExpr m;
			Block b, b1, b2;
			StringToken i;
			s = null;
		
		
		switch ( LA(1)) {
		case TK_if:
		{
			{
			ift = LT(1);
			ift_AST = astFactory.create(ift);
			astFactory.addASTChild(currentAST, ift_AST);
			match(TK_if);
			AST tmp35_AST = null;
			tmp35_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp35_AST);
			match(LPAREN);
			e=expr();
			astFactory.addASTChild(currentAST, returnAST);
			AST tmp36_AST = null;
			tmp36_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp36_AST);
			match(RPAREN);
			b1=block();
			astFactory.addASTChild(currentAST, returnAST);
			
				s = new IfStmt(e,b1); 
				s.setLineNumber(ift.getLine());
				s.setColumnNumber(ift.getColumn());
			
			{
			switch ( LA(1)) {
			case TK_else:
			{
				AST tmp37_AST = null;
				tmp37_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp37_AST);
				match(TK_else);
				b2=block();
				astFactory.addASTChild(currentAST, returnAST);
				((IfStmt)s).setElseBlock(b2);
				break;
			}
			case TK_if:
			case TK_for:
			case TK_return:
			case TK_break:
			case TK_continue:
			case TK_callout:
			case LCURLY:
			case RCURLY:
			case ID:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			}
			statement_AST = (AST)currentAST.root;
			break;
		}
		case TK_for:
		{
			{
			fort = LT(1);
			fort_AST = astFactory.create(fort);
			astFactory.addASTChild(currentAST, fort_AST);
			match(TK_for);
			i=id();
			astFactory.addASTChild(currentAST, returnAST);
			AST tmp38_AST = null;
			tmp38_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp38_AST);
			match(ASSIGNEQ);
			e1=expr();
			astFactory.addASTChild(currentAST, returnAST);
			AST tmp39_AST = null;
			tmp39_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp39_AST);
			match(COMMA);
			e2=expr();
			astFactory.addASTChild(currentAST, returnAST);
			b=block();
			astFactory.addASTChild(currentAST, returnAST);
			
				s = new ForStmt(i.getString(),e1,e2,b);
				s.setLineNumber(fort.getLine());
				s.setColumnNumber(fort.getColumn());
			
			}
			statement_AST = (AST)currentAST.root;
			break;
		}
		case TK_return:
		{
			{
			{
			rett = LT(1);
			rett_AST = astFactory.create(rett);
			astFactory.addASTChild(currentAST, rett_AST);
			match(TK_return);
			
				s = new ReturnStmt(); 
				s.setLineNumber(rett.getLine());
				s.setColumnNumber(rett.getColumn());
			
			{
			switch ( LA(1)) {
			case TK_callout:
			case TK_true:
			case TK_false:
			case LPAREN:
			case CHAR:
			case MINUS:
			case NOT:
			case ID:
			case INTLIT:
			{
				e=expr();
				astFactory.addASTChild(currentAST, returnAST);
				((ReturnStmt)s).setExpression(e);
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			}
			AST tmp40_AST = null;
			tmp40_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp40_AST);
			match(SEMI);
			}
			statement_AST = (AST)currentAST.root;
			break;
		}
		case TK_break:
		{
			{
			breakt = LT(1);
			breakt_AST = astFactory.create(breakt);
			astFactory.addASTChild(currentAST, breakt_AST);
			match(TK_break);
			
				s = new BreakStmt(); 
				s.setLineNumber(breakt.getLine());
				s.setColumnNumber(breakt.getColumn());
			
			AST tmp41_AST = null;
			tmp41_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp41_AST);
			match(SEMI);
			}
			statement_AST = (AST)currentAST.root;
			break;
		}
		case TK_continue:
		{
			{
			cont = LT(1);
			cont_AST = astFactory.create(cont);
			astFactory.addASTChild(currentAST, cont_AST);
			match(TK_continue);
			
				s = new ContinueStmt(); 
				s.setLineNumber(cont.getLine());
				s.setColumnNumber(cont.getColumn());
			
			AST tmp42_AST = null;
			tmp42_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp42_AST);
			match(SEMI);
			}
			statement_AST = (AST)currentAST.root;
			break;
		}
		case LCURLY:
		{
			s=block();
			astFactory.addASTChild(currentAST, returnAST);
			statement_AST = (AST)currentAST.root;
			break;
		}
		default:
			if ((LA(1)==ID) && (_tokenSet_2.member(LA(2)))) {
				{
				l=location();
				astFactory.addASTChild(currentAST, returnAST);
				a=assign_op();
				astFactory.addASTChild(currentAST, returnAST);
				e=expr();
				astFactory.addASTChild(currentAST, returnAST);
				semi1 = LT(1);
				semi1_AST = astFactory.create(semi1);
				astFactory.addASTChild(currentAST, semi1_AST);
				match(SEMI);
				
					s = new AssignStmt(l,a,e); 
					s.setLineNumber(semi1.getLine());
					s.setColumnNumber(semi1.getColumn());	
				
				}
				statement_AST = (AST)currentAST.root;
			}
			else if ((LA(1)==TK_callout||LA(1)==ID) && (LA(2)==LPAREN)) {
				{
				m=method_call();
				astFactory.addASTChild(currentAST, returnAST);
				semi2 = LT(1);
				semi2_AST = astFactory.create(semi2);
				astFactory.addASTChild(currentAST, semi2_AST);
				match(SEMI);
				
					s = new InvokeStmt((CallExpr)m); 
					s.setLineNumber(semi2.getLine());
					s.setColumnNumber(semi2.getColumn());
				
				}
				statement_AST = (AST)currentAST.root;
			}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = statement_AST;
		return s;
	}
	
	public final Expression  unary_minus_term() throws RecognitionException, TokenStreamException {
		Expression e;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST unary_minus_term_AST = null;
		Token  mt = null;
		AST mt_AST = null;
		
			e = null;	
		
		
		switch ( LA(1)) {
		case MINUS:
		{
			{
			mt = LT(1);
			mt_AST = astFactory.create(mt);
			astFactory.addASTChild(currentAST, mt_AST);
			match(MINUS);
			e=unary_minus_term();
			astFactory.addASTChild(currentAST, returnAST);
			
				e = new UnaryOpExpr(UnaryOpType.MINUS,e); 
				e.setLineNumber(mt.getLine());
				e.setColumnNumber(mt.getColumn());
			
			}
			unary_minus_term_AST = (AST)currentAST.root;
			break;
		}
		case TK_callout:
		case TK_true:
		case TK_false:
		case LPAREN:
		case CHAR:
		case ID:
		case INTLIT:
		{
			e=expr_static();
			astFactory.addASTChild(currentAST, returnAST);
			unary_minus_term_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = unary_minus_term_AST;
		return e;
	}
	
	public final Expression  expr_static() throws RecognitionException, TokenStreamException {
		Expression e;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST expr_static_AST = null;
		
			e = null;
		
		
		{
		switch ( LA(1)) {
		case TK_true:
		case TK_false:
		case CHAR:
		case INTLIT:
		{
			e=literal();
			astFactory.addASTChild(currentAST, returnAST);
			break;
		}
		case LPAREN:
		{
			{
			AST tmp43_AST = null;
			tmp43_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp43_AST);
			match(LPAREN);
			e=expr();
			astFactory.addASTChild(currentAST, returnAST);
			AST tmp44_AST = null;
			tmp44_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp44_AST);
			match(RPAREN);
			}
			break;
		}
		default:
			if ((LA(1)==ID) && (_tokenSet_3.member(LA(2)))) {
				e=location();
				astFactory.addASTChild(currentAST, returnAST);
			}
			else if ((LA(1)==TK_callout||LA(1)==ID) && (LA(2)==LPAREN)) {
				e=method_call();
				astFactory.addASTChild(currentAST, returnAST);
			}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		expr_static_AST = (AST)currentAST.root;
		returnAST = expr_static_AST;
		return e;
	}
	
	public final Expression  not_term() throws RecognitionException, TokenStreamException {
		Expression e;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST not_term_AST = null;
		Token  nt = null;
		AST nt_AST = null;
		
			e = null;
		
		
		switch ( LA(1)) {
		case NOT:
		{
			{
			nt = LT(1);
			nt_AST = astFactory.create(nt);
			astFactory.addASTChild(currentAST, nt_AST);
			match(NOT);
			e=not_term();
			astFactory.addASTChild(currentAST, returnAST);
			
				e = new UnaryOpExpr(UnaryOpType.NOT, e); 
				e.setLineNumber(nt.getLine());
				e.setColumnNumber(nt.getColumn());
			
			}
			not_term_AST = (AST)currentAST.root;
			break;
		}
		case TK_callout:
		case TK_true:
		case TK_false:
		case LPAREN:
		case CHAR:
		case MINUS:
		case ID:
		case INTLIT:
		{
			e=unary_minus_term();
			astFactory.addASTChild(currentAST, returnAST);
			not_term_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = not_term_AST;
		return e;
	}
	
	public final TempExpression  mul_term_temp() throws RecognitionException, TokenStreamException {
		TempExpression e;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST mul_term_temp_AST = null;
		
			BinOpType bt;
			TempExpression te;
			Expression se;	
			e = null;
		
		
		switch ( LA(1)) {
		case MULDIV:
		case MOD:
		{
			{
			bt=mul_op();
			astFactory.addASTChild(currentAST, returnAST);
			se=not_term();
			astFactory.addASTChild(currentAST, returnAST);
			te=mul_term_temp();
			astFactory.addASTChild(currentAST, returnAST);
			
				if (te == null) e = new TempExpression(bt, se);
				else {
					e = new TempExpression(bt, se);
					e.setRightDeepChild(te);
				}
				e.setLineNumber(se.getLineNumber());
				e.setColumnNumber(se.getColumnNumber()); 
			
			}
			mul_term_temp_AST = (AST)currentAST.root;
			break;
		}
		case LCURLY:
		case RPAREN:
		case RSQUARE:
		case COMMA:
		case SEMI:
		case PLUS:
		case MINUS:
		case LESS:
		case MORE:
		case LEQ:
		case GEQ:
		case CEQ:
		case NEQ:
		case AND:
		case OR:
		{
			{
			e = null;
			}
			mul_term_temp_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = mul_term_temp_AST;
		return e;
	}
	
	public final Expression  mul_term() throws RecognitionException, TokenStreamException {
		Expression e;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST mul_term_AST = null;
		
			Expression se;
			Expression temp;
			TempExpression te;
			e = null;
		
		
		se=not_term();
		astFactory.addASTChild(currentAST, returnAST);
		te=mul_term_temp();
		astFactory.addASTChild(currentAST, returnAST);
		
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
		
		mul_term_AST = (AST)currentAST.root;
		returnAST = mul_term_AST;
		return e;
	}
	
	public final TempExpression  add_term_temp() throws RecognitionException, TokenStreamException {
		TempExpression e;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST add_term_temp_AST = null;
		
			BinOpType bt;
			TempExpression te;
			Expression se;	
			e = null;
		
		
		switch ( LA(1)) {
		case PLUS:
		case MINUS:
		{
			{
			bt=add_op();
			astFactory.addASTChild(currentAST, returnAST);
			se=mul_term();
			astFactory.addASTChild(currentAST, returnAST);
			te=add_term_temp();
			astFactory.addASTChild(currentAST, returnAST);
			
				if (te == null) e = new TempExpression(bt, se);
				else {
					e = new TempExpression(bt, se);
					e.setRightDeepChild(te);
				}
				
				e.setLineNumber(se.getLineNumber());
				e.setColumnNumber(se.getColumnNumber()); 
			
			}
			add_term_temp_AST = (AST)currentAST.root;
			break;
		}
		case LCURLY:
		case RPAREN:
		case RSQUARE:
		case COMMA:
		case SEMI:
		case LESS:
		case MORE:
		case LEQ:
		case GEQ:
		case CEQ:
		case NEQ:
		case AND:
		case OR:
		{
			{
			e=null;
			}
			add_term_temp_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = add_term_temp_AST;
		return e;
	}
	
	public final Expression  add_term() throws RecognitionException, TokenStreamException {
		Expression e;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST add_term_AST = null;
		
			Expression se;
			Expression temp;
			TempExpression te;
			e = null;
		
		
		se=mul_term();
		astFactory.addASTChild(currentAST, returnAST);
		te=add_term_temp();
		astFactory.addASTChild(currentAST, returnAST);
		
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
		
		add_term_AST = (AST)currentAST.root;
		returnAST = add_term_AST;
		return e;
	}
	
	public final TempExpression  relation_term_temp() throws RecognitionException, TokenStreamException {
		TempExpression e;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST relation_term_temp_AST = null;
		
			BinOpType bt;
			TempExpression te;
			Expression se;	
			e = null;
		
		
		switch ( LA(1)) {
		case LESS:
		case MORE:
		case LEQ:
		case GEQ:
		{
			{
			bt=rel_op();
			astFactory.addASTChild(currentAST, returnAST);
			se=add_term();
			astFactory.addASTChild(currentAST, returnAST);
			te=relation_term_temp();
			astFactory.addASTChild(currentAST, returnAST);
			
				if (te == null) e = new TempExpression(bt, se);
				else e = new TempExpression(bt, new BinOpExpr(se, te));
				e.setLineNumber(se.getLineNumber());
				e.setColumnNumber(se.getColumnNumber()); 
			
			}
			relation_term_temp_AST = (AST)currentAST.root;
			break;
		}
		case LCURLY:
		case RPAREN:
		case RSQUARE:
		case COMMA:
		case SEMI:
		case CEQ:
		case NEQ:
		case AND:
		case OR:
		{
			{
			e=null;
			}
			relation_term_temp_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = relation_term_temp_AST;
		return e;
	}
	
	public final Expression  relation_term() throws RecognitionException, TokenStreamException {
		Expression e;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST relation_term_AST = null;
		
			Expression se;
			TempExpression te;
			e = null;
		
		
		se=add_term();
		astFactory.addASTChild(currentAST, returnAST);
		te=relation_term_temp();
		astFactory.addASTChild(currentAST, returnAST);
		
			if (te == null)	e = se; 
			else e = new BinOpExpr(se,te);
			e.setLineNumber(se.getLineNumber());
			e.setColumnNumber(se.getColumnNumber()); 
		
		relation_term_AST = (AST)currentAST.root;
		returnAST = relation_term_AST;
		return e;
	}
	
	public final TempExpression  equality_term_temp() throws RecognitionException, TokenStreamException {
		TempExpression e;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST equality_term_temp_AST = null;
		
			BinOpType bt;
			TempExpression te;
			Expression se;	
			e = null;
		
		
		switch ( LA(1)) {
		case CEQ:
		case NEQ:
		{
			{
			bt=eq_op();
			astFactory.addASTChild(currentAST, returnAST);
			se=relation_term();
			astFactory.addASTChild(currentAST, returnAST);
			te=equality_term_temp();
			astFactory.addASTChild(currentAST, returnAST);
			
				if (te == null) e = new TempExpression(bt, se);
				else e = new TempExpression(bt, new BinOpExpr(se, te));
				e.setLineNumber(se.getLineNumber());
				e.setColumnNumber(se.getColumnNumber()); 
			
			}
			equality_term_temp_AST = (AST)currentAST.root;
			break;
		}
		case LCURLY:
		case RPAREN:
		case RSQUARE:
		case COMMA:
		case SEMI:
		case AND:
		case OR:
		{
			{
			e=null;
			}
			equality_term_temp_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = equality_term_temp_AST;
		return e;
	}
	
	public final Expression  equality_term() throws RecognitionException, TokenStreamException {
		Expression e;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST equality_term_AST = null;
		
			Expression se;
			TempExpression te;
			e = null;
		
		
		se=relation_term();
		astFactory.addASTChild(currentAST, returnAST);
		te=equality_term_temp();
		astFactory.addASTChild(currentAST, returnAST);
		
			if (te == null)	e = se; 
			else e = new BinOpExpr(se, te);
			e.setLineNumber(se.getLineNumber());
			e.setColumnNumber(se.getColumnNumber()); 	
		
		equality_term_AST = (AST)currentAST.root;
		returnAST = equality_term_AST;
		return e;
	}
	
	public final TempExpression  cond_and_term_temp() throws RecognitionException, TokenStreamException {
		TempExpression e;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST cond_and_term_temp_AST = null;
		Token  at = null;
		AST at_AST = null;
		
			BinOpType bt;
			TempExpression te;
			Expression se;	
			e = null;
		
		
		switch ( LA(1)) {
		case AND:
		{
			{
			at = LT(1);
			at_AST = astFactory.create(at);
			astFactory.addASTChild(currentAST, at_AST);
			match(AND);
			se=equality_term();
			astFactory.addASTChild(currentAST, returnAST);
			te=cond_and_term_temp();
			astFactory.addASTChild(currentAST, returnAST);
			
				if (te == null) e = new TempExpression(BinOpType.AND, se);
				else e = new TempExpression(BinOpType.AND, new BinOpExpr(se, te)); 
				e.setLineNumber(at.getLine());
				e.setColumnNumber(at.getColumn()); 
			
			}
			cond_and_term_temp_AST = (AST)currentAST.root;
			break;
		}
		case LCURLY:
		case RPAREN:
		case RSQUARE:
		case COMMA:
		case SEMI:
		case OR:
		{
			{
			e=null;
			}
			cond_and_term_temp_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = cond_and_term_temp_AST;
		return e;
	}
	
	public final Expression  cond_and_term() throws RecognitionException, TokenStreamException {
		Expression e;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST cond_and_term_AST = null;
		
			Expression se;
			TempExpression te;
			e = null;
		
		
		se=equality_term();
		astFactory.addASTChild(currentAST, returnAST);
		te=cond_and_term_temp();
		astFactory.addASTChild(currentAST, returnAST);
		
			if (te == null) e = se; 
			else e = new BinOpExpr(se,te);	
			e.setLineNumber(se.getLineNumber());
			e.setColumnNumber(se.getColumnNumber()); 	
		
		cond_and_term_AST = (AST)currentAST.root;
		returnAST = cond_and_term_AST;
		return e;
	}
	
	public final TempExpression  cond_or_term_temp() throws RecognitionException, TokenStreamException {
		TempExpression e;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST cond_or_term_temp_AST = null;
		Token  ot = null;
		AST ot_AST = null;
		
			BinOpType bt;
			TempExpression te;
			Expression se;	
			e = null;
		
		
		switch ( LA(1)) {
		case OR:
		{
			{
			ot = LT(1);
			ot_AST = astFactory.create(ot);
			astFactory.addASTChild(currentAST, ot_AST);
			match(OR);
			se=cond_and_term();
			astFactory.addASTChild(currentAST, returnAST);
			te=cond_or_term_temp();
			astFactory.addASTChild(currentAST, returnAST);
			
				if (te == null) e = new TempExpression(BinOpType.OR, se);
				else e = new TempExpression(BinOpType.OR, new BinOpExpr(se, te)); 
				e.setLineNumber(ot.getLine());
				e.setColumnNumber(ot.getColumn()); 
			
			}
			cond_or_term_temp_AST = (AST)currentAST.root;
			break;
		}
		case LCURLY:
		case RPAREN:
		case RSQUARE:
		case COMMA:
		case SEMI:
		{
			{
			e=null;
			}
			cond_or_term_temp_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = cond_or_term_temp_AST;
		return e;
	}
	
	public final Expression  cond_or_term() throws RecognitionException, TokenStreamException {
		Expression e;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST cond_or_term_AST = null;
		
			Expression se;
			TempExpression te;
			e = null;
		
		
		se=cond_and_term();
		astFactory.addASTChild(currentAST, returnAST);
		te=cond_or_term_temp();
		astFactory.addASTChild(currentAST, returnAST);
		
			if (te == null)	e = se; 
			else e = new BinOpExpr(se, te);
			e.setLineNumber(se.getLineNumber());
			e.setColumnNumber(se.getColumnNumber()); 
		
		cond_or_term_AST = (AST)currentAST.root;
		returnAST = cond_or_term_AST;
		return e;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"\"class\"",
		"\"if\"",
		"\"else\"",
		"\"for\"",
		"\"return\"",
		"\"break\"",
		"\"continue\"",
		"\"callout\"",
		"\"true\"",
		"\"false\"",
		"\"int\"",
		"\"boolean\"",
		"\"void\"",
		"{",
		"}",
		"(",
		")",
		"[",
		"]",
		"whitespace",
		"comment",
		"string literal",
		"char literal",
		"comma",
		"semicolon",
		"plus sign",
		"minus sign",
		"mul or div operation",
		"modulus operation",
		"ASSIGNPLUSEQ",
		"ASSIGNMINUSEQ",
		"ASSIGNEQ",
		"LESS",
		"MORE",
		"LEQ",
		"GEQ",
		"CEQ",
		"NEQ",
		"AND",
		"OR",
		"NOT",
		"identifier",
		"integer literal",
		"ESC",
		"VALIDCHARS",
		"ALPHA",
		"DIGIT",
		"HEX_DIGIT",
		"ALPHA_NUM",
		"DECIMAL",
		"HEX"
	};
	
	protected void buildTokenTypeASTClassMap() {
		tokenTypeToASTClassMap=null;
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 35184372223904L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 17592057200640L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 60131639296L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { 17531929755648L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	
	}
