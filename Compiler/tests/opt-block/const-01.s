.data
.outofbounds:
	.string	"RUNTIME ERROR: Array index out of bounds (%d, %d)\n"
.methodcfend:
	.string	"RUNTIME ERROR: Method at (%d, %d) reached end of control flow without returning\n"
	.comm	A, 1, 8

.text

foo:
	enter	$0, $0
	leave
	ret
.foo_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	$4, %r10
	mov	%r10, %rsi
	mov	$17, %r10
	mov	%r10, %rdx
	call	exception_handler
.foo_end:

	.globl main
main:
	enter	$24, $0
	mov	$0, %r10
	mov	%r10, -8(%rbp)
	mov	$0, %r10
	mov	%r10, -16(%rbp)
	mov	$0, %r10
	mov	%r10, -24(%rbp)
	mov	A, %r10
	mov	%r10, -8(%rbp)
	mov	$5, %r10
	mov	%r10, A
	mov	$10, %r10
	mov	%r10, -8(%rbp)
	mov	$10, %r10
	mov	%r10, -16(%rbp)
	mov	$10, %r10
	mov	%r10, -24(%rbp)
	mov	$5, %r10
	mov	%r10, -8(%rbp)
	mov	$15, %r10
	mov	%r10, -16(%rbp)
	mov	$15, %r10
	mov	%r10, -24(%rbp)
	call	foo
	mov	A, %r10
	mov	%r10, -8(%rbp)
	mov	$0, %r10
	mov	%r10, %rax
	leave
	ret
.main_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	$8, %r10
	mov	%r10, %rsi
	mov	$18, %r10
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
