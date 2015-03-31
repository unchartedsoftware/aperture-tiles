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
		assert(true === argParser.getBoolean("b1", ""))
		assert(true === argParser.getBoolean("b2", ""))
		assert(true === argParser.getBoolean("b3", ""))
		assert(true === argParser.getBoolean("b4", ""))
		assert(true === argParser.getBoolean("b5", ""))
		assert(false === argParser.getBoolean("b6", ""))
		assert(true === argParser.getBoolean("b7", ""))
		assert(true === argParser.getBoolean("b8", ""))
		assert(true === argParser.getBoolean("b9", ""))
		assert(true === argParser.getBoolean("b10", ""))
		assert(false === argParser.getBoolean("b11", ""))
		assert(true === argParser.getBoolean("b12", ""))
		assert(false === argParser.getBoolean("b13", ""))
		assert(true === argParser.getBoolean("b14", ""))
		assert(false === argParser.getBoolean("b15", ""))
	}

	test("integer property") {
		val args = Array("-i1", "1", "-i2", "2", "-i3", "3.0", "-i4", "invalid", "-i5", "-5")
		val argParser = new ArgumentParser(args)

		assert(1 === argParser.getInt("i1", ""))
		assert(2 === argParser.getInt("i2", ""))
		intercept[NumberFormatException](argParser.getInt("i3", ""))
		intercept[NumberFormatException](argParser.getInt("i4", ""))
		assert(-5 === argParser.getInt("i5", ""))
		assert(-6 === argParser.getInt("i6", "", Some(-6)))
	}

	test("integer sequence") {
		val args = Array("-is1", "1,2,3", "-is2", "1;3;5")
		val argParser = new ArgumentParser(args)

		assert(List(1, 2, 3) === argParser.getIntSeq("is1", "").toList)
		assert(List(1, 3, 5) === argParser.getIntSeq("is2", "", ";").toList)
		assert(List(1, 4, 7) === argParser.getIntSeq("is3", "", ",", Some(Seq(1, 4, 7))).toList)
	}

	test("integer prop seq") {
		val args = Array("-ips.0", "1", "-ips.1", "3", "-ips.2", "5")
		val argParser = new ArgumentParser(args)

		assert(List(1, 3, 5) == argParser.getIntPropSeq("ips", "").toList)
	}

	test("long property") {
		val args = Array("-L1", "1", "-L2", "2", "-L3", "3.0", "-L4", "invalid", "-L5", "-5")
		val argParser = new ArgumentParser(args)

		assert(1L === argParser.getLong("L1", ""))
		assert(2L === argParser.getLong("L2", ""))
		intercept[NumberFormatException](argParser.getLong("L3", ""))
		intercept[NumberFormatException](argParser.getLong("L4", ""))
		assert(-5L === argParser.getLong("L5", ""))
		assert(-6L === argParser.getLong("L6", "", Some(-6L)))
	}

	test("long sequence") {
		val args = Array("-ls1", "1,2,3", "-ls2", "1;3;5")
		val argParser = new ArgumentParser(args)

		assert(List(1L, 2L, 3L) === argParser.getLongSeq("ls1", "").toList)
		assert(List(1L, 3L, 5L) === argParser.getLongSeq("ls2", "", ";").toList)
		assert(List(1L, 4L, 7L) === argParser.getLongSeq("ls3", "", ",",
		                                                 Some(Seq(1L, 4L, 7L))).toList)
	}

	test("long prop seq") {
		val args = Array("-lps.0", "1", "-lps.1", "3", "-lps.2", "5")
		val argParser = new ArgumentParser(args)

		assert(List(1L, 3L, 5L) == argParser.getLongPropSeq("lps", "").toList)
	}

	test("float property") {
		val args = Array("-f1", "1", "-f2", "2.0", "-f3", "3.5", "-f4", "invalid", "-f5", "-5.2", "-f6", "NaN")
		val argParser = new ArgumentParser(args)

		assert(1.0f === argParser.getFloat("f1", ""))
		assert(2.0f === argParser.getFloat("f2", ""))
		assert(3.5f === argParser.getFloat("f3", ""))
		intercept[NumberFormatException](argParser.getFloat("f4", ""))
		assert(-5.2f === argParser.getFloat("f5", ""))
		assert(argParser.getFloat("f6", "").isNaN)
		assert(-6.3f === argParser.getFloat("f7", "", Some(-6.3f)))
	}

	test("float sequence") {
		val args = Array("-fs1", "1.1,2,3.3", "-fs2", "1.1;3;5.3")
		val argParser = new ArgumentParser(args)

		assert(List(1.1f, 2.0f, 3.3f) === argParser.getFloatSeq("fs1", "").toList)
		assert(List(1.1f, 3.0f, 5.3f) === argParser.getFloatSeq("fs2", "", ";").toList)
		assert(List(1.1f, 4.4f, 7.7f) === argParser.getFloatSeq("fs3", "", ",",
		                                                        Some(Seq(1.1f, 4.4f, 7.7f))).toList)
	}

	test("float prop seq") {
		val args = Array("-fps.0", "1.1", "-fps.1", "3", "-fps.2", "5.5")
		val argParser = new ArgumentParser(args)

		assert(List(1.1f, 3.0f, 5.5f) == argParser.getFloatPropSeq("fps", "").toList)
	}

	test("double property") {
		val args = Array("-d1", "1", "-d2", "2.0", "-d3", "3.5", "-d4", "invalid", "-d5", "-5.2", "-d6", "NaN")
		val argParser = new ArgumentParser(args)

		assert(1.0 === argParser.getDouble("d1", ""))
		assert(2.0 === argParser.getDouble("d2", ""))
		assert(3.5 === argParser.getDouble("d3", ""))
		intercept[NumberFormatException](argParser.getDouble("d4", ""))
		assert(-5.2 === argParser.getDouble("d5", ""))
		assert(argParser.getDouble("d6", "").isNaN)
		assert(-6.3 === argParser.getDouble("d7", "", Some(-6.3d)))
	}

	test("double sequence") {
		val args = Array("-ds1", "1.1,2,3.3", "-ds2", "1.1;3;5.3")
		val argParser = new ArgumentParser(args)

		assert(List(1.1, 2.0, 3.3) === argParser.getDoubleSeq("ds1", "").toList)
		assert(List(1.1, 3.0, 5.3) === argParser.getDoubleSeq("ds2", "", ";").toList)
		assert(List(1.1, 4.4, 7.7) === argParser.getDoubleSeq("ds3", "", ",",
		                                                      Some(Seq(1.1, 4.4, 7.7))).toList)
	}

	test("double prop seq") {
		val args = Array("-dps.0", "1.1", "-dps.1", "3", "-dps.2", "5.5")
		val argParser = new ArgumentParser(args)

		assert(List(1.1, 3.0, 5.5) == argParser.getDoublePropSeq("dps", "").toList)
	}

	test("key case sensitivity") {
		val args = Array("-TestKey", "1.0")
		val argParser = new ArgumentParser(args)

		assertResult(Some(1.0))(argParser.getDoubleOption("TestKey", ""))
		assertResult(None)(argParser.getDoubleOption("testkey", ""))
	}
}
