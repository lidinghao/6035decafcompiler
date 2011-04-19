.data
.outofbounds:
	.string	"RUNTIME ERROR: Array index out of bounds (%d, %d)\n"
.methodcfend:
	.string	"RUNTIME ERROR: Method at (%d, %d) reached end of control flow without returning\n"
	.comm	A, 80

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
	mov	$0, %r10
	mov	%r10, -32(%rbp)
	mov	$0, %r10
	mov	%r10, -40(%rbp)
	mov	$0, %r10
	mov	%r10, -48(%rbp)
	mov	-48(%rbp), %r10
	mov	$10, %r11
	cmp	%r11, %r10
	jge	.main_array0_fail
	mov	-32(%rbp), %r10
	mov	-24(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -48(%rbp)
	mov	-48(%rbp), %r10
	mov	%r10, -56(%rbp)
	mov	-48(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	jl 	.main_array0_fail
	jmp	.main_array0_pass
.main_array0_fail:
	mov	$.outofbounds, %r10
	mov	%r10, %rdi
	mov	$7, %r10
	mov	%r10, %rsi
	mov	$21, %r10
	mov	%r10, %rdx
	call	exception_handler
.main_array0_pass:
	mov	-40(%rbp), %r10
	mov	A(, %r10, 8), %r10
	mov	$4, %r11
	add	%r11, %r10
	mov	%r10, -8(%rbp)
	mov	-8(%rbp), %r10
	mov	%r10, -64(%rbp)
	mov	$0, %r10
	mov	%r10, %rax
	leave
	ret
.main_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	$4, %r10
	mov	%r10, %rsi
	mov	$18, %r10
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
