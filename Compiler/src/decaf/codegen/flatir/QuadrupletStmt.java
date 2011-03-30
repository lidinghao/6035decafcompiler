package decaf.codegen.flatir;

import java.io.PrintStream;

public class QuadrupletStmt extends LIRStatement {
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
		this.isLeader = false;
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
			case CMP:
				return "compare " + arg1 + ", " + arg2;
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
			case CMP:
				processCompareQuadruplet(out);
				break;
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
		out.println("\tidiv\t" + Register.R10);
		
		if(op == QuadrupletOp.DIV) {
			moveFromRegister(out, Register.RAX, this.getDestination(), Register.R10);
		} 
		else {
			moveFromRegister(out, Register.RDX, this.getDestination(), Register.R10);
		}
	}

	private void processMoveQuadruplet(PrintStream out) {
		moveToRegister(out, this.getArg1(), Register.R10);
		moveFromRegister(out, Register.R10, this.getDestination(), Register.R11);
	}

	private void processArithmeticQuadruplet(PrintStream out, QuadrupletOp op) {
		moveToRegister(out, this.getArg1(), Register.R10);
		moveToRegister(out, this.getArg2(), Register.R11);
		
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
		
		out.println(instr + Register.R11 + ", " + Register.R10);
		
		moveFromRegister(out, Register.R10, this.getDestination(), Register.R11);
	}
	
	private void processCompareQuadruplet(PrintStream out) {
		moveToRegister(out, this.getArg1(), Register.R10);
		moveToRegister(out, this.getArg2(), Register.R11);
		
		out.println("\tcmp\t" + Register.R11 + ", " + Register.R10);
	}
	
	private void moveToRegister(PrintStream out, Name name, Register register) {
		if (name.isArray()) {
			ArrayName arrayName = (ArrayName) name;
			String indexLocation = arrayName.getIndex().getLocation().getASMRepresentation();
			out.println("\tmov\t" + indexLocation + ", " + register);
			arrayName.setOffsetRegister(register);
		}
		
		out.println("\tmov\t" + name.getLocation().getASMRepresentation() + ", " + register);
	}
	
	private void moveFromRegister(PrintStream out, Register from, Name name, Register temp) {
		if (name.isArray()) {
			ArrayName arrayName = (ArrayName) name;
			String indexLocation = arrayName.getIndex().getLocation().getASMRepresentation();
			out.println("\tmov\t" + indexLocation + ", " + temp);
			arrayName.setOffsetRegister(temp);
		}
		
		out.println("\tmov\t" + from + ", " + name.getLocation().getASMRepresentation());		
	}
	
	@Override
	public int hashCode() {
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
		
		return true;
	}
	
	@Override
	public boolean isExpressionStatement() {
		if (this.operator == QuadrupletOp.CMP) {
			return false;
		}
		
		return true;
	}
	
	public int getMyId() {
		return myId;
	}

	public void setMyId() {
		this.myId = ID;
		QuadrupletStmt.ID++;
	}
}
