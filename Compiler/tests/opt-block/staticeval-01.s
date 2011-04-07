.data
.outofbounds:
	.string	"RUNTIME ERROR: Array index out of bounds (%d, %d)\n"
.methodcfend:
	.string	"RUNTIME ERROR: Method at (%d, %d) reached end of control flow without returning\n"
	.comm	A, 1, 8

.text

	.globl main
main:
	enter	$16, $0
	mov	$0, %r10
	mov	%r10, -8(%rbp)
	mov	$0, %r10
	mov	%r10, -16(%rbp)
	mov	$10, %r10
	mov	$5, %r11
	add	%r11, %r10
	mov	%r10, -8(%rbp)
	mov	-8(%rbp), %r10
	mov	$5, %r11
	sub	%r11, %r10
	mov	%r10, -8(%rbp)
	mov	$0, %rdx
	mov	-8(%rbp), %rax
	mov	$8, %r10
	idiv	%r10
	mov	%rdx, -8(%rbp)
	mov	$0, %rdx
	mov	-8(%rbp), %rax
	mov	$2, %r10
	idiv	%r10
	mov	%rax, -8(%rbp)
	mov	$10, %r10
	mov	-8(%rbp), %r11
	imul	%r11, %r10
	mov	%r10, -8(%rbp)
	mov	-16(%rbp), %r10
	mov	$16, %r11
	add	%r11, %r10
	mov	%r10, -16(%rbp)
	mov	-8(%rbp), %r10
	mov	-16(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -8(%rbp)
	mov	$0, %r10
	mov	%r10, %rax
	leave
	ret
.main_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	$4, %r10
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
