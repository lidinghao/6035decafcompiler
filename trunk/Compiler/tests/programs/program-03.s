.data
.outofbounds:
	.string	"Array index out of bounds (%d, %d)\n"
.blockg_str0:
	.string	"gggggg\n"
.blockg_str1:
	.string	"g    g\n"
.blockg_str2:
	.string	"g\n"
.blockg_str3:
	.string	"g  ggg\n"
.blockg_str4:
	.string	"g    g\n"
.blockg_str5:
	.string	"gggggg\n"
.main_str0:
	.string	"\n"
.main_str1:
	.string	"\n"

.text

blockg:
	enter	$48, $0
	mov	$.blockg_str0, %r10
	mov	%r10, %rdi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -8(%rbp)
	mov	$.blockg_str1, %r10
	mov	%r10, %rdi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -16(%rbp)
	mov	$.blockg_str2, %r10
	mov	%r10, %rdi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -24(%rbp)
	mov	$.blockg_str3, %r10
	mov	%r10, %rdi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -32(%rbp)
	mov	$.blockg_str4, %r10
	mov	%r10, %rdi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -40(%rbp)
	mov	$.blockg_str5, %r10
	mov	%r10, %rdi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -48(%rbp)
.blockg_epilogue:
	leave
	ret
.blockg_end:

	.globl main
main:
	enter	$24, $0
	mov	$.main_str0, %r10
	mov	%r10, %rdi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -8(%rbp)
	call	blockg
	mov	%rax, %r10
	mov	%r10, -16(%rbp)
	mov	$.main_str1, %r10
	mov	%r10, %rdi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -24(%rbp)
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
