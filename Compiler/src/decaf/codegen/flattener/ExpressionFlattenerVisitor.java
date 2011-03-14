package decaf.codegen.flattener;

import java.util.ArrayList;
import java.util.List;

import decaf.codegen.flatir.ArrayName;
import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.Constant;
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
	private Register[] argumentRegs = { Register.RDI, Register.RSI,
			Register.RDX, Register.RCX, Register.R8, Register.R9 };
	private List<LIRStatement> statements;
	private String methodName;
	private int andCount;
	private int orCount;
	private int currentAndId;
	private int currentOrId;
	private int strCount;
	private int inArrayLocation;
	private int arrayBoundId;

	public ExpressionFlattenerVisitor(List<LIRStatement> statements,
			String methodName) {
		this.statements = statements;
		this.methodName = methodName;
		this.andCount = 0;
		this.orCount = 0;
		this.currentAndId = 0;
		this.currentOrId = 0;
		this.strCount = 0;
		this.inArrayLocation = 0;
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
			return shortCircuitAnd(expr);
		}

		if (op == BinOpType.OR) {
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
		return new Constant(lit.getValue());
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
		
		// Save args in registers
		for (int i = 0; i < expr.getArguments().size() && i < argumentRegs.length; i++) {
			this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE,
					new RegisterName(argumentRegs[i]), argNames.get(i), null));
		}
		
		// Push other args to stack
		if (expr.getArguments().size() > argumentRegs.length) {
			for (int i = expr.getArguments().size() - 1; i >= argumentRegs.length; i--) {
				this.statements.add(new PushStmt(argNames.get(i)));
			}
		}
		
		//Set %rax to 0
		this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, new RegisterName(Register.RAX), new Constant(0), null));
		
		// Call method
		this.statements.add(new CallStmt(expr.getMethodName()));
		
		// Pop args off stack
		if (expr.getArguments().size() > argumentRegs.length) {
			int sizeToDecrease = expr.getArguments().size() - argumentRegs.length;
			RegisterName rsp = new RegisterName(Register.RSP);
			this.statements.add(new QuadrupletStmt(QuadrupletOp.SUB, rsp, rsp, new Constant(sizeToDecrease)));
		}
		
		// Save return value
		this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, rtnValue, new RegisterName(Register.RAX), null));

		return rtnValue;
	}

	@Override
	public Name visit(CharLiteral lit) {
		int value = (int) lit.getValue().charAt(0);
		Constant constant = new Constant(value);
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
		return new Constant(lit.getValue());
	}

	@Override
	public Name visit(InvokeStmt stmt) {
		return null;
	}

	@Override
	public Name visit(MethodCallExpr expr) {
		Name rtnValue = new TempName();
		
		// Save args in registers
		for (int i = 0; i < expr.getArguments().size() && i < argumentRegs.length; i++) {
			Name src = expr.getArguments().get(i).accept(this);
			this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE,
					new RegisterName(argumentRegs[i]), src, null));
		}
		
		// Push other args to stack
		if (expr.getArguments().size() > argumentRegs.length) {
			for (int i = expr.getArguments().size() - 1; i >= argumentRegs.length; i--) {
				this.statements.add(new PushStmt(expr.getArguments().get(i).accept(this)));
			}
		}
		
		// Call method
		this.statements.add(new CallStmt(expr.getName()));
		
		// Pop args off stack
		if (expr.getArguments().size() > argumentRegs.length) {
			int sizeToDecrease = expr.getArguments().size() - argumentRegs.length;
			RegisterName rsp = new RegisterName(Register.RSP);
			this.statements.add(new QuadrupletStmt(QuadrupletOp.SUB, rsp, rsp, new Constant(sizeToDecrease)));
		}
		
		// Save return value
		this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, rtnValue, new RegisterName(Register.RAX), null));

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
		return varName;
	}

	private Name shortCircuitAnd(BinOpExpr expr) {
		int oldAndId = currentAndId;
		currentAndId = ++andCount;

		Name dest = new TempName();

		// Test LHS
		this.statements.add(new LabelStmt(getAndTestLHS()));
		Name lhs = expr.getLeftOperand().accept(this);
		this.statements.add(new QuadrupletStmt(QuadrupletOp.CMP, null, lhs,
				new Constant(0)));
		this.statements.add(new JumpStmt(JumpCondOp.NEQ, new LabelStmt(
				getAndTestRHS())));
		this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, dest,
				new Constant(0), null));
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
		this.statements.add(new QuadrupletStmt(QuadrupletOp.CMP, null, lhs,
				new Constant(0)));
		this.statements.add(new JumpStmt(JumpCondOp.EQ, new LabelStmt(
				getOrTestRHS())));
		this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, dest,
				new Constant(1), null));
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
		return methodName + "_and" + currentAndId + "_testLHS";
	}

	private String getAndTestRHS() {
		return methodName + "_and" + currentAndId + "_testRHS";
	}

	private String getAndEnd() {
		return methodName + "_and" + currentAndId + "_end";
	}

	private String getOrTestLHS() {
		return methodName + "_or" + currentOrId + "_testLHS";
	}

	private String getOrTestRHS() {
		return methodName + "_or" + currentOrId + "_testRHS";
	}

	private String getOrEnd() {
		return methodName + "_or" + currentOrId + "_end";
	}
	
	private void addArrayBoundCheck(ArrayLocation loc, Name index) {
		LabelStmt arrayCheckPass = new LabelStmt(getArrayBoundPass());
		LabelStmt arrayCheckFail = new LabelStmt(getArrayBoundFail());
		arrayBoundId++;
		
		this.statements.add(new QuadrupletStmt(QuadrupletOp.CMP, null, index, new Constant(loc.getSize())));
		this.statements.add(new JumpStmt(JumpCondOp.GTE, arrayCheckFail)); // size >= length?
		this.statements.add(new QuadrupletStmt(QuadrupletOp.CMP, null, index, new Constant(0)));
		this.statements.add(new JumpStmt(JumpCondOp.LT, arrayCheckFail)); // size < 0?
		this.statements.add(new JumpStmt(JumpCondOp.NONE, arrayCheckPass)); // passed
		
		// Exception Handler
		this.statements.add(arrayCheckFail);
		VarName error = new VarName("$." + ProgramFlattener.exceptionErrorLabel);
		error.isString();
		
		// Move args to regs
		this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE,
				new RegisterName(argumentRegs[0]), error, null));
		this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE,
				new RegisterName(argumentRegs[1]), new Constant(loc.getLineNumber()), null));
		this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE,
				new RegisterName(argumentRegs[2]), new Constant(loc.getColumnNumber()), null));
		
		// Call exception handler
		this.statements.add(new CallStmt(ProgramFlattener.exceptionHandlerLabel));
		
		this.statements.add(arrayCheckPass); // Array check passed label
		
	}
	
	private String getArrayBoundFail() {
		return methodName + "_array" + arrayBoundId + "_fail";
	}
	
	private String getArrayBoundPass() {
		return methodName + "_array" + arrayBoundId + "_pass";
	}
}
