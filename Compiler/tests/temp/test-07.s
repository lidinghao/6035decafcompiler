.data
.outofbounds:
	.string	"RUNTIME ERROR: Array index out of bounds (%d, %d)\n"
.methodcfend:
	.string	"RUNTIME ERROR: Method at (%d, %d) reached end of control flow without returning\n"

.text

foo:
	enter	$40, $0
	mov	%rcx, %r10
	mov	%r10, -32(%rbp)
	mov	%rdx, %r10
	mov	%rcx, %r11
	add	%r11, %r10
	mov	%r10, -8(%rbp)
	mov	-8(%rbp), %r10
	mov	%r10, -40(%rbp)
	mov	%rdx, %r10
	mov	%r10, -8(%rbp)
	mov	-40(%rbp), %r10
	mov	%r10, -16(%rbp)
	mov	%rdx, %r10
	mov	-40(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -24(%rbp)
	leave
	ret
.foo_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	$2, %r10
	mov	%r10, %rsi
	mov	$18, %r10
	mov	%r10, %rdx
	call	exception_handler
.foo_end:

	.globl main
main:
	enter	$0, $0
	mov	$0, %r10
	mov	%r10, %rax
	leave
	ret
.main_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	$10, %r10
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
