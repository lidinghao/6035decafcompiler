.data
.outofbounds:
	.string	"Array index out of bounds (%d, %d)\n"
.foo_str0:
	.string	"%d %d %d\n"
.main_str0:
	.string	"%d %d %d\n"

.text

foo:
	enter	$32, $0
	mov	%rdi, %r10
	mov	%r10, -8(%rbp)
	mov	%rsi, %r10
	mov	%r10, -16(%rbp)
	mov	%rdx, %r10
	mov	%r10, -24(%rbp)
	mov	-8(%rbp), %r10
	mov	$3, %r11
	add	%r11, %r10
	mov	%r10, -8(%rbp)
	mov	-16(%rbp), %r10
	mov	$3, %r11
	add	%r11, %r10
	mov	%r10, -16(%rbp)
	mov	-24(%rbp), %r10
	mov	$3, %r11
	add	%r11, %r10
	mov	%r10, -24(%rbp)
	mov	$.foo_str0, %r10
	mov	%r10, %rdi
	mov	-8(%rbp), %r10
	mov	%r10, %rsi
	mov	-16(%rbp), %r10
	mov	%r10, %rdx
	mov	-24(%rbp), %r10
	mov	%r10, %rcx
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -32(%rbp)
.foo_epilogue:
	leave
	ret
.foo_end:

	.globl main
main:
	enter	$40, $0
	mov	$0, %r10
	mov	%r10, -8(%rbp)
	mov	$0, %r10
	mov	%r10, -16(%rbp)
	mov	$0, %r10
	mov	%r10, -24(%rbp)
	mov	$1, %r10
	mov	%r10, -8(%rbp)
	mov	$2, %r10
	mov	%r10, -16(%rbp)
	mov	$3, %r10
	mov	%r10, -24(%rbp)
	mov	-8(%rbp), %r10
	mov	%r10, %rdi
	mov	-16(%rbp), %r10
	mov	%r10, %rsi
	mov	-24(%rbp), %r10
	mov	%r10, %rdx
	call	foo
	mov	%rax, %r10
	mov	%r10, -32(%rbp)
	mov	$.main_str0, %r10
	mov	%r10, %rdi
	mov	-8(%rbp), %r10
	mov	%r10, %rsi
	mov	-16(%rbp), %r10
	mov	%r10, %rdx
	mov	-24(%rbp), %r10
	mov	%r10, %rcx
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -40(%rbp)
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
