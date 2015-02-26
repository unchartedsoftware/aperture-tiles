/*
 * Copyright (c) 2015 Oculus Info Inc.
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
package com.oculusinfo.twitter.binning;

import java.io.Serializable;

public class RecentTweet implements Serializable {
	private final String text;
	private final long time;
	private final String user;
	private final String sentiment;

	public RecentTweet(String text, long time, String user, String sentiment) {
		this.text = text;
		this.time = time;
		this.user = user;
		this.sentiment = sentiment;
	}

	public String getText() {
		return text;
	}

	public long getTime() {
		return time;
	}

	public String getUser() {
		return user;
	}

	public String getSentiment() {
		return sentiment;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		RecentTweet that = (RecentTweet) o;

		if (time != that.time) return false;
		if (sentiment != null ? !sentiment.equals(that.sentiment) : that.sentiment != null) return false;
		if (text != null ? !text.equals(that.text) : that.text != null) return false;
		if (user != null ? !user.equals(that.user) : that.user != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = text != null ? text.hashCode() : 0;
		result = 31 * result + (int) (time ^ (time >>> 32));
		result = 31 * result + (user != null ? user.hashCode() : 0);
		result = 31 * result + (sentiment != null ? sentiment.hashCode() : 0);
		return result;
	}
}
