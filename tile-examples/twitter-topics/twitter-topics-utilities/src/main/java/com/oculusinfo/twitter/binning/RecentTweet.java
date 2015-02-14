package com.oculusinfo.twitter.binning;

public class RecentTweet {
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
