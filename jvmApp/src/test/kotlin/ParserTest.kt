import moe.him188.assembly.interpreter.CallParser
import org.junit.Test
import kotlin.test.assertFails

internal class ParserTest {
    @Test
    fun test1() {
        CallParser().parse("STO 1", null)
    }

    @Test
    fun test2() {
        CallParser().parse("LABEL: STO 1", null)
    }

    @Test
    fun test3() {
        CallParser().parse("LABEL: STO #1", null)
    }

    @Test
    fun test4() {
        CallParser().parse("LABEL: STO #&FF", null)
    }

    @Test
    fun test5() {
        CallParser().parse("LABEL: STO #B111", null)
    }

    @Test
    fun test6() {
        assertFails {
            CallParser().parse("LABEL: STO #B1112", null)
        }
    }

    @Test
    fun test7() {
        assertFails {
            CallParser().parse("LABEL: STO #&G", null)
        }
    }
}