package decaf.codegen.flatir;

public enum Register {
	RAX, // temp register; return value
	RBX, // callee-saved
	RSP, // stack pointer
	RBP, // callee-saved; base pointer
	RCX, // used to pass 4th argument to functions
	RDX, // used to pass 3rd argument to functions
	RSI, // used to pass 2nd argument to functions
	RDI, // used to pass 1st argument to functions
	R8, // used to pass 5th argument to functions
	R9, // used to pass 6th argument to functions
	R10, // temp register
	R11, // temp register
	R12, // callee-saved register
	R13, // callee-saved register
	R14, // callee-saved register
	R15; // callee-saved register
	
	@Override 
	public String toString() {
		String s = super.toString();
		return "%" + s.toLowerCase();
	}
	
	public static Register[] argumentRegs = { RDI, RSI, RDX, RCX, R8, R9 };
	public static Register[] calleeSaved = { RBX, R12, R13, R14, R15 }; // No need to save RBP
	public static Register[] callerSaved = { R10, R11, RDI, RSI, RDX, RCX, R8, R9, RAX }; // Save only live ones
	public static Register[] availableRegs = { RBX, R12, R13, R14, R15, R10, R11, RDI, RSI, RCX, R8, R9, RDX, RAX }; // Use in this order 
}
