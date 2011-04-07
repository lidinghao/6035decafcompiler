.data
.outofbounds:
	.string	"RUNTIME ERROR: Array index out of bounds (%d, %d)\n"
.methodcfend:
	.string	"RUNTIME ERROR: Method at (%d, %d) reached end of control flow without returning\n"

.text

foo:
	enter	$104, $0
	mov	%rdi, %r10
	mov	%r10, -8(%rbp)
	mov	-8(%rbp), %r10
	mov	%r10, -40(%rbp)
	mov	%rsi, %r10
	mov	%r10, -16(%rbp)
	mov	-16(%rbp), %r10
	mov	%r10, -48(%rbp)
	mov	%rdx, %r10
	mov	%r10, -24(%rbp)
	mov	-24(%rbp), %r10
	mov	%r10, -56(%rbp)
	mov	%rcx, %r10
	mov	%r10, -32(%rbp)
	mov	-32(%rbp), %r10
	mov	%r10, -64(%rbp)
	mov	-8(%rbp), %r10
	mov	-16(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -24(%rbp)
	mov	-24(%rbp), %r10
	mov	%r10, -72(%rbp)
	mov	-24(%rbp), %r10
	mov	%r10, -80(%rbp)
	mov	-24(%rbp), %r10
	mov	%r10, -32(%rbp)
	leave
	ret
.foo_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	%rdi, %r10
	mov	%r10, -88(%rbp)
	mov	$2, %r10
	mov	%r10, %rsi
	mov	%rsi, %r10
	mov	%r10, -96(%rbp)
	mov	$17, %r10
	mov	%r10, %rdx
	mov	%rdx, %r10
	mov	%r10, -104(%rbp)
	call	exception_handler
.foo_end:

	.globl main
main:
	enter	$32, $0
	mov	$0, %r10
	mov	%r10, %rax
	mov	%rax, %r10
	mov	%r10, -8(%rbp)
	leave
	ret
.main_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	%rdi, %r10
	mov	%r10, -16(%rbp)
	mov	$7, %r10
	mov	%r10, %rsi
	mov	%rsi, %r10
	mov	%r10, -24(%rbp)
	mov	$18, %r10
	mov	%r10, %rdx
	mov	%rdx, %r10
	mov	%r10, -32(%rbp)
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
