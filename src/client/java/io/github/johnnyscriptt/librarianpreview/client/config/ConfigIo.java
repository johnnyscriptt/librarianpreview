package io.github.johnnyscriptt.librarianpreview.client.config;

import io.github.johnnyscriptt.librarianpreview.LibrarianPreviewMod;
import io.github.johnnyscriptt.librarianpreview.core.config.LibrarianPreviewConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Loads and saves the single {@link LibrarianPreviewConfig} instance at config/librarianpreview.json. */
public final class ConfigIo {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static LibrarianPreviewConfig config;

    private ConfigIo() {
    }

    public static synchronized LibrarianPreviewConfig get() {
        if (config == null) {
            config = load();
        }
        return config;
    }

    public static synchronized void save() {
        if (config == null) {
            return;
        }
        config.clamp();
        try {
            Files.writeString(path(), GSON.toJson(config));
        } catch (IOException e) {
            LibrarianPreviewMod.LOGGER.warn("Could not save librarianpreview.json", e);
        }
    }

    private static LibrarianPreviewConfig load() {
        Path file = path();
        if (Files.exists(file)) {
            try {
                LibrarianPreviewConfig loaded = GSON.fromJson(Files.readString(file), LibrarianPreviewConfig.class);
                if (loaded != null) {
                    loaded.clamp();
                    return loaded;
                }
            } catch (Exception e) {
                LibrarianPreviewMod.LOGGER.warn("Could not read librarianpreview.json, using defaults", e);
            }
        }
        return new LibrarianPreviewConfig();
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("librarianpreview.json");
    }
}
