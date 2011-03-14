package decaf.codegen.flatir;

import java.io.FileWriter;
import java.io.IOException;

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
	public void generateAssembly(FileWriter out) throws IOException {
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

	private void processUnaryQuadruplet(FileWriter out, QuadrupletOp operator) throws IOException {
		Constant falseLiteral = new Constant(0);
		falseLiteral.setLocation(new ConstantLocation(falseLiteral.getValue()));
		Constant trueLiteral = new Constant(1);
		trueLiteral.setLocation(new ConstantLocation(trueLiteral.getValue()));
		
		moveToRegister(out, this.getArg1(), Register.R10);
		if (operator == QuadrupletOp.MINUS) {
			out.write("\tneg\t" + Register.R10);
		} 
		else if (operator == QuadrupletOp.NOT) {
			out.write("\tcmp\t" + falseLiteral.getLocation() + ", " + Register.R10);
			moveToRegister(out, falseLiteral, Register.R11);
			out.write("\tcmovne\t" + Register.R11 + ", " + Register.R10);
			moveToRegister(out, trueLiteral, Register.R11);
			out.write("\tcmove\t" + Register.R11 + ", " + Register.R10);
		}
		
		moveFromRegister(out, Register.R10, this.getDestination(), Register.R11);
	}

	private void processConditionalQuadruplet(FileWriter out,
			QuadrupletOp op) throws IOException {
		moveToRegister(out, this.getArg1(), Register.R10);
		moveToRegister(out, this.getArg2(), Register.R11);
		
		out.write("\tcmp\t" + Register.R11 + ", " + Register.R10);
		
		Constant falseLiteral = new Constant(0);
		falseLiteral.setLocation(new ConstantLocation(falseLiteral.getValue()));
		Constant trueLiteral = new Constant(1);
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
		
		out.write(instr + Register.R11 + ", " + Register.R10);
		moveFromRegister(out, Register.R10, this.getDestination(), Register.R11);
	}

	private void processDivModQuadruplet(FileWriter out, QuadrupletOp op) throws IOException {
		Constant falseLiteral = new Constant(0);
		falseLiteral.setLocation(new ConstantLocation(falseLiteral.getValue()));
		
		moveToRegister(out, falseLiteral, Register.RDX);
		moveToRegister(out, this.getArg1(), Register.RAX);
		moveToRegister(out, this.getArg2(), Register.R10);
		out.write("\tidiv\t" + Register.R10);
		
		if(op == QuadrupletOp.DIV) {
			moveFromRegister(out, Register.RAX, this.getDestination(), Register.R10);
		} 
		else {
			moveFromRegister(out, Register.RDX, this.getDestination(), Register.R10);
		}
	}

	private void processMoveQuadruplet(FileWriter out) throws IOException {
		moveToRegister(out, this.getArg1(), Register.R10);
		moveFromRegister(out, Register.R10, this.getDestination(), Register.R11);
	}

	private void processArithmeticQuadruplet(FileWriter out, QuadrupletOp op) throws IOException {
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
		
		out.write(instr + Register.R11 + ", " + Register.R10);
		
		moveFromRegister(out, Register.R10, this.getDestination(), Register.R11);
	}
	
	private void processCompareQuadruplet(FileWriter out) throws IOException {
		moveToRegister(out, this.getArg1(), Register.R10);
		moveToRegister(out, this.getArg2(), Register.R11);
		
		out.write("\tcmp\t" + Register.R11 + ", " + Register.R10);
	}
	
	private void moveToRegister(FileWriter out, Name name, Register register) throws IOException {
		if (name.isArray()) {
			ArrayName arrayName = (ArrayName) name;
			String indexLocation = arrayName.getIndex().getLocation().getASMRepresentation();
			out.write("\tmov\t" + indexLocation + ", " + register);
			arrayName.setOffsetRegister(register);
		}
		
		out.write("\tmov\t" + name.getLocation().getASMRepresentation() + ", " + register);
	}
	
	private void moveFromRegister(FileWriter out, Register from, Name name, Register temp) throws IOException {
		if (name.isArray()) {
			ArrayName arrayName = (ArrayName) name;
			String indexLocation = arrayName.getIndex().getLocation().getASMRepresentation();
			out.write("\tmov\t" + indexLocation + ", " + temp);
			arrayName.setOffsetRegister(temp);
		}
		
		out.write("\tmov\t" + from + ", " + name.getLocation().getASMRepresentation());		
	}
}
