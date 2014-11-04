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
package com.oculusinfo.tile.rest;

import org.restlet.data.MediaType;
import org.restlet.representation.OutputRepresentation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author dgray
 */
public class ImageOutputRepresentation extends OutputRepresentation {
	private BufferedImage _image;
	
	/**
	 * @param mediaType
	 * @param image
	 */
	public ImageOutputRepresentation(MediaType mediaType, BufferedImage image) {
		super(mediaType);

		_image = image;
	}

	/* (non-Javadoc)
	 * @see org.restlet.representation.Representation#write(java.io.OutputStream)
	 */
	@Override
	public void write(OutputStream outputStream) throws IOException {
		// TODO: create a constant map of MediaType to ImageIO file type for the "png".
		ImageIO.write(_image, "png", outputStream);
	}
}
