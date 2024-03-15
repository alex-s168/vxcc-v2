import vxcc.cg.ua16.UA16Env
import vxcc.cg.x86.Target
import vxcc.cg.x86.X86Env
import vxcc.ir.ir

fun main() {
    /*
    val target = Target().apply {
        mmx = true
    }

    val env = X86Env(target)
    env.regAlloc = false
    env.stackAlloc = false
    // env.optMode = Env.OptMode.SIZE

    val code = """
        type int = :int w:32
        type sint = :int w:16
        type ptr = :int w:32
        type u8 = :int w:8
        
        data putc_char 0
        
        fn putc
            %charIn'al ? u8
        
            %char @mem u8 ::putc_char
            %char = %charIn
            ~ %char
            ~ %charIn
            %'eax = int 4
            %'ebx = int 1
            %'ecx = ptr [addr, ::putc_char]
            %'edx = int 1
            ! int 0x80
        end
        
        export fn _start
            %counter = sint 0
        :loop
            %'al = int 65
            [call, ::putc]
            %counter = int [add, %counter, sint 1]
            [brl, :loop, %counter, sint 100]
            
            ~ %counter
            %'eax = int 1
            %'ebx = int 0
            ! int 0x80
        end
    """

     */

    val env = UA16Env(0)

    val code = """
        type int = :int w:16
        
        export fn _start
            %counter = int 0
        :loop
            %counter = int [add, %counter, int 1]
            [brl, :loop, %counter, int 100]
            
            ~ %counter
        end
    """

   val exception = runCatching {
        ir(code.lines().iterator(), env, verbose = true)

        env.finish()
    }.exceptionOrNull()

    println(env.source)

    exception?.let { throw it }
}
