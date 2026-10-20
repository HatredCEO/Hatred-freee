package dev.Mardexx.client.media;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public final class NativeMediaInfo {
	private static final String RESOURCE_PATH = "/win32-x86-64/rabbids-media.dll";
	private static final Object LOAD_LOCK = new Object();
	private static volatile MediaLibrary library;
	private static volatile boolean loadAttempted;
	private static volatile String loadError = "";

	private NativeMediaInfo() {
	}

	public static MediaInfo getCurrentMedia() {
		MediaLibrary loaded = library();
		if (loaded == null) {
			return new MediaInfo(false, false, "", "", "", "", "", "", false, 0L, 0L, loadError);
		}

		try {
			Pointer pointer = loaded.GetCurrentMediaJson();
			if (pointer == null) {
				return new MediaInfo(false, false, "", "", "", "", "", "", false, 0L, 0L, "native returned null");
			}
			return parse(pointer.getString(0, StandardCharsets.UTF_8.name()));
		} catch (Throwable throwable) {
			String message = throwable.getMessage();
			return new MediaInfo(false, false, "", "", "", "", "", "", false, 0L, 0L,
					message == null ? throwable.getClass().getSimpleName() : message);
		}
	}

	public static boolean control(String action) {
		MediaLibrary loaded = library();
		if (loaded == null || action == null || action.isBlank()) {
			return false;
		}

		try {
			return loaded.ControlCurrentMedia(action.toLowerCase(Locale.ROOT)) != 0;
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static MediaLibrary library() {
		if (library != null || loadAttempted) {
			return library;
		}

		synchronized (LOAD_LOCK) {
			if (library != null || loadAttempted) {
				return library;
			}

			loadAttempted = true;
			try {
				library = Native.load(extractDll().toString(), MediaLibrary.class);
			} catch (Throwable throwable) {
				loadError = throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
				library = null;
			}
			return library;
		}
	}

	private static Path extractDll() throws IOException {
		InputStream stream = NativeMediaInfo.class.getResourceAsStream(RESOURCE_PATH);
		if (stream == null) {
			Path devPath = Path.of("src", "client", "resources", "win32-x86-64", "rabbids-media.dll").toAbsolutePath();
			if (Files.exists(devPath)) {
				return devPath;
			}
			throw new IOException("rabbids-media.dll missing in " + RESOURCE_PATH);
		}

		Path directory = Path.of(
				System.getProperty("java.io.tmpdir"),
				"hatred-free-native",
				Long.toString(ProcessHandle.current().pid())
		);
		Files.createDirectories(directory);
		Path dll = directory.resolve("rabbids-media.dll");
		try (stream) {
			Files.copy(stream, dll, StandardCopyOption.REPLACE_EXISTING);
		}
		directory.toFile().deleteOnExit();
		dll.toFile().deleteOnExit();
		return dll;
	}

	private static MediaInfo parse(String json) {
		JsonObject object = JsonParser.parseString(json).getAsJsonObject();
		return new MediaInfo(
				getBoolean(object, "ok"),
				getBoolean(object, "hasSession"),
				getString(object, "title"),
				getString(object, "artist"),
				getString(object, "album"),
				getString(object, "coverBase64"),
				getString(object, "sourceApp"),
				getString(object, "status"),
				getBoolean(object, "isPlaying"),
				getLong(object, "positionSeconds"),
				getLong(object, "durationSeconds"),
				getString(object, "error")
		);
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

	private interface MediaLibrary extends StdCallLibrary {
		Pointer GetCurrentMediaJson();

		int ControlCurrentMedia(String action);
	}
}
