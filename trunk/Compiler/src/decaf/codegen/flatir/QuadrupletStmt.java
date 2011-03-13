package decaf.codegen.flatir;

import java.io.PrintStream;

public class QuadrupletStmt extends LIRStatement {
	private QuadrupletOp operator;
	private Name dest;
	private Name arg1;
	private Name arg2;
	
	public QuadrupletStmt(QuadrupletOp operator, Name dest, Name arg1, Name arg2) {
		this.operator = operator;
		this.dest = dest;
		this.arg1 = arg1;
		this.arg2 = arg2;
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
				processCompare(out);
				break;
			case MOVE:
				processMove(out);
				break;
			case MINUS:
			case NOT:
				break;
			case MOD:
			case DIV:
				processDivMod(out, this.operator);
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

	private void processConditionalQuadruplet(PrintStream out,
			QuadrupletOp operator2) {
		// TODO Auto-generated method stub
		
	}

	private void processDivMod(PrintStream out, QuadrupletOp op) {
		moveToRegister(out, new Constant(0), Register.RDX);
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

	private void processMove(PrintStream out) {
		moveToRegister(out, this.getArg1(), Register.R10);
		moveFromRegister(out, Register.R10, this.getDestination(), Register.R11);
		out.println("\tidiv\t" + Register.R10 + ", " + Register.R11);
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
		
		out.println(instr + Register.R10 + ", " + Register.R11);
		
		moveFromRegister(out, Register.R10, this.getDestination(), Register.R11);
	}
	
	private void processCompare(PrintStream out) {
		moveToRegister(out, this.getArg1(), Register.R10);
		moveToRegister(out, this.getArg2(), Register.R11);
		
		out.println("\tcmp\t" + Register.R10 + ", " + Register.R11);
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
}
