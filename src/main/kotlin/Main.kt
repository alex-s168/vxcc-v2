import vxcc.cg.x86.Target
import vxcc.cg.x86.X86Env
import vxcc.ir.ir

fun main() {
    val target = Target().apply {
        mmx = true
    }

    val env = X86Env(target)

    val code = """
        type int = :int w:32
        
        export fn _start
            %0 = int 0
            %1 = int 1
            %2 = int 100
        :loop
            %0 = int (add %0 %1)
            (brl :loop %0 %2)
            ~ %0
            ~ %1
            ~ %2
            
            %syscall'eax = int 1
            %a0'ebx = int 0
            ! int 0x80
        end
    """

    ir(code.lines().iterator(), env)
}
