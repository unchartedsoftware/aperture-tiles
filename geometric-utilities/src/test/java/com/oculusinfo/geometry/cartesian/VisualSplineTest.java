/*
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
package com.oculusinfo.geometry.cartesian;

import com.oculusinfo.math.linearalgebra.Vector;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class VisualSplineTest extends JFrame {
    private static final long serialVersionUID = 1L;


    public static void main (String[] args) {
        VisualSplineTest app = new VisualSplineTest();
        app.setVisible(true);
    }


    private VisualSplineTest () {
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setupPanels();

        setLocation(100, 100);
        setSize(800, 800);
    }

    private void setupPanels () {
        SplinePanel sp = new SplinePanel();
        sp.addSpline(CubicBSpline.fit(new double[] {0, 1, 2, 3, 4, 5, 6},
                                      new Vector(0.25, 0.25),
                                      new Vector(0.50, 0.15),
                                      new Vector(0.75, 0.25),
                                      new Vector(0.85, 0.50),
                                      new Vector(0.75, 0.75),
                                      new Vector(0.50, 0.85),
                                      new Vector(0.25, 0.75)),
                     Color.CYAN);

        sp.addSpline(CubicBSpline.fit(new double[] {0, 1, 2, 3},
                                      new Vector(0.25, 0.25),
                                      new Vector(0.75, 0.25),
                                      new Vector(0.75, 0.75),
                                      new Vector(0.25, 0.75)),
                     Color.BLUE);
        sp.addSpline(CubicBSpline.fit(new double[] {0, 1, 2, 3},
                                      new Vector(0.25, 0.25),
                                      new Vector(0.70, 0.30),
                                      new Vector(0.80, 0.70),
                                      new Vector(0.20, 0.80)),
                     Color.RED);
        sp.addSpline(CubicBSpline.fit(new double[] {0, 1, 2, 3},
                                      new Vector(0.20, 0.30),
                                      new Vector(0.70, 0.20),
                                      new Vector(0.70, 0.80),
                                      new Vector(0.30, 0.70)),
                     Color.GREEN);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(sp, BorderLayout.CENTER);
    }


    static class SplinePanel extends JPanel {
        private static final long  serialVersionUID = 1L;
        private List<CubicBSpline> _splines;
        private List<Color>        _colors;

        SplinePanel () {
            _splines = new ArrayList<CubicBSpline>();
            _colors = new ArrayList<Color>();
        }
        public void addSpline (CubicBSpline s, Color c) {
            _splines.add(s);
            _colors.add(c);
        }
        @Override
        public void paint (Graphics g) {
            Dimension size = getSize();
            int w = size.width;
            int h = size.height;

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);

            Point p00 = getPoint(w, h, new Vector(0, 0));
            Point p10 = getPoint(w, h, new Vector(1, 0));
            Point p01 = getPoint(w, h, new Vector(0, 1));
            Point p11 = getPoint(w, h, new Vector(1, 1));
            g.setColor(Color.BLACK);
            g.drawLine(p00.x, p00.y, p10.x, p10.y);
            g.drawLine(p10.x, p10.y, p11.x, p11.y);
            g.drawLine(p11.x, p11.y, p01.x, p01.y);
            g.drawLine(p01.x, p01.y, p00.x, p00.y);

            for (int i=0; i<_splines.size(); ++i) {
                CubicBSpline s = _splines.get(i);
                g.setColor(_colors.get(i));

                // Draw the control points, emphasizing every third.
                int n = s.getNumSegments();
                for (int j=0; j<n; ++j) {
                    drawPoint(g, getPoint(w, h, s.getControlPoint(j, 0)), j, 0, true);
                    drawPoint(g, getPoint(w, h, s.getControlPoint(j, 1)), j, 1, false);
                    drawPoint(g, getPoint(w, h, s.getControlPoint(j, 2)), j, 2, false);

                    if (n-1 == j) {
                        drawPoint(g, getPoint(w, h, s.getControlPoint(j, 3)), j, 3, true);
                    }
                }

                // Now draw the spline
                Point lastPt = null;
                for (double t=0.0; t<1.0; t+=0.01) {
                    Vector v = s.getPoint(t);
//                    System.out.println(String.format("%.4f: %s", t, v.toString()));
                    Point pt = getPoint(w, h, v);
                    if (null != lastPt) {
                        g.drawLine(lastPt.x, lastPt.y, pt.x, pt.y);
                    }
                    lastPt = pt;
                }
                Point pt = getPoint(w, h, s.getPoint(1.0));
                g.drawLine(lastPt.x, lastPt.y, pt.x, pt.y);
            }
        }

        private void drawPoint (Graphics g, Point pt, int sNum, int pNum, boolean emphasized) {
            g.drawArc(pt.x-3, pt.y-3, 7, 7, 0, 360);
            int w=3;
            if (emphasized) {
                g.drawArc(pt.x-6, pt.y-6, 13, 13, 0, 360);
                w=6;
            }
            g.drawLine(pt.x, pt.y-w, pt.x, pt.y+w);
            g.drawLine(pt.x-w, pt.y, pt.x+w, pt.y);
            g.drawString(String.format("P%d_%d", sNum, pNum), pt.x+9, pt.y+9);
        }
        private Point getPoint (int w, int h, Vector v) {
            double x = v.coord(0);
            double y= v.coord(1);
            x = (x+0.25)/1.5;
            y = (y+0.25)/1.5;
            return new Point((int) Math.round(w*x), (int) Math.round(h*y));
        }
    }
}
