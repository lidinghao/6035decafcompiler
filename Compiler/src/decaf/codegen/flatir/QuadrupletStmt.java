package decaf.codegen.flatir;

import java.io.PrintStream;

public class QuadrupletStmt extends LIRStatement {
	public boolean USERIGIDHASH = false;
	private static int ID = 0;
	private QuadrupletOp operator;
	private int myId;
	private Name dest;
	private Name arg1;
	private Name arg2;
	
	public QuadrupletStmt(QuadrupletOp operator, Name dest, Name arg1, Name arg2) {
		this.operator = operator;
		this.dest = dest;
		this.arg1 = arg1;
		this.arg2 = arg2;
		this.setDepth();
	}

	public QuadrupletOp getOperator() {
		return operator;
	}

	public void setOperator(QuadrupletOp operator) {
		this.operator = operator;
	}

	public Name getDestination() {
		return dest;
	}

	public void setDestination(Name destination) {
		this.dest = destination;
	}

	public Name getArg1() {
		return arg1;
	}

	public void setArg1(Name arg1) {
		this.arg1 = arg1;
	}

	public Name getArg2() {
		return arg2;
	}

	public void setArg2(Name arg2) {
		this.arg2 = arg2;
	}
	
	@Override
	public String toString() {
		switch (this.operator) {
			case ADD:
				return dest + " = " + arg1 + " + " + arg2;
			case SUB:
				return dest + " = " + arg1 + " - " + arg2;
			case MUL:
				return dest + " = " + arg1 + " * " + arg2;
			case DIV:
				return dest + " = " + arg1 + " / " + arg2;
			case MOD:
				return dest + " = " + arg1 + " % " + arg2;
			case MOVE:
				return dest + " = " + arg1;
			case NOT:
				return dest + " = " + "!" + arg1;
			case MINUS:
				return dest + " = " + "-" + arg1;
			case EQ:
				return dest + " = " + arg1 + " == " + arg2;
			case NEQ:
				return dest + " = " + arg1 + " != " + arg2;
			case LT:
				return dest + " = " + arg1 + " < " + arg2;
			case LTE:
				return dest + " = " + arg1 + " <= " + arg2;
			case GT:
				return dest + " = " + arg1 + " > " + arg2;
			case GTE:
				return dest + " = " + arg1 + " <= " + arg2;
		}
		
		return null;
	}

	@Override
	public void generateAssembly(PrintStream out) {
		switch(this.operator) {
			case MOVE:
				processMoveQuadruplet(out);
				break;
			case MINUS:
			case NOT:
				processUnaryQuadruplet(out, this.operator);
				break;
			case MOD:
			case DIV:
				processDivModQuadruplet(out, this.operator);
				break;
			case LT:
			case LTE:
			case GT:
			case GTE:
			case EQ:
			case NEQ:
				processConditionalQuadruplet(out, this.operator);
				break;
			default:
				processArithmeticQuadruplet(out, this.operator);
				break;
		}
	}

	private void processUnaryQuadruplet(PrintStream out, QuadrupletOp operator) {
		ConstantName falseLiteral = new ConstantName(0);
		falseLiteral.setLocation(new ConstantLocation(falseLiteral.getValue()));
		ConstantName trueLiteral = new ConstantName(1);
		trueLiteral.setLocation(new ConstantLocation(trueLiteral.getValue()));
		
		moveToRegister(out, this.getArg1(), Register.R10);
		if (operator == QuadrupletOp.MINUS) {
			out.println("\tneg\t" + Register.R10);
		} 
		else if (operator == QuadrupletOp.NOT) {
			out.println("\tcmp\t" + falseLiteral.getLocation() + ", " + Register.R10);
			moveToRegister(out, falseLiteral, Register.R11);
			out.println("\tcmovne\t" + Register.R11 + ", " + Register.R10);
			moveToRegister(out, trueLiteral, Register.R11);
			out.println("\tcmove\t" + Register.R11 + ", " + Register.R10);
		}
		
		moveFromRegister(out, Register.R10, this.getDestination(), Register.R11);
	}

	private void processConditionalQuadruplet(PrintStream out,
			QuadrupletOp op) {
		moveToRegister(out, this.getArg1(), Register.R10);
		moveToRegister(out, this.getArg2(), Register.R11);
		
		out.println("\tcmp\t" + Register.R11 + ", " + Register.R10);
		
		ConstantName falseLiteral = new ConstantName(0);
		falseLiteral.setLocation(new ConstantLocation(falseLiteral.getValue()));
		ConstantName trueLiteral = new ConstantName(1);
		trueLiteral.setLocation(new ConstantLocation(trueLiteral.getValue()));
		
		moveToRegister(out, falseLiteral, Register.R10);
		moveToRegister(out, trueLiteral, Register.R11);
		
		String instr = "\tcmov";
		switch (op) {
			case LT:
				instr += "l\t";
				break;
			case LTE:
				instr += "le\t";
				break;
			case GT:
				instr += "g\t";
				break;
			case GTE:
				instr += "ge\t";
				break;
			case EQ:
				instr += "e\t";
				break;
			case NEQ:
				instr += "ne\t";
				break;
		}
		
		out.println(instr + Register.R11 + ", " + Register.R10);
		moveFromRegister(out, Register.R10, this.getDestination(), Register.R11);
	}

