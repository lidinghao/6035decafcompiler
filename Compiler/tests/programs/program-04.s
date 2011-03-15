.data
.outofbounds:
	.string	"Array index out of bounds (%d, %d)\n"
.main_str0:
	.string	"Your collection is worth\n "
.main_str1:
	.string	"\t%d dollar"
.main_str2:
	.string	"s, "
.main_str3:
	.string	", "
.main_str4:
	.string	"%d quarter"
.main_str5:
	.string	"s, "
.main_str6:
	.string	", "
.main_str7:
	.string	"%d dime"
.main_str8:
	.string	"s, "
.main_str9:
	.string	", "
.main_str10:
	.string	"%d nickel"
.main_str11:
	.string	"s, "
.main_str12:
	.string	", "
.main_str13:
	.string	"%d penn"
.main_str14:
	.string	"s, "
.main_str15:
	.string	", "

.text

	.globl main
main:
	enter	$176, $0
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
	mov	$5, %r10
	mov	%r10, -32(%rbp)
	mov	$10, %r10
	mov	%r10, -24(%rbp)
	mov	$20, %r10
	mov	%r10, -16(%rbp)
	mov	$5, %r10
	mov	%r10, -8(%rbp)
	mov	$25, %r10
	mov	-32(%rbp), %r11
	imul	%r11, %r10
	mov	%r10, -56(%rbp)
	mov	$10, %r10
	mov	-24(%rbp), %r11
	imul	%r11, %r10
	mov	%r10, -64(%rbp)
	mov	-56(%rbp), %r10
	mov	-64(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -56(%rbp)
	mov	$5, %r10
	mov	-16(%rbp), %r11
	imul	%r11, %r10
	mov	%r10, -64(%rbp)
	mov	-56(%rbp), %r10
	mov	-64(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -56(%rbp)
	mov	-56(%rbp), %r10
	mov	-8(%rbp), %r11
	add	%r11, %r10
	mov	%r10, -56(%rbp)
	mov	-56(%rbp), %r10
	mov	%r10, -48(%rbp)
	mov	$.main_str0, %r10
	mov	%r10, %rdi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -56(%rbp)
	mov	$0, %rdx
	mov	-48(%rbp), %rax
	mov	$100, %r10
	idiv	%r10
	mov	%rax, -64(%rbp)
	mov	-64(%rbp), %r10
	mov	%r10, -40(%rbp)
	mov	$.main_str1, %r10
	mov	%r10, %rdi
	mov	-40(%rbp), %r10
	mov	%r10, %rsi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -64(%rbp)
.main_if1_test:
	mov	-40(%rbp), %r10
	mov	$1, %r11
	cmp	%r11, %r10
	mov	$0, %r10
	mov	$1, %r11
	cmove	%r11, %r10
	mov	%r10, -72(%rbp)
	mov	-72(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	jne	.main_if1_true
.main_for1_else:
	mov	$.main_str2, %r10
	mov	%r10, %rdi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -72(%rbp)
	jmp	.main_if1_end
.main_if1_true:
	mov	$.main_str3, %r10
	mov	%r10, %rdi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -80(%rbp)
.main_if1_end:
	mov	$0, %rdx
	mov	-48(%rbp), %rax
	mov	$100, %r10
	idiv	%r10
	mov	%rdx, -88(%rbp)
	mov	-88(%rbp), %r10
	mov	%r10, -48(%rbp)
	mov	$0, %rdx
	mov	-48(%rbp), %rax
	mov	$25, %r10
	idiv	%r10
	mov	%rax, -88(%rbp)
	mov	-88(%rbp), %r10
	mov	%r10, -40(%rbp)
	mov	$.main_str4, %r10
	mov	%r10, %rdi
	mov	-40(%rbp), %r10
	mov	%r10, %rsi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -88(%rbp)
.main_if2_test:
	mov	-40(%rbp), %r10
	mov	$1, %r11
	cmp	%r11, %r10
	mov	$0, %r10
	mov	$1, %r11
	cmove	%r11, %r10
	mov	%r10, -96(%rbp)
	mov	-96(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	jne	.main_if2_true
.main_for2_else:
	mov	$.main_str5, %r10
	mov	%r10, %rdi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -96(%rbp)
	jmp	.main_if2_end
.main_if2_true:
	mov	$.main_str6, %r10
	mov	%r10, %rdi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -104(%rbp)
.main_if2_end:
	mov	$0, %rdx
	mov	-48(%rbp), %rax
	mov	$25, %r10
	idiv	%r10
	mov	%rdx, -112(%rbp)
	mov	-112(%rbp), %r10
	mov	%r10, -48(%rbp)
	mov	$0, %rdx
	mov	-48(%rbp), %rax
	mov	$10, %r10
	idiv	%r10
	mov	%rax, -112(%rbp)
	mov	-112(%rbp), %r10
	mov	%r10, -40(%rbp)
	mov	$.main_str7, %r10
	mov	%r10, %rdi
	mov	-40(%rbp), %r10
	mov	%r10, %rsi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -112(%rbp)
.main_if3_test:
	mov	-40(%rbp), %r10
	mov	$1, %r11
	cmp	%r11, %r10
	mov	$0, %r10
	mov	$1, %r11
	cmove	%r11, %r10
	mov	%r10, -120(%rbp)
	mov	-120(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	jne	.main_if3_true
.main_for3_else:
	mov	$.main_str8, %r10
	mov	%r10, %rdi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -120(%rbp)
	jmp	.main_if3_end
.main_if3_true:
	mov	$.main_str9, %r10
	mov	%r10, %rdi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -128(%rbp)
.main_if3_end:
	mov	$0, %rdx
	mov	-48(%rbp), %rax
	mov	$10, %r10
	idiv	%r10
	mov	%rdx, -136(%rbp)
	mov	-136(%rbp), %r10
	mov	%r10, -48(%rbp)
	mov	$0, %rdx
	mov	-48(%rbp), %rax
	mov	$5, %r10
	idiv	%r10
	mov	%rax, -136(%rbp)
	mov	-136(%rbp), %r10
	mov	%r10, -40(%rbp)
	mov	$.main_str10, %r10
	mov	%r10, %rdi
	mov	-40(%rbp), %r10
	mov	%r10, %rsi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -136(%rbp)
.main_if4_test:
	mov	-40(%rbp), %r10
	mov	$1, %r11
	cmp	%r11, %r10
	mov	$0, %r10
	mov	$1, %r11
	cmove	%r11, %r10
	mov	%r10, -144(%rbp)
	mov	-144(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	jne	.main_if4_true
.main_for4_else:
	mov	$.main_str11, %r10
	mov	%r10, %rdi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -144(%rbp)
	jmp	.main_if4_end
.main_if4_true:
	mov	$.main_str12, %r10
	mov	%r10, %rdi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -152(%rbp)
.main_if4_end:
	mov	$0, %rdx
	mov	-48(%rbp), %rax
	mov	$5, %r10
	idiv	%r10
	mov	%rdx, -160(%rbp)
	mov	-160(%rbp), %r10
	mov	%r10, -48(%rbp)
	mov	$.main_str13, %r10
	mov	%r10, %rdi
	mov	-48(%rbp), %r10
	mov	%r10, %rsi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -160(%rbp)
.main_if5_test:
	mov	-40(%rbp), %r10
	mov	$1, %r11
	cmp	%r11, %r10
	mov	$0, %r10
	mov	$1, %r11
	cmove	%r11, %r10
	mov	%r10, -168(%rbp)
	mov	-168(%rbp), %r10
	mov	$0, %r11
	cmp	%r11, %r10
	jne	.main_if5_true
.main_for5_else:
	mov	$.main_str14, %r10
	mov	%r10, %rdi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -168(%rbp)
	jmp	.main_if5_end
.main_if5_true:
	mov	$.main_str15, %r10
	mov	%r10, %rdi
	mov	$0, %r10
	mov	%r10, %rax
	call	printf
	mov	%rax, %r10
	mov	%r10, -176(%rbp)
.main_if5_end:
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
