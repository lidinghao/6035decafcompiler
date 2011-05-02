.data
.outofbounds:
	.string	"RUNTIME ERROR: Array index out of bounds (%d, %d)\n"
.methodcfend:
	.string	"RUNTIME ERROR: Method at (%d, %d) reached end of control flow without returning\n"
.main_str0:
	.string	"%d\n"

.text

	.globl main
main:
	enter	$24, $0
	mov	$0, %r10
	mov	%r10, -8(%rbp)
	mov	$0, %r10
	mov	%r10, -16(%rbp)
.main.for1.init:
	mov	$0, %r10
	mov	%r10, -24(%rbp)
	mov	$5, %r10
	mov	%r10, -16(%rbp)
.main.for1.test:
	mov	-24(%rbp), %r10
	mov	$10, %r11
	cmp	%r11, %r10
	jge	.main.for1.end
.main.for1.body:
	mov	$1, %r10
	mov	%r10, -8(%rbp)
	mov	-8(%rbp), %r10
	mov	-24(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -8(%rbp)
.main.for1.incr:
	mov	-24(%rbp), %r10
	mov	$1, %r11
	add	%r11, %r10
	mov	%r10, -24(%rbp)
	jmp	.main.for1.test
.main.for1.end:
.main.mcall.print.0.begin:
	mov	$.main_str0, %r10
	mov	%r10, %rdi
	mov	-16(%rbp), %r10
	mov	%r10, %rsi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
.main.mcall.print.0.end:
	mov	$0, %r10
	mov	%r10, %rax
	leave
	ret
.main.end:
exception_handler:
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	$1, %r10
	mov	%r10, %rax
	mov	$1, %r10
	mov	%r10, %rbx
	int	$0x80
