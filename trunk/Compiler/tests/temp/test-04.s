.data
.outofbounds:
	.string	"RUNTIME ERROR: Array index out of bounds (%d, %d)\n"
.methodcfend:
	.string	"RUNTIME ERROR: Method at (%d, %d) reached end of control flow without returning\n"
	.comm	x, 8
	.comm	z, 8
	.comm	A, 80

.text

foo:
	enter	$40, $0
	push	%rbx
	push	%r12
	push	%r13
	push	%r14
	push	%r15
	mov	%rdi, %r10
	mov	%r10, -8(%rbp)
	mov	%rsi, %r10
	mov	%r10, -16(%rbp)
	mov	%rdx, %r10
	mov	%r10, -24(%rbp)
	mov	-8(%rbp), %r10
	mov	-16(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -32(%rbp)
	mov	-32(%rbp), %r10
	mov	z, %r11
	add	%r11, %r10
	mov	%r10, -40(%rbp)
.foo_if1_test:
	mov	-24(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	jne	.foo_if1_true
.foo_if1_else:
	jmp	.foo_if1_end
.foo_if1_true:
	mov	-16(%rbp), %r10
	mov	%r10, -40(%rbp)
.foo_if1_end:
	pop	%r15
	pop	%r14
	pop	%r13
	pop	%r12
	pop	%rbx
	leave
	ret
.foo_end:

bar:
	enter	$0, $0
	push	%rbx
	push	%r12
	push	%r13
	push	%r14
	push	%r15
	mov	$1000, %r10
	mov	%r10, %rax
	pop	%r15
	pop	%r14
	pop	%r13
	pop	%r12
	pop	%rbx
	leave
	ret
.bar_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	$16, %r10
	mov	%r10, %rsi
	mov	$10, %r10
	mov	%r10, %rdx
	call	exception_handler
.bar_end:

	.globl main
main:
	enter	$0, $0
	push	%rbx
	push	%r12
	push	%r13
	push	%r14
	push	%r15
	mov	$0, %r10
	mov	%r10, %rax
	pop	%r15
	pop	%r14
	pop	%r13
	pop	%r12
	pop	%rbx
	leave
	ret
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
