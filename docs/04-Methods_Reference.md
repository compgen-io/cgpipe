# Methods reference

Every value in CGPipe has a type (`string`, `int`, `float`, `bool`, `list`, `range`). Methods are called with dot syntax on either a variable or a literal:

    name = "chr1.bam"
    print name.basename()       # chr1.bam
    print name.sub(".bam","")   # chr1

    samples = ["A","B","C"]
    print samples.length()      # 3
    print samples.join(",")     # A,B,C

    print "hello".upper()       # HELLO
    print [1,2,3].join("-")     # 1-2-3
    print (1..5).length()       # 5

Method calls work on literals and on the result of other method calls, so chains compose:

    "/data/sample.bam".basename().sub(".bam","").upper()    # SAMPLE

Argument counts are checked at runtime — passing too few or too many will throw `Method call error`.

## All types — `type()`

Returns the type name as a string (`"string"`, `"int"`, `"float"`, `"bool"`, `"list"`, `"range"`).

    print "x".type()    # string
    print 1.type()      # int
    print [].type()     # list

Useful for defensive checks when a variable may have come from a command-line argument (which is always a string until parsed).

## string

| Method | Args | Returns | Description |
|--------|------|---------|-------------|
| `split(delim)` | `delim`: string (optional) | list of string | Split on `delim`; if omitted, splits into individual characters |
| `sub(pattern, replacement)` | both string | string | Regex replace-all (Java regex syntax) |
| `upper()` | none | string | Uppercase |
| `lower()` | none | string | Lowercase |
| `length()` | none | int | Character count |
| `contains(substring)` | string | bool | Substring test |
| `join(list)` | list | string | Use the receiver as separator between list elements |
| `basename()` | none | string | File basename (e.g. `/a/b/c.bam` → `c.bam`) |
| `dirname()` | none | string | Absolute parent directory (`/a/b/c.bam` → `/a/b`); empty for bare filenames |
| `abspath()` | none | string | Resolved absolute path |
| `exists()` | none | bool | True if file or directory exists |
| `isfile()` | none | bool | True if path exists and is a regular file |
| `isdir()` | none | bool | True if path exists and is a directory |

### Notes on string methods

- `sub` uses Java regex. To strip a literal suffix like `.bam`, escape the dot: `name.sub("\\.bam","")`. The unescaped `.` form (`name.sub(".bam","")`) usually still works because `.bam` only appears at the end, but escaping is safer.
- `split` returns a list even for single-element strings (`"x".split(",")` → `["x"]`).
- `join` reads strangely at first: the receiver string is the *separator*, the argument is the *list*. Equivalent to `",".join(["a","b"])` → `"a,b"`. Most pipelines use the list form (`["a","b"].join(",")`) instead.
- File-test methods (`exists`, `isfile`, `isdir`) consult the filesystem at evaluation time, not at job-execution time. Useful for early validation of pipeline arguments.

## list

| Method | Args | Returns | Description |
|--------|------|---------|-------------|
| `length()` | none | int | Number of elements |
| `contains(value)` | any | bool | Element-equality test |
| `join(separator)` | string | string | Join elements into a string |

Lists are also accessed by index (`samples[0]`, `samples[-1]`), sliced (`samples[1:3]`), and appended with `+=` (see [Language Syntax](03-Language_Syntax.md#lists)). The receiver-flipped form of `join` (`",".join(samples)`) also works and is equivalent.

## range

| Method | Args | Returns | Description |
|--------|------|---------|-------------|
| `length()` | none | int | Number of values in the range |

Ranges are produced with `from..to` and are most commonly used in `for` loops. They can also be iterated, indexed, and passed to most places that accept a list.

    print (1..10).length()    # 10
    for i in 1..3
        print i
    done

## int, float, bool

These types currently expose only `type()`. Arithmetic and comparisons happen through operators, not methods.

## Method dispatch and missing-method errors

If a method name isn't defined for the receiver's type, CGPipe throws `Method not found: <name>`. There's no automatic type coercion — call the appropriate cast first if you need it (e.g., convert numeric strings to int via arithmetic: `("1" + 0)`, though this is rarely needed because CLI args are usually consumed as strings).
