.data
.outofbounds:
	.string	"RUNTIME ERROR: Array index out of bounds (%d, %d)\n"
.methodcfend:
	.string	"RUNTIME ERROR: Method at (%d, %d) reached end of control flow without returning\n"
	.comm	x, 8
	.comm	A, 80

.text

	.globl main
main:
	enter	$16, $0
	mov	$0, %r10
	mov	%r10, -8(%rbp)
	mov	$0, %r10
	mov	%r10, -16(%rbp)
.main_array_A_0_begin:
	mov	$0, %r10
	mov	$10, %r11
	cmp	%r11, %r10
	jge	.main_array_A_0_fail
	mov	-8(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	jl 	.main_array_A_0_fail
	jmp	.main_array_A_0_pass
.main_array_A_0_fail:
	mov	$.outofbounds, %r10
	mov	%r10, %rdi
	mov	$6, %r10
	mov	%r10, %rsi
	mov	$13, %r10
	mov	%r10, %rdx
	call	exception_handler
.main_array_A_0_pass:
	mov	-8(%rbp), %r10
	mov	A(, %r10, 8), %r10
	mov	%r10, -8(%rbp)
.main_array_A_1_begin:
	mov	-16(%rbp), %r10
	mov	$10, %r11
	cmp	%r11, %r10
	jge	.main_array_A_1_fail
	mov	-16(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	jl 	.main_array_A_1_fail
	jmp	.main_array_A_1_pass
.main_array_A_1_fail:
	mov	$.outofbounds, %r10
	mov	%r10, %rdi
	mov	$7, %r10
	mov	%r10, %rsi
	mov	$13, %r10
	mov	%r10, %rdx
	call	exception_handler
.main_array_A_1_pass:
	mov	-16(%rbp), %r10
	mov	A(, %r10, 8), %r10
	mov	%r10, -16(%rbp)
	mov	$0, %r10
	mov	%r10, %rax
	leave
	ret
.main_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	$4, %r10
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
