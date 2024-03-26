package vxcc.asm

import vxcc.arch.etca.ETCATarget
import vxcc.asm.etca.ETCAAssembler

fun main() {
    val asm = ETCAAssembler(0, ETCATarget().also {
        it.int = true
        it.stack = true
        it.bm1 = true
    })
    val code = """
test_jmp:
    jmp  test_zero_on
.fail:
    jmp  .fail

test_zero_on:
    mov  r0, 0
    test r0, -1

    jmp.nz  .fail
    jmp.z   test_zero_off
.fail:
    jmp .fail ; program counter will indicate failure

test_zero_off:
    mov  r0, 1
    test r0, -1

    jmp.z   .fail
    jmp.nz  test_negative_on
.fail:
    jmp  .fail

test_negative_on:
    mov  r0, -1
    test r0, -1

    jmp.p  .fail
    jmp.n   test_negative_off
.fail:
    jmp  .fail

test_negative_off:
    mov  r0, 0
    test r0, -1

    jmp.n   .fail
    jmp.p  test_carry_on
.fail:
    jmp  .fail

test_carry_on:
    mov  r0, -1
    add  r0, 15

    jmp.nc  .fail
    jmp.c   test_carry_off
.fail:
    jmp  .fail

test_carry_off:
    mov  r0, 15
    add  r0, 15
    jmp.c   .fail
    jmp.nc  test_borrow_on
.fail:
    jmp  .fail

test_borrow_on:
    mov  r0, 0
    cmp  r0, 1
    jmp.nc  .fail
    jmp.c   test_borrow_off
.fail:
    jmp  .fail

test_borrow_off:
    mov  r0, 1
    cmp  r0, 1
    jmp.c   .fail
    jmp.nc  test_overflow_on
.fail:
    jmp  .fail

test_overflow_on:
    mov  r0, h7fff
    add  r0, 1
    jmp.nv  .fail
    jmp.v   test_overflow_off
.fail:
    jmp  .fail

test_overflow_off:
    mov  r0, h7fff
    sub  r0, 1
    jmp.v   .fail
    jmp.nv  test_equal
.fail:
    jmp  .fail

test_equal:
    cmp  r0, r0
    jmp.ne  .fail
    jmp.b   .fail
    jmp.a   .fail
    jmp.l   .fail
    jmp.g   .fail

    jmp.e   .s1
    jmp  .fail
.s1:
    jmp.be  .s2
    jmp  .fail
.s2:
    jmp.ae  .s3
    jmp  .fail
.s3:
    jmp.le  .s4
    jmp  .fail
.s4:
    jmp.ge  test_not_equal
.fail:
    jmp  .fail

test_not_equal:
    mov  r0, 0
    cmp  r0, 1

    jmp.e   .fail
    jmp.ne  .s1
    jmp  .fail
.s1:
    jmp.b   .s2
    jmp.a   .s2
    jmp  .fail
.s2:
    jmp.l   test_ucomp_1
    jmp.g   test_ucomp_1
.fail:
    jmp  .fail

test_ucomp_1:
    mov  r0, 10
    cmp  r0, 5
    jmp.b   .fail
    jmp.be  .fail
    jmp.a   .s1
    jmp  .fail
.s1:
    jmp.ae  test_ucomp_2
.fail:
    jmp  .fail

test_ucomp_2:
    mov  r0, 5
    cmp  r0, 10
    jmp.a   .fail
    jmp.ae  .fail
    jmp.b   .s
    jmp  .fail
.s:
    jmp.be  test_ucomp_3
.fail:
    jmp  .fail

test_ucomp_3:
    mov  r0, -10
    cmp  r0, 5
    jmp.b   .fail
    jbe  .fail
    jmp.a   .s
    jmp  .fail
.s:
    jmp.ae  test_ucomp_4
.fail:
    jmp  .fail

test_ucomp_4:
    mov  r0, 5
    cmp  r0, -10
    jmp.a   .fail
    jmp.ae  .fail
    jmp.b   .s
    jmp  .fail
.s:
    jmp.be  test_scomp_1
.fail:
    jmp  .fail

test_scomp_1:
    mov  r0, 10
    cmp  r0, 5
    jmp.l   .fail
    jmp.le  .fail
    jmp.g   .s
    jmp  .fail
.s:
    jmp.ge  test_scomp_2
.fail:
    jmp  .fail

test_scomp_2:
    mov  r0, 5
    cmp  r0, 10
    jmp.g   .fail
    jmp.ge  .fail
    jmp.l   .s
    jmp  .fail
.s:
    jmp.le  test_scomp_3
.fail:
    jmp  .fail

test_scomp_3:
    mov  r0, 5
    cmp  r0, -10
    jmp.l   .fail
    jmp.le  .fail
    jmp.g   .s
    jmp  .fail
.s:
    jmp.ge  test_scomp_4
.fail:
    jmp  .fail

test_scomp_4:
    mov  r0, -10
    cmp  r0, 5
    jg   .fail
    jge  .fail
    jl   .s
    jmp  .fail
.s:
    jmp.le  done
.fail:
    jmp  .fail

done:
    mov  r7,1
.hlt:
    jmp  .hlt
    """
    assemble(code, asm)
    println(asm.finish().joinToString { it.toUByte().toString(2).padStart(8, '0') })
}