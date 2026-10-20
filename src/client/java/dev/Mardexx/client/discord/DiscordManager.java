package dev.Mardexx.client.discord;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DiscordManager {
	private static final DiscordManager INSTANCE = new DiscordManager();
	private static final String[] CLIENT_IDS = {
			"383226320970055681", // VS Code (verified Discord IPC client)
			"1064560737229750372",
			"729388305096572979",
			"909873979808448552",
			"483281987570532352"
	};
	private static final ResourceLocation AVATAR_LOCATION = ResourceLocation.fromNamespaceAndPath("modid", "textures/discord_avatar.png");
	private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "Discord-IPC-Thread");
		t.setDaemon(true);
		return t;
	});

	private volatile String discordUsername = null;
	private volatile String discordAvatarUrl = null;
	private volatile boolean avatarLoaded = false;
	private DynamicTexture avatarTexture = null;

	private DiscordManager() {
		init();
	}

	public static DiscordManager getInstance() {
		return INSTANCE;
	}

	public void init() {
		executor.submit(this::connectDiscordIpc);
	}

	public String getUsername() {
		if (discordUsername != null && !discordUsername.isBlank()) {
			return discordUsername;
		}
		Minecraft mc = Minecraft.getInstance();
		return mc.getUser() != null ? mc.getUser().getName() : "Player";
	}

	public ResourceLocation getAvatarLocation() {
		return avatarLoaded ? AVATAR_LOCATION : null;
	}

	public boolean hasAvatar() {
		return avatarLoaded && avatarTexture != null;
	}

	private void connectDiscordIpc() {
		for (String clientId : CLIENT_IDS) {
			for (int i = 0; i < 10; i++) {
				File pipeFile = new File("\\\\.\\pipe\\discord-ipc-" + i);
				if (!pipeFile.exists()) {
					continue;
				}
				try (RandomAccessFile pipe = new RandomAccessFile(pipeFile, "rw")) {
					String handshakeJson = "{\"v\":1,\"client_id\":\"" + clientId + "\"}";
					byte[] jsonBytes = handshakeJson.getBytes(java.nio.charset.StandardCharsets.UTF_8);

					ByteBuffer sendBuffer = ByteBuffer.allocate(8 + jsonBytes.length).order(ByteOrder.LITTLE_ENDIAN);
					sendBuffer.putInt(0); // HANDSHAKE opcode
					sendBuffer.putInt(jsonBytes.length);
					sendBuffer.put(jsonBytes);
					pipe.write(sendBuffer.array());

					byte[] headerBytes = new byte[8];
					pipe.readFully(headerBytes);
					ByteBuffer headerBuffer = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN);
					int opcode = headerBuffer.getInt();
					int length = headerBuffer.getInt();

					if (length > 0 && length < 1024 * 1024) {
						byte[] responseBytes = new byte[length];
						pipe.readFully(responseBytes);
						String responseJson = new String(responseBytes, java.nio.charset.StandardCharsets.UTF_8);
						if (parseReadyEvent(responseJson)) {
							return;
						}
					}
				} catch (Throwable ignored) {
				}
			}
		}
	}

	private boolean parseReadyEvent(String json) {
		try {
			JsonObject root = JsonParser.parseString(json).getAsJsonObject();
			if (root.has("data")) {
				JsonObject data = root.getAsJsonObject("data");
				if (data.has("user")) {
					JsonObject user = data.getAsJsonObject("user");
					String id = user.has("id") ? user.get("id").getAsString() : null;
					String username = user.has("username") ? user.get("username").getAsString() : null;
					String globalName = user.has("global_name") && !user.get("global_name").isJsonNull()
							? user.get("global_name").getAsString() : null;
					String avatarHash = user.has("avatar") && !user.get("avatar").isJsonNull()
							? user.get("avatar").getAsString() : null;

					this.discordUsername = globalName != null && !globalName.isBlank() ? globalName : username;
					if (id != null) {
						if (avatarHash != null) {
							this.discordAvatarUrl = "https://cdn.discordapp.com/avatars/" + id + "/" + avatarHash + ".png?size=128";
						} else {
							this.discordAvatarUrl = "https://cdn.discordapp.com/embed/avatars/0.png";
						}
						downloadAvatar(this.discordAvatarUrl);
					}
					return true;
				}
			}
		} catch (Throwable ignored) {
		}
		return false;
	}

	private void downloadAvatar(String urlStr) {
		try {
			URI uri = URI.create(urlStr);
			HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);
			conn.connect();

			if (conn.getResponseCode() == 200) {
				try (InputStream in = conn.getInputStream()) {
					BufferedImage src = ImageIO.read(in);
					if (src != null) {
						int size = Math.min(src.getWidth(), src.getHeight());
						BufferedImage circleImg = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
						Graphics2D g2 = circleImg.createGraphics();
						g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
						g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
						g2.setClip(new Ellipse2D.Float(0, 0, size, size));
						g2.drawImage(src, 0, 0, size, size, null);
						g2.dispose();

						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ImageIO.write(circleImg, "PNG", baos);
						ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
						NativeImage nativeImage = NativeImage.read(bais);

						Minecraft.getInstance().execute(() -> {
							try {
								if (avatarTexture != null) {
									avatarTexture.close();
								}
								avatarTexture = new DynamicTexture(nativeImage);
								Minecraft.getInstance().getTextureManager().register(AVATAR_LOCATION, avatarTexture);
								avatarLoaded = true;
							} catch (Throwable ignored) {
							}
						});
					}
				}
			}
		} catch (Throwable ignored) {
		}
	}
}