	private void processDivModQuadruplet(PrintStream out, QuadrupletOp op) {
		ConstantName falseLiteral = new ConstantName(0);
		falseLiteral.setLocation(new ConstantLocation(falseLiteral.getValue()));
		
		moveToRegister(out, falseLiteral, Register.RDX);
		moveToRegister(out, this.getArg1(), Register.RAX);
		moveToRegister(out, this.getArg2(), Register.R10);
		out.println("\tdiv\t" + Register.R10);
		
		if(op == QuadrupletOp.DIV) {
			moveFromRegister(out, Register.RAX, this.getDestination(), Register.R10);
		} 
		else {
			moveFromRegister(out, Register.RDX, this.getDestination(), Register.R10);
		}
	}

	private void processMoveQuadruplet(PrintStream out) {
		moveToRegister(out, this.getArg1(), Register.R10);
		//if(!this.getDestination().isArray()) {
			//out.println("\tmov\t" + Register.R10 + ", " + this.getDestination().getLocation().getASMRepresentation());
		//} else {
			moveFromRegister(out, Register.R10, this.getDestination(), Register.R11);
		//}
	}

	private void processArithmeticQuadruplet(PrintStream out, QuadrupletOp op) {
		String instr = "\t";
		
//		if(this.getArg2().getClass().equals(ConstantName.class)) {
//			argConst((ConstantName) this.getArg2(), this.getArg1(), out, op, instr);
//		} else if(this.getArg1().getClass().equals(ConstantName.class)){
//			argConst((ConstantName) this.getArg1(), this.getArg2(), out, op, instr);
//		} else {
		switch(op) {
			case ADD:
				instr += "add\t";
				break;
			case SUB:
				instr += "sub\t";
				break;
			case MUL:
				instr += "imul\t";
				break;
		}
		moveToRegister(out, this.getArg1(), Register.R10);
		moveToRegister(out, this.getArg2(), Register.R11);
		out.println(instr + Register.R11 + ", " + Register.R10);
		moveFromRegister(out, Register.R10, this.getDestination(), Register.R11);
			//out.println("\tmov\t" + Register.R10 + ", " + this.getDestination().getLocation().getASMRepresentation());
		
	}
	
	private void moveToRegister(PrintStream out, Name name, Register register) {
		if (name.isArray()) {
			ArrayName arrayName = (ArrayName) name;
			moveToRegister(out, arrayName.getIndex(), register);
//			String indexLocation = arrayName.getIndex().getLocation().getASMRepresentation();
//			out.println("\tmov\t" + indexLocation + ", " + register);
			arrayName.setOffsetRegister(register);
		}
		
		out.println("\tmov\t" + name.getLocation().getASMRepresentation() + ", " + register);
	}
	
	private void moveFromRegister(PrintStream out, Register from, Name name, Register temp) {
		if (name.isArray()) {
			ArrayName arrayName = (ArrayName) name;
			moveToRegister(out, arrayName.getIndex(), temp);
//			String indexLocation = arrayName.getIndex().getLocation().getASMRepresentation();
//			out.println("\tmov\t" + indexLocation + ", " + temp);
			arrayName.setOffsetRegister(temp);
		}
		
		out.println("\tmov\t" + from + ", " + name.getLocation().getASMRepresentation());		
	}
	
	@Override
	public int hashCode() {
		if (USERIGIDHASH) {
			return toString().hashCode() + 13 * this.myId;
		}
		return toString().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!o.getClass().equals(QuadrupletStmt.class)) return false;
		
		QuadrupletStmt stmt = (QuadrupletStmt) o;
		if (this.operator != stmt.getOperator()) {
			return false;
		}
		
		if (this.arg1 != null) {
			if (!this.arg1.equals(stmt.getArg1())) {
				return false;
			}
		} 
		else {
			if (stmt.getArg1() != null) {
				return false;
			}
		}
		
		if (this.arg2 != null) {
			if (!this.arg2.equals(stmt.getArg2())) {
				return false;
			}
		} 
		else {
			if (stmt.getArg2() != null) {
				return false;
			}
		}
		
		if (this.dest != null) {
			if (!this.dest.equals(stmt.getDestination())) {
				return false;
			}
		} 
		else {
			if (stmt.getDestination() != null) {
				return false;
			}
		}
		
