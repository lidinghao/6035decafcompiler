// $ANTLR 2.7.7 (2006-11-01): "Parser.g" -> "DecafParser.java"$

  package decaf;
  import ir.ast.*;
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

	public final String  id() throws RecognitionException, TokenStreamException {
		String s;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST id_AST = null;
		Token  myid = null;
		AST myid_AST = null;
		
			s = null;	
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case ID:
			{
				{
				myid = LT(1);
				myid_AST = astFactory.create(myid);
				astFactory.addASTChild(currentAST, myid_AST);
				match(ID);
				s = myid.getText();
				}
				id_AST = (AST)currentAST.root;
				break;
			}
			case TK_Program:
			{
				{
				AST tmp1_AST = null;
				tmp1_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp1_AST);
				match(TK_Program);
				s = "Program";
				}
				id_AST = (AST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_0);
		}
		returnAST = id_AST;
		return s;
	}
	
	public final String  intLit() throws RecognitionException, TokenStreamException {
		String s;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST intLit_AST = null;
		Token  myint = null;
		AST myint_AST = null;
		
			s = null;	
		
		
		try {      // for error handling
			{
			myint = LT(1);
			myint_AST = astFactory.create(myint);
			astFactory.addASTChild(currentAST, myint_AST);
			match(INTLIT);
			s = myint.getText();
			}
			intLit_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_1);
		}
		returnAST = intLit_AST;
		return s;
	}
	
	public final String  charLit() throws RecognitionException, TokenStreamException {
		String s;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST charLit_AST = null;
		Token  c = null;
		AST c_AST = null;
		
			s = null;	
		
		
		try {      // for error handling
			{
			c = LT(1);
			c_AST = astFactory.create(c);
			astFactory.addASTChild(currentAST, c_AST);
			match(CHAR);
			s = c.getText();
			}
			charLit_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_1);
		}
		returnAST = charLit_AST;
		return s;
	}
	
	public final String  str() throws RecognitionException, TokenStreamException {
		String s;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST str_AST = null;
		Token  st = null;
		AST st_AST = null;
		
			s = null;
		
		
		try {      // for error handling
			{
			st = LT(1);
			st_AST = astFactory.create(st);
			astFactory.addASTChild(currentAST, st_AST);
			match(STRING);
			s=st.getText();
			}
			str_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_2);
		}
		returnAST = str_AST;
		return s;
	}
	
	public final ClassDecl  program() throws RecognitionException, TokenStreamException {
		ClassDecl classDecl;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST program_AST = null;
		
			List<FieldDecl> fieldDecls = new ArrayList<FieldDecl>();
			List<MethodDecl> methodDecls = new ArrayList<MethodDecl>();
			classDecl = new ClassDecl(fieldDecls, methodDecls);
			FieldDecl f;
			MethodDecl m;
		
		
		try {      // for error handling
			{
			AST tmp2_AST = null;
			tmp2_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp2_AST);
			match(TK_class);
			AST tmp3_AST = null;
			tmp3_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp3_AST);
			match(TK_Program);
			AST tmp4_AST = null;
			tmp4_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp4_AST);
			match(LCURLY);
			{
			_loop13:
			do {
				if ((LA(1)==TK_int||LA(1)==TK_boolean) && (LA(2)==TK_Program||LA(2)==ID) && (LA(3)==LSQUARE||LA(3)==COMMA||LA(3)==SEMI)) {
					f=field_decl();
					astFactory.addASTChild(currentAST, returnAST);
					fieldDecls.add(f);
				}
				else {
					break _loop13;
				}
				
			} while (true);
			}
			{
			_loop15:
			do {
				if (((LA(1) >= TK_int && LA(1) <= TK_void))) {
					m=method_decl();
					astFactory.addASTChild(currentAST, returnAST);
					methodDecls.add(m);
				}
				else {
					break _loop15;
				}
				
			} while (true);
			}
			AST tmp5_AST = null;
			tmp5_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp5_AST);
			match(RCURLY);
			AST tmp6_AST = null;
			tmp6_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp6_AST);
			match(Token.EOF_TYPE);
			}
			program_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_3);
		}
		returnAST = program_AST;
		return classDecl;
	}
	
	public final FieldDecl  field_decl() throws RecognitionException, TokenStreamException {
		FieldDecl fDecl;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST field_decl_AST = null;
		
			Type t; 
			List<Field> fList;
			fDecl = null;
		
		
		try {      // for error handling
			{
			t=type();
			astFactory.addASTChild(currentAST, returnAST);
			fList=field_decl_group_list();
			astFactory.addASTChild(currentAST, returnAST);
			fDecl = new FieldDecl(fList,t);
			AST tmp7_AST = null;
			tmp7_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp7_AST);
			match(SEMI);
			}
			field_decl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_4);
		}
		returnAST = field_decl_AST;
		return fDecl;
	}
	
	public final MethodDecl  method_decl() throws RecognitionException, TokenStreamException {
		MethodDecl mDecl;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST method_decl_AST = null;
		
			mDecl = new MethodDecl(); 
			Type rt;
			List<Parameter> params;
			Block b;
			String i;
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case TK_int:
			case TK_boolean:
			{
				{
				rt=type();
				astFactory.addASTChild(currentAST, returnAST);
				mDecl.setType(rt);
				}
				break;
			}
			case TK_void:
			{
				{
				AST tmp8_AST = null;
				tmp8_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp8_AST);
				match(TK_void);
				mDecl.setType(Type.VOID);
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
			AST tmp9_AST = null;
			tmp9_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp9_AST);
			match(LPAREN);
			params=method_decl_group_list();
			astFactory.addASTChild(currentAST, returnAST);
			AST tmp10_AST = null;
			tmp10_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp10_AST);
			match(RPAREN);
			b=block();
			astFactory.addASTChild(currentAST, returnAST);
			
				mDecl.setId(i);
				mDecl.setParameters(params);
				mDecl.setBlock(b);	
			
			}
			method_decl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_5);
		}
		returnAST = method_decl_AST;
		return mDecl;
	}
	
	public final Field  field_decl_group() throws RecognitionException, TokenStreamException {
		Field f;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST field_decl_group_AST = null;
		
			String i;
			String l;
			f = null;
		
		
		try {      // for error handling
			if ((LA(1)==TK_Program||LA(1)==ID) && (LA(2)==COMMA||LA(2)==SEMI)) {
				{
				i=id();
				astFactory.addASTChild(currentAST, returnAST);
				f = new Field(i);
				}
				field_decl_group_AST = (AST)currentAST.root;
			}
			else if ((LA(1)==TK_Program||LA(1)==ID) && (LA(2)==LSQUARE)) {
				{
				i=id();
				astFactory.addASTChild(currentAST, returnAST);
				AST tmp11_AST = null;
				tmp11_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp11_AST);
				match(LSQUARE);
				l=intLit();
				astFactory.addASTChild(currentAST, returnAST);
				AST tmp12_AST = null;
				tmp12_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp12_AST);
				match(RSQUARE);
				f = new Field(i,l);
				}
				field_decl_group_AST = (AST)currentAST.root;
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_6);
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
		
		
		try {      // for error handling
			{
			f=field_decl_group();
			astFactory.addASTChild(currentAST, returnAST);
			fields.add(f);
			{
			_loop22:
			do {
				if ((LA(1)==COMMA)) {
					AST tmp13_AST = null;
					tmp13_AST = astFactory.create(LT(1));
					astFactory.addASTChild(currentAST, tmp13_AST);
					match(COMMA);
					f=field_decl_group();
					astFactory.addASTChild(currentAST, returnAST);
					fields.add(f);
				}
				else {
					break _loop22;
				}
				
			} while (true);
			}
			}
			field_decl_group_list_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_7);
		}
		returnAST = field_decl_group_list_AST;
		return fields;
	}
	
	public final Type  type() throws RecognitionException, TokenStreamException {
		Type t;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST type_AST = null;
		
			t = null;	
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case TK_int:
			{
				{
				AST tmp14_AST = null;
				tmp14_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp14_AST);
				match(TK_int);
				t = Type.INT;
				}
				type_AST = (AST)currentAST.root;
				break;
			}
			case TK_boolean:
			{
				{
				AST tmp15_AST = null;
				tmp15_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp15_AST);
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
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_8);
		}
		returnAST = type_AST;
		return t;
	}
	
	public final Parameter  method_decl_group() throws RecognitionException, TokenStreamException {
		Parameter p;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST method_decl_group_AST = null;
		
			Type t;
			String i;
			p = null;
		
		
		try {      // for error handling
			{
			t=type();
			astFactory.addASTChild(currentAST, returnAST);
			i=id();
			astFactory.addASTChild(currentAST, returnAST);
			p = new Parameter(t,i);
			}
			method_decl_group_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_2);
		}
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
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case TK_int:
			case TK_boolean:
			{
				p=method_decl_group();
				astFactory.addASTChild(currentAST, returnAST);
				parameters.add(p);
				{
				_loop30:
				do {
					if ((LA(1)==COMMA)) {
						AST tmp16_AST = null;
						tmp16_AST = astFactory.create(LT(1));
						astFactory.addASTChild(currentAST, tmp16_AST);
						match(COMMA);
						p=method_decl_group();
						astFactory.addASTChild(currentAST, returnAST);
						parameters.add(p);
					}
					else {
						break _loop30;
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
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_9);
		}
		returnAST = method_decl_group_list_AST;
		return parameters;
	}
	
	public final Block  block() throws RecognitionException, TokenStreamException {
		Block b;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST block_AST = null;
		
			List<Statement> stmts = new ArrayList<Statement>();
			List<FieldDecl> fields = new ArrayList<FieldDecl>();
			FieldDecl f;
			Statement s;	
			b = null;
		
		
		try {      // for error handling
			AST tmp17_AST = null;
			tmp17_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp17_AST);
			match(LCURLY);
			{
			_loop72:
			do {
				if ((LA(1)==TK_int||LA(1)==TK_boolean)) {
					f=field_decl();
					astFactory.addASTChild(currentAST, returnAST);
					fields.add(f);
				}
				else {
					break _loop72;
				}
				
			} while (true);
			}
			{
			_loop74:
			do {
				if ((_tokenSet_10.member(LA(1)))) {
					s=statement();
					astFactory.addASTChild(currentAST, returnAST);
					stmts.add(s);
				}
				else {
					break _loop74;
				}
				
			} while (true);
			}
			AST tmp18_AST = null;
			tmp18_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp18_AST);
			match(RCURLY);
			b=new Block(stmts,fields);
			block_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_11);
		}
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
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case TK_true:
			{
				{
				t1 = LT(1);
				t1_AST = astFactory.create(t1);
				astFactory.addASTChild(currentAST, t1_AST);
				match(TK_true);
				bl = new BooleanLiteral(t1.getText());
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
				}
				boolLiteral_AST = (AST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_1);
		}
		returnAST = boolLiteral_AST;
		return bl;
	}
	
	public final Literal  literal() throws RecognitionException, TokenStreamException {
		Literal lit;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST literal_AST = null;
		
			String l;	
			String c;
			lit = null;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case INTLIT:
			{
				{
				l=intLit();
				astFactory.addASTChild(currentAST, returnAST);
				lit = new IntLiteral(l);
				}
				literal_AST = (AST)currentAST.root;
				break;
			}
			case CHAR:
			{
				{
				c=charLit();
				astFactory.addASTChild(currentAST, returnAST);
				lit = new CharLiteral(c);
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
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_1);
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
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case PLUS:
			{
				AST tmp19_AST = null;
				tmp19_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp19_AST);
				match(PLUS);
				bt = BinOpType.PLUS;
				break;
			}
			case MINUS:
			{
				AST tmp20_AST = null;
				tmp20_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp20_AST);
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
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_12);
		}
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
		
		
		try {      // for error handling
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
				AST tmp21_AST = null;
				tmp21_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp21_AST);
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
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_12);
		}
		returnAST = mul_op_AST;
		return bt;
	}
	
	public final BinOpType  rel_op() throws RecognitionException, TokenStreamException {
		BinOpType bt;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST rel_op_AST = null;
		
			bt = null;	
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case LESS:
			{
				AST tmp22_AST = null;
				tmp22_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp22_AST);
				match(LESS);
				bt = BinOpType.LE;
				break;
			}
			case MORE:
			{
				AST tmp23_AST = null;
				tmp23_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp23_AST);
				match(MORE);
				bt = BinOpType.GE;
				break;
			}
			case LEQ:
			{
				AST tmp24_AST = null;
				tmp24_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp24_AST);
				match(LEQ);
				bt = BinOpType.LEQ;
				break;
			}
			case GEQ:
			{
				AST tmp25_AST = null;
				tmp25_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp25_AST);
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
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_12);
		}
		returnAST = rel_op_AST;
		return bt;
	}
	
	public final BinOpType  eq_op() throws RecognitionException, TokenStreamException {
		BinOpType bt;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST eq_op_AST = null;
		
			bt = null;	
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case CEQ:
			{
				AST tmp26_AST = null;
				tmp26_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp26_AST);
				match(CEQ);
				bt = BinOpType.CEQ;
				break;
			}
			case NEQ:
			{
				AST tmp27_AST = null;
				tmp27_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp27_AST);
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
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_12);
		}
		returnAST = eq_op_AST;
		return bt;
	}
	
	public final AssignOpType  assign_op() throws RecognitionException, TokenStreamException {
		AssignOpType a;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST assign_op_AST = null;
		
			a = null;
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case ASSIGNPLUSEQ:
			{
				AST tmp28_AST = null;
				tmp28_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp28_AST);
				match(ASSIGNPLUSEQ);
				a = AssignOpType.INCREMENT;
				break;
			}
			case ASSIGNPLUSNEQ:
			{
				AST tmp29_AST = null;
				tmp29_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp29_AST);
				match(ASSIGNPLUSNEQ);
				a = AssignOpType.DECREMENT;
				break;
			}
			case ASSIGNEQ:
			{
				AST tmp30_AST = null;
				tmp30_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp30_AST);
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
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_12);
		}
		returnAST = assign_op_AST;
		return a;
	}
	
	public final Location  location() throws RecognitionException, TokenStreamException {
		Location loc;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST location_AST = null;
		
			Expression e;
			String i;
			loc = null;
		
		
		try {      // for error handling
			{
			if ((LA(1)==TK_Program||LA(1)==ID) && (_tokenSet_13.member(LA(2)))) {
				i=id();
				astFactory.addASTChild(currentAST, returnAST);
				loc = new VarLocation(i);
			}
			else if ((LA(1)==TK_Program||LA(1)==ID) && (LA(2)==LSQUARE)) {
				i=id();
				astFactory.addASTChild(currentAST, returnAST);
				AST tmp31_AST = null;
				tmp31_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp31_AST);
				match(LSQUARE);
				e=expr();
				astFactory.addASTChild(currentAST, returnAST);
				AST tmp32_AST = null;
				tmp32_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp32_AST);
				match(RSQUARE);
				loc = new ArrayLocation(i,e);
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			location_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_13);
		}
		returnAST = location_AST;
		return loc;
	}
	
	public final Expression  expr() throws RecognitionException, TokenStreamException {
		Expression e;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST expr_AST = null;
		
			e = null;	
		
		
		try {      // for error handling
			e=cond_or_term();
			astFactory.addASTChild(currentAST, returnAST);
			expr_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_14);
		}
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
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case TK_callout:
			case TK_true:
			case TK_false:
			case TK_Program:
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
				_loop61:
				do {
					if ((LA(1)==COMMA)) {
						AST tmp33_AST = null;
						tmp33_AST = astFactory.create(LT(1));
						astFactory.addASTChild(currentAST, tmp33_AST);
						match(COMMA);
						e=expr();
						astFactory.addASTChild(currentAST, returnAST);
						exprs.add(e);
					}
					else {
						break _loop61;
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
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_9);
		}
		returnAST = expr_argument_list_AST;
		return exprs;
	}
	
	public final CalloutArg  callout_argument() throws RecognitionException, TokenStreamException {
		CalloutArg arg;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST callout_argument_AST = null;
		
			Expression e;
			String s;
			arg = null;
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case TK_callout:
			case TK_true:
			case TK_false:
			case TK_Program:
			case LPAREN:
			case CHAR:
			case MINUS:
			case NOT:
			case ID:
			case INTLIT:
			{
				e=expr();
				astFactory.addASTChild(currentAST, returnAST);
				arg=new CalloutArg(e);
				break;
			}
			case STRING:
			{
				s=str();
				astFactory.addASTChild(currentAST, returnAST);
				arg=new CalloutArg(s);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			callout_argument_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_2);
		}
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
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case COMMA:
			{
				AST tmp34_AST = null;
				tmp34_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp34_AST);
				match(COMMA);
				a=callout_argument();
				astFactory.addASTChild(currentAST, returnAST);
				args.add(a);
				{
				_loop67:
				do {
					if ((LA(1)==COMMA)) {
						AST tmp35_AST = null;
						tmp35_AST = astFactory.create(LT(1));
						astFactory.addASTChild(currentAST, tmp35_AST);
						match(COMMA);
						a=callout_argument();
						astFactory.addASTChild(currentAST, returnAST);
						args.add(a);
					}
					else {
						break _loop67;
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
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_9);
		}
		returnAST = callout_argument_list_AST;
		return args;
	}
	
	public final CallExpr  method_call() throws RecognitionException, TokenStreamException {
		CallExpr callExpr;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST method_call_AST = null;
		
			List<CalloutArg> args;
			List<Expression> exprs;
			String i;
			String s;
			callExpr = null;
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case TK_Program:
			case ID:
			{
				i=id();
				astFactory.addASTChild(currentAST, returnAST);
				AST tmp36_AST = null;
				tmp36_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp36_AST);
				match(LPAREN);
				exprs=expr_argument_list();
				astFactory.addASTChild(currentAST, returnAST);
				AST tmp37_AST = null;
				tmp37_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp37_AST);
				match(RPAREN);
				callExpr = new MethodCallExpr(i,exprs);
				break;
			}
			case TK_callout:
			{
				AST tmp38_AST = null;
				tmp38_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp38_AST);
				match(TK_callout);
				AST tmp39_AST = null;
				tmp39_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp39_AST);
				match(LPAREN);
				s=str();
				astFactory.addASTChild(currentAST, returnAST);
				args=callout_argument_list();
				astFactory.addASTChild(currentAST, returnAST);
				AST tmp40_AST = null;
				tmp40_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp40_AST);
				match(RPAREN);
				callExpr = new CalloutExpr(s,args);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			method_call_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_1);
		}
		returnAST = method_call_AST;
		return callExpr;
	}
	
	public final Statement  statement() throws RecognitionException, TokenStreamException {
		Statement s;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST statement_AST = null;
		
			Location l;
			AssignOpType a;
			Expression e, e1, e2;
			CallExpr m;
			Block b, b1, b2;
			String i;
			s = null;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case TK_if:
			{
				{
				AST tmp41_AST = null;
				tmp41_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp41_AST);
				match(TK_if);
				AST tmp42_AST = null;
				tmp42_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp42_AST);
				match(LPAREN);
				e=expr();
				astFactory.addASTChild(currentAST, returnAST);
				AST tmp43_AST = null;
				tmp43_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp43_AST);
				match(RPAREN);
				b1=block();
				astFactory.addASTChild(currentAST, returnAST);
				s = new IfStmt(e,b1);
				{
				switch ( LA(1)) {
				case TK_else:
				{
					AST tmp44_AST = null;
					tmp44_AST = astFactory.create(LT(1));
					astFactory.addASTChild(currentAST, tmp44_AST);
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
				case TK_Program:
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
				AST tmp45_AST = null;
				tmp45_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp45_AST);
				match(TK_for);
				i=id();
				astFactory.addASTChild(currentAST, returnAST);
				AST tmp46_AST = null;
				tmp46_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp46_AST);
				match(ASSIGNEQ);
				e1=expr();
				astFactory.addASTChild(currentAST, returnAST);
				AST tmp47_AST = null;
				tmp47_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp47_AST);
				match(COMMA);
				e2=expr();
				astFactory.addASTChild(currentAST, returnAST);
				b=block();
				astFactory.addASTChild(currentAST, returnAST);
				s = new ForStmt(i,e1,e2,b);
				}
				statement_AST = (AST)currentAST.root;
				break;
			}
			case TK_return:
			{
				{
				{
				AST tmp48_AST = null;
				tmp48_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp48_AST);
				match(TK_return);
				s = new ReturnStmt();
				{
				switch ( LA(1)) {
				case TK_callout:
				case TK_true:
				case TK_false:
				case TK_Program:
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
				AST tmp49_AST = null;
				tmp49_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp49_AST);
				match(SEMI);
				}
				statement_AST = (AST)currentAST.root;
				break;
			}
			case TK_break:
			{
				{
				AST tmp50_AST = null;
				tmp50_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp50_AST);
				match(TK_break);
				s = new BreakStmt();
				AST tmp51_AST = null;
				tmp51_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp51_AST);
				match(SEMI);
				}
				statement_AST = (AST)currentAST.root;
				break;
			}
			case TK_continue:
			{
				{
				AST tmp52_AST = null;
				tmp52_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp52_AST);
				match(TK_continue);
				s = new ContinueStmt();
				AST tmp53_AST = null;
				tmp53_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp53_AST);
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
				if ((LA(1)==TK_Program||LA(1)==ID) && (_tokenSet_15.member(LA(2)))) {
					{
					l=location();
					astFactory.addASTChild(currentAST, returnAST);
					a=assign_op();
					astFactory.addASTChild(currentAST, returnAST);
					e=expr();
					astFactory.addASTChild(currentAST, returnAST);
					s = new AssignStmt(l,a,e);
					AST tmp54_AST = null;
					tmp54_AST = astFactory.create(LT(1));
					astFactory.addASTChild(currentAST, tmp54_AST);
					match(SEMI);
					}
					statement_AST = (AST)currentAST.root;
				}
				else if ((LA(1)==TK_callout||LA(1)==TK_Program||LA(1)==ID) && (LA(2)==LPAREN)) {
					{
					m=method_call();
					astFactory.addASTChild(currentAST, returnAST);
					s = new InvokeStmt((MethodCallExpr)m);
					AST tmp55_AST = null;
					tmp55_AST = astFactory.create(LT(1));
					astFactory.addASTChild(currentAST, tmp55_AST);
					match(SEMI);
					}
					statement_AST = (AST)currentAST.root;
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_16);
		}
		returnAST = statement_AST;
		return s;
	}
	
	public final Expression  unary_minus_term() throws RecognitionException, TokenStreamException {
		Expression e;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST unary_minus_term_AST = null;
		
			e = null;	
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case MINUS:
			{
				{
				AST tmp56_AST = null;
				tmp56_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp56_AST);
				match(MINUS);
				e=expr_static();
				astFactory.addASTChild(currentAST, returnAST);
				e = new UnaryOpExpr(UnaryOpType.MINUS,e);
				}
				unary_minus_term_AST = (AST)currentAST.root;
				break;
			}
			case TK_callout:
			case TK_true:
			case TK_false:
			case TK_Program:
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
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_1);
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
		
		
		try {      // for error handling
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
				AST tmp57_AST = null;
				tmp57_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp57_AST);
				match(LPAREN);
				e=expr();
				astFactory.addASTChild(currentAST, returnAST);
				AST tmp58_AST = null;
				tmp58_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp58_AST);
				match(RPAREN);
				}
				break;
			}
			default:
				if ((LA(1)==TK_Program||LA(1)==ID) && (_tokenSet_17.member(LA(2)))) {
					e=location();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else if ((LA(1)==TK_callout||LA(1)==TK_Program||LA(1)==ID) && (LA(2)==LPAREN)) {
					e=method_call();
					astFactory.addASTChild(currentAST, returnAST);
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			expr_static_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_1);
		}
		returnAST = expr_static_AST;
		return e;
	}
	
	public final Expression  not_term() throws RecognitionException, TokenStreamException {
		Expression e;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST not_term_AST = null;
		
			e = null;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case NOT:
			{
				{
				AST tmp59_AST = null;
				tmp59_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp59_AST);
				match(NOT);
				e=unary_minus_term();
				astFactory.addASTChild(currentAST, returnAST);
				e = new UnaryOpExpr(UnaryOpType.NOT, e);
				}
				not_term_AST = (AST)currentAST.root;
				break;
			}
			case TK_callout:
			case TK_true:
			case TK_false:
			case TK_Program:
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
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_1);
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
		
		
		try {      // for error handling
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
					else 	e = new TempExpression(bt, new BinOpExpr(se, te)); 
				
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
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_18);
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
			TempExpression te;
			e = null;
		
		
		try {      // for error handling
			se=not_term();
			astFactory.addASTChild(currentAST, returnAST);
			te=mul_term_temp();
			astFactory.addASTChild(currentAST, returnAST);
			
				if (te == null)	e = se; 
				else e = new BinOpExpr(se, te);		
			
			mul_term_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_18);
		}
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
		
		
		try {      // for error handling
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
					else e = new TempExpression(bt, new BinOpExpr(se, te)); 
				
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
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_19);
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
			TempExpression te;
			e = null;
		
		
		try {      // for error handling
			se=mul_term();
			astFactory.addASTChild(currentAST, returnAST);
			te=add_term_temp();
			astFactory.addASTChild(currentAST, returnAST);
			
				if (te == null)	e = se; 
				else e = new BinOpExpr(se,te);		
			
			add_term_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_19);
		}
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
		
		
		try {      // for error handling
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
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_20);
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
		
		
		try {      // for error handling
			se=add_term();
			astFactory.addASTChild(currentAST, returnAST);
			te=relation_term_temp();
			astFactory.addASTChild(currentAST, returnAST);
			
				if (te == null)	e = se; 
				else e = new BinOpExpr(se,te);		
			
			relation_term_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_20);
		}
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
		
		
		try {      // for error handling
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
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_21);
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
		
		
		try {      // for error handling
			se=relation_term();
			astFactory.addASTChild(currentAST, returnAST);
			te=equality_term_temp();
			astFactory.addASTChild(currentAST, returnAST);
			
				if (te == null)	e = se; 
				else e = new BinOpExpr(se, te);		
			
			equality_term_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_21);
		}
		returnAST = equality_term_AST;
		return e;
	}
	
	public final TempExpression  cond_and_term_temp() throws RecognitionException, TokenStreamException {
		TempExpression e;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST cond_and_term_temp_AST = null;
		
			BinOpType bt;
			TempExpression te;
			Expression se;	
			e = null;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case AND:
			{
				{
				AST tmp60_AST = null;
				tmp60_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp60_AST);
				match(AND);
				se=equality_term();
				astFactory.addASTChild(currentAST, returnAST);
				te=cond_and_term_temp();
				astFactory.addASTChild(currentAST, returnAST);
				
					if (te == null) e = new TempExpression(BinOpType.AND, se);
					else e = new TempExpression(BinOpType.AND, new BinOpExpr(se, te)); 
				
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
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_22);
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
		
		
		try {      // for error handling
			se=equality_term();
			astFactory.addASTChild(currentAST, returnAST);
			te=cond_and_term_temp();
			astFactory.addASTChild(currentAST, returnAST);
			
				if (te == null) e = se; 
				else e = new BinOpExpr(se,te);		
			
			cond_and_term_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_22);
		}
		returnAST = cond_and_term_AST;
		return e;
	}
	
	public final TempExpression  cond_or_term_temp() throws RecognitionException, TokenStreamException {
		TempExpression e;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST cond_or_term_temp_AST = null;
		
			BinOpType bt;
			TempExpression te;
			Expression se;	
			e = null;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case OR:
			{
				{
				AST tmp61_AST = null;
				tmp61_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp61_AST);
				match(OR);
				se=cond_and_term();
				astFactory.addASTChild(currentAST, returnAST);
				te=cond_or_term_temp();
				astFactory.addASTChild(currentAST, returnAST);
				
					if (te == null) e = new TempExpression(BinOpType.OR, se);
					else e = new TempExpression(BinOpType.OR, new BinOpExpr(se, te)); 
				
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
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_14);
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
		
		
		try {      // for error handling
			se=cond_and_term();
			astFactory.addASTChild(currentAST, returnAST);
			te=cond_or_term_temp();
			astFactory.addASTChild(currentAST, returnAST);
			
				if (te == null)	e = se; 
				else e = new BinOpExpr(se, te);		
			
			cond_or_term_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_14);
		}
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
		"\"Program\"",
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
		"HEX",
		"ASSIGNPLUSNEQ"
	};
	
	protected void buildTokenTypeASTClassMap() {
		tokenTypeToASTClassMap=null;
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 72092743797833728L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 35063855316992L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 270532608L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { 2L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = { 70368745213856L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = { 638976L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
		long[] data = { 805306368L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	private static final long[] mk_tokenSet_7() {
		long[] data = { 536870912L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_7 = new BitSet(mk_tokenSet_7());
	private static final long[] mk_tokenSet_8() {
		long[] data = { 70368744308736L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_8 = new BitSet(mk_tokenSet_8());
	private static final long[] mk_tokenSet_9() {
		long[] data = { 2097152L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_9 = new BitSet(mk_tokenSet_9());
	private static final long[] mk_tokenSet_10() {
		long[] data = { 70368744574880L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_10 = new BitSet(mk_tokenSet_10());
	private static final long[] mk_tokenSet_11() {
		long[] data = { 70368745213920L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_11 = new BitSet(mk_tokenSet_11());
	private static final long[] mk_tokenSet_12() {
		long[] data = { 246292887517184L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_12 = new BitSet(mk_tokenSet_12());
	private static final long[] mk_tokenSet_13() {
		long[] data = { 72092743792590848L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_13 = new BitSet(mk_tokenSet_13());
	private static final long[] mk_tokenSet_14() {
		long[] data = { 816054272L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_14 = new BitSet(mk_tokenSet_14());
	private static final long[] mk_tokenSet_15() {
		long[] data = { 72057679941468160L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_15 = new BitSet(mk_tokenSet_15());
	private static final long[] mk_tokenSet_16() {
		long[] data = { 70368745099168L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_16 = new BitSet(mk_tokenSet_16());
	private static final long[] mk_tokenSet_17() {
		long[] data = { 35063859511296L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_17 = new BitSet(mk_tokenSet_17());
	private static final long[] mk_tokenSet_18() {
		long[] data = { 35050970415104L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_18 = new BitSet(mk_tokenSet_18());
	private static final long[] mk_tokenSet_19() {
		long[] data = { 35047749189632L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_19 = new BitSet(mk_tokenSet_19());
	private static final long[] mk_tokenSet_20() {
		long[] data = { 32986164887552L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_20 = new BitSet(mk_tokenSet_20());
	private static final long[] mk_tokenSet_21() {
		long[] data = { 26389095120896L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_21 = new BitSet(mk_tokenSet_21());
	private static final long[] mk_tokenSet_22() {
		long[] data = { 17593002098688L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_22 = new BitSet(mk_tokenSet_22());
	
	}
