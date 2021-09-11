import kotlin.random.Random
import kotlin.random.nextInt
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

    /**
     * Array of length len with numbers in [lowerBound, upperBound - 1]
     */
    fun genArray(len: Int, lowerBound: Int, upperBound: Int) : Array<Int> = Array(len) { Random.nextInt(lowerBound, upperBound) }

    fun genString(len: Int, alphabet: Int) = genArray(len, 'a'.code, 'a'.code + alphabet).toString()

    fun genLongArray(len: Int, max: Int) : Array<Long> = genArray(len, 0, max).map{ it.toLong() }.toTypedArray()

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

    /**
     * Hash test based on the birthday paradox
     */
    @Test
    fun randomHashTests() {
        val setOfHashes = mutableSetOf<Long>()
        val iterations = 10000
        repeat(iterations) {
            val len = Random.nextInt(100..300)
            val alphabet = Random.nextInt(2..26)
            setOfHashes.add(longHash(genString(len, alphabet)))
        }
        assertEquals(iterations, setOfHashes.size)
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
        runLcsTest(emptyArray(), emptyArray(), 0)
        runLcsTest(arrayOf(1), arrayOf(1), 1)
        runLcsTest(arrayOf(1), arrayOf(2), 0)
        runLcsTest(arrayOf(1, 2, 3), arrayOf(1, 2, 3), 3)
        runLcsTest(arrayOf(1, 2, 3), arrayOf(3, 2, 1), 1)
        runLcsTest(arrayOf(1, 1, 1, 2, 2, 2), arrayOf(2, 2, 2, 1, 1, 1), 3)
    }



    @Test
    fun randomLcsTests() {
        repeat(10000) {
            val max = 1 shl Random.nextInt(20)
            runLcsTest(genLongArray(Random.nextInt(1..15), max), genLongArray(Random.nextInt(1..15), max), null)
        }
    }

    fun testDiffOutput(arrA: Array<Long>, arrB: Array<Long>, output: List<LinePosition>) {
        var ptrA = 0
        var ptrB = 0
        assertEquals(arrA.size + arrB.size - lcs(arrA, arrB), output.size)
        for ((posA, posB) in output) {
            if (posA > 0 && posB > 0) {
                assert(ptrA < arrA.size) { "ptrA < arrA.size" }
                assert(ptrB < arrB.size) { "ptrB < arrB.size" }
                assertEquals(arrA[posA - 1], arrB[posB - 1])
                ++ptrA
                ++ptrB
            } else if (posA > 0) {
                assert(ptrA < arrA.size) { "ptrA < arrA.size" }
                assertEquals(ptrA, posA - 1)
                ++ptrA
            } else {
                assertNotEquals(0, posB)
                assert(ptrB < arrB.size) { "ptrB < arrB.size" }
                assertEquals(ptrB, posB - 1)
                ++ptrB
            }
        }
    }

    @Test
    fun randomDiffTests() {
        repeat(10000) {
            val max = 1 shl Random.nextInt(20)
            val arrA = genLongArray(Random.nextInt(1..100), max)
            val arrB = genLongArray(Random.nextInt(1..100), max)
            val output = diff(arrA, arrB)
            testDiffOutput(arrA, arrB, output)
        }
    }
}


