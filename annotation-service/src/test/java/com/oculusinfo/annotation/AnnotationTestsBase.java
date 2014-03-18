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
package com.oculusinfo.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.oculusinfo.annotation.io.*;

import org.junit.Assert;
import org.junit.Test;


public class AnnotationTestsBase {
	
	static final int      NUM_ENTRIES = 10000;
	static final int      NUM_TESTS = 25;
	static final double[] BOUNDS = {-180.0, -90.0, 180.0, 90.0};
	static final String   TABLE_NAME = "AnnotationTable";
	
	/*
	 * Annotation list printing utility function
	 */
	protected void print( List<AnnotationData> annotations ) {
		
		int i = 0;
		for (AnnotationData annotation : annotations ) {
			
			System.out.println("\tEntry "+i+": " + annotation.getIndex().getX() 
											     + ", " 
											     + annotation.getIndex().getY() 
											     + ", "
											     + annotation.getIndex().getIndex() 
											     + ", "
											     + annotation.getComment() );
			i++;
		}
	}

	/*
	 * Annotation index generation function
	 */
	protected AnnotationIndex generateIndex() {

		final Random rand = new Random();
		
		double x = BOUNDS[0] + (rand.nextDouble() * (BOUNDS[2] - BOUNDS[0]));
		double y = BOUNDS[1] + (rand.nextDouble() * (BOUNDS[3] - BOUNDS[1]));
			
		return new AnnotationIndex( x, y ,BOUNDS );
	}
	
	/*
	 * Annotation index generation function
	 */	
	protected AnnotationIndex generateIndex(double lon, double lat) {
	    		
		return new AnnotationIndex( lon, lat, BOUNDS );
	}
	
	
	protected String randomComment() {
		int LENGTH = 256;		
		Random rng = new Random();
		String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

	    char[] text = new char[LENGTH];
	    for (int i = 0; i < LENGTH; i++)
	    {
	        text[i] = CHARACTERS.charAt(rng.nextInt(CHARACTERS.length()));
	    }
	    return new String(text);
	}
	
	/*
	 * Annotation index generation function
	 */
	protected List<AnnotationData> generateAnnotations(int numEntries) {

		List<AnnotationData> annotations = new ArrayList<>();		
		for (int i=0; i<numEntries; i++) {
			
			AnnotationIndex index = generateIndex();	
			annotations.add(new AnnotationData( index, "p"+Integer.toString((int)Math.random()*5), randomComment() ));	
		}
		return annotations;
	}

	
}
