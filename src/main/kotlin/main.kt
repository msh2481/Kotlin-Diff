import java.io.BufferedReader
import java.io.File
import java.lang.Integer.max
import kotlin.system.exitProcess

/**
 * Testing i-th bit in a
 *
 * Note that i must be in [0, 32)
 *
 * Usage:
 * assertEquals(true, testBit(11, 1))
 * assertEquals(false, testBit(11, 2))
 *
 * @param[a] mask
 * @param[i] bit index
 * @return true if i-th bit is set
 */
fun testBit(a : Int, i : Int) : Boolean {
    return ((a shr i) and 1) != 0
}

/**
 * Slow algorithm for finding LCS length for two arrays
 *
 * Works in O(2 ^ min(|a|, |b|) * max(|a|, |b|)), where a, b are the given arrays
 * 1. Select a subsequence of the first array
 * 2. Try to greedily match it with elements of b
 *
 * Usage:
 * assertEquals(3, lcsBaseline(arrayOf(4, 1, 5, 6, 2, 7, 3), arrayOf(1, 9, 10, 11, 2, 3, 8))
 *
 * @param[a] first array
 * @param[b] second array
 * @return length of LCS(a, b)
 */
fun lcsBaseline(a: Array<Long>, b: Array<Long>) : Int {
    var small = a
    var big = b
    if (small.size > big.size) {
        small = big.also { big = small }
    }
    assert(a.size <= 20)
    val n = a.size
    var bestAns = 0
    for (mask in 0 until (1 shl n)) {
        var ptr = 0
        var ans = 0
        for (i in small.indices) {
            if (!testBit(mask, i)) {
                continue
            }
            while (ptr < big.size && big[ptr] != small[i]) {
                ++ptr
            }
            if (ptr < big.size && big[ptr] == small[i]) {
                ++ans
                ++ptr
            } else {
                break
            }
        }
        bestAns = max(bestAns, ans)
    }
    return bestAns
}

/**
 * Dynamic programming for finding LCS length for two arrays
 *
 * Works in O(|a| * |b|), where a, b are the given arrays
 * (Algorithm on Wikipedia)[https://en.wikipedia.org/wiki/Longest_common_subsequence_problem#Solution_for_two_sequences]
 *
 * Usage:
 * println(lcsDP(arrayOf(4, 1, 5, 6, 2, 7, 3), arrayOf(1, 9, 10, 11, 2, 3, 8)).contentDeepToString())
 *
 * @param[a] first array
 * @param[b] second array
 * @return dp table
 */
fun lcsDP(a: Array<Long>, b: Array<Long>) : Array<Array<Int>> {
    val n = a.size
    val m = b.size
    val dp : Array<Array<Int>> = Array (n + 1) { Array(m + 1) {0} }
    for (i in 1..n) {
        for (j in 1..m) {
            if (a[i - 1] == b[j - 1]) {
                dp[i][j] = dp[i - 1][j - 1] + 1
            } else {
                dp[i][j] = max(dp[i - 1][j], dp[i][j - 1])
            }
        }
    }
    return dp
}

/**
 * LCS length for two arrays
 *
 * Runs lcsDP and takes last value from the table
 *
 * Usage:
 * assertEquals(3, lcs(arrayOf(4, 1, 5, 6, 2, 7, 3), arrayOf(1, 9, 10, 11, 2, 3, 8))
 *
 * @param[a] first array
 * @param[b] second array
 * @return
 */
fun lcs(a: Array<Long>, b: Array<Long>) : Int {
    return lcsDP(a, b)[a.size][b.size]
}

/**
 * Optimaly matches elements from two arrays
 *
 * Firstly, it runs lcsDP and recovers path to optimal answer
 * Then in creates diff = {(posA, posB) for each line in A U B},
 * where posA = index of the line in A or -1, posB = same for B
 *
 * Usage:
 * assertEquals(arrayOf(Pair(1, -1), Pair(2, 2), Pair(1, 3), Pair(2, 2)), diff(arrayOf(1, 3, 2), arrayOf(3, 4))
 *
 * @param[a] first array
 * @param[b] second array
 * @return diff as described above
 */

