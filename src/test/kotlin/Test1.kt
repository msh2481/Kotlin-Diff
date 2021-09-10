import kotlin.test.*

internal class Test1 {
    @Test
    fun bitTests() {
        assertEquals(true, testBit(11, 0), "1011_2 at 0")
        assertEquals(true, testBit(11, 1), "1011_2 at 1")
        assertEquals(false, testBit(11, 2), "1011_2 at 2")
        assertEquals(true, testBit(11, 3), "1011_2 at 3")
        assertEquals(false, testBit(11, 4), "1011_2 at 4")
        assertEquals(false, testBit(11, 31), "1011_2 at 31")
    }

    fun runHashTest(a: String, b: String)  {
        val expectedResult = (a == b)
        assert(
            (longHash(a) == longHash(b)) == expectedResult
        ) { "runHashTest($a, $b)" }
    }

    @Test
    fun smallHashTests() {
        runHashTest("a\nb", "a\nb")
        runHashTest("Abc\nГде\n\n\n789\n#!?", "Abc\nГде\n\n\n789\n#!?")
        runHashTest("", "\n")
        runHashTest("\n", "\n\n")
        runHashTest("a\n", "a\na\n")
        runHashTest("a\n", "a\n\n")
        runHashTest("b\n", "b\n")
        runHashTest("a\n", "aa\n")
        runHashTest("aa\n", "aaa\n")
    }

    fun runLcsTest(a: Array<Long>, b: Array<Long>, knownAnswer: Int?) {
        if (knownAnswer == null) {
            assertEquals(lcsBaseline(a, b), lcs(a, b), "stress, runLcsTest(${a.contentToString()}, ${b.contentToString()})")
        } else {
            assertEquals(knownAnswer, lcs(a, b), "main, runLcsTest(${a.contentToString()}, ${b.contentToString()})")
            assertEquals(knownAnswer, lcsBaseline(a, b), "baseline, runLcsTest(${a.contentToString()}, ${b.contentToString()})")
        }
    }
    @Test
    fun smallLcsTests() {
        runLcsTest(emptyArray<Long>(), emptyArray<Long>(), 0)
        runLcsTest(arrayOf(1), arrayOf(1), 1)
        runLcsTest(arrayOf(1), arrayOf(2), 0)
        runLcsTest(arrayOf(1, 2, 3), arrayOf(1, 2, 3), 3)
        runLcsTest(arrayOf(1, 2, 3), arrayOf(3, 2, 1), 1)
        runLcsTest(arrayOf(1, 1, 1, 2, 2, 2), arrayOf(2, 2, 2, 1, 1, 1), 3)
    }
    @Test
    fun diffTests() {
        assertEquals(listOf(LinePosition(1, 0), LinePosition(2, 1), LinePosition(0, 2), LinePosition(3, 0)), diff(arrayOf(1, 3, 2), arrayOf(3, 4)))
    }
}


