package decaf.codegen.flattener;

import java.util.ArrayList;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.JumpCondOp;
import decaf.codegen.flatir.JumpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.PushStmt;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.Register;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flatir.TempName;
import decaf.codegen.flatir.VarName;
import decaf.ir.ASTVisitor;
import decaf.ir.ast.ArrayLocation;
import decaf.ir.ast.AssignStmt;
import decaf.ir.ast.BinOpExpr;
import decaf.ir.ast.BinOpType;
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
import decaf.ir.ast.UnaryOpExpr;
import decaf.ir.ast.UnaryOpType;
import decaf.ir.ast.VarDecl;
import decaf.ir.ast.VarLocation;

public class ExpressionFlattenerVisitor implements ASTVisitor<Name> {	
	public static int MAXBOUNDCHECKS = 0;
	private List<LIRStatement> statements;
	private String methodName;
	private int andCount;
	private int orCount;
	private int currentAndId;
	private int currentOrId;
	private int strCount;
	private int inArrayLocation;
	private int arrayBoundId;
	private int callCount;
	private ClassDecl classDecl;

	public ExpressionFlattenerVisitor(List<LIRStatement> statements,
			String methodName, ClassDecl cd) {
		this.statements = statements;
		this.methodName = methodName;
		this.andCount = 0;
		this.orCount = 0;
		this.currentAndId = 0;
		this.currentOrId = 0;
		this.strCount = 0;
		this.inArrayLocation = 0;
		this.callCount = 0;
		this.classDecl = cd;
	}