fun diff(a: Array<Long>, b: Array<Long>) : List<Pair<Int, Int>> {
    val diff = mutableListOf<Pair<Int, Int>>()

    fun addFromA(i: Int, j: Int) = diff.add(Pair(i, 0))
    fun addFromB(i: Int, j: Int) = diff.add(Pair(0, j))
    fun addFromBoth(i: Int, j: Int) = diff.add(Pair(i, j))
    val dp = lcsDP(a, b)
    var i = a.size
    var j = b.size
    while (i > 0 && j > 0) {
        if (dp[i][j] == dp[i - 1][j]) {
            addFromA(i, j)
            --i
        } else if (dp[i][j] == dp[i][j - 1]) {
            addFromB(i, j)
            --j
        } else {
            assert(dp[i - 1][j - 1] + 1 == dp[i][j]) {"Incorrect dp"}
            addFromBoth(i, j)
            --i
            --j
        }
    }
    while (i > 0) {
        addFromA(i, j)
        --i
    }
    while (j > 0) {
        addFromB(i, j)
        --j
    }
    return diff.reversed()
}

/**
 * Polynomial 64-bit string hash
 *
 * Namely, it equals (sum (s_i * BASE ^ i)) mod MOD
 *
 * Usage:
 * assertNotEquals(longHash("abacaba"), longHash("abracadabra"))
 * assertEquals(longHash("abacaba"), longHash("abacaba"))
 *
 * @param[s] given string
 * @return hash of s
 */
fun longHash(s: String) : Long {
    val BASE : Long = 59
    val MOD : Long = 1e17.toLong() + 3
    var sum : Long = 0
    for (c in s) {
        sum = (BASE * sum + c.code.toLong()) % MOD
    }
    return sum
}

/**
 * Splits the whole text into a smaller parts by entries of specified symbols
 *
 * They can be lines (\n), words ( ), sentences (.)
 *
 * Usage:
 * assert(arrayOf("a", "b") contentEquals arrayOf("a\nb"))
 */
fun tokenizeLines(s: String, delimiters: String) : Array<String> {
    val parts = mutableListOf<String>()
    var buffer = ""
    for (c in s) {
        if (c in delimiters) {
            if (buffer.length > 0) {
                parts.add(buffer)
                buffer = ""
            }
        } else {
            buffer += c
        }
    }
    if (buffer.length > 0) {
        parts.add(buffer)
    }
    return parts.toTypedArray()
}

/**
 * Replace lines of the text with their hash values
 *
 * Usage:
 * assert(toHashArray(arrayOf("a", b")) contentEquals arrayOf(97, 98))
 *
 * @param[lines] given string
 */
fun toHashArray(lines: Array<String>) : Array<Long> {
    val hashList = mutableListOf<Long>()
    for (s in lines) {
        hashList.add(longHash(s))
    }
    return hashList.toTypedArray()
}

fun main(args: Array<String>) {
    if (args.size != 2) {
        println("There should be exactly two input files")
        exitProcess(0)
    }
    val colorOutput = true
    val inputDelim = "\n "
    val outputDelim = " "

    val TEXT_RESET = "\u001B[0m"
    val TEXT_BLACK = "\u001B[30m"
    val TEXT_RED = "\u001B[31m"
    val TEXT_GREEN = "\u001B[32m"
    val TEXT_YELLOW = "\u001B[33m"
    val TEXT_BLUE = "\u001B[34m"
    val TEXT_PURPLE = "\u001B[35m"
    val TEXT_CYAN = "\u001B[36m"
    val TEXT_WHITE = "\u001B[37m"

    val textA = File(args[0]).bufferedReader().use(BufferedReader::readText)
    val textB = File(args[1]).bufferedReader().use(BufferedReader::readText)
    val tokensA = tokenizeLines(textA, inputDelim)
    val tokensB = tokenizeLines(textB, inputDelim)
    println("${tokensA.size}, ${tokensB.size}")
    val arrA = toHashArray(tokensA)
    val arrB = toHashArray(tokensB)
    val result = diff(arrA, arrB)
    for ((i, j) in result) {
        if (i > 0 && j > 0) {
            print(if (colorOutput) TEXT_WHITE else "=")
            print("${tokensA[i - 1]}$outputDelim")
        } else if (i > 0) {
            print(if (colorOutput) TEXT_RED else "<")
            print("$TEXT_RED${tokensA[i - 1]}$TEXT_RESET$outputDelim")
        } else if (j > 0) {
            print(if (colorOutput) TEXT_GREEN else ">")
            print("${tokensB[j - 1]}$TEXT_RESET$outputDelim")
        } else {
            assert(false) {"Every line should come from somewhere"}
        }
        if (colorOutput) {
            print(TEXT_RESET)
        }
    }
}
