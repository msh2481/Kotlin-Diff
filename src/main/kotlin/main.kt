import java.io.File
import kotlin.system.exitProcess

/** TODO
 * ND algorithm
 * Block edit
 */



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
fun mySplit(s: String, delimiters: Set<Char>, ignoreDelim: Boolean) : List<String> {
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
fun toHashArray(lines: List<String>) : LongArray {
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
-f, --fast                      use fast approximation algorithm
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

data class CommandlineArguments(
    val files : Pair<String, String>,
    val colorOutput : Boolean,
    val ignoreCase : Boolean,
    val ignoreDelim : Boolean,
    val inputDelim : Set<Char>,
    val outputDelim : String,
    val fastMode : Boolean
)

fun parseArgs(args: Array<String>) : CommandlineArguments {
    val files = mutableListOf<String>()
    var colorOutput = false
    var ignoreCase = false
    var ignoreDelim = false
    var inputDelim = setOf('\n')
    var outputDelim = ""
    var fastMode = false

    for (arg in args) {
        when (arg.substringBefore("=").trim()) {
            "-c", "--color" -> colorOutput = true
            "-g", "--ignore-case" -> ignoreCase = true
            "-n", "--ignore-delim" -> ignoreDelim = true
            "-i", "--input-delim" -> inputDelim = arg.substringAfter("=").toSet()
            "-o", "--output-delim" -> outputDelim = arg.substringAfter("=").trim('"')
            "-f", "--fast" -> fastMode = true
            "-h", "--help" -> printHelp().also{ exitProcess(0) }
            else -> {
                assert(arg.length > 0 && arg.first() != '-')
                files.add(arg)
            }
        }
    }
    assert(files.size == 2)
    return CommandlineArguments(Pair(files[0], files[1]), colorOutput, ignoreCase, ignoreDelim, inputDelim, outputDelim, fastMode)
}

fun readInputFromFile(name: String, args : CommandlineArguments) : List<String> {
    val raw : String = File(name).readText()
    val text : String = if (args.ignoreCase) raw.map{ it.lowercaseChar() }.toString() else raw
    val tokens : List<String> = mySplit(text, args.inputDelim, args.ignoreDelim)
    return tokens
}

fun printDiff(tokensA : List<String>, tokensB : List<String>, result : List<LinePosition>, args : CommandlineArguments) {
    for ((i, j) in result) {
        if (i > 0 && j > 0) {
            print(if (args.colorOutput) Color.Reset else "=")
            print("${tokensA[i - 1]}${args.outputDelim}")
        } else if (i > 0) {
            print(if (args.colorOutput) Color.Red else "<")
            print("${tokensA[i - 1]}${args.outputDelim}")
        } else if (j > 0) {
            print(if (args.colorOutput) Color.Green else ">")
            print("${tokensB[j - 1]}${args.outputDelim}")
        } else {
            assert(false) {"Every line should come from somewhere"}
        }
        if (args.colorOutput) {
            print(Color.Reset)
        }
    }
}

fun main(rawArgs: Array<String>) {
    val args = parseArgs(rawArgs)
    val tokensA = readInputFromFile(args.files.first, args)
    val tokensB = readInputFromFile(args.files.second, args)
    val arrA = toHashArray(tokensA)
    val arrB = toHashArray(tokensB)

    val result = if (args.fastMode) HeuristicLCS.diff(arrA, arrB) else FastLCS.diff(arrA, arrB)
    printDiff(tokensA, tokensB, result, args)
}
