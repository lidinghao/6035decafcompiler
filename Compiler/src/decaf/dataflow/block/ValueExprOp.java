package decaf.dataflow.block;

public enum ValueExprOp {
	NONE,
	MINUS, // - 
	NOT, // !
	ADD, // +
	SUB, // -
	MUL, // *
	DIV, // /
	MOD, // %
	EQ, // ==
	NEQ, // !=
	GT, // >
	GTE, // >=
	LT, // <
	LTE; // <=
}
