.data
.outofbounds:
	.string	"RUNTIME ERROR: Array index out of bounds (%d, %d)\n"
.methodcfend:
	.string	"RUNTIME ERROR: Method at (%d, %d) reached end of control flow without returning\n"
.main_str0:
	.string	"INCORRECT\n"
.main_str1:
	.string	"correct\n"
.main_str2:
	.string	"sum of 1 - 8 is %d (36)\n"

.text

add:
	enter	$80, $0
	mov	%rdi, %r10
	mov	%r10, -8(%rbp)
	mov	-8(%rbp), %r10
	mov	%r10, -24(%rbp)
	mov	%rsi, %r10
	mov	%r10, -16(%rbp)
	mov	-16(%rbp), %r10
	mov	%r10, -32(%rbp)
	mov	-8(%rbp), %r10
	mov	-16(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -40(%rbp)
	mov	-40(%rbp), %r10
	mov	%r10, -48(%rbp)
	mov	-40(%rbp), %r10
	mov	%r10, %rax
	mov	%rax, %r10
	mov	%r10, -56(%rbp)
	leave
	ret
.add_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	%rdi, %r10
	mov	%r10, -64(%rbp)
	mov	$3, %r10
	mov	%r10, %rsi
	mov	%rsi, %r10
	mov	%r10, -72(%rbp)
	mov	$12, %r10
	mov	%r10, %rdx
	mov	%rdx, %r10
	mov	%r10, -80(%rbp)
	call	exception_handler
.add_end:

sub:
	enter	$80, $0
	mov	%rdi, %r10
	mov	%r10, -8(%rbp)
	mov	-8(%rbp), %r10
	mov	%r10, -24(%rbp)
	mov	%rsi, %r10
	mov	%r10, -16(%rbp)
	mov	-16(%rbp), %r10
	mov	%r10, -32(%rbp)
	mov	-8(%rbp), %r10
	mov	-16(%rbp), %r11
	sub	%r11, %r10
	mov	%r10, -40(%rbp)
	mov	-40(%rbp), %r10
	mov	%r10, -48(%rbp)
	mov	-40(%rbp), %r10
	mov	%r10, %rax
	mov	%rax, %r10
	mov	%r10, -56(%rbp)
	leave
	ret
.sub_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	%rdi, %r10
	mov	%r10, -64(%rbp)
	mov	$8, %r10
	mov	%r10, %rsi
	mov	%rsi, %r10
	mov	%r10, -72(%rbp)
	mov	$12, %r10
	mov	%r10, %rdx
	mov	%rdx, %r10
	mov	%r10, -80(%rbp)
	call	exception_handler
.sub_end:

sum:
	enter	$192, $0
	mov	%rdi, %r10
	mov	%r10, -8(%rbp)
	mov	-8(%rbp), %r10
	mov	%r10, -56(%rbp)
	mov	%rsi, %r10
	mov	%r10, -16(%rbp)
	mov	-16(%rbp), %r10
	mov	%r10, -64(%rbp)
	mov	%rdx, %r10
	mov	%r10, -24(%rbp)
	mov	-24(%rbp), %r10
	mov	%r10, -72(%rbp)
	mov	%rcx, %r10
	mov	%r10, -32(%rbp)
	mov	-32(%rbp), %r10
	mov	%r10, -80(%rbp)
	mov	%r8, %r10
	mov	%r10, -40(%rbp)
	mov	-40(%rbp), %r10
	mov	%r10, -88(%rbp)
	mov	%r9, %r10
	mov	%r10, -48(%rbp)
	mov	-48(%rbp), %r10
	mov	%r10, -96(%rbp)
	mov	-8(%rbp), %r10
	mov	-16(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -104(%rbp)
	mov	-104(%rbp), %r10
	mov	%r10, -112(%rbp)
	mov	-104(%rbp), %r10
	mov	-24(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -104(%rbp)
	mov	-104(%rbp), %r10
	mov	%r10, -120(%rbp)
	mov	-104(%rbp), %r10
	mov	-32(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -104(%rbp)
	mov	-104(%rbp), %r10
	mov	%r10, -128(%rbp)
	mov	-104(%rbp), %r10
	mov	-40(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -104(%rbp)
	mov	-104(%rbp), %r10
	mov	%r10, -136(%rbp)
	mov	-104(%rbp), %r10
	mov	-48(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -104(%rbp)
	mov	-104(%rbp), %r10
	mov	%r10, -144(%rbp)
	mov	-104(%rbp), %r10
	mov	16(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -104(%rbp)
	mov	-104(%rbp), %r10
	mov	%r10, -152(%rbp)
	mov	-104(%rbp), %r10
	mov	24(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -104(%rbp)
	mov	-104(%rbp), %r10
	mov	%r10, -160(%rbp)
	mov	-104(%rbp), %r10
	mov	%r10, %rax
	mov	%rax, %r10
	mov	%r10, -168(%rbp)
	leave
	ret
.sum_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	%rdi, %r10
	mov	%r10, -176(%rbp)
	mov	$13, %r10
	mov	%r10, %rsi
	mov	%rsi, %r10
	mov	%r10, -184(%rbp)
	mov	$12, %r10
	mov	%r10, %rdx
	mov	%rdx, %r10
	mov	%r10, -192(%rbp)
	call	exception_handler
.sum_end:

	.globl main
main:
	enter	$312, $0
	mov	$0, %r10
	mov	%r10, -8(%rbp)
	mov	-8(%rbp), %r10
	mov	%r10, -16(%rbp)
	mov	-16(%rbp), %r10
	mov	%r10, -24(%rbp)
	mov	-16(%rbp), %r10
	mov	%r10, -32(%rbp)
	mov	-16(%rbp), %r10
	mov	%r10, -40(%rbp)
	mov	$1, %r10
	mov	%r10, -8(%rbp)
	mov	-8(%rbp), %r10
	mov	%r10, -48(%rbp)
	mov	$2, %r10
	mov	%r10, -24(%rbp)
	mov	-24(%rbp), %r10
	mov	%r10, -56(%rbp)
	mov	-8(%rbp), %r10
	mov	%r10, %rdi
	mov	%rdi, %r10
	mov	%r10, -64(%rbp)
	mov	-24(%rbp), %r10
	mov	%r10, %rsi
	mov	%rsi, %r10
	mov	%r10, -72(%rbp)
	call	add
	mov	%rax, %r10
	mov	%r10, -80(%rbp)
	mov	-80(%rbp), %r10
	mov	%r10, -88(%rbp)
	mov	-80(%rbp), %r10
	mov	%r10, -32(%rbp)
	mov	-32(%rbp), %r10
	mov	%r10, -96(%rbp)
	mov	-32(%rbp), %r10
	mov	%r10, %rdi
	mov	%rdi, %r10
	mov	%r10, -104(%rbp)
	mov	-72(%rbp), %r10
	mov	%r10, %rsi
	call	sub
	mov	%rax, %r10
	mov	%r10, -80(%rbp)
	mov	-80(%rbp), %r10
	mov	%r10, -112(%rbp)
	mov	-80(%rbp), %r10
	mov	%r10, -40(%rbp)
	mov	-40(%rbp), %r10
	mov	%r10, -120(%rbp)
.main_if1_test:
	mov	-40(%rbp), %r10
	mov	-8(%rbp), %r11
	cmp	%r11, %r10
	mov	$0, %r10
	mov	$1, %r11
	cmove	%r11, %r10
	mov	%r10, -80(%rbp)
	mov	-80(%rbp), %r10
	mov	%r10, -128(%rbp)
	mov	-80(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	jne	.main_if1_true
.main_if1_else:
	mov	$.main_str0, %r10
	mov	%r10, %rdi
	mov	%rdi, %r10
	mov	%r10, -136(%rbp)
	mov	$0, %r10
	mov	%r10, %rax
	mov	%rax, %r10
	mov	%r10, -144(%rbp)
	call	printf
	mov	%rax, %r10
	mov	%r10, -80(%rbp)
	mov	-80(%rbp), %r10
	mov	%r10, -152(%rbp)
	jmp	.main_if1_end
.main_if1_true:
	mov	$.main_str1, %r10
	mov	%r10, %rdi
	mov	%rdi, %r10
	mov	%r10, -160(%rbp)
	mov	$0, %r10
	mov	%r10, %rax
	mov	%rax, %r10
	mov	%r10, -168(%rbp)
	call	printf
	mov	%rax, %r10
	mov	%r10, -176(%rbp)
	mov	-176(%rbp), %r10
	mov	%r10, -184(%rbp)
.main_if1_end:
	mov	$1, %r10
	mov	%r10, %rdi
	mov	%rdi, %r10
	mov	%r10, -192(%rbp)
	mov	$2, %r10
	mov	%r10, %rsi
	mov	%rsi, %r10
	mov	%r10, -200(%rbp)
	mov	$3, %r10
	mov	%r10, %rdx
	mov	%rdx, %r10
	mov	%r10, -208(%rbp)
	mov	$4, %r10
	mov	%r10, %rcx
	mov	%rcx, %r10
	mov	%r10, -216(%rbp)
	mov	$5, %r10
	mov	%r10, %r8
	mov	%r8, %r10
	mov	%r10, -224(%rbp)
	mov	$6, %r10
	mov	%r10, %r9
	mov	%r9, %r10
	mov	%r10, -232(%rbp)
	push	$8
	push	$7
	call	sum
	mov	%rsp, %r10
	mov	$2, %r11
	sub	%r11, %r10
	mov	%r10, %rsp
	mov	%rsp, %r10
	mov	%r10, -240(%rbp)
	mov	%rax, %r10
	mov	%r10, -248(%rbp)
	mov	-248(%rbp), %r10
	mov	%r10, -256(%rbp)
	mov	$.main_str2, %r10
	mov	%r10, %rdi
	mov	%rdi, %r10
	mov	%r10, -264(%rbp)
	mov	-248(%rbp), %r10
	mov	%r10, %rsi
	mov	%rsi, %r10
	mov	%r10, -272(%rbp)
	mov	$0, %r10
	mov	%r10, %rax
	mov	%rax, %r10
	mov	%r10, -280(%rbp)
	call	printf
	mov	%rax, %r10
	mov	%r10, -248(%rbp)
	mov	-248(%rbp), %r10
	mov	%r10, -288(%rbp)
	mov	-280(%rbp), %r10
	mov	%r10, %rax
	leave
	ret
.main_cfendhandler:
	mov	$.methodcfend, %r10
	mov	%r10, %rdi
	mov	%rdi, %r10
	mov	%r10, -296(%rbp)
	mov	$19, %r10
	mov	%r10, %rsi
	mov	%rsi, %r10
	mov	%r10, -304(%rbp)
	mov	$14, %r10
	mov	%r10, %rdx
	mov	%rdx, %r10
	mov	%r10, -312(%rbp)
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
