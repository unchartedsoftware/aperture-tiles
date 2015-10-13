/*
 * Copyright (c) 2015 Uncharted Software Inc.
 * http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tilegen.util



import java.text.ParseException
import java.util.regex.Pattern
import scala.collection.mutable.Buffer



/**
 * Some more complex methods on strings that are needed somewhat globally
 */
object StringUtilities {
  private val TEXT = 0      // token ID for plain text, used by separateString
  private val ESCAPE = 1    // token ID for escape characters, used by separateString
  private val QUOTE = 2     // token ID for quote characters, used by separateString
  private val SEPARATOR = 3 // Token ID for separator characters, used by separateString

  // Take a text, and separate out the tokens that match the given pattern, recording noting both a token ID for those
  // tokens, and a position (so errors can return something meaningful).
  private def tokenize (text: String, pattern: String, token: Int): Seq[(String, Int, Int)] = {
    val matcher = Pattern.compile(pattern).matcher(text)
    var n = 0
    val output = Buffer[(String, Int, Int)]()
    while (matcher.find) {
      val (start, end) = (matcher.start, matcher.end)
      if (n < start)
        output += ((text.substring(n, start), TEXT, n))
      output += ((text.substring(start, end), token, start))
      n = end
    }
    if (n < text.length)
      output += ((text.substring(n), TEXT, n))

    output
  }

  /**
   * Separate a string into sub-strings, separating at the given separator, and accounting for both escaped characters
   * and quotes
   * @param text the text to parse
   * @param separator A regular expression describing the separator for which to search to separate the text into
   *                  substrings
   * @param quoter an optional regular expression denoting a quote sequence that forces all tokens between paired
   *               instances to be treated literally
   * @param escaper An optional regular expression denoting an escape sequence that forces the next token to be treated
   *                literally
   * @return An array of unescaped, unquoted strings representing the portion of the input text in between separator
   *         sequences.
   */
  def separateString(text: String, separator: String, quoter: Option[String], escaper: Option[String]): Array[String] = {
    // Tokenize the string
    val withEscapes: Seq[(String, Int, Int)] =
      escaper.map(escapeSequence => tokenize(text, escapeSequence, ESCAPE))
        .getOrElse(Seq((text, TEXT, 0)))
    val withQuotes: Seq[(String, Int, Int)] =
      quoter.map(quoteSequence => withEscapes.flatMap {
        case (subtext, 0, position) => tokenize(subtext, quoteSequence, QUOTE).map{
          case (subsubtext, tid, subposition) => (subsubtext, tid, position + subposition)
        }
        case (token, tid, position) => Array((token, tid, position))
      })
        .getOrElse(withEscapes)
    val fullyTokenized: Seq[(String, Int, Int)] = withQuotes.flatMap{
      case (subtext, 0, position) => tokenize(subtext, separator, SEPARATOR).map{
        case (subsubtext, tid, subposition) => (subsubtext, tid, position + subposition)
      }
      case (token, tid, position) => Array((token, tid, position))
    }
    // Unescape and unquote text
    val despecialized = Buffer[(String, Int, Int)]()
    var n = 0
    while (n < fullyTokenized.length) {
      val (token, tid, position) = fullyTokenized(n)
      if (ESCAPE == tid) {
        n = n + 1
        if (n < fullyTokenized.length) {
          // take this token literally
          val (etoken, etid, eposition) = fullyTokenized(n)
          despecialized += ((etoken, TEXT, eposition))
        } else {
          throw new ParseException("Hanging trailing escape", position)
        }
      } else if (QUOTE == tid) {
        n = n + 1
        while (n < fullyTokenized.length && fullyTokenized(n)._2 != QUOTE) {
          val (qtoken, qtid, qposition) = fullyTokenized(n)
          despecialized += ((qtoken, TEXT, qposition))
          n = n + 1
        }
        if (n == fullyTokenized.length) throw new ParseException("Unmatched quote", position)
      } else {
        despecialized += ((token, tid, position))
      }
      n = n + 1
    }

    // Now, finally, tokens between separators into an output buffer
    val output = Buffer[String]()
    n = 0;
    var separated = ""
    while (n < despecialized.length) {
      val (token, tid, position) = despecialized(n)
      if (SEPARATOR == tid) {
        output += separated
        separated = ""
      } else {
        separated = separated + token
      }
      n = n + 1
    }
    output += separated

    output.toArray
  }
}
