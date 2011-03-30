.data
.outofbounds:
	.string	"RUNTIME ERROR: Array index out of bounds (%d, %d)\n"
.methodcfend:
	.string	"RUNTIME ERROR: Method at (%d, %d) reached end of control flow without returning\n"
	.comm	a, 1, 8
	.comm	b, 1, 8
	.comm	c, 1, 8
.main_str0:
	.string	"%d + %d = %d (15)\n"

.text

	.globl main
main:
	enter	$104, $0
	mov	$10, %r10
	mov	%r10, a
	mov	a, %r10
	mov	%r10, -8(%rbp)
	mov	$5, %r10
	mov	%r10, b
	mov	b, %r10
	mov	%r10, -16(%rbp)
	mov	a, %r10
	mov	b, %r11
	add	%r11, %r10
	mov	%r10, -24(%rbp)
	mov	-24(%rbp), %r10
	mov	%r10, -32(%rbp)
	mov	-24(%rbp), %r10
	mov	%r10, c
	mov	c, %r10
	mov	%r10, -40(%rbp)
	mov	$.main_str0, %r10
	mov	%r10, %rdi
	mov	%rdi, %r10
	mov	%r10, -48(%rbp)
	mov	a, %r10
	mov	%r10, %rsi
	mov	%rsi, %r10
	mov	%r10, -56(%rbp)
	mov	b, %r10
	mov	%r10, %rdx
	mov	%rdx, %r10
	mov	%r10, -64(%rbp)
	mov	c, %r10
	mov	%r10, %rcx
	mov	%rcx, %r10
	mov	%r10, -72(%rbp)
	mov	$0, %r10
	mov	%r10, %rax
	mov	%rax, %r10
	mov	%r10, -80(%rbp)
	call	printf
	mov	%rax, %r10
	mov	%r10, -24(%rbp)
	mov	-24(%rbp), %r10
	mov	%r10, -88(%rbp)
	mov	-80(%rbp), %r10
	mov	%r10, %rax
	leave
	ret
.main_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	%rdi, %r10
	mov	%r10, -96(%rbp)
	mov	-16(%rbp), %r10
	mov	%r10, %rsi
	mov	$14, %r10
	mov	%r10, %rdx
	mov	%rdx, %r10
	mov	%r10, -104(%rbp)
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
