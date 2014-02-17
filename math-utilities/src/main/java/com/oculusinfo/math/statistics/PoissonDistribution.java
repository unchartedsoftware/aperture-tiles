/*
 * Copyright (c) 2014 Oculus Info Inc. http://www.oculusinfo.com/
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



package com.oculusinfo.math.statistics;



import java.io.Serializable;



/**
 * Simple class to naively generate a poisson distribution
 * 
 * @author nkronenfeld
 */
public class PoissonDistribution implements Serializable {
    private static final long serialVersionUID = -486283073321373471L;



    private double _lambda;



    /**
     * Create an object that can create a population of numbers with a Poisson
     * distribution.
     * 
     * @param lambda The desired mean of the distribution.
     */
    public PoissonDistribution (double lambda) {
        _lambda = lambda;
    }

    /**
     * Actually generate the distribution, one element at a time.
     * 
     * @return A single entry in the distribution.
     */
    public int sample () {
        int j = 0;
        double p = Math.exp(-_lambda);
        double F = p;
        double U = Math.random();

        while (U > F) {
            p = _lambda * p / (j+1);
            F = F + p;
            j = j + 1;
        }
        return j;
    }
}
