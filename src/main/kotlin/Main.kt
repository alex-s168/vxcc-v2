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
        :loop
            %0 = int [add, %0, int 1]
            [brl, :loop, %0, int 100]
            ~ %0
            
            %0 @mem int 50
            %0 = int 1
            ~ %0
            
            %syscall'eax = int 1
            %a0'ebx = int 0
            ! int 0x80
        end
    """

    ir(code.lines().iterator(), env, verbose = true)

    env.finish()
}
