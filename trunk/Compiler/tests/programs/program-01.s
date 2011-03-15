.data
.outofbounds:
	.string	"Array index out of bounds (%d, %d)\n"
.main_str0:
	.string	"Hello World!\n"

.text

	.globl main
main:
	enter	$8, $0
	mov	$.main_str0, %r10
	mov	%r10, %rdi
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
