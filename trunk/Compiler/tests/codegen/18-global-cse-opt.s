.data
.outofbounds:
	.string	"RUNTIME ERROR: Array index out of bounds (%d, %d)\n"
.methodcfend:
	.string	"RUNTIME ERROR: Method at (%d, %d) reached end of control flow without returning\n"

.text

foo:
	enter	$144, $0
	mov	%rdi, %r10
	mov	%r10, -8(%rbp)
	mov	%rsi, %r10
	mov	%r10, -16(%rbp)
	mov	$0, %r10
	mov	%r10, -24(%rbp)
	mov	$0, %r10
	mov	%r10, -32(%rbp)
	mov	$0, %r10
	mov	%r10, -40(%rbp)
	mov	$0, %r10
	mov	%r10, -48(%rbp)
	mov	$0, %r10
	mov	%r10, -56(%rbp)
	mov	$0, %r10
	mov	%r10, -64(%rbp)
	mov	$0, %r10
	mov	%r10, -72(%rbp)
	mov	$4, %r10
	mov	%r10, -56(%rbp)
	mov	$5, %r10
	mov	%r10, -72(%rbp)
	mov	-8(%rbp), %r10
	mov	-16(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -24(%rbp)
	mov	-24(%rbp), %r10
	mov	%r10, -80(%rbp)
.foo_if1_test:
	mov	-8(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	mov	$0, %r10
	mov	$1, %r11
	cmove	%r11, %r10
	mov	%r10, -88(%rbp)
	mov	-88(%rbp), %r10
	mov	%r10, -96(%rbp)
	mov	-88(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	jne	.foo_if1_true
.foo_if1_else:
	jmp	.foo_if1_end
.foo_if1_true:
	mov	-56(%rbp), %r10
	mov	%r10, -8(%rbp)
	mov	-8(%rbp), %r10
	mov	-16(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -32(%rbp)
	mov	-32(%rbp), %r10
	mov	%r10, -80(%rbp)
.foo_if1_end:
	mov	-80(%rbp), %r10
	mov	%r10, -64(%rbp)
.foo_if2_test:
	mov	-64(%rbp), %r10
	mov	-72(%rbp), %r11
	cmp	%r11, %r10
	mov	$0, %r10
	mov	$1, %r11
	cmovl	%r11, %r10
	mov	%r10, -88(%rbp)
	mov	-88(%rbp), %r10
	mov	%r10, -104(%rbp)
	mov	-88(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	jne	.foo_if2_true
.foo_if2_else:
	mov	-80(%rbp), %r10
	mov	%r10, -48(%rbp)
	jmp	.foo_if2_end
.foo_if2_true:
	mov	-80(%rbp), %r10
	mov	%r10, -40(%rbp)
	mov	-64(%rbp), %r10
	mov	-40(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -64(%rbp)
	mov	-64(%rbp), %r10
	mov	%r10, -112(%rbp)
.foo_if2_end:
	leave
	ret
.foo_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	$2, %r10
	mov	%r10, %rsi
	mov	$17, %r10
	mov	%r10, %rdx
	call	exception_handler
.foo_end:

	.globl main
main:
	enter	$0, $0
	mov	$3, %r10
	mov	%r10, %rdi
	mov	$4, %r10
	mov	%r10, %rsi
	call	foo
	mov	$0, %r10
	mov	%r10, %rax
	leave
	ret
.main_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	$20, %r10
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
