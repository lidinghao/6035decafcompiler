.data
.outofbounds:
	.string	"RUNTIME ERROR: Array index out of bounds (%d, %d)\n"
.methodcfend:
	.string	"RUNTIME ERROR: Method at (%d, %d) reached end of control flow without returning\n"

.text

	.globl main
main:
	enter	$32, $0
	mov	$0, %r10
	mov	%r10, -8(%rbp)
	mov	$0, %r10
	mov	%r10, -16(%rbp)
	mov	$10, %r10
	mov	%r10, -8(%rbp)
	mov	$1, %r10
	mov	%r10, -16(%rbp)
.main_if1_test:
	mov	-8(%rbp), %r10
	mov	$10, %r11
	cmp	%r11, %r10
	mov	$0, %r10
	mov	$1, %r11
	cmovg	%r11, %r10
	mov	%r10, -24(%rbp)
	mov	-24(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	jne	.main_if1_true
.main_if1_else:
	mov	$5, %r10
	mov	%r10, -8(%rbp)
	jmp	.main_if1_end
.main_if1_true:
	mov	$15, %r10
	mov	%r10, -8(%rbp)
.main_for1_init:
	mov	$0, %r10
	mov	%r10, -32(%rbp)
.main_for1_test:
	mov	-32(%rbp), %r10
	mov	$10, %r11
	cmp	%r11, %r10
	mov	$0, %r10
	mov	$1, %r11
	cmovl	%r11, %r10
	mov	%r10, -24(%rbp)
	mov	-24(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	je	.main_for1_end
.main_for1_body:
	mov	-8(%rbp), %r10
	mov	$1, %r11
	add	%r11, %r10
	mov	%r10, -8(%rbp)
.main_for1_incr:
	mov	-32(%rbp), %r10
	mov	$1, %r11
	add	%r11, %r10
	mov	%r10, -32(%rbp)
	jmp	.main_for1_test
.main_for1_end:
.main_if1_end:
	mov	$0, %r10
	mov	%r10, %rax
	leave
	ret
.main_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	$2, %r10
	mov	%r10, %rsi
	mov	$18, %r10
	mov	%r10, %rdx
	call	exception_handler
.main_end:

add:
	enter	$56, $0
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
	mov	%r10, -24(%rbp)
	mov	$4, %r10
	mov	%r10, -32(%rbp)
.add_if1_test:
	mov	-16(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	mov	$0, %r10
	mov	$1, %r11
	cmove	%r11, %r10
	mov	%r10, -48(%rbp)
	mov	-48(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	jne	.add_if1_true
.add_if1_else:
	mov	$2, %r10
	mov	%r10, -40(%rbp)
	jmp	.add_if1_end
.add_if1_true:
	mov	$1, %r10
	mov	%r10, -40(%rbp)
.add_if1_end:
.add_for1_init:
	mov	$0, %r10
	mov	%r10, -56(%rbp)
.add_for1_test:
	mov	-56(%rbp), %r10
	mov	-8(%rbp), %r11
	cmp	%r11, %r10
	mov	$0, %r10
	mov	$1, %r11
	cmovl	%r11, %r10
	mov	%r10, -48(%rbp)
	mov	-48(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	je	.add_for1_end
.add_for1_body:
	mov	-32(%rbp), %r10
	mov	-40(%rbp), %r11
	imul	%r11, %r10
	mov	%r10, -48(%rbp)
	mov	-24(%rbp), %r10
	mov	-48(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -48(%rbp)
	mov	-48(%rbp), %r10
	mov	%r10, -24(%rbp)
.add_for1_incr:
	mov	-56(%rbp), %r10
	mov	$1, %r11
	add	%r11, %r10
	mov	%r10, -56(%rbp)
	jmp	.add_for1_test
.add_for1_end:
	mov	-24(%rbp), %r10
	mov	%r10, %rax
	leave
	ret
.add_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	$19, %r10
	mov	%r10, %rsi
	mov	$16, %r10
	mov	%r10, %rdx
	call	exception_handler
.add_end:
exception_handler:
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	$1, %r10
	mov	%r10, %rax
	mov	$1, %r10
	mov	%r10, %rbx
	int	$0x80
