/*
 * Copyright (c) 2015 Uncharted Software. http://www.uncharted.software/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.binning.util;

/**
 * A simple class to implement a unary operation.  Extend as necessary.
 */
public class UnaryOperator {
	public enum OPERATOR_TYPE {
		LOG_10,
		LOG_2
	}

	public final OPERATOR_TYPE _operator;

	/**
	 * Create a unary operation
	 */
	public UnaryOperator(OPERATOR_TYPE operator) {
		_operator = operator;
	}

	/**
	 * Apply the unary operation.
	 * @param operand The unary operand.
	 * @param errorValue A value to return when an error condition ie. log10(0) is encountered.
	 * @return The calculated value or the error value.
	 */
	public Number calculate(Number operand, Number errorValue) {
		switch (_operator) {
			case LOG_10:
				if (errorValue != null & operand.doubleValue() <= 0.0) {
					return errorValue;
				}
				return Math.log10(operand.doubleValue());
			case LOG_2:
				if (errorValue != null & operand.doubleValue() <= 0.0) {
					return errorValue;
				}
				return Math.log(operand.doubleValue()) / Math.log(2);
			default:
				return operand;
		}
	}

	/**
	 * Apply the unary operation.
	 * @param operand The unary operand.
	 * @return The calculated value or the error value.
	 */
	public Number calculate(Number operand) {
		return calculate(operand, null);
	}
}
