.data
.outofbounds:
	.string	"RUNTIME ERROR: Array index out of bounds (%d, %d)\n"
.methodcfend:
	.string	"RUNTIME ERROR: Method at (%d, %d) reached end of control flow without returning\n"
.main_str0:
	.string	"%d\n"
.main_str1:
	.string	"%d\n"

.text

get_int:
	enter	$8, $0
	mov	%rdi, %r10
	mov	%r10, -8(%rbp)
	mov	-8(%rbp), %r10
	mov	%r10, %rax
	leave
	ret
.get_int_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	$2, %r10
	mov	%r10, %rsi
	mov	$14, %r10
	mov	%r10, %rdx
	call	exception_handler
.get_int_end:

	.globl main
main:
	enter	$80, $0
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
	mov	$0, %r10
	mov	%r10, -48(%rbp)
	mov	$2, %r10
	mov	%r10, %rdi
	call	get_int
	mov	%rax, %r10
	mov	%r10, -8(%rbp)
	mov	$3, %r10
	mov	%r10, %rdi
	call	get_int
	mov	%rax, %r10
	mov	%r10, -16(%rbp)
	mov	$4, %r10
	mov	%r10, %rdi
	call	get_int
	mov	%rax, %r10
	mov	%r10, -40(%rbp)
	mov	$0, %r10
	mov	%r10, -24(%rbp)
	mov	$0, %r10
	mov	%r10, -32(%rbp)
	mov	-8(%rbp), %r10
	mov	-16(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -24(%rbp)
	mov	-24(%rbp), %r10
	mov	%r10, -56(%rbp)
.main_for1_init:
	mov	$5, %r10
	mov	%r10, -64(%rbp)
.main_for1_test:
	mov	-72(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	je	.main_for1_end
.main_for1_body:
	mov	-56(%rbp), %r10
	mov	%r10, -32(%rbp)
.main_for1_incr:
	mov	-64(%rbp), %r10
	mov	$1, %r11
	add	%r11, %r10
	mov	%r10, -64(%rbp)
	mov	-64(%rbp), %r10
	mov	%r10, -80(%rbp)
	jmp	.main_for1_test
.main_for1_end:
	mov	$.main_str0, %r10
	mov	%r10, %rdi
	mov	-24(%rbp), %r10
	mov	%r10, %rsi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	$.main_str1, %r10
	mov	%r10, %rdi
	mov	-32(%rbp), %r10
	mov	%r10, %rsi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	$0, %r10
	mov	%r10, %rax
	leave
	ret
.main_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	$6, %r10
	mov	%r10, %rsi
	mov	$12, %r10
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
