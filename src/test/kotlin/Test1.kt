import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.math.min
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
            assertEquals(lcsBaseline(a, b), lcs(a, b), "stress, runLcsTest(${a.contentToString()}, ${b.contentToString()})")
        } else {
            assertEquals(knownAnswer, lcs(a, b), "main, runLcsTest(${a.contentToString()}, ${b.contentToString()})")
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
        assertEquals(arrA.size + arrB.size - lcs(arrA, arrB), output.size)
        numOfEdits?.let{ assert(it >= output.size) { "better edit script is known: $it vs ${output.size}"} }
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

    @Test
    fun trickyDiffTests() {
        repeat(10000) {
            val max = 1 shl Random.nextInt(20)
            val arrA = genLongArray(Random.nextInt(1..100), max)
            val buff = arrA.toMutableList()
            val edits = Random.nextInt(0..min(10, arrA.size))
            repeat(edits) {
                if (Random.nextInt(2) == 0) {
                    buff.removeAt(Random.nextInt(buff.size))
                } else {
                    buff.add(Random.nextInt(buff.size + 1), Random.nextInt(max).toLong())
                }
            }
            val arrB = buff.toLongArray()
            val output = diff(arrA, arrB)
            testDiffOutput(arrA, arrB, output)
        }
    }

    fun runFullTest(textA: String, textB: String, cmdArgs: List<String>, correctOutput: String) {
        setUp()
        File("A.txt").writeText(textA)
        File("B.txt").writeText(textB)
        main((listOf("A.txt", "B.txt") + cmdArgs).toTypedArray())
        assertEquals(correctOutput.trim(), stream.toString().trim())
        tearDown()
    }

    fun clockFullTest(lines: Int, avgLineLen: Int, changes: Int) {
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
        val milliseconds = 1e-6 * measureNanoTime { main(arrayOf("A.txt", "B.txt")) }
        tearDown()
        println("N = $lines, L = $avgLineLen, D = $changes: $milliseconds ms")
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
        runFullTest("import kotlin.test.Test\n" +
                "import kotlin.test.assertEquals\n" +
                "\n" +
                "class Task3Test {\n" +
                "    @Test\n" +
                "    fun fTest() {\n" +
                "        assertEquals(1.0, f(0.0), 1e-5)\n" +
                "        TODO(\"Необходимо добавить больше тестов\")\n" +
                "    }\n" +
                "\n" +
                "    @Test\n" +
                "    fun numberOfDaysTest() {\n" +
                "        assertEquals(365, numberOfDays(2021))\n" +
                "        TODO(\"Необходимо добавить больше тестов\")\n" +
                "    }\n" +
                "\n" +
                "    @Test\n" +
                "    fun rotate2Test() {\n" +
                "        assertEquals('С', rotate2('С', 1, -1))\n" +
                "        TODO(\"Необходимо добавить больше тестов\")\n" +
                "    }\n" +
                "\n" +
                "    @Test\n" +
                "    fun ageDescriptionTest() {\n" +
                "        assertEquals(\"сорок два года\", ageDescription(42))\n" +
                "        TODO(\"Необходимо добавить больше тестов\")\n" +
                "    }\n" +
                "\n" +
                "}",
            "import kotlin.test.Test\n" +
                "import kotlin.test.assertEquals\n" +
                "\n" +
                "class Task3Test {\n" +
                "    @Test\n" +
                "    fun fTest() {\n" +
                "        assertEquals(1.0, f(0.0), 1e-5)\n" +
                "        assertEquals(0.0, f(-0.1), 1e-5)\n" +
                "        assertEquals(1.0, f(0.1), 1e-5)\n" +
                "        assertEquals(1.0, f(0.9), 1e-5)\n" +
                "        assertEquals(-1.0, f(1.0), 1e-5)\n" +
                "        assertEquals(-1.0, f(1.9), 1e-5)\n" +
                "        assertEquals(1.0, f(2.0), 1e-5)\n" +
                "        assertEquals(1.0, f(4.0), 1e-5)\n" +
                "        assertEquals(-1.0, f(5.0), 1e-5)\n" +
                "        assertEquals(-1.0, f(7.5), 1e-5)\n" +
                "        assertEquals(1.0, f(10000.1), 1e-5)\n" +
                "    }\n" +
                "\n" +
                "    @Test\n" +
                "    fun numberOfDaysTest() {\n" +
                "        assertEquals(365, numberOfDays(2021))\n" +
                "        assertEquals(365, numberOfDays(2022))\n" +
                "        assertEquals(366, numberOfDays(2024))\n" +
                "        assertEquals(366, numberOfDays(2000))\n" +
                "        assertEquals(365, numberOfDays(2100))\n" +
                "    }\n" +
                "\n" +
                "    @Test\n" +
                "    fun rotate2Test() {\n" +
                "        assertEquals('С', rotate2('С', 1, -1))\n" +
                "        assertEquals('Ю', rotate2('С', -1, -1))\n" +
                "        assertEquals('Ю', rotate2('С', 1, 1))\n" +
                "        assertEquals('В', rotate2('С', 1, 2))\n" +
                "        assertEquals('З', rotate2('С', -1, 2))\n" +
                "        assertEquals('С', rotate2('С', 2, 2))\n" +
                "        assertEquals('Ю', rotate2('В', 2, 1))\n" +
                "        assertEquals('З', rotate2('Ю', 2, 1))\n" +
                "        assertEquals('С', rotate2('З', 2, 1))\n" +
                "    }\n" +
                "\n" +
                "    @Test\n" +
                "    fun ageDescriptionTest() {\n" +
                "        assertEquals(\"шестьдесят девять лет\", ageDescription(69))\n" +
                "        assertEquals(\"пятьдесят восемь лет\", ageDescription(58))\n" +
                "        assertEquals(\"сорок семь лет\", ageDescription(47))\n" +
                "        assertEquals(\"тридцать шесть лет\", ageDescription(36))\n" +
                "        assertEquals(\"двадцать пять лет\", ageDescription(25))\n" +
                "        assertEquals(\"шестьдесят четыре года\", ageDescription(64))\n" +
                "        assertEquals(\"пятьдесят три года\", ageDescription(53))\n" +
                "        assertEquals(\"сорок два года\", ageDescription(42))\n" +
                "        assertEquals(\"тридцать один год\", ageDescription(31))\n" +
                "        assertEquals(\"двадцать лет\", ageDescription(20))\n" +
                "    }\n" +
                "\n" +
                "}",
                listOf(),
        "=import kotlin.test.Test\n" +
                "=import kotlin.test.assertEquals\n" +
                "=\n" +
                "=class Task3Test {\n" +
                "=    @Test\n" +
                "=    fun fTest() {\n" +
                "=        assertEquals(1.0, f(0.0), 1e-5)\n" +
                ">        assertEquals(0.0, f(-0.1), 1e-5)\n" +
                ">        assertEquals(1.0, f(0.1), 1e-5)\n" +
                ">        assertEquals(1.0, f(0.9), 1e-5)\n" +
                ">        assertEquals(-1.0, f(1.0), 1e-5)\n" +
                ">        assertEquals(-1.0, f(1.9), 1e-5)\n" +
                ">        assertEquals(1.0, f(2.0), 1e-5)\n" +
                ">        assertEquals(1.0, f(4.0), 1e-5)\n" +
                ">        assertEquals(-1.0, f(5.0), 1e-5)\n" +
                ">        assertEquals(-1.0, f(7.5), 1e-5)\n" +
                ">        assertEquals(1.0, f(10000.1), 1e-5)\n" +
                "<        TODO(\"Необходимо добавить больше тестов\")\n" +
                "=    }\n" +
                "=\n" +
                "=    @Test\n" +
                "=    fun numberOfDaysTest() {\n" +
                "=        assertEquals(365, numberOfDays(2021))\n" +
                ">        assertEquals(365, numberOfDays(2022))\n" +
                ">        assertEquals(366, numberOfDays(2024))\n" +
                ">        assertEquals(366, numberOfDays(2000))\n" +
                ">        assertEquals(365, numberOfDays(2100))\n" +
                "<        TODO(\"Необходимо добавить больше тестов\")\n" +
                "=    }\n" +
                "=\n" +
                "=    @Test\n" +
                "=    fun rotate2Test() {\n" +
                "=        assertEquals('С', rotate2('С', 1, -1))\n" +
                ">        assertEquals('Ю', rotate2('С', -1, -1))\n" +
                ">        assertEquals('Ю', rotate2('С', 1, 1))\n" +
                ">        assertEquals('В', rotate2('С', 1, 2))\n" +
                ">        assertEquals('З', rotate2('С', -1, 2))\n" +
                ">        assertEquals('С', rotate2('С', 2, 2))\n" +
                ">        assertEquals('Ю', rotate2('В', 2, 1))\n" +
                ">        assertEquals('З', rotate2('Ю', 2, 1))\n" +
                ">        assertEquals('С', rotate2('З', 2, 1))\n" +
                "<        TODO(\"Необходимо добавить больше тестов\")\n" +
                "=    }\n" +
                "=\n" +
                "=    @Test\n" +
                "=    fun ageDescriptionTest() {\n" +
                ">        assertEquals(\"шестьдесят девять лет\", ageDescription(69))\n" +
                ">        assertEquals(\"пятьдесят восемь лет\", ageDescription(58))\n" +
                ">        assertEquals(\"сорок семь лет\", ageDescription(47))\n" +
                ">        assertEquals(\"тридцать шесть лет\", ageDescription(36))\n" +
                ">        assertEquals(\"двадцать пять лет\", ageDescription(25))\n" +
                ">        assertEquals(\"шестьдесят четыре года\", ageDescription(64))\n" +
                ">        assertEquals(\"пятьдесят три года\", ageDescription(53))\n" +
                "=        assertEquals(\"сорок два года\", ageDescription(42))\n" +
                ">        assertEquals(\"тридцать один год\", ageDescription(31))\n" +
                ">        assertEquals(\"двадцать лет\", ageDescription(20))\n" +
                "<        TODO(\"Необходимо добавить больше тестов\")\n" +
                "=    }\n" +
                "=\n" +
                "=}")
    }
    @Test
    fun bigFullTests() {
        clockFullTest(10, 30, 10)
        clockFullTest(100, 30, 100)
        clockFullTest(1000, 30, 1000)
        clockFullTest(3000, 30, 3000)
        clockFullTest(3000, 30, 30)
        clockFullTest(3000, 30, 300)
        clockFullTest(3000, 30, 3)
        clockFullTest(10000, 30, 100)
        clockFullTest(16000, 30, 1000)
        clockFullTest(16000, 300, 16000)
    }
}


