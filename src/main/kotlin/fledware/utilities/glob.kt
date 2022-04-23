package fledware.utilities

/**
 * https://stackoverflow.com/questions/1247772/is-there-an-equivalent-of-java-util-regex-for-glob-type-patterns
 *
 * modified to respect * vs **
 *
 * @this A glob pattern.
 * @return A regex pattern to recognize the given glob pattern.
 */
fun String.globToRegex(): Regex {
  val sb = StringBuilder(this.length)
  var inGroup = 0
  var inClass = 0
  var firstIndexInClass = -1
  val arr = this.toCharArray()
  var i = 0
  while (i < arr.size) {
    when (val ch = arr[i]) {
      '\\' -> if (++i >= arr.size) {
        sb.append('\\')
      }
      else {
        val next = arr[i]
        when (next) {
          ',' -> {
          }
          'Q', 'E' -> {
            // extra escape needed
            sb.append('\\')
            sb.append('\\')
          }
          else -> sb.append('\\')
        }
        sb.append(next)
      }
      '*' -> {
        if (inClass == 0) {
          if (i + 1 < arr.size && arr[i + 1] == '*') {
            i++
            sb.append(".*")
          }
          else {
            sb.append("[^/]*")
          }
        } else sb.append('*')
      }
      '?' -> if (inClass == 0) sb.append('.') else sb.append('?')
      '[' -> {
        inClass++
        firstIndexInClass = i + 1
        sb.append('[')
      }
      ']' -> {
        inClass--
        sb.append(']')
      }
      '.', '(', ')', '+', '|', '^', '$', '@', '%' -> {
        if (inClass == 0 || firstIndexInClass == i && ch == '^') sb.append('\\')
        sb.append(ch)
      }
      '!' -> if (firstIndexInClass == i) sb.append('^') else sb.append('!')
      '{' -> {
        inGroup++
        sb.append('(')
      }
      '}' -> {
        inGroup--
        sb.append(')')
      }
      ',' -> if (inGroup > 0) sb.append('|') else sb.append(',')
      else -> sb.append(ch)
    }
    i++
  }
  return sb.toString().toRegex()
}
