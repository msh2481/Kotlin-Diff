import org.junit.jupiter.api.TestTemplate
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.system.measureNanoTime
import kotlin.test.*

internal class Test1 {
    private val standardOut = System.out
    private val standardIn = System.`in`
    private var stream = ByteArrayOutputStream()

    fun setUp() {
        stream = ByteArrayOutputStream()
        System.setOut(PrintStream(stream))
    }

    fun tearDown() {
        System.setOut(standardOut)
        System.setIn(standardIn)
    }

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

    fun genLongArray(len: Int, max: Int) : LongArray = genArray(len, 0, max).map{ it.toLong() }.toLongArray()

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

    fun runLcsTest(a: LongArray, b: LongArray, knownAnswer: Int?) {
        if (knownAnswer == null) {
            assertEquals(lcsBaseline(a, b), FastLCS.lcs(a, b), "stress, runLcsTest(${a.contentToString()}, ${b.contentToString()})")
        } else {
            assertEquals(knownAnswer, FastLCS.lcs(a, b), "main, runLcsTest(${a.contentToString()}, ${b.contentToString()})")
            assertEquals(knownAnswer, lcsBaseline(a, b), "baseline, runLcsTest(${a.contentToString()}, ${b.contentToString()})")
        }
    }

    @Test
    fun smallLcsTests() {
        runLcsTest(longArrayOf(), longArrayOf(), 0)
        runLcsTest(longArrayOf(1), longArrayOf(1), 1)
        runLcsTest(longArrayOf(1), longArrayOf(2), 0)
        runLcsTest(longArrayOf(1, 2, 3), longArrayOf(1, 2, 3), 3)
        runLcsTest(longArrayOf(1, 2, 3), longArrayOf(3, 2, 1), 1)
        runLcsTest(longArrayOf(1, 1, 1, 2, 2, 2), longArrayOf(2, 2, 2, 1, 1, 1), 3)
    }



    @Test
    fun randomLcsTests() {
        repeat(10000) {
            val max = 1 shl Random.nextInt(20)
            runLcsTest(genLongArray(Random.nextInt(1..15), max), genLongArray(Random.nextInt(1..15), max), null)
        }
    }

