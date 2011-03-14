package decaf.codegen.flattener;

import java.util.ArrayList;
import java.util.List;

import decaf.codegen.flatir.Constant;
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
import decaf.ir.ast.UnaryOpExpr;
import decaf.ir.ast.VarDecl;
import decaf.ir.ast.VarLocation;

public class MethodFlatennerVisitor implements ASTVisitor<Integer> {
	private List<LIRStatement> statements;
	private ExpressionFlatennerVisitor exprFlatenner;
	private String methodName;
	private int ifCount; // 0 in if and for count means method local block
	private int forCount; 
	private int currentIfId;
	private int currentForId;
	private int totalLocalVars;
	
	public MethodFlatennerVisitor(String methodName) {
		this.statements = new ArrayList<LIRStatement>();
		this.exprFlatenner = new ExpressionFlatennerVisitor(statements, methodName);
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
			this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, dest, src, null));
		}
		else if (op == AssignOpType.INCREMENT) {
			this.statements.add(new QuadrupletStmt(QuadrupletOp.ADD, dest, dest, src));
		}
		else {
			this.statements.add(new QuadrupletStmt(QuadrupletOp.SUB, dest, dest, src));
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
		for (VarDecl vd: block.getVarDeclarations()) {
			for (String v: vd.getVariables()) {
				VarName varName = new VarName(v);
				varName.setBlockId(block.getBlockId());
				this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, varName, new Constant(0), null));
			}
			
			this.totalLocalVars += vd.getVariables().size(); // Add to the total local vars in method
		}
		
		for (Statement s: block.getStatements()) {
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
		this.statements.add(new JumpStmt(JumpCondOp.NONE, new LabelStmt(getForIncrement())));

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
		
		// Increment local variable count (because for loop not "declared" in the body)
		this.totalLocalVars++;
		
		// Initialization block
		this.statements.add(new LabelStmt(getForInit()));
		Name initValue = stmt.getInitialValue().accept(this.exprFlatenner);
		this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, loopId, initValue, null));
		
		// Test block
		this.statements.add(new LabelStmt(getForTest()));
		Name finalValue = stmt.getFinalValue().accept(this.exprFlatenner);
		this.statements.add(new QuadrupletStmt(QuadrupletOp.CMP, null, loopId, finalValue));
		this.statements.add(new JumpStmt(JumpCondOp.GTE, new LabelStmt(getForEnd())));
		
		// Body block
		this.statements.add(new LabelStmt(getForBody()));
		stmt.getBlock().accept(this);
		
		// Increment loop var and jump to test
		this.statements.add(new LabelStmt(getForIncrement()));
		this.statements.add(new QuadrupletStmt(QuadrupletOp.ADD, loopId, loopId, new Constant(1)));
		this.statements.add(new JumpStmt(JumpCondOp.NONE, new LabelStmt(getForTest())));
		
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
		this.statements.add(new QuadrupletStmt(QuadrupletOp.CMP, null, condition, new Constant(0)));
		this.statements.add(new JumpStmt(JumpCondOp.NEQ, new LabelStmt(getIfTrue())));
		
		// Else block (if any)
		this.statements.add(new LabelStmt(getIfElse()));
		if (stmt.getElseBlock() != null) {
			stmt.getElseBlock().accept(this);
		}
		
		this.statements.add(new JumpStmt(JumpCondOp.NONE, new LabelStmt(getIfEnd())));
		
		// True block
		this.statements.add(new LabelStmt(getIfTrue()));
		stmt.getIfBlock().accept(this);
		
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
		// TODO: Modify because we don't want to save return value of method call in this case
		stmt.getMethodCall().accept(this.exprFlatenner);
		
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
		// TODO: Don't need it until we implement a register allocator
		
		// Method body
		md.getBlock().accept(this);
		
		// Method epilogue
		this.statements.add(new LabelStmt(getMethodEpilogue()));
		this.statements.add(new LeaveStmt());
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
			this.statements.add(new QuadrupletStmt(QuadrupletOp.MOVE, new RegisterName(Register.RAX), rtn, null));
		}
		this.statements.add(new JumpStmt(JumpCondOp.NONE, new LabelStmt(getMethodEpilogue())));
		
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
		this.exprFlatenner = new ExpressionFlatennerVisitor(statements, methodName);
	}
	
	private String getIfTest() {
		return methodName + "_if" + currentIfId + "_test";
	}
	
	private String getIfTrue() {
		return methodName + "_if" + currentIfId + "_true";
	}
	
	private String getIfElse() {
		return methodName + "_for" + currentIfId + "_else";
	}

	private String getIfEnd() {
		return methodName + "_if" + currentIfId + "_end";
	}
	
	private String getForInit() {
		return methodName + "_for" + currentForId + "_init";
	}
	
	private String getForTest() {
		return methodName + "_for" + currentForId + "_test";
	}
	
	private String getForBody() {
		return methodName + "_for" + currentForId + "_body";
	}

	private String getForIncrement() {
		return methodName + "_for" + currentForId + "_incr";
	}
	
	private String getForEnd() {
		return methodName + "_for" + currentForId + "_end";
	}
	
	private String getMethodEpilogue() {
		return methodName + "_epilogue";
	}
	
	private String getMethodEnd() {
		return methodName + "_end";
	}
	
	public void setMethodName(String methodName) {
		this.methodName = methodName;
		this.reset();
	}
}
