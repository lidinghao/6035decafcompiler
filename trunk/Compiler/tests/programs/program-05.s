.data
	.comm	n, 1, 8
	.comm	ret, 1, 8
.outofbounds:
	.string	"Array index out of bounds (%d, %d)\n"
.main_str0:
	.string	"!%d = %d\n"

.text

factorial:
	enter	$16, $0
	mov	%rdi, %r10
	mov	%r10, -8(%rbp)
.factorial_if1_test:
	mov	-8(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	mov	$0, %r10
	mov	$1, %r11
	cmove	%r11, %r10
	mov	%r10, -16(%rbp)
	mov	-16(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	jne	.factorial_if1_true
.factorial_for1_else:
	mov	-8(%rbp), %r10
	mov	$1, %r11
	sub	%r11, %r10
	mov	%r10, -16(%rbp)
	mov	-16(%rbp), %r10
	mov	%r10, %rdi
	call	factorial
	mov	%rax, %r10
	mov	%r10, -16(%rbp)
	mov	-8(%rbp), %r10
	mov	-16(%rbp), %r11
	imul	%r11, %r10
	mov	%r10, -16(%rbp)
	mov	-16(%rbp), %r10
	mov	%r10, %rax
	jmp	.factorial_epilogue
	jmp	.factorial_if1_end
.factorial_if1_true:
	mov	$1, %r10
	mov	%r10, %rax
	jmp	.factorial_epilogue
.factorial_if1_end:
.factorial_epilogue:
	leave
	ret
.factorial_end:

	.globl main
main:
	enter	$8, $0
	mov	$10, %r10
	mov	%r10, n
	mov	n, %r10
	mov	%r10, %rdi
	call	factorial
	mov	%rax, %r10
	mov	%r10, -8(%rbp)
	mov	-8(%rbp), %r10
	mov	%r10, ret
	mov	$.main_str0, %r10
	mov	%r10, %rdi
	mov	n, %r10
	mov	%r10, %rsi
	mov	ret, %r10
	mov	%r10, %rdx
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -8(%rbp)
.main_epilogue:
	mov	$0, %r10
	mov	%r10, %rax
	leave
	ret
.main_end:
arrayexception:
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	$1, %r10
	mov	%r10, %rax
	mov	$1, %r10
	mov	%r10, %rbx
	int	$0x80
