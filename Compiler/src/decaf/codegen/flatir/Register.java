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
	R15, // callee-saved register
}
