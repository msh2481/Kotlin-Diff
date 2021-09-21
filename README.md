# Курс основ программирования на МКН СПбГУ
## Проект 1: утилита diff

[Постановка задачи](./TASK.md)

## Использование 

Если вы запускаете эту утилиту в виде jar-файла (назовём его diff.jar), то формат будет такой:
    
    java -jar diff.jar ФАЙЛ1 ФАЙЛ2 [ОПЦИИ]

А отдельные опции подробно описаны в справке, которую можно вызвать и из программы:

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

## Описание

На данный момент в основе программы лежит базовый [алгоритм](https://en.wikipedia.org/wiki/Longest_common_subsequence_problem#Solution_for_two_sequences) использующий динамическое программирование за O(NM) времени и памяти, где N и M - количество строк (или иных сравниваемых частей) в данных файлах.
На моём ноутбуке он работает вплоть до N = 16000 (для простоты, N = M), причём работа занимает не больше секунды,
а при большем количестве перестаёт хватать памяти.
Насколько мне известно, в общем случае решения работающие существенно быстрее пока не найдено,
но существуют оптимизации для определённых случаев:
* Алгоритм Хиршберга, который будучи немного медленнее (неасимптотически) уменьшает требования к памяти до O(N + M)
* Алгоритм за O((N + M)D) времени и памяти, где D - размер оптимального ответа. На нём основаны большинство алгоритмов для подобных программ.
* Аппроксимация оптимального ответа за O((N + M) log (N + M)) в среднем. Для этого считается наибольшая общая подстрока, что можно сделать за O(N + M) с хешами и хеш-таблицей, а затем рекурсивно находятся ответы слева и справа от неё. 
