import kotlin.math.min

/**
 * Calculates polynomial hashes for substrings
 *
 * base = BASE and mod = 2^64 (Long overflow)
 */
class PolynomialHasher(arr: LongArray = longArrayOf(), BASE : Long = 263) {
    val prefixHash : List<Long>
    val basePowers : List<Long>
    init {
        var buff = MutableList<Long>(arr.size + 1) {0}
        for (i in arr.indices) {
            buff[i + 1] = buff[i] * BASE + arr[i]
        }
        prefixHash = buff
        buff = MutableList<Long>(arr.size + 1) {1}
        for (i in 1 until buff.size) {
            buff[i] = buff[i - 1] * BASE
        }
        basePowers = buff
    }

    /**
     * Hash for arr[l..r-1]
     */
    fun rangeHash(l: Int, r: Int) : Long {
        return prefixHash[r] - prefixHash[l] * basePowers[r - l]
    }
}

/**
 * Finds approximate diff in (O((N + M)log(N + M)) * map complexity)
 * Approximate means it isn't always optimal, but it still should be correct
 *
 * Algorithm:
 * Find large common substring, assume it wasn't edited and split recursively to left and right parts from it
 * If one of arrays becomes empty answer is trivial
 *
 * Usage:
 * assertEqual(listOf(LinePosition(0, 1), LinePosition(1, 0), LinePosition(2, 2)), HeuristicLCS.diff(longArrayOf(1, 3), longArrayOf(2, 3)))
 */
object HeuristicLCS {
    var hasherA = PolynomialHasher()
    var hasherB = PolynomialHasher()
    var answer = mutableListOf<LinePosition>()
    var arrA = longArrayOf()
    var arrB = longArrayOf()
    var currentLen = 0

    fun diff(arrA: LongArray, arrB: LongArray) : List<LinePosition> {
        this.arrA = arrA
        this.arrB = arrB
        hasherA = PolynomialHasher(arrA)
        hasherB = PolynomialHasher(arrB)
        answer = mutableListOf<LinePosition>()
        currentLen = min(arrA.size, arrB.size)
        solve(0, arrA.size, 0, arrB.size, min(arrA.size, arrB.size))
        return answer
    }

    /**
     * Recursively find diff for a[la..ra-1] and b[lb..rb-1], where largest common substring is less or equal to maxCommon
     *
     * Return nothing and puts found differences to a list of LinePosition (answer)
     */
    fun solve(la: Int, ra: Int, lb: Int, rb: Int, maxCommon: Int) {
//        println("solve $la $ra $lb $rb")
        if (la == ra) {
//            println("empty a")
            for (i in lb until rb) {
                answer.add(LinePosition(0, i + 1))
            }
        } else if (lb == rb) {
//            println("empty b")
            for (i in la until ra) {
                answer.add(LinePosition(i + 1, 0))
            }
        } else {
            assert(la < ra && lb < rb)
            val start = commonSubstring(la, ra, lb, rb, maxCommon)
            val maxLength = min(ra - start.posA, rb - start.posB)
            var length = 0
            assert(la <= start.posA && lb <= start.posB)
//            println("common: ${start.posA} ${start.posB} $length")
            while (length < maxLength && arrA[start.posA + length] == arrB[start.posB + length]) {
                ++length
            }
            solve(la, start.posA, lb, start.posB, min(length, min(start.posA - la, start.posB - lb)))
            assert(start.posA + length <= ra && start.posB + length <= rb)
            for (i in 1..length) {
                answer.add(LinePosition(start.posA + i, start.posB + i))
            }
            solve(start.posA + length, ra, start.posB + length, rb, min(length, min(ra - start.posA, rb - start.posB) - length))
        }
    }

    /**
     * Find (approximately) the largest common substring for two given substrings
     *
     * Returns the beginning of the largest common substring (length is ignored, because it anyway must be checked without hashes)
     */
    fun commonSubstring(la: Int, ra: Int, lb: Int, rb: Int, maxCommon: Int) : LinePosition {
        while (currentLen > maxCommon) {
            currentLen = currentLen * 2 / 3
        }
        while (true) {
            val start = findByLen(la, ra, lb, rb)
            if (start == null) {
                currentLen = currentLen * 2 / 3
            } else {
                return start
            }
        }
    }

    /**
     * Tries to find common substring with length = currentLen
     * Returns it's beginning or null
     */
    fun findByLen(la: Int, ra: Int, lb: Int, rb: Int) : LinePosition? {
        val hash2PosA = mutableMapOf<Long, Int>()
        for (i in la..ra-currentLen) {
            hash2PosA[hasherA.rangeHash(i, i + currentLen)] = i
        }
        for (i in lb..rb-currentLen) {
            val posA = hash2PosA[hasherB.rangeHash(i, i + currentLen)]
            if (posA != null) {
                return LinePosition(posA, i)
            }
        }
        return null
    }
}