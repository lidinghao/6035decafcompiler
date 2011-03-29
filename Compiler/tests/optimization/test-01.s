.data
.outofbounds:
	.string	"RUNTIME ERROR: Array index out of bounds (%d, %d)\n"
.methodcfend:
	.string	"RUNTIME ERROR: Method at (%d, %d) reached end of control flow without returning\n"
.main_str0:
	.string	"%d\n"
.main_str1:
	.string	"%d\n"

.text

	.globl main
main:
	enter	$112, $0
	mov	$0, %r10
	mov	%r10, -8(%rbp)
	mov	-8(%rbp), %r10
	mov	%r10, -16(%rbp)
	mov	-16(%rbp), %r10
	mov	%r10, -24(%rbp)
	mov	$10, %r10
	mov	$3, %r11
	add	%r11, %r10
	mov	%r10, -32(%rbp)
	mov	-32(%rbp), %r10
	mov	%r10, -40(%rbp)
	mov	-32(%rbp), %r10
	mov	%r10, -8(%rbp)
	mov	-8(%rbp), %r10
	mov	%r10, -48(%rbp)
	mov	-40(%rbp), %r10
	mov	%r10, -32(%rbp)
	mov	-48(%rbp), %r10
	mov	%r10, -24(%rbp)
	mov	$.main_str0, %r10
	mov	%r10, %rdi
	mov	%rdi, %r10
	mov	%r10, -56(%rbp)
	mov	-8(%rbp), %r10
	mov	%r10, %rsi
	mov	%rsi, %r10
	mov	%r10, -64(%rbp)
	mov	-16(%rbp), %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -32(%rbp)
	mov	-32(%rbp), %r10
	mov	%r10, -72(%rbp)
	mov	$.main_str1, %r10
	mov	%r10, %rdi
	mov	%rdi, %r10
	mov	%r10, -80(%rbp)
	mov	-64(%rbp), %r10
	mov	%r10, %rsi
	mov	-16(%rbp), %r10
	mov	%r10, %rax
	call	printf
	mov	-72(%rbp), %r10
	mov	%r10, -88(%rbp)
	mov	-16(%rbp), %r10
	mov	%r10, %rax
	leave
	ret
.main_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	%rdi, %r10
	mov	%r10, -96(%rbp)
	mov	$2, %r10
	mov	%r10, %rsi
	mov	%rsi, %r10
	mov	%r10, -104(%rbp)
	mov	$18, %r10
	mov	%r10, %rdx
	mov	%rdx, %r10
	mov	%r10, -112(%rbp)
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
