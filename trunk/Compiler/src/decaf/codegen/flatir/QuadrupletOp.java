package decaf.codegen.flatir;

public enum QuadrupletOp {
	CMP,
	ADD,
	SUB,
	IMUL,
	IDIV,
	EQ, // Move
	CMOVE, // Conditional moves, required for boolean expressions
	CMOVNE,
	CMOVG,
	CMOVGE,
	CMOVL,
	CMOVLE,
}
