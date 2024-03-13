import vxcc.cg.x86.Target
import vxcc.cg.x86.X86Env
import vxcc.ir.ir

fun main() {
    val target = Target().apply {
        amd64_v1 = true
        mmx = true
    }

    val env = X86Env(target)

    val code = """
        type int = :int w:32
        
        export fn main
            %0'edi = int 10
            %1 = int 20
            %0 = int (add %0 %1)
            ~ %1
            %1 = int 1
            %0 = int (add %0 %1)
            %0 <> eax
        end
    """

    ir(code.lines().iterator(), env)
}
