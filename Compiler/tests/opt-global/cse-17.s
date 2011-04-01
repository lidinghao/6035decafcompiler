.data
.outofbounds:
	.string	"RUNTIME ERROR: Array index out of bounds (%d, %d)\n"
.methodcfend:
	.string	"RUNTIME ERROR: Method at (%d, %d) reached end of control flow without returning\n"
	.comm	a, 1, 8
	.comm	b, 1, 8
	.comm	c, 1, 8
	.comm	d, 1, 8
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
	mov	$4, %r10
	mov	%r10, %rsi
	mov	$14, %r10
	mov	%r10, %rdx
	call	exception_handler
.get_int_end:

foo:
	enter	$0, $0
	mov	$1, %r10
	mov	%r10, a
	leave
	ret
.foo_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	$8, %r10
	mov	%r10, %rsi
	mov	$11, %r10
	mov	%r10, %rdx
	call	exception_handler
.foo_end:

	.globl main
main:
	enter	$16, $0
	mov	$2, %r10
	mov	%r10, %rdi
	call	get_int
	mov	%rax, %r10
	mov	%r10, a
	mov	$3, %r10
	mov	%r10, %rdi
	call	get_int
	mov	%rax, %r10
	mov	%r10, b
	mov	$0, %r10
	mov	%r10, c
	mov	$0, %r10
	mov	%r10, d
	mov	a, %r10
	mov	b, %r11
	add	%r11, %r10
	mov	%r10, c
	call	foo
	mov	a, %r10
	mov	b, %r11
	add	%r11, %r10
	mov	%r10, d
	mov	$.main_str0, %r10
	mov	%r10, %rdi
	mov	c, %r10
	mov	%r10, %rsi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	$.main_str1, %r10
	mov	%r10, %rdi
	mov	d, %r10
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
	mov	$12, %r10
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
