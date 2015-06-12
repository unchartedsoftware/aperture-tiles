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
 * Simple abstraction of a mathematical operator that we can pass around as a parameter
 */
public class BinaryOperator {

	public enum OPERATOR_TYPE {
		ADD,
		SUBTRACT,
		DIVIDE,
		MULTIPLY
	};

	public final OPERATOR_TYPE _operator;

	public BinaryOperator(OPERATOR_TYPE operator) {
		_operator = operator;
	}

	/**
	 * Uses the class member operator to perform the calculation.  More operations can
	 * be added here as needed.
	 *
	 * @param operand1 the first value to use in the calculation
	 * @param operand2 the second value to use in the calculation
	 * @param errorVal optional error value to use when bad input detected
	 * @return Number represents the result of the calculation
	 */
	public Number calculate(Number operand1, Number operand2, Number errorVal) {
		if (_operator.equals(OPERATOR_TYPE.ADD)) {
			return operand1.doubleValue() + operand2.doubleValue();
		} else if (_operator.equals(OPERATOR_TYPE.SUBTRACT)) {
			return operand1.doubleValue() - operand2.doubleValue();
		} else if (_operator.equals(OPERATOR_TYPE.MULTIPLY)) {
			return operand1.doubleValue() * operand2.doubleValue();
		} else if (_operator.equals(OPERATOR_TYPE.DIVIDE)) {
			if (operand2.doubleValue() == 0.0 && errorVal != null) {
				return errorVal;
			}
			return operand1.doubleValue() / operand2.doubleValue();
		} else {
			throw new ExceptionInInitializerError();
		}
	}

	/**
	 * Uses the class member operator to perform the calculation.  More operations can
	 * be added here as needed.
	 *
	 * @param operand1 the first value to use in the calculation
	 * @param operand2 the second value to use in the calculation
	 * @return Number represents the result of the calculation
	 */
	public Number calculate(Number operand1, Number operand2) {
		return calculate(operand1, operand2, null);
	}
}
