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
 * [Algorithm on Wikipedia](https://en.wikipedia.org/wiki/Longest_common_subsequence_problem#Solution_for_two_sequences)
 * Returns the computed dp table
 *
 * Some time measurements are available in Benchmarks.txt
 *
 * Usage:
 * println(lcsDP(arrayOf(4, 1, 5, 6, 2, 7, 3), arrayOf(1, 9, 10, 11, 2, 3, 8))[7, 7])
 */
fun lcsDP(a: LongArray, b: LongArray) : ShortMatrix {
    val n = a.size
    val m = b.size
    val dp = ShortMatrix(n + 1, m + 1)
    for (row in 1..n) {
        for (column in 1..m) {
            if (a[row - 1] == b[column - 1]) {
                dp[row, column] = dp[row - 1, column - 1] + 1
            } else if (dp[row - 1, column] > dp[row, column - 1]) {
                dp[row, column] = dp[row - 1, column]
            } else {
                dp[row, column] = dp[row, column - 1]
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

data class LinePosition(val posA : Int, val posB : Int)

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