		if (USERIGIDHASH) {
			if (stmt.myId != this.myId) {
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public boolean isAvailableExpression() {
		if (this.operator == QuadrupletOp.MOVE) {
			return false;
		}
		
		return true;
	}
	
	@Override
	public boolean isUseStatement() {
		return true;
	}
	
	// Returns true if statement is of the form a = b, false otherwise
	public boolean isAssignmentStatement() {
		return (getOperator() == QuadrupletOp.MOVE);
	}
	
	public int getMyId() {
		return myId;
	}

	public void setMyId() {
		this.myId = ID;
		QuadrupletStmt.ID++;
	}
	

	public static int getID() {
		return ID;
	}

	public static void setID(int iD) {
		ID = iD;
	}
	
	@Override
	public Object clone() {
		return new QuadrupletStmt(this.operator, this.dest, this.arg1, this.arg2);
	}
	
	public boolean hasTwoArgs() {
		return this.operator != QuadrupletOp.MOVE && this.operator != QuadrupletOp.NOT && this.operator != QuadrupletOp.MINUS;
	}
	
	/**
	 * REGISTER ALLOCATOR ASM
	 */

	@Override
	public void generateRegAllocAssembly(PrintStream out) {
		out.println("\t; " + this.toString());
		switch(this.operator) {
			case MOVE:
				asmMoveQuadruplet(out);
				break;
			case MINUS:
			case NOT:
				asmUnaryQuadruplet(out, this.operator);
				break;
			case MOD:
			case DIV:
				asmDivModQuadruplet(out, this.operator);
				break;
			case LT:
			case LTE:
			case GT:
			case GTE:
			case EQ:
			case NEQ:
				asmConditionalQuadruplet(out, this.operator);
				break;
			default:
				asmArithmeticQuadruplet(out, this.operator);
				break;
		}
		out.println("\t;");
	}
	
	private void asmDivModQuadruplet(PrintStream out, QuadrupletOp op) {
		// TODO : Just doing naive shit right now
		
		out.println("\tpush\t" + Register.RDX);
		out.println("\tpush\t" + Register.RAX);
		
		out.println("\tmov\t" + "$0" + ", " + Register.RDX);
		out.println("\tmov\t" + this.arg1.getRegister() + ", " + Register.RAX);
		out.println("\tdiv\t" + this.arg2.getRegister());
		
		if(op == QuadrupletOp.DIV) {
			out.println("\tmov\t" + Register.RAX + ", " + this.dest.getRegister());
		} 
		else {
			out.println("\tmov\t" + Register.RDX + ", " + this.dest.getRegister());
		}
		
		out.println("\tpop\t" + Register.RAX);
		out.println("\tpop\t" + Register.RDX);
	}

	private void asmArithmeticQuadruplet(PrintStream out, QuadrupletOp op) {
		String instr = "\t";
		
		switch(op) {
			case ADD:
				instr += "add\t";
				break;
			case SUB:
				instr += "sub\t";
				break;
			case MUL:
				instr += "imul\t";
				break;
		}
		
		this.asmMove(arg1, dest, out);
		out.println(instr + arg2.getRegister() + ", " + dest.getRegister());
	}

	private void asmConditionalQuadruplet(PrintStream out, QuadrupletOp op) {
		out.println("\tcmp\t" + this.arg1.getRegister() + ", " + this.arg2.getRegister());
		out.println("\tmov\t" + "$0" + ", " + dest.getRegister());
		
		String instr = "\tcmov";
		switch (op) {
			case LT:
				instr += "l\t";
				break;
			case LTE:
				instr += "le\t";
				break;
			case GT:
				instr += "g\t";
				break;
			case GTE:
				instr += "ge\t";
				break;
			case EQ:
				instr += "e\t";
				break;
			case NEQ:
				instr += "ne\t";
				break;
		}
		

		out.println(instr + "$1" + ", " + dest.getRegister());		
	}

	private void asmMove(Name from, Name dest, PrintStream out) {
		//if (!from.getRegister().equals(dest.getRegister())) {
			out.println("\tmov\t" + from.getRegister() + ", " + dest.getRegister());
		//}
	}

	private void asmUnaryQuadruplet(PrintStream out, QuadrupletOp operator2) {
		if (operator == QuadrupletOp.MINUS) {
			this.asmMove(arg1, dest, out);
			out.println("\tneg\t" + dest.getRegister());
		} 
		else if (operator == QuadrupletOp.NOT) {
			out.println("\tcmp\t" + "$0" + ", " + arg1.getRegister());
			out.println("\tmov\t" + "$0" + ", " + dest.getRegister());
			out.println("\tcmove\t" + "$1" + ", " + dest.getRegister());
		}		
	}

	private void asmMoveQuadruplet(PrintStream out) {
		this.asmMove(arg1, dest, out);
	}
}
