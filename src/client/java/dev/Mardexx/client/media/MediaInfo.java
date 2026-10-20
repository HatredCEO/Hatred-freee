package dev.Mardexx.client.media;

public record MediaInfo(
		boolean ok,
		boolean hasSession,
		String title,
		String artist,
		String album,
		String coverBase64,
		String sourceApp,
		String status,
		boolean playing,
		long positionSeconds,
		long durationSeconds,
		String error
) {
	public static final MediaInfo EMPTY = new MediaInfo(true, false, "", "", "", "", "", "", false, 0L, 0L, "");

	public boolean displayable() {
		return ok && hasSession && (!title.isBlank() || !artist.isBlank());
	}

	public String displayTitle() {
		return title.isBlank() ? "Unknown title" : title;
	}

	public String displayArtist() {
		if (!artist.isBlank()) {
			return artist;
		}
		return sourceApp.isBlank() ? "Unknown artist" : friendlySource();
	}

	public String friendlySource() {
		String source = sourceApp == null ? "" : sourceApp.toLowerCase();
		if (source.contains("spotify")) return "Spotify";
		if (source.contains("chrome")) return "Chrome";
		if (source.contains("msedge")) return "Edge";
		if (source.contains("firefox")) return "Firefox";
		if (source.contains("opera")) return "Opera";
		if (source.contains("youtube")) return "YouTube";
		if (sourceApp == null || sourceApp.isBlank()) return "Media";
		int dot = sourceApp.lastIndexOf('.');
		return dot >= 0 && dot + 1 < sourceApp.length() ? sourceApp.substring(dot + 1) : sourceApp;
	}
}
