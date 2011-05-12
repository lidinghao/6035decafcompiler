package decaf.codegen.flattener;

import java.util.ArrayList;
import java.util.List;

import decaf.codegen.flatir.CallStmt;
import decaf.codegen.flatir.CmpStmt;
import decaf.codegen.flatir.ConstantName;
import decaf.codegen.flatir.EnterStmt;
import decaf.codegen.flatir.JumpCondOp;
import decaf.codegen.flatir.JumpStmt;
import decaf.codegen.flatir.LIRStatement;
import decaf.codegen.flatir.LabelStmt;
import decaf.codegen.flatir.LeaveStmt;
import decaf.codegen.flatir.Name;
import decaf.codegen.flatir.QuadrupletOp;
import decaf.codegen.flatir.QuadrupletStmt;
import decaf.codegen.flatir.Register;
import decaf.codegen.flatir.RegisterName;
import decaf.codegen.flatir.TempName;
import decaf.codegen.flatir.VarName;
import decaf.ir.ASTVisitor;
import decaf.ir.ast.ArrayLocation;
import decaf.ir.ast.AssignOpType;
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
import decaf.ir.ast.VarDecl;
import decaf.ir.ast.VarLocation;

public class MethodFlattenerVisitor implements ASTVisitor<Integer> {
	public static int DEPTH = 0;
	
	private List<LIRStatement> statements;
	private ExpressionFlattenerVisitor exprFlatenner;
	private String methodName;
	private int ifCount; // 0 in if and for count means method local block
	private int forCount;
	private int currentIfId;
	private int currentForId;
	private int totalLocalVars;
	private ClassDecl classDecl;

	public MethodFlattenerVisitor(String methodName, ClassDecl cd) {
		this.classDecl = cd;
		this.statements = new ArrayList<LIRStatement>();
		this.exprFlatenner = new ExpressionFlattenerVisitor(statements,
				methodName, this.classDecl);
		this.methodName = methodName;
		this.reset();
	}

	public List<LIRStatement> getStatements() {
		return this.statements;
	}

	@Override
	public Integer visit(ArrayLocation loc) {
		return 0;
	}

