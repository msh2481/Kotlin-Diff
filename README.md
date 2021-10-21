# Basics of programming course: diff tool

## Running the diff

General format is:
    
    java -jar diff.jar ФАЙЛ1 ФАЙЛ2 [ОПЦИИ]

And the particular options are described in help text:

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

## Algorithms description

This utility can use one of two algorithms: a basic dynamic programming in O(NM) time and space and a fast heuristic in O((N+M) log (N+M)) time and O(N + M) space.

The first one always finds optimal answers and works for files with a few thousand lines (if using newlines as delimiters), e.g. 16k lines for my 16GB RAM laptop in less than a second. For larger inputs it runs out of memory.

Contrary, the second always can find larger diffs than possible (though, still correct), but runs much faster. For example, it have processed two million-line random files in a few seconds. The idea of this algorithm is as follows: choose some length from 1 to N (N initially), check if there is a common substring of this length (linear time by hashing and hash tables), if there is -- invoke recursive calls for left and right sides, else -- multiply the chosen length by constant.
