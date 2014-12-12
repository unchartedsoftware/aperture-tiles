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
package com.oculusinfo.annotation.filter.impl;

import com.oculusinfo.annotation.AnnotationData;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.LinkedList;
import java.util.List;


public class ScriptableFilter extends EmptyFilter {

    private String _script;

    public ScriptableFilter( String script ) {
        _script = script;
    }

    public List<AnnotationData<?>> filterAnnotations( List<AnnotationData<?>> annotations ) {

        List<AnnotationData<?>> filtered = new LinkedList<>();
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");

        try
        {
            /*
            int size = annotations.size();
            List<String> annotationStrings = new ArrayList<>( size );
            int totalLength = 0;
            for ( int i=0; i<size; i++ ) {
                String annoStr = annotations.get(i).toJSON().toString();
                if ( i != size-1 ) {
                    annoStr += ",";
                }
                annotationStrings.add( annoStr );
                totalLength += annoStr.length();
            };

            String scriptPrefix = "(function() { var annotations = [";

            String scriptPostfix = "];"
                                +"var len = annotations.length, "
                                +"results = java.lang.reflect.Array.newInstance(java.lang.Boolean, len),"
                                +"result, i, annotation;"
                                +"for(i=0; i<len; i++) {"
                                    +"annotation = annotations[i];"
                                    +"result = " + _script + ";"
                                    +"results[i] = result;"
                                +"}"
                                +"return results; })();";

            StringBuilder script = new StringBuilder( scriptPrefix.length()
                                                            + totalLength
                                                            + scriptPostfix.length() );
            // create script
            script.append( scriptPrefix );
            for ( String annoStr : annotationStrings ) {
                script.append( annoStr );
            }
            script.append( scriptPostfix );

            //String func = "var annotation = "+ annotation.toJSON().toString() +"; "+ _script;
            Boolean[] results = (Boolean[])engine.eval( script.toString() );

            for ( int i=0; i<results.length; i++ ) {
                if (results[i]) {
                    filtered.add( annotations.get(i) );
                }
            }
            */

            for ( AnnotationData<?> annotation : annotations ) {

                String func = "var annotation = "+ annotation.toJSON().toString() +"; "+ _script;
                Boolean result = (Boolean)engine.eval( func );
                if (result) {
                    filtered.add( annotation );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return filtered;
    }
}
