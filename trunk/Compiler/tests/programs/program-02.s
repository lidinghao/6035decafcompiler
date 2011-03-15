.data
.outofbounds:
	.string	"Array index out of bounds (%d, %d)\n"
.main_str0:
	.string	"\t  n  \t    2^n\n"
.main_str1:
	.string	"\t================\n"
.main_str2:
	.string	"\t%3d \t %6d\n"

.text

	.globl main
main:
	enter	$64, $0
	mov	$0, %r10
	mov	%r10, -8(%rbp)
	mov	$0, %r10
	mov	%r10, -16(%rbp)
	mov	$0, %r10
	mov	%r10, -24(%rbp)
	mov	$1, %r10
	mov	%r10, -16(%rbp)
	mov	$32, %r10
	mov	%r10, -8(%rbp)
	mov	$.main_str0, %r10
	mov	%r10, %rdi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -32(%rbp)
	mov	$.main_str1, %r10
	mov	%r10, %rdi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -40(%rbp)
.main_for1_init:
	mov	$0, %r10
	mov	%r10, -48(%rbp)
.main_for1_test:
	mov	-48(%rbp), %r10
	mov	-8(%rbp), %r11
	cmp	%r11, %r10
	jge	.main_for1_end
.main_for1_body:
	mov	$.main_str2, %r10
	mov	%r10, %rdi
	mov	-48(%rbp), %r10
	mov	%r10, %rsi
	mov	-16(%rbp), %r10
	mov	%r10, %rdx
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -56(%rbp)
	mov	$2, %r10
	mov	-16(%rbp), %r11
	imul	%r11, %r10
	mov	%r10, -64(%rbp)
	mov	-64(%rbp), %r10
	mov	%r10, -16(%rbp)
.main_for1_incr:
	mov	-48(%rbp), %r10
	mov	$1, %r11
	add	%r11, %r10
	mov	%r10, -48(%rbp)
	jmp	.main_for1_test
.main_for1_end:
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
