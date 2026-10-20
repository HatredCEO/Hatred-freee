package dev.Mardexx.client.media;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MediaSessionProvider {
	public static final MediaSessionProvider INSTANCE = new MediaSessionProvider();
	private static final long REFRESH_INTERVAL_MS = 100L;

	private final ExecutorService executor = Executors.newSingleThreadExecutor(new MediaThreadFactory());
	private final AtomicBoolean refreshRunning = new AtomicBoolean(false);
	private volatile MediaInfo current = MediaInfo.EMPTY;
	private volatile long positionAnchorMillis;
	private volatile long positionAnchorTimeMillis;
	private volatile long lastRawPositionMillis;
	private volatile long nextRefreshTime;

	private MediaSessionProvider() {
	}

	public MediaInfo getCurrent() {
		long now = System.currentTimeMillis();
		if (now >= nextRefreshTime && refreshRunning.compareAndSet(false, true)) {
			nextRefreshTime = now + REFRESH_INTERVAL_MS;
			executor.execute(() -> {
				try {
					MediaInfo fetched = NativeMediaInfo.getCurrentMedia();
					updatePositionAnchor(fetched);
				} finally {
					refreshRunning.set(false);
				}
			});
		}
		return current;
	}

	public synchronized long positionMillis(MediaInfo media) {
		long position;
		if (media == current) {
			position = positionAnchorMillis;
			if (media.playing()) {
				position += Math.max(0L, System.currentTimeMillis() - positionAnchorTimeMillis);
			}
		} else {
			position = Math.max(0L, media.positionSeconds()) * 1_000L;
		}
		long duration = Math.max(0L, media.durationSeconds()) * 1_000L;
		return duration > 0L ? Math.min(position, duration) : position;
	}

	private synchronized void updatePositionAnchor(MediaInfo fetched) {
		long now = System.currentTimeMillis();
		long reportedPosition = Math.max(0L, fetched.positionSeconds()) * 1_000L;
		long anchoredPosition = reportedPosition;
		MediaInfo previous = current;
		if (sameTrack(previous, fetched) && previous.playing() && fetched.playing()) {
			long extrapolated = positionAnchorMillis + Math.max(0L, now - positionAnchorTimeMillis);
			boolean realBackwardSeek = reportedPosition + 1_500L < lastRawPositionMillis;
			if (!realBackwardSeek) {
				// A stale rounded Windows timestamp must never move a playing track backwards.
				anchoredPosition = Math.max(reportedPosition, extrapolated);
			}
		}
		positionAnchorMillis = anchoredPosition;
		positionAnchorTimeMillis = now;
		lastRawPositionMillis = reportedPosition;
		current = fetched;
	}

	private static boolean sameTrack(MediaInfo first, MediaInfo second) {
		return first.displayable()
				&& second.displayable()
				&& first.title().equals(second.title())
				&& first.artist().equals(second.artist());
	}

	public void close() {
		executor.shutdownNow();
	}

	private static final class MediaThreadFactory implements ThreadFactory {
		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(runnable, "HatredFree-MediaSession");
			thread.setDaemon(true);
			return thread;
		}
	}
}
