.data
.outofbounds:
	.string	"RUNTIME ERROR: Array index out of bounds (%d, %d)\n"
.methodcfend:
	.string	"RUNTIME ERROR: Method at (%d, %d) reached end of control flow without returning\n"
	.comm	a, 1, 8
	.comm	A, 80
.main_str0:
	.string	"%d\n"
.main_str1:
	.string	"%d\n"
.main_str2:
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
	mov	$3, %r10
	mov	%r10, %rsi
	mov	$14, %r10
	mov	%r10, %rdx
	call	exception_handler
.get_int_end:

bool:
	enter	$56, $0
	mov	%rdi, %r10
	mov	%r10, -8(%rbp)
	mov	%rsi, %r10
	mov	%r10, -16(%rbp)
	mov	%rdx, %r10
	mov	%r10, -24(%rbp)
	mov	%rcx, %r10
	mov	%r10, -32(%rbp)
	mov	$0, %r10
	mov	%r10, -40(%rbp)
.bool_or1_testLHS:
.bool_or2_testLHS:
	mov	-8(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	je	.bool_or2_testRHS
	mov	$1, %r10
	mov	%r10, -48(%rbp)
	jmp	.bool_or2_end
.bool_or2_testRHS:
	mov	-16(%rbp), %r10
	mov	%r10, -48(%rbp)
.bool_or2_end:
	mov	-48(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	je	.bool_or1_testRHS
	mov	$1, %r10
	mov	%r10, -48(%rbp)
	jmp	.bool_or1_end
.bool_or1_testRHS:
.bool_and1_testLHS:
	mov	-24(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	jne	.bool_and1_testRHS
	mov	$0, %r10
	mov	%r10, -56(%rbp)
	jmp	.bool_and1_end
.bool_and1_testRHS:
	mov	-32(%rbp), %r10
	mov	%r10, -56(%rbp)
.bool_and1_end:
	mov	-56(%rbp), %r10
	mov	%r10, -48(%rbp)
.bool_or1_end:
	mov	-48(%rbp), %r10
	mov	%r10, -40(%rbp)
	leave
	ret
.bool_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	$7, %r10
	mov	%r10, %rsi
	mov	$12, %r10
	mov	%r10, %rdx
	call	exception_handler
.bool_end:

test:
	enter	$56, $0
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
	mov	$0, %r10
	mov	%r10, -56(%rbp)
	mov	16(%rbp), %r10
	mov	%r10, -56(%rbp)
	leave
	ret
.test_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	$12, %r10
	mov	%r10, %rsi
	mov	$12, %r10
	mov	%r10, %rdx
	call	exception_handler
.test_end:

get_bool:
	enter	$8, $0
	mov	%rdi, %r10
	mov	%r10, -8(%rbp)
	mov	-8(%rbp), %r10
	mov	%r10, %rax
	leave
	ret
.get_bool_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	$17, %r10
	mov	%r10, %rsi
	mov	$19, %r10
	mov	%r10, %rdx
	call	exception_handler
.get_bool_end:

	.globl main
main:
	enter	$48, $0
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
	mov	$1, %r10
	mov	%r10, %rdi
	call	get_bool
	mov	%rax, %r10
	mov	%r10, -48(%rbp)
	mov	$0, %r10
	mov	%r10, -24(%rbp)
	mov	$0, %r10
	mov	%r10, -32(%rbp)
	mov	$0, %r10
	mov	%r10, -40(%rbp)
	mov	-8(%rbp), %r10
	mov	-16(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -24(%rbp)
.main_if1_test:
	mov	-48(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	jne	.main_if1_true
.main_if1_else:
	jmp	.main_if1_end
.main_if1_true:
	mov	-8(%rbp), %r10
	mov	-16(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -32(%rbp)
	mov	$1, %r10
	mov	%r10, -8(%rbp)
.main_if1_end:
	mov	-8(%rbp), %r10
	mov	-16(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -40(%rbp)
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
	mov	$.main_str2, %r10
	mov	%r10, %rdi
	mov	-40(%rbp), %r10
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
	mov	$21, %r10
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
