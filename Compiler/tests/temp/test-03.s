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

foo:
	enter	$56, $0
	mov	%rdi, %r10
	mov	%r10, -8(%rbp)
	mov	-8(%rbp), %r10
	mov	%r10, -16(%rbp)
.foo_if1_test:
	mov	-8(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	jne	.foo_if1_true
.foo_if1_else:
	mov	$2, %r10
	mov	%r10, %rax
	mov	%rax, %r10
	mov	%r10, -24(%rbp)
	leave
	ret
	jmp	.foo_if1_end
.foo_if1_true:
	mov	$1, %r10
	mov	%r10, %rax
	mov	%rax, %r10
	mov	%r10, -32(%rbp)
	leave
	ret
.foo_if1_end:
.foo_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	%rdi, %r10
	mov	%r10, -40(%rbp)
	mov	$3, %r10
	mov	%r10, %rsi
	mov	%rsi, %r10
	mov	%r10, -48(%rbp)
	mov	$16, %r10
	mov	%r10, %rdx
	mov	%rdx, %r10
	mov	%r10, -56(%rbp)
	call	exception_handler
.foo_end:

	.globl main
main:
	enter	$120, $0
	mov	$1, %r10
	mov	%r10, %rdi
	mov	%rdi, %r10
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
	mov	-16(%rbp), %r10
	mov	%r10, %rsi
	mov	%rsi, %r10
	mov	%r10, -40(%rbp)
	mov	$0, %r10
	mov	%r10, %rax
	mov	%rax, %r10
	mov	%r10, -48(%rbp)
	call	printf
	mov	%rax, %r10
	mov	%r10, -16(%rbp)
	mov	-16(%rbp), %r10
	mov	%r10, -56(%rbp)
	mov	%rax, %r10
	mov	%r10, %rdi
	call	foo
	mov	%rax, %r10
	mov	%r10, -64(%rbp)
	mov	-64(%rbp), %r10
	mov	%r10, -72(%rbp)
	mov	$.main_str1, %r10
	mov	%r10, %rdi
	mov	%rdi, %r10
	mov	%r10, -80(%rbp)
	mov	-64(%rbp), %r10
	mov	%r10, %rsi
	mov	%rsi, %r10
	mov	%r10, -88(%rbp)
	mov	%rax, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -64(%rbp)
	mov	-64(%rbp), %r10
	mov	%r10, -96(%rbp)
	mov	-48(%rbp), %r10
	mov	%r10, %rax
	leave
	ret
.main_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	%rdi, %r10
	mov	%r10, -104(%rbp)
	mov	$11, %r10
	mov	%r10, %rsi
	mov	%rsi, %r10
	mov	%r10, -112(%rbp)
	mov	$18, %r10
	mov	%r10, %rdx
	mov	%rdx, %r10
	mov	%r10, -120(%rbp)
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
