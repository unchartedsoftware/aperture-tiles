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

import com.oculusinfo.binning.util.JsonUtilities;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.json.JSONArray;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;


public class AnnotationRESTServiceTests extends AnnotationTestsBase {

    static final int NUM_THREADS = 8;
    static final int NUM_WRITES = 10000;

    static final String URL = "http://localhost:8080/twitter-community-demo/";
    static final String REST_ENDPOINT = "rest/annotation";
    static final String LAYER_NAME = "twitter.community.centralnodes.annotations.debug";

    public class AnnotationTestClient implements Runnable {

        String _name;

        AnnotationTestClient( String name ) {
            _name = name;
            Logger.getLogger("org.apache.http").setLevel(Level.WARN);
        }

        public void write( JSONObject annotationJSON ) throws IOException {

            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost( URL + REST_ENDPOINT);
            try {

                StringEntity input = new StringEntity(
                                "{"+
                                "\"type\": \"write\","+
                                "\"layer\":\"" + LAYER_NAME + "\","+
                                "\"annotation\":" + annotationJSON.toString() +
                                "}");

                input.setContentType("application/json");
                post.setEntity(input);
                client.execute(post);

            } catch ( Exception e ) {

                throw new IOException( e );
            }
        }


        protected JSONArray randomLineage( int level, int growthFactor ) {

            JSONArray arr = new JSONArray();
            try {
                JSONObject node = new JSONObject();
                node.put("index", JSONObject.NULL);
                node.put("count", growthFactor);
                arr.put(node);

                for (int i=0; i<=level; i++) {
                    node = new JSONObject();
                    node.put("index", _rand.nextInt( growthFactor ));
                    if (i!=level) {
                        node.put("count", growthFactor);
                    } else {
                        node.put("count", JSONObject.NULL);
                    }
                    arr.put(node);
                }

            } catch ( Exception e ) {
                e.printStackTrace();
            }

            return arr;
        }

        protected String getUser( int growthFactor, JSONArray lineage ) {

            try {

                int index = 1;
                for (int i = 1; i<lineage.length(); i++) {
                    int child = lineage.getJSONObject( i ).getInt("index");
                    index = growthFactor * (index-1) + 2 + child;
                }
                return "user_" + (index-2);

            } catch ( Exception e ) {
                e.printStackTrace();
            }
            return "user_error";
        }

        protected String getParent( int growthFactor, JSONArray lineage ) {

            JSONArray parentLineage = new JSONArray();
            try {
                for (int i=0; i<lineage.length()-1; i++) {
                    parentLineage.put( JsonUtilities.deepClone( lineage.getJSONObject(i) ) );
                }
            } catch ( Exception e ) {
                e.printStackTrace();
            }
            return getUser( growthFactor, parentLineage );
        }

        /*
	     * Annotation index generation function
	     */
        protected JSONObject generateJSON() {

            double [] xy = randomPosition();

            try {
                JSONObject anno = new JSONObject();
                anno.put("x", xy[0]);
                anno.put("y", xy[1]);

                int level = (int)(_rand.nextDouble() * 10);
                anno.put("level", level );

                JSONObject range = new JSONObject();
                range.put("min", level );
                range.put("max", level );
                anno.put("range", range );

                anno.put("group", "Central Node" );

                JSONObject data = new JSONObject();
                JSONArray lineage = randomLineage( level, 3 );
                data.put("user",  getUser(3, lineage) );
                data.put("parent",  getParent(3, lineage) );
                data.put("lineage", lineage );
                anno.put("data", data);
                return anno;

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
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
