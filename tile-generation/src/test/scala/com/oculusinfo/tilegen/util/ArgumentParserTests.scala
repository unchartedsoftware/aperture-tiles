/**
 * Copyright (c) 2014 Oculus Info Inc.
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



import java.util.Properties

import org.scalatest.FunSuite



class ArgumentParserTestSuite extends FunSuite {
  test("boolean property") {
    val args = Array("-b1", "t", "-b2", "tr", "-b3", "tru", "-b4", "true", "-b5", " true", "-b6", "truely",
                     "-b7", "y", "-b8", "ye", "-b9", "yes", "-b10", "yes ", "-b11", "yesterday",
                     "-b12", " -1 ", "-b13", " 0 ", "-b14", " 1 ", "-b15", "1.0")
    val argParser = new ArgumentParser(args)
    assert(true === argParser.getBooleanArgument("b1", ""))
    assert(true === argParser.getBooleanArgument("b2", ""))
    assert(true === argParser.getBooleanArgument("b3", ""))
    assert(true === argParser.getBooleanArgument("b4", ""))
    assert(true === argParser.getBooleanArgument("b5", ""))
    assert(false === argParser.getBooleanArgument("b6", ""))
    assert(true === argParser.getBooleanArgument("b7", ""))
    assert(true === argParser.getBooleanArgument("b8", ""))
    assert(true === argParser.getBooleanArgument("b9", ""))
    assert(true === argParser.getBooleanArgument("b10", ""))
    assert(false === argParser.getBooleanArgument("b11", ""))
    assert(true === argParser.getBooleanArgument("b12", ""))
    assert(false === argParser.getBooleanArgument("b13", ""))
    assert(true === argParser.getBooleanArgument("b14", ""))
    assert(false === argParser.getBooleanArgument("b15", ""))
  }
}
