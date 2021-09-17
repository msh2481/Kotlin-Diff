import java.io.BufferedReader
import java.io.File
import java.lang.Integer.max
import kotlin.system.exitProcess

/** TODO
 * ND algorithm
 * NlogN algorithm (find largest common substring, split by it, run recursion)
 * Block edit
 */

/**
 * Testing i-th bit in a
 *
 * Note that i must be in [0, 32)
 *
 * Usage:
 * assertEquals(true, testBit(11, 1))
 * assertEquals(false, testBit(11, 2))
 */
fun testBit(a : Int, i : Int) : Boolean {
    return ((a shr i) and 1) != 0
}

data class LinePosition(val posA : Int, val posB : Int)

/**
 * Slow algorithm for finding LCS length for two arrays
 *
 * Works in O(2 ^ min(|a|, |b|) * max(|a|, |b|)), where a, b are the given arrays
 * 1. Select a subsequence of the first array
 * 2. Try to greedily match it with elements of b
 *
 * Usage:
 * assertEquals(3, lcsBaseline(arrayOf(4, 1, 5, 6, 2, 7, 3), arrayOf(1, 9, 10, 11, 2, 3, 8))
 */
fun lcsBaseline(a: LongArray, b: LongArray) : Int {
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
 * Optimized version of Array<Array<Short>>
 *
 * Data lies in one continuous array
 *
 * Usage:
 * val a = ShortMatrix(n, m)
 * a[0, 0] = 1
 * println(a[0, 0])
 */
class ShortMatrix(val n: Int, val m: Int) {
    var arr = ShortArray(n * m)
    operator fun get(i: Int, j: Int) : Short = arr[i * m + j]
    operator fun set(i: Int, j: Int, b: Short) {
        arr[i * m + j] = b
    }
    operator fun set(i: Int, j: Int, b: Int) {
        arr[i * m + j] = b.toShort()
    }
}

/**
 * Dynamic programming for finding LCS length for two arrays
 *
 * Works in O(|a| * |b|), where a, b are the given arrays
 * (Algorithm on Wikipedia)[https://en.wikipedia.org/wiki/Longest_common_subsequence_problem#Solution_for_two_sequences]
 * Returns the computed dp table
 *
 * Array<Int> -> ShortArray increases speed by 4 times
 * max(dp, dp) -> if (dp > dp) gives another 20%
 * Array<ShortArray> -> ShortMatrix - another 20%
 *
 * Usage:
 * println(lcsDP(arrayOf(4, 1, 5, 6, 2, 7, 3), arrayOf(1, 9, 10, 11, 2, 3, 8))[7, 7])
 */
fun lcsDP(a: LongArray, b: LongArray) : ShortMatrix {
    val n = a.size
    val m = b.size
    val dp = ShortMatrix(n + 1, m + 1)
    for (i in 1..n) {
        for (j in 1..m) {
            if (a[i - 1] == b[j - 1]) {
                dp[i, j] = dp[i - 1, j - 1] + 1
            } else if (dp[i - 1, j] > dp[i, j - 1]) {
                dp[i, j] = dp[i - 1, j]
            } else {
                dp[i, j] = dp[i, j - 1]
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
 */
fun lcs(a: LongArray, b: LongArray) : Int {
    return lcsDP(a, b)[a.size, b.size].toInt()
}

/**
 * Optimally matches elements from two arrays
 *
 * Firstly, it runs lcsDP and recovers path to optimal answer
 * Then in creates diff = {(posA, posB) for each line in A U B},
 * where posA = index of the line in A or -1, posB = same for B
 *
 * Usage:
 * assertEquals(arrayOf(Pair(1, -1), Pair(2, 2), Pair(1, 3), Pair(2, 2)), diff(arrayOf(1, 3, 2), arrayOf(3, 4))
 */
fun diff(a: LongArray, b: LongArray) : List<LinePosition> {
    val diff = mutableListOf<LinePosition>()

    fun addFromA(i: Int, j: Int) = diff.add(LinePosition(i, 0))
    fun addFromB(i: Int, j: Int) = diff.add(LinePosition(0, j))
    fun addFromBoth(i: Int, j: Int) = diff.add(LinePosition(i, j))
    val dp = lcsDP(a, b)
    var i = a.size
    var j = b.size
    while (i > 0 && j > 0) {
        if (dp[i, j] == dp[i - 1, j]) {
            addFromA(i, j)
            --i
        } else if (dp[i, j] == dp[i, j - 1]) {
            addFromB(i, j)
            --j
        } else {
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

/** Splits text into parts ending with one of delimiters
 *
 * Not the same as String.split, because it can preserve delimiters
 * * means any symbol
 * And if delimiters are ' ' or '\n', they are added to the last part (for correct diff output)
 *
 * Usage:
 * assertEquals(listOf("one;", " two;", " three"), split("one; two; three", ";"))
 * assertEquals(listOf("one;", " ", "two;", " ", "three"), split("one; two; three", "; "))
 */
fun split(s: String, delimiters: String, ignoreDelim: Boolean) : List<String> {
    val parts = mutableListOf<String>()
    val buff = StringBuilder()
    val star = '*' in delimiters
    for (c in s) {
        val isDelim = c in delimiters
        if (!isDelim || !ignoreDelim || star) {
            buff.append(c)
        }
        if (star || c in delimiters) {
            if (buff.length > 0) {
                parts.add(buff.toString())
            }
            buff.clear()
        }
    }
    if (buff.length > 0) {
        if (!ignoreDelim) {
            if ('\n' in delimiters) {
                buff.append('\n')
            } else if (' ' in delimiters) {
                buff.append(' ')
            }
        }
        parts.add(buff.toString())
    }
    return parts
}

/**
 * Polynomial 64-bit string hash
 *
 * Namely, it equals (sum (s_i * BASE ^ i)) mod MOD
 *
 * Usage:
 * assertNotEquals(longHash("abacaba"), longHash("abracadabra"))
 * assertEquals(longHash("abacaba"), longHash("abacaba"))
 */
fun longHash(s: String) : Long {
    val base : Long = 59
    val mod : Long = 1e17.toLong() + 3
    var sum : Long = 0
    for (c in s) {
        sum = (base * sum + c.code.toLong()) % mod
    }
    return sum
}

/**
 * Replace lines of the text with their hash values
 *
 * Usage:
 * assert(toHashArray(arrayOf("a", b")) contentEquals arrayOf(97, 98))
 */
fun toHashArray(lines: Array<String>) : LongArray {
    val hashList = mutableListOf<Long>()
    for (s in lines) {
        hashList.add(longHash(s))
    }
    return hashList.toLongArray()
}

/**
 * Just prints help
 */
fun printHelp() {
    println("""
                                                                Usage: diff [OPTION]... FILES
                                                                              (C) diff --help


This program compares files line by line or by any other unit you can define with a regex.
There should be exactly two files and any number of options which begin with a hyphen.
File order matters while option order do not.

-c, --color                     use colors instead of '<', '>' and '='
-i, --input-delim=CHARS         splits input into parts ending with CHARS
-o, --output-delim=STRING       joins output with STRING
-n, --ignore-delim              removes delimiters while splitting
-h, --help                      display this help and exit
-g, --ignore-case               convert all input to lowercase before comparing

Usage:

diff A B                        plain text line-by-line diff for A and B
diff -n --color A B             colored diff without newlines
diff -i=" " -o="\n" A B         word-by-word diff with one word at line
diff -i="*" -o="" A B           char-by-char diff
diff -n -c -i=" " -o=" " A B    colored word-by-word diff without spaces
diff A --input-delim=".?!" --output-delim=";\t" B --color
                                colored sentence-by-sentence diff
                                for A and B with output separated by ";\t"
    """
    )
}

enum class Color(val code: String) {
    Reset("\u001B[0m"),
    Red("\u001B[31m"),
    Green("\u001B[32m");
    override fun toString() : String {
        return code
    }
}

fun main(args: Array<String>) {
    fun argValue(vararg names: String) : String? {
        for (name in names) {
            args.find{ it.length >= name.length && it.substring(0, name.length) == name }?.let{
                return if (it.length > name.length && it[name.length] == '=') it.substring(name.length + 1) else ""
            }
        }
        return null
    }
    val files = args.filter{ it.length > 0 && it.first() != '-' }
    if (files.size != 2 || argValue("-h", "--help") != null) {
        printHelp()
        exitProcess(0)
    }
    val colorOutput = argValue("-c", "--color") != null
    val ignoreCase = argValue("-g", "--ignore-case") != null
    val ignoreDelim = argValue("-n", "--ignore-delim") != null
    val inputDelim = (argValue("-i", "--input-delim") ?: "\n").trim('"')
    val outputDelim = (argValue("-o", "--output-delim") ?: "").trim('"')

    fun readInputFromFile(name: String) : Array<String> {
        val raw : String = File(name).readText()
        val text : String = if (ignoreCase) raw.map{ it.lowercaseChar() }.toString() else raw
        val tokens : List<String> = split(text, inputDelim, ignoreDelim)
        return tokens.toTypedArray()
    }

    val tokensA = readInputFromFile(files[0])
    val tokensB = readInputFromFile(files[1])
    val arrA = toHashArray(tokensA)
    val arrB = toHashArray(tokensB)

    val result = diff(arrA, arrB)
    for ((i, j) in result) {
        if (i > 0 && j > 0) {
            print(if (colorOutput) Color.Reset else "=")
            print("${tokensA[i - 1]}$outputDelim")
        } else if (i > 0) {
            print(if (colorOutput) Color.Red else "<")
            print("${tokensA[i - 1]}$outputDelim")
        } else if (j > 0) {
            print(if (colorOutput) Color.Green else ">")
            print("${tokensB[j - 1]}$outputDelim")
        } else {
            assert(false) {"Every line should come from somewhere"}
        }
        if (colorOutput) {
            print(Color.Reset)
        }
    }
}
