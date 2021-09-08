import java.lang.Integer.max

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
 * Main algorithm for finding LCS length for two arrays
 *
 * Works in O(|a| * |b|), where a, b are the given arrays
 * (Algorithm on Wikipedia)[https://en.wikipedia.org/wiki/Longest_common_subsequence_problem#Solution_for_two_sequences]
 *
 * Usage:
 * assertEquals(3, lcsBaseline(arrayOf(4, 1, 5, 6, 2, 7, 3), arrayOf(1, 9, 10, 11, 2, 3, 8))
 *
 * @param[a] first array
 * @param[b] second array
 * @return length of LCS(a, b)
 */
fun lcs(a: Array<Long>, b: Array<Long>) : Int {
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
    return dp[n][m]
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
 * Splits text into lines and replace them with hash values
 *
 * Let's maintain buffer for current line. When we reach '\n'
 * let's add hash of the line to list and clean buffer. Also
 * there is extra '\n' added to the end of the string, so that
 * we can not deal with last line manually.
 *
 * Usage:
 * assert(toHashArray("a\nb") contentEquals arrayOf(97, 98))
 *
 * @param[s] given string
 */
fun toHashArray(s: String) : Array<Long> {
    val sWithEnding = s + "\n"
    var currentLine = ""
    val hashList = MutableList<Long>(0) {0}
    for (c in sWithEnding) {
        if (c == '\n') {
            hashList.add(longHash(currentLine))
            currentLine = ""
        } else {
            currentLine += c
        }
    }
    return hashList.toTypedArray()
}

fun main(args: Array<String>) {
    assert(args.size == 2)
}
