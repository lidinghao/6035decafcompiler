.data
.outofbounds:
	.string	"Array index out of bounds (%d, %d)\n"
.main_str0:
	.string	"\n\n\tI \t Fibonacci(I) \n\t=====================\n"
.main_str1:
	.string	"\t%d \t   %d\n"
.main_str2:
	.string	"The number should be positive.\n"

.text

	.globl main
main:
	enter	$72, $0
	mov	$0, %r10
	mov	%r10, -8(%rbp)
	mov	$0, %r10
	mov	%r10, -16(%rbp)
	mov	$0, %r10
	mov	%r10, -24(%rbp)
	mov	$0, %r10
	mov	%r10, -32(%rbp)
	mov	$0, %r10
	mov	%r10, -40(%rbp)
	mov	$8, %r10
	mov	%r10, -8(%rbp)
.main_if1_test:
	mov	-8(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	mov	$0, %r10
	mov	$1, %r11
	cmovle	%r11, %r10
	mov	%r10, -48(%rbp)
	mov	-48(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	jne	.main_if1_true
.main_for1_else:
	mov	$.main_str0, %r10
	mov	%r10, %rdi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -48(%rbp)
	mov	$1, %r10
	mov	%r10, -32(%rbp)
	mov	$1, %r10
	mov	%r10, -24(%rbp)
.main_for1_init:
	mov	$1, %r10
	mov	%r10, -56(%rbp)
.main_for1_test:
	mov	-56(%rbp), %r10
	mov	-8(%rbp), %r11
	cmp	%r11, %r10
	jge	.main_for1_end
.main_for1_body:
	mov	$.main_str1, %r10
	mov	%r10, %rdi
	mov	-56(%rbp), %r10
	mov	%r10, %rsi
	mov	-24(%rbp), %r10
	mov	%r10, %rdx
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -64(%rbp)
	mov	-24(%rbp), %r10
	mov	-32(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -72(%rbp)
	mov	-72(%rbp), %r10
	mov	%r10, -40(%rbp)
	mov	-32(%rbp), %r10
	mov	%r10, -24(%rbp)
	mov	-40(%rbp), %r10
	mov	%r10, -32(%rbp)
.main_for1_incr:
	mov	-56(%rbp), %r10
	mov	$1, %r11
	add	%r11, %r10
	mov	%r10, -56(%rbp)
	jmp	.main_for1_test
.main_for1_end:
	jmp	.main_if1_end
.main_if1_true:
	mov	$.main_str2, %r10
	mov	%r10, %rdi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -72(%rbp)
.main_if1_end:
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
