package dev.Mardexx.client.media;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LyricsProvider {
	public static final LyricsProvider INSTANCE = new LyricsProvider();
	private static final String API_URL = "https://lrclib.net/api";
	private static final String USER_AGENT = "Hatred-Free/1.0.0 (https://github.com/HatredCEO/Hatred-Free)";
	private static final long SYNC_LEAD_MILLIS = 450L;
	private static final Pattern TIMESTAMP = Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]");
	private static final Pattern OFFSET = Pattern.compile("\\[offset:([+-]?\\d+)]", Pattern.CASE_INSENSITIVE);

	private final HttpClient client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5L))
			.build();
	private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "HatredFree-Lyrics");
		thread.setDaemon(true);
		return thread;
	});

	private volatile String activeKey = "";
	private volatile LyricsData activeLyrics = LyricsData.message("", "Lyrics unavailable");

	private LyricsProvider() {
	}

	public LyricsDisplay display(MediaInfo media, long positionMillis) {
		String key = key(media);
		if (!key.equals(activeKey)) {
			request(key, media);
		}
		LyricsData lyrics = activeLyrics;
		if (!key.equals(lyrics.key())) {
			return new LyricsDisplay("", "Loading lyrics...", "");
		}
		return lyrics.at(positionMillis + SYNC_LEAD_MILLIS, media.durationSeconds());
	}

	public void close() {
		executor.shutdownNow();
	}

	private synchronized void request(String key, MediaInfo media) {
		if (key.equals(activeKey)) {
			return;
		}
		activeKey = key;
		activeLyrics = LyricsData.message(key, "Loading lyrics...");
		executor.execute(() -> {
			LyricsData loaded = fetch(key, media);
			if (key.equals(activeKey)) {
				activeLyrics = loaded;
			}
		});
	}

	private LyricsData fetch(String key, MediaInfo media) {
		try {
			HttpResponse<String> response = send(getUri(media));
			if (response.statusCode() == 200) {
				return parse(key, JsonParser.parseString(response.body()).getAsJsonObject());
			}
			if (response.statusCode() == 404) {
				Thread.sleep(300L);
				HttpResponse<String> search = send(searchUri(media));
				if (search.statusCode() == 200) {
					JsonObject result = bestResult(JsonParser.parseString(search.body()).getAsJsonArray(), media);
					if (result != null) {
						return parse(key, result);
					}
				}
			}
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		} catch (Throwable ignored) {
		}
		return LyricsData.message(key, "Lyrics unavailable");
	}

	private HttpResponse<String> send(URI uri) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(uri)
				.timeout(Duration.ofSeconds(10L))
				.header("Accept", "application/json")
				.header("User-Agent", USER_AGENT)
				.GET()
				.build();
		return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
	}

	private static URI getUri(MediaInfo media) {
		return URI.create(API_URL + "/get?track_name=" + encode(media.displayTitle())
				+ "&artist_name=" + encode(media.displayArtist())
				+ "&album_name=" + encode(media.album())
				+ "&duration=" + Math.max(0L, media.durationSeconds()));
	}

	private static URI searchUri(MediaInfo media) {
		return URI.create(API_URL + "/search?track_name=" + encode(media.displayTitle())
				+ "&artist_name=" + encode(media.displayArtist()));
	}

	private static String encode(String value) {
		return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
	}

	private static JsonObject bestResult(JsonArray results, MediaInfo media) {
		if (results == null || results.isEmpty()) {
			return null;
		}
		List<JsonObject> candidates = new ArrayList<>();
		for (JsonElement element : results) {
			if (element.isJsonObject()) {
				candidates.add(element.getAsJsonObject());
			}
		}
		return candidates.stream()
				.min(Comparator.comparingLong(candidate -> matchScore(candidate, media)))
				.orElse(null);
	}

	private static long matchScore(JsonObject candidate, MediaInfo media) {
		long score = Math.abs(getLong(candidate, "duration") - media.durationSeconds());
		if (!getString(candidate, "trackName").equalsIgnoreCase(media.displayTitle())) {
			score += 10_000L;
		}
		if (!getString(candidate, "artistName").equalsIgnoreCase(media.displayArtist())) {
			score += 5_000L;
		}
		return score;
	}

	private static LyricsData parse(String key, JsonObject object) {
		if (getBoolean(object, "instrumental")) {
			return LyricsData.message(key, "Instrumental");
		}
		List<LyricsLine> synced = parseSynced(getString(object, "syncedLyrics"));
		if (!synced.isEmpty()) {
			long durationMillis = Math.max(0L, getLong(object, "duration")) * 1_000L;
			return new LyricsData(key, buildTimedWords(synced, durationMillis), List.of(), "");
		}
		String plainLyrics = getString(object, "plainLyrics").strip();
		List<String> plainWords = plainLyrics.isBlank() ? List.of() : List.of(plainLyrics.split("\\s+"));
		return plainWords.isEmpty()
				? LyricsData.message(key, "Lyrics unavailable")
				: new LyricsData(key, List.of(), plainWords, "");
	}

	private static List<LyricsLine> parseSynced(String lyrics) {
		if (lyrics == null || lyrics.isBlank()) {
			return List.of();
		}
		List<LyricsLine> lines = new ArrayList<>();
		long offsetMillis = parseOffset(lyrics);
		for (String rawLine : lyrics.lines().toList()) {
			Matcher matcher = TIMESTAMP.matcher(rawLine);
			List<Long> timestamps = new ArrayList<>();
			int textStart = 0;
			while (matcher.find()) {
					timestamps.add(Math.max(0L, timestampMillis(matcher) + offsetMillis));
				textStart = matcher.end();
			}
			String text = rawLine.substring(Math.min(textStart, rawLine.length())).strip();
			if (!text.isBlank()) {
				for (long timestamp : timestamps) {
					lines.add(new LyricsLine(timestamp, text));
				}
			}
		}
		lines.sort(Comparator.comparingLong(LyricsLine::timestampMillis));
		return List.copyOf(lines);
	}

	private static List<LyricsWord> buildTimedWords(List<LyricsLine> lines, long durationMillis) {
		List<LyricsWord> words = new ArrayList<>();
		for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
			LyricsLine line = lines.get(lineIndex);
			String[] lineWords = line.text().split("\\s+");
			long start = line.timestampMillis();
			long fallbackEnd = start + Math.max(1_000L, lineWords.length * 350L);
			long end = lineIndex + 1 < lines.size()
					? lines.get(lineIndex + 1).timestampMillis()
					: durationMillis > start ? durationMillis : fallbackEnd;
			if (end <= start) {
				end = fallbackEnd;
			}
			long span = end - start;
			for (int wordIndex = 0; wordIndex < lineWords.length; wordIndex++) {
				String word = lineWords[wordIndex].strip();
				if (!word.isBlank()) {
					long timestamp = start + span * wordIndex / Math.max(1, lineWords.length);
					words.add(new LyricsWord(timestamp, word));
				}
			}
		}
		words.sort(Comparator.comparingLong(LyricsWord::timestampMillis));
		return List.copyOf(words);
	}

	private static long parseOffset(String lyrics) {
		Matcher matcher = OFFSET.matcher(lyrics);
		if (!matcher.find()) {
			return 0L;
		}
		try {
			return Long.parseLong(matcher.group(1));
		} catch (NumberFormatException ignored) {
			return 0L;
		}
	}

	private static long timestampMillis(Matcher matcher) {
		long minutes = Long.parseLong(matcher.group(1));
		long seconds = Long.parseLong(matcher.group(2));
		String fraction = matcher.group(3);
		long millis = 0L;
		if (fraction != null) {
			millis = switch (fraction.length()) {
				case 1 -> Long.parseLong(fraction) * 100L;
				case 2 -> Long.parseLong(fraction) * 10L;
				default -> Long.parseLong(fraction.substring(0, Math.min(3, fraction.length())));
			};
		}
		return (minutes * 60L + seconds) * 1_000L + millis;
	}

	private static String key(MediaInfo media) {
		return media.displayTitle() + '\u0000' + media.displayArtist() + '\u0000' + media.album() + '\u0000' + media.durationSeconds();
	}

	private static String getString(JsonObject object, String key) {
		return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
	}

	private static boolean getBoolean(JsonObject object, String key) {
		return object.has(key) && !object.get(key).isJsonNull() && object.get(key).getAsBoolean();
	}

	private static long getLong(JsonObject object, String key) {
		return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsLong() : 0L;
	}

	public record LyricsDisplay(String previousLine, String currentLine, String nextLine) {
	}

	private record LyricsLine(long timestampMillis, String text) {
	}

	private record LyricsWord(long timestampMillis, String text) {
	}

	private record LyricsData(String key, List<LyricsWord> syncedWords, List<String> plainWords, String message) {
		private static LyricsData message(String key, String message) {
			return new LyricsData(key, List.of(), List.of(), message);
		}

		private LyricsDisplay at(long positionMillis, long durationSeconds) {
			if (!message.isBlank()) {
				return new LyricsDisplay("", message, "");
			}
			if (!syncedWords.isEmpty()) {
				positionMillis = Math.max(0L, positionMillis);
				int currentWord = 0;
				for (int index = 0; index < syncedWords.size(); index++) {
					if (syncedWords.get(index).timestampMillis() > positionMillis) {
						break;
					}
					currentWord = index;
				}
				int groupStart = currentWord / 5 * 5;
				return new LyricsDisplay(
						joinTimedWords(syncedWords, groupStart - 5),
						joinTimedWords(syncedWords, groupStart),
						joinTimedWords(syncedWords, groupStart + 5)
				);
			}
			float progress = durationSeconds <= 0L ? 0.0F : Math.min(1.0F, positionMillis / (durationSeconds * 1_000.0F));
			int currentWord = Math.min(plainWords.size() - 1, Math.max(0, (int) (progress * plainWords.size())));
			int groupStart = currentWord / 5 * 5;
			return new LyricsDisplay(
					joinWords(plainWords, groupStart - 5),
					joinWords(plainWords, groupStart),
					joinWords(plainWords, groupStart + 5)
			);
		}

		private static String joinTimedWords(List<LyricsWord> words, int start) {
			if (start < 0 || start >= words.size()) {
				return "";
			}
			return words.subList(start, Math.min(start + 5, words.size())).stream()
					.map(LyricsWord::text)
					.reduce((first, second) -> first + " " + second)
					.orElse("");
		}

		private static String joinWords(List<String> words, int start) {
			if (start < 0 || start >= words.size()) {
				return "";
			}
			return String.join(" ", words.subList(start, Math.min(start + 5, words.size())));
		}
	}
}
