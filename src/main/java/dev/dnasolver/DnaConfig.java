package dev.dnasolver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Plain JSON config at config/dna-analyzer-solver.json. Edit the file and restart the game. */
public final class DnaConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static DnaConfig instance;

    public boolean enabled = true;
    public boolean useMiddleClick = true;
    public boolean blockAccidentalClosing = true;
    public boolean hideTooltips = true;
    /**
     * SkyHanni reads this flag from its online repo. If the first and last strands
     * of the board can also be rearranged in the game, set this to true.
     */
    public boolean allowEnds = false;

    public static DnaConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static DnaConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("dna-analyzer-solver.json");
        DnaConfig cfg = null;
        if (Files.exists(path)) {
            try (Reader r = Files.newBufferedReader(path)) {
                cfg = GSON.fromJson(r, DnaConfig.class);
            } catch (Exception e) {
                System.err.println("[DnaAnalyzerSolver] Could not read config, using defaults: " + e);
            }
        }
        if (cfg == null) {
            cfg = new DnaConfig();
        }
        try (Writer w = Files.newBufferedWriter(path)) {
            GSON.toJson(cfg, w);
        } catch (IOException e) {
            System.err.println("[DnaAnalyzerSolver] Could not write config: " + e);
        }
        return cfg;
    }
}
