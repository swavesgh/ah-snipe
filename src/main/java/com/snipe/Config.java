package com.snipe;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class Config {
    public static class Target {
        public String name; public double maxPrice;
        public Target(String n, double p) { name = n; maxPrice = p; }
    }

    public List<Target> targets = new ArrayList<>();
    public String command = "ah";                 // command that opens the auction house
    public String priceRegex = "(?i)price\\D*([0-9][0-9,]*\\.?[0-9]*)\\s*([kmbt]?)"; // group1 = number, group2 = suffix
    public String nextName = "next";              // name of the next-page button (contains, case-insens.)
    public String confirmName = "confirm";        // name of the confirm button (contains, case-insens.)
    public int delayTicks = 6;                    // ticks between clicks
    public int maxPages = 20;                     // pages scanned per refresh
    public int refreshTicks = 40;                 // wait between full scans
    public boolean pricePerItem = true;           // divide listing price by stack size

    public static Config I = new Config();
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("snipe.json"); }

    public static void load() {
        try { if (Files.exists(path())) I = GSON.fromJson(Files.readString(path()), Config.class); } catch (Exception e) { e.printStackTrace(); }
        if (I == null) I = new Config();
    }
    public static void save() { try { Files.writeString(path(), GSON.toJson(I)); } catch (Exception e) { e.printStackTrace(); } }

    public static double suffix(String s) {
        return switch (s.toLowerCase()) { case "k" -> 1e3; case "m" -> 1e6; case "b" -> 1e9; case "t" -> 1e12; default -> 1; };
    }
    /** Parses things like 16799111, 16,799,111, 16.8m, 2k. Returns -1 on failure. */
    public static double parseUser(String s) {
        Matcher m = Pattern.compile("([0-9][0-9,]*\\.?[0-9]*)\\s*([kKmMbBtT]?)").matcher(s.trim());
        if (!m.find()) return -1;
        try { return Double.parseDouble(m.group(1).replace(",", "")) * suffix(m.group(2)); } catch (Exception e) { return -1; }
    }
    public static String fmt(double v) {
        if (v >= 1e9) return String.format("%.2fB", v / 1e9);
        if (v >= 1e6) return String.format("%.2fM", v / 1e6);
        if (v >= 1e3) return String.format("%.1fK", v / 1e3);
        return String.format("%.0f", v);
    }
}