	@Override
	public Name visit(ArrayLocation loc) {
		this.inArrayLocation++;
		
		Name rtnName = null;
		
		String id = loc.getId();
		Name index = loc.getExpr().accept(this);
		
		// Add index out of bound check
		addArrayBoundCheck(loc, index);
		
		// Check for nested array accesses
		if (inArrayLocation <= 1) {
			rtnName = new ArrayName(id, index);
		}
		else {
			ArrayName arrayName = new ArrayName(id, index);
			rtnName = new TempName();
			this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, rtnName, arrayName, null));
		}
		
		this.inArrayLocation--;
		
		return rtnName;
	}

	@Override
	public Name visit(AssignStmt stmt) {
		return null;
	}

	@Override
	public Name visit(BinOpExpr expr) {
		BinOpType op = expr.getOperator();

		if (op == BinOpType.AND) {
			if (expr.getLeftOperand().getClass().equals(BooleanLiteral.class) ||
					expr.getRightOperand().getClass().equals(BooleanLiteral.class)) {
				return optimizeAnd(expr);
			}
			return shortCircuitAnd(expr);
		}

		if (op == BinOpType.OR) {
			if (expr.getLeftOperand().getClass().equals(BooleanLiteral.class) ||
					expr.getRightOperand().getClass().equals(BooleanLiteral.class)) {
				return optimizeOr(expr);
			}
			return shortCircuitOr(expr);
		}

		Name arg1 = expr.getLeftOperand().accept(this);
		Name arg2 = expr.getRightOperand().accept(this);

		QuadrupletOp qOp = null;
		switch (op) {
			case PLUS:
				qOp = QuadrupletOp.ADD;
				break;
			case MINUS:
				qOp = QuadrupletOp.SUB;
				break;
			case MULTIPLY:
				qOp = QuadrupletOp.MUL;
				break;
			case DIVIDE:
				qOp = QuadrupletOp.DIV;
				break;
			case MOD:
				qOp = QuadrupletOp.MOD;
				break;
			case LE:
				qOp = QuadrupletOp.LT;
				break;
			case LEQ:
				qOp = QuadrupletOp.LTE;
				break;
			case GE:
				qOp = QuadrupletOp.GT;
				break;
			case GEQ:
				qOp = QuadrupletOp.GTE;
				break;
			case CEQ:
				qOp = QuadrupletOp.EQ;
				break;
			case NEQ:
				qOp = QuadrupletOp.NEQ;
				break;
		}

		TempName dest = new TempName();
		QuadrupletStmt qStmt = new QuadrupletStmt(qOp, dest, arg1, arg2);
		this.statements.add(qStmt);

		return dest;
	}

	@Override
	public Name visit(Block block) {
		return null;
	}

	@Override
	public Name visit(BooleanLiteral lit) {
		return new ConstantName(lit.getValue());
	}

	@Override
	public Name visit(BreakStmt stmt) {
		return null;
	}

	@Override
	public Name visit(CalloutArg arg) {
		if (arg.isString()) {
			// Store strings as global locations and store strings 
			// as global locations
			String uniqueId = methodName + "_str" + Integer.toString(strCount);
			strCount++;
			return new VarName(uniqueId, true, arg.getStringArg());
		} else {
			return arg.getExpression().accept(this);
		}
	}

	@Override
	public Name visit(CalloutExpr expr) {
		Name rtnValue = new TempName();
		
		// Go through arguments and generate List<Name>
		List<Name> argNames = new ArrayList<Name>();
		for (CalloutArg arg : expr.getArguments()) {
			Name argName = this.visit(arg);
			argNames.add(argName);
		}
		
		LabelStmt start = new LabelStmt(getMethodCallStart(expr.getMethodName().substring(1, expr.getMethodName().length()-2)));
		LabelStmt end = new LabelStmt(getMethodCallEnd(expr.getMethodName().substring(1, expr.getMethodName().length()-2)));
		callCount++;
		
		// Add method call label	
		this.statements.add(start);
		
		// Save args in registers
		for (int i = 0; i < expr.getArguments().size() && i < Register.argumentRegs.length; i++) {
			this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE,
					new RegisterName(Register.argumentRegs[i]), argNames.get(i), null));
		}
		
		// Push other args to stack
		if (expr.getArguments().size() > Register.argumentRegs.length) {
			for (int i = expr.getArguments().size() - 1; i >= Register.argumentRegs.length; i--) {
				this.statements.add(new PushStmt(argNames.get(i)));
			}
		}
		
		//Set %rax to 0
		this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, new RegisterName(Register.RAX), new ConstantName(0), null));
		
		// Call method
		this.statements.add(new CallStmt(expr.getMethodName()));
		
		// Pop args off stack
		if (expr.getArguments().size() > Register.argumentRegs.length) {
			int sizeToDecrease = expr.getArguments().size() - Register.argumentRegs.length;
			RegisterName rsp = new RegisterName(Register.RSP);
			this.statements.add(new QuadrupletStmt(QuadrupletOp.SUB, rsp, rsp, new ConstantName(sizeToDecrease)));
		}
		
		// Save return value
		this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, rtnValue, new RegisterName(Register.RAX), null));
		
		this.statements.add(end);

		return rtnValue;
	}

	@Override
	public Name visit(CharLiteral lit) {
		int value = (int) lit.getValue().charAt(0);
		ConstantName constant = new ConstantName(value);
		return constant;
	}

	@Override
	public Name visit(ClassDecl cd) {
		return null;
	}

	@Override
	public Name visit(ContinueStmt stmt) {
		return null;
	}

	@Override
	public Name visit(Field f) {
		return null;
	}

	@Override
	public Name visit(FieldDecl fd) {
		return null;
	}

	@Override
	public Name visit(ForStmt stmt) {
		return null;
	}

	@Override
	public Name visit(IfStmt stmt) {
		return null;
	}

	@Override
	public Name visit(IntLiteral lit) {
		return new ConstantName(lit.getValue());
	}

	@Override
	public Name visit(InvokeStmt stmt) {
		return null;
	}

	@Override
	public Name visit(MethodCallExpr expr) {
		Name rtnValue = new TempName();
		
		// Go through arguments and generate List<Name>
		List<Name> argNames = new ArrayList<Name>();
		for (Expression arg : expr.getArguments()) {
			Name argName = arg.accept(this);
			argNames.add(argName);
		}
		
		LabelStmt start = new LabelStmt(getMethodCallStart(expr.getName()));
		LabelStmt end = new LabelStmt(getMethodCallEnd(expr.getName()));
		callCount++;
		
		// Add method call label		
		this.statements.add(start);
		
		// Save args in registers
		for (int i = 0; i < argNames.size() && i < Register.argumentRegs.length; i++) {
			this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE,
					new RegisterName(Register.argumentRegs[i]), argNames.get(i), null));
		}
		
		// Push other args to stack
		if (expr.getArguments().size() > Register.argumentRegs.length) {
			for (int i = expr.getArguments().size() - 1; i >= Register.argumentRegs.length; i--) {
				this.statements.add(new PushStmt(argNames.get(i)));
			}
		}
		
		// Call method
		this.statements.add(new CallStmt(expr.getName()));
		
		// Pop args off stack
		if (expr.getArguments().size() > Register.argumentRegs.length) {
			int sizeToDecrease = expr.getArguments().size() - Register.argumentRegs.length;
			RegisterName rsp = new RegisterName(Register.RSP);
			this.statements.add(new QuadrupletStmt(QuadrupletOp.SUB, rsp, rsp, new ConstantName(sizeToDecrease)));
		}
		
		// Save return value
		this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, rtnValue, new RegisterName(Register.RAX), null));
		
		// Add method call end label
		this.statements.add(end);

		return rtnValue;
	}

	@Override
	public Name visit(MethodDecl md) {
		return null;
	}

	@Override
	public Name visit(Parameter param) {
		return null;
	}

	@Override
	public Name visit(ReturnStmt stmt) {
		return null;
	}

	@Override
	public Name visit(UnaryOpExpr expr) {
		Name arg1 = expr.getExpression().accept(this);
		UnaryOpType op = expr.getOperator();
		QuadrupletOp qOp = null;
		switch (op) {
			case NOT:
				qOp = QuadrupletOp.NOT;
				break;
			case MINUS:
				qOp = QuadrupletOp.MINUS;
				break;
		}

		TempName dest = new TempName();
		QuadrupletStmt qStmt = new QuadrupletStmt(qOp, dest, arg1, null);
		this.statements.add(qStmt);

		return dest;
	}

	@Override
	public Name visit(VarDecl vd) {
		return null;
	}

	@Override
	public Name visit(VarLocation loc) {
		String id = loc.getId();
		VarName varName = new VarName(id);
		varName.setBlockId(loc.getBlockId());
		
		if (loc.getBlockId() == -2) { // Set stack param field
			for (MethodDecl md: this.classDecl.getMethodDeclarations()) {
				if (md.getId().equals(this.methodName)) {
					for (int i = 0; i < md.getParameters().size(); i++) {
						Parameter param =  md.getParameters().get(i);
						if (param.getId().equals(id) && i > 5) { // 7th param onward
							varName.setStackParam(true);
						}
					}
				}
			}
		}
				
		return varName;
	}

	private Name shortCircuitAnd(BinOpExpr expr) {
		int oldAndId = currentAndId;
		currentAndId = ++andCount;

		Name dest = new TempName();

		// Test LHS
		this.statements.add(new LabelStmt(getAndTestLHS()));
		Name lhs = expr.getLeftOperand().accept(this);
		this.statements.add(new CmpStmt(lhs, new ConstantName(0)));
		this.statements.add(new JumpStmt(JumpCondOp.NEQ, new LabelStmt(
				getAndTestRHS())));
		this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, dest,
				new ConstantName(0), null));
		this.statements.add(new JumpStmt(JumpCondOp.NONE, new LabelStmt(
				getAndEnd())));

		// Test RHS
		this.statements.add(new LabelStmt(getAndTestRHS()));
		Name rhs = expr.getRightOperand().accept(this);
		this.statements
				.add(new QuadrupletStmt(QuadrupletOp.MOVE, dest, rhs, null));

		// End block
		this.statements.add(new LabelStmt(getAndEnd()));

		currentAndId = oldAndId;

		return dest;
	}

	private Name shortCircuitOr(BinOpExpr expr) {
		int oldOrId = currentOrId;
		currentOrId = ++orCount;

		Name dest = new TempName();

		// Test LHS
		this.statements.add(new LabelStmt(getOrTestLHS()));
		Name lhs = expr.getLeftOperand().accept(this);
		this.statements.add(new CmpStmt(lhs, new ConstantName(0)));
		this.statements.add(new JumpStmt(JumpCondOp.EQ, new LabelStmt(
				getOrTestRHS())));
		this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, dest,
				new ConstantName(1), null));
		this.statements.add(new JumpStmt(JumpCondOp.NONE, new LabelStmt(
				getOrEnd())));

		// Test RHS
		this.statements.add(new LabelStmt(getOrTestRHS()));
		Name rhs = expr.getRightOperand().accept(this);
		this.statements
				.add(new QuadrupletStmt(QuadrupletOp.MOVE, dest, rhs, null));

		// End block
		this.statements.add(new LabelStmt(getOrEnd()));

		currentOrId = oldOrId;

		return dest;
	}

	private String getAndTestLHS() {
		return methodName + ".and" + currentAndId + ".testLHS";
	}

	private String getAndTestRHS() {
		return methodName + ".and" + currentAndId + ".testRHS";
	}

	private String getAndEnd() {
		return methodName + ".and" + currentAndId + ".end";
	}

	private String getOrTestLHS() {
		return methodName + ".or" + currentOrId + ".testLHS";
	}

	private String getOrTestRHS() {
		return methodName + ".or" + currentOrId + ".testRHS";
	}

	private String getOrEnd() {
		return methodName + ".or" + currentOrId + ".end";
	}
	
	private void addArrayBoundCheck(ArrayLocation loc, Name index) {
		LabelStmt arrayCheckStart = new LabelStmt(getArrayBoundBegin(loc.getId()));
		LabelStmt arrayCheckPass = new LabelStmt(getArrayBoundPass(loc.getId()));
		LabelStmt arrayCheckFail = new LabelStmt(getArrayBoundFail(loc.getId()));
		arrayBoundId++;
		if (ExpressionFlattenerVisitor.MAXBOUNDCHECKS < arrayBoundId) {
			ExpressionFlattenerVisitor.MAXBOUNDCHECKS = arrayBoundId;
		}
		
		this.statements.add(arrayCheckStart);
		
		//Name index = loc.getExpr().accept(this); // Re-eval expressions
		this.statements.add(new CmpStmt(index, new ConstantName(loc.getSize())));
		this.statements.add(new JumpStmt(JumpCondOp.GTE, arrayCheckFail)); // size >= length?
		
		//index = loc.getExpr().accept(this); // Re-eval expressions
		this.statements.add(new CmpStmt(index, new ConstantName(0)));
		this.statements.add(new JumpStmt(JumpCondOp.GTE, arrayCheckPass));
//		this.statements.add(new JumpStmt(JumpCondOp.LT, arrayCheckFail)); // size >= 0? --> pass, else flow to fail
//		this.statements.add(new JumpStmt(JumpCondOp.NONE, arrayCheckPass)); // passed
		
		// Exception Handler
		this.statements.add(arrayCheckFail);
		VarName error = new VarName(ProgramFlattener.arrayExceptionErrorLabel);
		error.setIsString(true);
		
		// Move args to regs
		this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE,
				new RegisterName(Register.argumentRegs[0]), error, null));
		this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE,
				new RegisterName(Register.argumentRegs[1]), new ConstantName(loc.getLineNumber()), null));
		this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE,
				new RegisterName(Register.argumentRegs[2]), new ConstantName(loc.getColumnNumber()), null));
		
		// Call exception handler
		this.statements.add(new CallStmt(ProgramFlattener.exceptionHandlerLabel));
		
		this.statements.add(arrayCheckPass); // Array check passed label
		
	}
	
	private String getArrayBoundBegin(String name) {
		return methodName + ".array." + name + "." + arrayBoundId + ".begin";
	}
	
	private String getArrayBoundFail(String name) {
		return methodName + ".array." + name + "." + arrayBoundId + ".fail";
	}
	
	private String getArrayBoundPass(String name) {
		return methodName + ".array." + name + "." + arrayBoundId + ".pass";
	}
	
	private String getMethodCallStart(String name) {
		return methodName + ".mcall." + name + "." + callCount + ".begin";
	}
	
	private String getMethodCallEnd(String name) {
		return methodName + ".mcall." + name + "." + callCount + ".end";
	}
	
	private Name optimizeOr(BinOpExpr expr) {
		BooleanLiteral arg1 = null;
		BooleanLiteral arg2 = null;
		TempName dest = new TempName();
		
		if (expr.getLeftOperand().getClass().equals(BooleanLiteral.class)) {
			arg1 = (BooleanLiteral)expr.getLeftOperand();
		}
		
		if (expr.getRightOperand().getClass().equals(BooleanLiteral.class)) {
			arg2 = (BooleanLiteral)expr.getRightOperand();
		}
		
		if (arg1 != null && arg2 != null) {
			boolean a = arg1.getValue() == 1 ? true : false;
			boolean b = arg2.getValue() == 1 ? true : false;
			
			if (a || b) {
				this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, dest, new ConstantName(1), null));
			}
			else {
				this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, dest, new ConstantName(0), null));
			}
		}
		else if (arg1 != null) {
			boolean a = arg1.getValue() == 1 ? true : false;
			
			if (a) {
				this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, dest, new ConstantName(1), null));
			}
			else {
				Name val = expr.getRightOperand().accept(this);
				this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, dest, val, null));
			}
		}
		else {
			boolean b = arg2.getValue() == 1 ? true : false;
			
			if (b) {
				this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, dest, new ConstantName(1), null));
			}
			else {
				Name val = expr.getLeftOperand().accept(this);
				this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, dest, val, null));
			}
		}
		
		return dest;
	}

	private Name optimizeAnd(BinOpExpr expr) {
		BooleanLiteral arg1 = null;
		BooleanLiteral arg2 = null;
		TempName dest = new TempName();
		
		if (expr.getLeftOperand().getClass().equals(BooleanLiteral.class)) {
			arg1 = (BooleanLiteral)expr.getLeftOperand();
		}
		
		if (expr.getRightOperand().getClass().equals(BooleanLiteral.class)) {
			arg2 = (BooleanLiteral)expr.getRightOperand();
		}
		
		if (arg1 != null && arg2 != null) {
			boolean a = arg1.getValue() == 1 ? true : false;
			boolean b = arg2.getValue() == 1 ? true : false;
			
			if (a && b) {
				this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, dest, new ConstantName(1), null));
			}
			else {
				this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, dest, new ConstantName(0), null));
			}
		}
		else if (arg1 != null) {
			boolean a = arg1.getValue() == 1 ? true : false;
			
			if (a) {
				Name val = expr.getRightOperand().accept(this);
				this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, dest, val, null));
			}
			else {
				this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, dest, new ConstantName(0), null));
			}
		}
		else {
			boolean b = arg2.getValue() == 1 ? true : false;
			
			if (b) {
				Name val = expr.getLeftOperand().accept(this);
				this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, dest, val, null));
			}
			else {
				this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, dest, new ConstantName(0), null));
			}
		}
		
		return dest;
	}
}
