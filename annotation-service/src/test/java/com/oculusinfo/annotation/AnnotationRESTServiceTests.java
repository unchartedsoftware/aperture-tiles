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

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;


public class AnnotationRESTServiceTests extends AnnotationTestsBase {

    static final int NUM_THREADS = 8;
    static final int NUM_WRITES = 25000;

    public class AnnotationTestClient implements Runnable {

        String _name;

        AnnotationTestClient( String name ) {
            _name = name;
            Logger.getLogger("org.apache.http").setLevel(Level.WARN);
        }

        public void write( JSONObject annotationJSON ) throws IOException {

            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://localhost:8080/bitcoin-demo/rest/annotation");
            try {

                StringEntity input = new StringEntity(
                                "{"+
                                "\"type\": \"write\","+
                                "\"layer\":\"test-annotations-0\","+
                                "\"annotation\":" + annotationJSON.toString() +
                                "}");

                input.setContentType("application/json");
                post.setEntity(input);
                client.execute(post);

            } catch ( Exception e ) {

                throw new IOException( e );
            }
        }

        public void run() {

            for (int i=0; i<NUM_WRITES; i++) {

                try {

                    write(generateJSON());
                    System.out.println( "Client " + _name + " successfully wrote annotation " + i);

                } catch ( IOException e ) {
                    e.printStackTrace();
                }
            }
        }
    }


    @Test
    public void writeToServer () {

        List<Thread> threads = new LinkedList<>();

        // write / read
        for (int i = 0; i < NUM_THREADS; i++) {

            Thread t = new Thread( new AnnotationTestClient("" + i) );
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

}
