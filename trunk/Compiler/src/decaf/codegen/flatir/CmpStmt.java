package decaf.codegen.flatir;

import java.io.PrintStream;

import decaf.ralloc.ASMGenerator;

public class CmpStmt extends LIRStatement {
	private Name arg1;
	private Name arg2;
	
	public CmpStmt(Name arg1, Name arg2) {
		this.arg1 = arg1;
		this.arg2 = arg2;
		this.setDepth();
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
		return "cmp " + arg1 + ", " + arg2;
	}
	
	private void processStmt(PrintStream out) {
		moveToRegister(out, this.getArg1(), Register.R10);
		moveToRegister(out, this.getArg2(), Register.R11);
		
		out.println("\tcmp\t" + Register.R11 + ", " + Register.R10);
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

	@Override
	public void generateAssembly(PrintStream out) {
		processStmt(out);		
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!o.getClass().equals(CmpStmt.class)) return false;
		
		CmpStmt stmt = (CmpStmt) o;
		if (!this.arg1.equals(stmt.getArg1())) {
			return false;
		}
		
		if (!this.arg2.equals(stmt.getArg2())) {
			return false;
		}
		
		return true;
	}
	
	@Override
	public Object clone() {
		return new CmpStmt(this.arg1, this.arg2);
	}
	
	@Override
	public boolean isUseStatement() {
		return true;
	}

	@Override
	public void generateRegAllocAssembly(PrintStream out) {
		out.println("\t; " + this.toString());
		
		if (this.arg1.getMyRegister() == null && this.arg2.getMyRegister() == null) {
			out.println("\tmov\t" + ASMGenerator.getLocationForName(this.arg2, out, false) + ", " + Register.RCX);
			out.println("\tcmp\t" + ASMGenerator.getLocationForName(this.arg1, out, false) + ", " + Register.RCX);
		}
		else {
			out.println("\tcmp\t" + ASMGenerator.getLocationForName(this.arg1, out, false) + ", " + ASMGenerator.getLocationForName(this.arg2, out, false));
		}		
	}
}
