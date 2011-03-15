.data
.outofbounds:
	.string	"Array index out of bounds (%d, %d)\n"
.main_str0:
	.string	"max of 3 nums 3,4,5 is %d"

.text

max:
	enter	$40, $0
	mov	%rdi, %r10
	mov	%r10, -8(%rbp)
	mov	%rsi, %r10
	mov	%r10, -16(%rbp)
	mov	%rdx, %r10
	mov	%r10, -24(%rbp)
	mov	$0, %r10
	mov	%r10, -32(%rbp)
	mov	-8(%rbp), %r10
	mov	%r10, -32(%rbp)
.max_if1_test:
	mov	-8(%rbp), %r10
	mov	-16(%rbp), %r11
	cmp	%r11, %r10
	mov	$0, %r10
	mov	$1, %r11
	cmovl	%r11, %r10
	mov	%r10, -40(%rbp)
	mov	-40(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	jne	.max_if1_true
.max_for1_else:
	jmp	.max_if1_end
.max_if1_true:
	mov	-16(%rbp), %r10
	mov	%r10, -32(%rbp)
.max_if1_end:
.max_if2_test:
	mov	-32(%rbp), %r10
	mov	-24(%rbp), %r11
	cmp	%r11, %r10
	mov	$0, %r10
	mov	$1, %r11
	cmovl	%r11, %r10
	mov	%r10, -40(%rbp)
	mov	-40(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	jne	.max_if2_true
.max_for2_else:
	jmp	.max_if2_end
.max_if2_true:
	mov	-24(%rbp), %r10
	mov	%r10, -32(%rbp)
.max_if2_end:
	mov	-24(%rbp), %r10
	mov	%r10, %rax
	jmp	.max_epilogue
.max_epilogue:
	leave
	ret
.max_end:

	.globl main
main:
	enter	$8, $0
	mov	$3, %r10
	mov	%r10, %rdi
	mov	$4, %r10
	mov	%r10, %rsi
	mov	$5, %r10
	mov	%r10, %rdx
	call	max
	mov	%rax, %r10
	mov	%r10, -8(%rbp)
	mov	$.main_str0, %r10
	mov	%r10, %rdi
	mov	-8(%rbp), %r10
	mov	%r10, %rsi
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