    fun testDiffOutput(arrA: LongArray, arrB: LongArray, output: List<LinePosition>, numOfEdits: Int? = null) {
        var ptrA = 0
        var ptrB = 0
        assertEquals(arrA.size + arrB.size - FastLCS.lcs(arrA, arrB), output.size)
        if (numOfEdits != null && output.size > min(arrA.size, arrB.size) + numOfEdits) {
            println("better edit script is known: ${output.size} vs ${min(arrA.size, arrB.size) + numOfEdits}")
            println(arrA.contentToString())
            println(arrB.contentToString())
            println(output)
            assert(false)
        }
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
            val output = FastLCS.diff(arrA, arrB)
            testDiffOutput(arrA, arrB, output)
        }
    }

    @Test
    fun trickyDiffTests() {
        val ratios = mutableListOf<Double>()
        repeat(1000) {
            val max = 1 shl Random.nextInt(5..10)
            val arrA = genLongArray(Random.nextInt(10..300), max)
            val buff = arrA.toMutableList()
            val edits = Random.nextInt(10..min(100, arrA.size))
            repeat(edits) {
                if (Random.nextInt(2) == 0) {
                    buff.removeAt(Random.nextInt(buff.size))
                } else {
                    buff.add(Random.nextInt(buff.size + 1), Random.nextInt(max).toLong())
                }
            }
            val arrB = buff.toLongArray()
            val output = FastLCS.diff(arrA, arrB)
            testDiffOutput(arrA, arrB, output, edits)
            val heuristic = HeuristicLCS.diff(arrA, arrB)
            testDiffOutput(arrA, arrB, output)
            assert(output.size <= heuristic.size)
            ratios.add(heuristic.size.toDouble() / output.size)
        }
        ratios.sort()
        println("Average ratio: ${ratios.average()}")
        println("Quantiles: ${ratios.first()} ${ratios[ratios.size / 4]} ${ratios[ratios.size / 2]} ${ratios[ratios.size * 3 / 4]} ${ratios.last()}")
    }

    fun runFullTest(textA: String, textB: String, cmdArgs: List<String>, correctOutput: String) {
        setUp()
        File("A.txt").writeText(textA)
        File("B.txt").writeText(textB)
        main((listOf("A.txt", "B.txt") + cmdArgs).toTypedArray())
        assertEquals(correctOutput.trim(), stream.toString().trim())
        tearDown()
    }

    fun runFullTestFromFile(fileA: String, fileB: String, cmdArgs: List<String>, ansFile: String) {
        val relAddress = "test/full/"
        setUp()
        main((listOf(relAddress + fileA, relAddress + fileB) + cmdArgs).toTypedArray())
        assertEquals(File(relAddress + ansFile).readText().trim(), stream.toString().trim())
        tearDown()
    }

    fun clockFullTest(lines: Int, avgLineLen: Int, changes: Int, fastMode: Boolean) {
        val textA = MutableList(lines) { genString(avgLineLen, 26) }
        val textB = textA
        repeat(changes) {
            val pos = Random.nextInt(0, lines)
            when (Random.nextBoolean()) {
                true -> textA[pos] = genString(avgLineLen, 26)
                false -> textB[pos] = genString(avgLineLen, 26)
            }
        }
        setUp()
        File("A.txt").writeText(textA.joinToString("\n"))
        File("B.txt").writeText(textB.joinToString("\n"))
        val args = if (fastMode) arrayOf("A.txt", "B.txt", "-f") else arrayOf("A.txt", "B.txt")
        val milliseconds = (1e-6 * measureNanoTime { main(args) }).roundToInt()
        tearDown()
        println("Fast = $fastMode,\tN = $lines, L = $avgLineLen,\tD = $changes:\t$milliseconds ms")
    }

    @Test
    fun cornerCaseFullTests() {
        runFullTest("a", "b", listOf(), ">b\n<a\n")
        runFullTest("a", "b", listOf("-c"), "${Color.Green}b\n${Color.Reset}${Color.Red}a\n${Color.Reset}")
        runFullTest("a", "b", listOf("-n", "-o=\" \""), ">b <a")
        runFullTest("a", "b", listOf("-o=\" \""), ">b\n <a\n")
        runFullTest("абв", "вба", listOf(), ">вба\n<абв\n")
        runFullTest("абв", "вба", listOf("-i=\"*\""), ">в>б=а<б<в")
        runFullTest("абв", "вба", listOf("-n", "-i=\"*\""), ">в>б=а<б<в")
        runFullTest("abc", "cba", listOf("-o=\";\t\""), ">cba\n;\t<abc\n;\t")
        runFullTest("abc", "cba", listOf("-n", "-o=\";\t\""), ">cba;\t<abc;\t")
        runFullTest("abc", "cba", listOf("-n", "-i=b"), ">c=a<c")
        runFullTest("abc", "cba", listOf("-i=b"), ">cb>a<ab<c")
        runFullTest("a\n\nb", "a\nb", listOf(), "=a\n<\n=b")
    }

    @Test
    fun fileFullTests() {
        runFullTestFromFile("msh2481A.kt", "msh2481B.kt", listOf(), "msh2481.ans")
        runFullTestFromFile("linuxA.c", "linuxB.c", listOf(), "linux.ans")
    }

    @Test
    fun maxFullTests() {
        clockFullTest(10000, 30, 100, false)
        clockFullTest(10000, 300, 100, false)
        clockFullTest(10000, 30, 10000, false)
        clockFullTest(10000, 300, 10000, false)
        clockFullTest(16000, 30, 1000, false)
        clockFullTest(16000, 300, 1000, false)
        clockFullTest(16000, 30, 16000, false)
        clockFullTest(16000, 300, 16000, false)

        clockFullTest(10000, 30, 10000, true)
        clockFullTest(16000, 30, 16000, true)
        clockFullTest(50000, 30, 50000, true)
        clockFullTest(100000, 30, 100000, true)
        clockFullTest(900000, 30, 900000, true)
    }

    @Test
    fun hasherTests() {
        val a = listOf<Long>(1, 2, 1, 3, 1, 2, 1)
        val h = PolynomialHasher(a.toLongArray())
        val p = mutableListOf<Pair<List<Long>, Long>>()
        for (i in a.indices) {
            for (j in i..a.size) {
                val cur : Pair<List<Long>, Long> = Pair(a.subList(i, j), h.rangeHash(i, j))
                p.add(cur)
            }
        }
        for (x in p) {
            for (y in p) {
                assertEquals(x.first == y.first, x.second == y.second, "${x.first} ${y.first} ${x.second} ${y.second}")
            }
        }
    }


}


