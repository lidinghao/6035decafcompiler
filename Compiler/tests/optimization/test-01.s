.data
.outofbounds:
	.string	"RUNTIME ERROR: Array index out of bounds (%d, %d)\n"
.methodcfend:
	.string	"RUNTIME ERROR: Method at (%d, %d) reached end of control flow without returning\n"

.text

	.globl main
main:
	enter	$24, $0
	mov	$0, %r10
	mov	%r10, -8(%rbp)
	mov	$0, %r10
	mov	%r10, -16(%rbp)
	mov	$10, %r10
	mov	%r10, -8(%rbp)
	mov	$1, %r10
	mov	%r10, -16(%rbp)
.main_if1_test:
	mov	-16(%rbp), %r10
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
	mov	%r10, -24(%rbp)
.main_for1_test:
	mov	-24(%rbp), %r10
	mov	$10, %r11
	cmp	%r11, %r10
	jge	.main_for1_end
.main_for1_body:
	mov	-8(%rbp), %r10
	mov	$1, %r11
	add	%r11, %r10
	mov	%r10, -8(%rbp)
.main_for1_incr:
	mov	-24(%rbp), %r10
	mov	$1, %r11
	add	%r11, %r10
	mov	%r10, -24(%rbp)
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
exception_handler:
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	$1, %r10
	mov	%r10, %rax
	mov	$1, %r10
	mov	%r10, %rbx
	int	$0x80
