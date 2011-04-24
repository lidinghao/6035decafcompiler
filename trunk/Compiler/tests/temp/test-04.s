.data
.outofbounds:
	.string	"RUNTIME ERROR: Array index out of bounds (%d, %d)\n"
.methodcfend:
	.string	"RUNTIME ERROR: Method at (%d, %d) reached end of control flow without returning\n"
	.comm	x, 1, 8
.main_str0:
	.string	"should be 42: %d\n"

.text

foo:
	enter	$32, $0
	mov	$42, %r10
	mov	%r10, x
	mov	x, %r10
	mov	%r10, -8(%rbp)
	leave
	ret
.foo_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	%rdi, %r10
	mov	%r10, -16(%rbp)
	mov	$4, %r10
	mov	%r10, %rsi
	mov	%rsi, %r10
	mov	%r10, -24(%rbp)
	mov	$11, %r10
	mov	%r10, %rdx
	mov	%rdx, %r10
	mov	%r10, -32(%rbp)
	call	exception_handler
.foo_end:

	.globl main
main:
	enter	$80, $0
	mov	$0, %r10
	mov	%r10, x
	mov	x, %r10
	mov	%r10, -8(%rbp)
	call	foo
	mov	%rax, %r10
	mov	%r10, -16(%rbp)
	mov	-16(%rbp), %r10
	mov	%r10, -24(%rbp)
	mov	$.main_str0, %r10
	mov	%r10, %rdi
	mov	%rdi, %r10
	mov	%r10, -32(%rbp)
	mov	x, %r10
	mov	%r10, %rsi
	mov	%rsi, %r10
	mov	%r10, -40(%rbp)
	mov	-8(%rbp), %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -48(%rbp)
	mov	-48(%rbp), %r10
	mov	%r10, -56(%rbp)
	mov	-8(%rbp), %r10
	mov	%r10, %rax
	leave
	ret
.main_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	%rdi, %r10
	mov	%r10, -64(%rbp)
	mov	$8, %r10
	mov	%r10, %rsi
	mov	%rsi, %r10
	mov	%r10, -72(%rbp)
	mov	$12, %r10
	mov	%r10, %rdx
	mov	%rdx, %r10
	mov	%r10, -80(%rbp)
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