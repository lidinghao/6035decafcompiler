.data
.outofbounds:
	.string	"RUNTIME ERROR: Array index out of bounds (%d, %d)\n"
.methodcfend:
	.string	"RUNTIME ERROR: Method at (%d, %d) reached end of control flow without returning\n"
.main_str0:
	.string	"false || true : %d\n"
.main_str1:
	.string	"false && true : %d\n"

.text

	.globl main
main:
	enter	$32, $0
	mov	$0, %r10
	mov	%r10, -8(%rbp)
	mov	$0, %r10
	mov	%r10, -16(%rbp)
	mov	$0, %r10
	mov	%r10, -24(%rbp)
	mov	$0, %r10
	mov	%r10, -8(%rbp)
	mov	$1, %r10
	mov	%r10, -16(%rbp)
.main_or1_testLHS:
	mov	-8(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	je	.main_or1_testRHS
	mov	$1, %r10
	mov	%r10, -32(%rbp)
	jmp	.main_or1_end
.main_or1_testRHS:
	mov	-16(%rbp), %r10
	mov	%r10, -32(%rbp)
.main_or1_end:
	mov	-32(%rbp), %r10
	mov	%r10, -24(%rbp)
	mov	$.main_str0, %r10
	mov	%r10, %rdi
	mov	-24(%rbp), %r10
	mov	%r10, %rsi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
.main_and1_testLHS:
	mov	-8(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	jne	.main_and1_testRHS
	mov	$0, %r10
	mov	%r10, -32(%rbp)
	jmp	.main_and1_end
.main_and1_testRHS:
	mov	-16(%rbp), %r10
	mov	%r10, -32(%rbp)
.main_and1_end:
	mov	-32(%rbp), %r10
	mov	%r10, -24(%rbp)
	mov	$.main_str1, %r10
	mov	%r10, %rdi
	mov	-24(%rbp), %r10
	mov	%r10, %rsi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	$0, %r10
	mov	%r10, %rax
	leave
	ret
.main_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	$3, %r10
	mov	%r10, %rsi
	mov	$14, %r10
	mov	%r10, %rdx
	call	exception_handler
.main_end:
exception_handler:
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	$1, %r10
	mov	%r10, %rax
	mov	$1, %r10
	mov	%r10, %rbx
	int	$0x80
