.data
.outofbounds:
	.string	"RUNTIME ERROR: Array index out of bounds (%d, %d)\n"
.methodcfend:
	.string	"RUNTIME ERROR: Method at (%d, %d) reached end of control flow without returning\n"
	.comm	field_in_class, 1, 8
.test_long_args_str0:
	.string	"args: %d+%d+%d+%d+%d+%d=%d\n"
.test_args_str0:
	.string	"args: %d + %d + %d=%d\n"
.main_str0:
	.string	"hello world from main\n"
.main_str1:
	.string	"some values (13,14): %d %d\n"
.main_str2:
	.string	"returned a variable, value is %d, should be 81\n"

.text

test_long_args:
	enter	$96, $0
	mov	%rdi, %r10
	mov	%r10, -8(%rbp)
	mov	%rsi, %r10
	mov	%r10, -16(%rbp)
	mov	%rdx, %r10
	mov	%r10, -24(%rbp)
	mov	%rcx, %r10
	mov	%r10, -32(%rbp)
	mov	%r8, %r10
	mov	%r10, -40(%rbp)
	mov	%r9, %r10
	mov	%r10, -48(%rbp)
	mov	-8(%rbp), %r10
	mov	-16(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -56(%rbp)
	mov	-56(%rbp), %r10
	mov	%r10, -64(%rbp)
	mov	-56(%rbp), %r10
	mov	-24(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -56(%rbp)
	mov	-56(%rbp), %r10
	mov	%r10, -72(%rbp)
	mov	-56(%rbp), %r10
	mov	-32(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -56(%rbp)
	mov	-56(%rbp), %r10
	mov	%r10, -80(%rbp)
	mov	-56(%rbp), %r10
	mov	-40(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -56(%rbp)
	mov	-56(%rbp), %r10
	mov	%r10, -88(%rbp)
	mov	-56(%rbp), %r10
	mov	-48(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -56(%rbp)
	mov	$.test_long_args_str0, %r10
	mov	%r10, %rdi
	mov	-8(%rbp), %r10
	mov	%r10, %rsi
	mov	-16(%rbp), %r10
	mov	%r10, %rdx
	mov	-24(%rbp), %r10
	mov	%r10, %rcx
	mov	-32(%rbp), %r10
	mov	%r10, %r8
	mov	-40(%rbp), %r10
	mov	%r10, %r9
	push	-56(%rbp)
	push	-48(%rbp)
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rsp, %r10
	mov	$2, %r11
	sub	%r11, %r10
	mov	%r10, %rsp
	mov	-64(%rbp), %r10
	mov	%r10, -96(%rbp)
	mov	-72(%rbp), %r10
	mov	%r10, -96(%rbp)
	mov	-80(%rbp), %r10
	mov	%r10, -96(%rbp)
	mov	-88(%rbp), %r10
	mov	%r10, -96(%rbp)
	mov	-56(%rbp), %r10
	mov	%r10, -96(%rbp)
	mov	-96(%rbp), %r10
	mov	%r10, %rax
	leave
	ret
.test_long_args_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	$5, %r10
	mov	%r10, %rsi
	mov	$21, %r10
	mov	%r10, %rdx
	call	exception_handler
.test_long_args_end:

test_args:
	enter	$32, $0
	mov	%rdi, %r10
	mov	%r10, -8(%rbp)
	mov	%rsi, %r10
	mov	%r10, -16(%rbp)
	mov	%rdx, %r10
	mov	%r10, -24(%rbp)
	mov	-8(%rbp), %r10
	mov	-16(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -32(%rbp)
	mov	-32(%rbp), %r10
	mov	-24(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -32(%rbp)
	mov	$.test_args_str0, %r10
	mov	%r10, %rdi
	mov	-8(%rbp), %r10
	mov	%r10, %rsi
	mov	-16(%rbp), %r10
	mov	%r10, %rdx
	mov	-24(%rbp), %r10
	mov	%r10, %rcx
	mov	-32(%rbp), %r10
	mov	%r10, %r8
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	leave
	ret
.test_args_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	$11, %r10
	mov	%r10, %rsi
	mov	$17, %r10
	mov	%r10, %rdx
	call	exception_handler
.test_args_end:

	.globl main
main:
	enter	$32, $0
	mov	$0, %r10
	mov	%r10, -8(%rbp)
	mov	$0, %r10
	mov	%r10, -16(%rbp)
	mov	$0, %r10
	mov	%r10, -24(%rbp)
	mov	$0, %r10
	mov	%r10, -32(%rbp)
	mov	$.main_str0, %r10
	mov	%r10, %rdi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	$13, %r10
	mov	%r10, field_in_class
	mov	$14, %r10
	mov	%r10, -8(%rbp)
	mov	$.main_str1, %r10
	mov	%r10, %rdi
	mov	$13, %r10
	mov	%r10, %rsi
	mov	$14, %r10
	mov	%r10, %rdx
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	field_in_class, %r10
	mov	%r10, %rdi
	mov	$14, %r10
	mov	%r10, %rsi
	mov	$14, %r10
	mov	%r10, %rdx
	call	test_args
	mov	field_in_class, %r10
	mov	%r10, %rdi
	mov	%rdi, %r10
	mov	%r10, %rsi
	mov	%rdi, %r10
	mov	%r10, %rdx
	mov	$14, %r10
	mov	%r10, %rcx
	mov	$14, %r10
	mov	%r10, %r8
	mov	$14, %r10
	mov	%r10, %r9
	call	test_long_args
	mov	%rax, %r10
	mov	%r10, -16(%rbp)
	mov	$.main_str2, %r10
	mov	%r10, %rdi
	mov	-16(%rbp), %r10
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
	mov	$16, %r10
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