	@Override
	public Integer visit(AssignStmt stmt) {
		Name dest = stmt.getLocation().accept(exprFlatenner);
		Name src = stmt.getExpression().accept(exprFlatenner);

		AssignOpType op = stmt.getOperator();
		if (op == AssignOpType.ASSIGN) {
			if (!src.getClass().equals(TempName.class)) {
				this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, dest, src, null));
			}
			else {
			// OPTIMIZE
				if (this.statements.get(this.statements.size() - 1).getClass().equals(QuadrupletStmt.class)) {
					QuadrupletStmt qStmt = (QuadrupletStmt) this.statements.get(this.statements.size() - 1);
					qStmt.setDestination(dest);
				}
				else {
					this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, dest, src, null));
				}
			}
			
		} else if (op == AssignOpType.INCREMENT) {
			this.statements.add(new QuadrupletStmt(QuadrupletOp.ADD, dest, dest,
					src));
		} else {
			this.statements.add(new QuadrupletStmt(QuadrupletOp.SUB, dest, dest,
					src));
		}

		return 0;
	}

	@Override
	public Integer visit(BinOpExpr expr) {
		return 0;
	}

	@Override
	public Integer visit(Block block) {
		// Initialize local vars to zero/false
		for (VarDecl vd : block.getVarDeclarations()) {
			for (String v : vd.getVariables()) {
				VarName varName = new VarName(v);
				varName.setBlockId(block.getBlockId());
				this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, varName,
						new ConstantName(0), null));
			}

			this.totalLocalVars += vd.getVariables().size(); // Add to the total
																				// local vars in
																				// method
		}

		for (Statement s : block.getStatements()) {
			s.accept(this);
		}

		return 0;
	}

	@Override
	public Integer visit(BooleanLiteral lit) {
		return 0;
	}

	@Override
	public Integer visit(BreakStmt stmt) {
		// Can only occur in a for loop
		statements.add(new JumpStmt(JumpCondOp.NONE, new LabelStmt(getForEnd())));

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
		return 0;
	}

	@Override
	public Integer visit(ContinueStmt stmt) {
		// Can only occur in for loop
		this.statements.add(new JumpStmt(JumpCondOp.NONE, new LabelStmt(
				getForIncrement())));

		return 0;
	}

	@Override
	public Integer visit(Field f) {
		return 0;
	}

	@Override
	public Integer visit(FieldDecl fd) {
		return 0;
	}

	@Override
	public Integer visit(ForStmt stmt) {
		int oldForId = currentForId;
		currentForId = ++forCount;

		VarName loopId = new VarName(stmt.getId());
		loopId.setBlockId(stmt.getBlock().getBlockId());

		// Increment local variable count (because for loop not "declared" in the
		// body)
		this.totalLocalVars++;

		// Initialization block
		Name initValue = stmt.getInitialValue().accept(this.exprFlatenner);
		this.statements.add(new LabelStmt(getForInit()));
		this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, loopId,
				initValue, null));

		MethodFlattenerVisitor.DEPTH++;
		
		// Test block		
		this.statements.add(new LabelStmt(getForTest()));
		Name finalValue = stmt.getFinalValue().accept(this.exprFlatenner);
		//TempName dest = new TempName();
		//this.statements.add(new QuadrupletStmt(QuadrupletOp.LT, dest, loopId, finalValue));
		//this.statements.add(new CmpStmt(dest, new ConstantName(0)));
		this.statements.add(new CmpStmt(loopId, finalValue));
		this.statements.add(new JumpStmt(JumpCondOp.GTE, new LabelStmt(
				getForEnd())));

		// Body block
		this.statements.add(new LabelStmt(getForBody()));
		stmt.getBlock().accept(this);

		// Increment loop var and jump to test
		this.statements.add(new LabelStmt(getForIncrement()));
		this.statements.add(new QuadrupletStmt(QuadrupletOp.ADD, loopId, loopId,
				new ConstantName(1)));
		this.statements.add(new JumpStmt(JumpCondOp.NONE, new LabelStmt(
				getForTest())));
		
		MethodFlattenerVisitor.DEPTH--;

		// End block
		this.statements.add(new LabelStmt(getForEnd()));

		currentForId = oldForId; // Return to parent for loop (if any)

		return 0;
	}

	@Override
	public Integer visit(IfStmt stmt) {
		int oldIfId = currentIfId;
		currentIfId = ++ifCount;

		// Test block
		this.statements.add(new LabelStmt(getIfTest()));
		Name condition = stmt.getCondition().accept(this.exprFlatenner);
		this.statements.add(new CmpStmt(condition, new ConstantName(0)));
		
		if (stmt.getElseBlock() != null) {
			this.statements.add(new JumpStmt(JumpCondOp.EQ, new LabelStmt(
					getIfElse())));
		}
		else {
			this.statements.add(new JumpStmt(JumpCondOp.EQ, new LabelStmt(
					getIfEnd())));
		}
		
		// True block
		this.statements.add(new LabelStmt(getIfTrue()));
		stmt.getIfBlock().accept(this);

		// Else block (if any)
		if (stmt.getElseBlock() != null) {
			this.statements.add(new JumpStmt(JumpCondOp.NONE, new LabelStmt(
					getIfEnd())));
			
			this.statements.add(new LabelStmt(getIfElse()));
			
			stmt.getElseBlock().accept(this);
		}

		// End block
		this.statements.add(new LabelStmt(getIfEnd()));

		currentIfId = oldIfId; // Return to parent if (if any)

		return 0;
	}

	@Override
	public Integer visit(IntLiteral lit) {
		return 0;
	}

	@Override
	public Integer visit(InvokeStmt stmt) {
		stmt.getMethodCall().accept(this.exprFlatenner);
		this.statements.remove(this.statements.size() - 2); // Remove temp = %rax

		return 0;
	}

	@Override
	public Integer visit(MethodCallExpr expr) {
		return 0;
	}

	@Override
	public Integer visit(MethodDecl md) {
		// Method prologue
		LabelStmt methodLabel = new LabelStmt(methodName);
		methodLabel.setMethodLabel(true);
		this.statements.add(methodLabel);
		this.statements.add(new EnterStmt());

		// Save callee-saved registers
		for (int i = 0; i < Register.calleeSaved.length; i++) {
			//this.statements.add(new PushStmt(new RegisterName(Register.calleeSaved[i])));
		}

		// Save params on stack (first 6)
		VarName varName;
		for (int i = 0; i < Math.min(md.getParameters().size(), 6); i++) {
			varName = new VarName(md.getParameters().get(i).getId());
			varName.setBlockId(-2);
			switch (i) {
				case 0:
					this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE,
							varName, new RegisterName(Register.RDI), null));
					break;
				case 1:
					this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE,
							varName, new RegisterName(Register.RSI), null));
					break;
				case 2:
					this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE,
							varName, new RegisterName(Register.RDX), null));
					break;
				case 3:
					this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE,
							varName, new RegisterName(Register.RCX), null));
					break;
				case 4:
					this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE,
							varName, new RegisterName(Register.R8), null));
					break;
				case 5:
					this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE,
							varName, new RegisterName(Register.R9), null));
					break;
			}
		}

		// Method body
		md.getBlock().accept(this);

		// Main method return value
		if (this.methodName.equals("main")) {
			this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE,
					new RegisterName(Register.RAX), new ConstantName(0), null));
		}
		
		// Void method return	
		if (md.getReturnType() == Type.VOID) {
			// Restore callee-saved regs (reverse order as saved!)
			for (int i = Register.calleeSaved.length - 1; i >= 0; i--) {
				//this.statements.add(new PopStmt(new RegisterName(Register.calleeSaved[i])));
			}
			
			this.statements.add(new LeaveStmt());
		}
		
		// Method cf handler (only for non-void return methods)
		if (md.getReturnType() != Type.VOID) {
			this.statements.add(new LabelStmt(getMethodCfHandler()));
			VarName error = new VarName(ProgramFlattener.methodExceptionErrorLabel);
			error.setIsString(true);
			
			// Move args to regs
			this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE,
					new RegisterName(Register.argumentRegs[0]), error, null));
			this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE,
					new RegisterName(Register.argumentRegs[1]), new ConstantName(md.getLineNumber()), null));
			this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE,
					new RegisterName(Register.argumentRegs[2]), new ConstantName(md.getColumnNumber()), null));
			
			// Call exception handler
			this.statements.add(new CallStmt(ProgramFlattener.exceptionHandlerLabel));
		}
		
		this.statements.add(new LabelStmt(getMethodEnd()));

		return this.totalLocalVars;
	}

	@Override
	public Integer visit(Parameter param) {
		return 0;
	}

	@Override
	public Integer visit(ReturnStmt stmt) {
		if (stmt.getExpression() != null) {
			Name rtn = stmt.getExpression().accept(this.exprFlatenner);
			this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE,
					new RegisterName(Register.RAX), rtn, null));
		}
		
		// Restore callee-saved regs (reverse order as saved!)
		for (int i = Register.calleeSaved.length - 1; i >= 0; i--) {
			//this.statements.add(new PopStmt(new RegisterName(Register.calleeSaved[i])));
		}
		
		this.statements.add(new LeaveStmt());

		return 0;
	}

	@Override
	public Integer visit(UnaryOpExpr expr) {
		return 0;
	}

	@Override
	public Integer visit(VarDecl vd) {
		return 0;
	}

	@Override
	public Integer visit(VarLocation loc) {
		return 0;
	}

	public void reset() {
		this.ifCount = 0;
		this.forCount = 0;
		this.currentForId = 0;
		this.currentIfId = 0;
		this.totalLocalVars = 0;
		this.statements = new ArrayList<LIRStatement>();
		this.exprFlatenner = new ExpressionFlattenerVisitor(statements,
				methodName, this.classDecl);
		MethodFlattenerVisitor.DEPTH = 0;
	}

	private String getIfTest() {
		return methodName + ".if" + currentIfId + ".test";
	}

	private String getIfTrue() {
		return methodName + ".if" + currentIfId + ".true";
	}

	private String getIfElse() {
		return methodName + ".if" + currentIfId + ".else";
	}

	private String getIfEnd() {
		return methodName + ".if" + currentIfId + ".end";
	}

	private String getForInit() {
		return methodName + ".for" + currentForId + ".init";
	}

	private String getForTest() {
		return methodName + ".for" + currentForId + ".test";
	}

	private String getForBody() {
		return methodName + ".for" + currentForId + ".body";
	}

	private String getForIncrement() {
		return methodName + ".for" + currentForId + ".incr";
	}

	private String getForEnd() {
		return methodName + ".for" + currentForId + ".end";
	}

	private String getMethodCfHandler() {
		return methodName + ".cfendhandler";
	}

	private String getMethodEnd() {
		return methodName + ".end";
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
		this.reset();
	}
}
