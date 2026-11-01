package com.snipe;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.util.*;

/** Stores the lowest unit price seen for each item name on every scan. */
public class Market {
    static Map<String, List<double[]>> data = new HashMap<>(); // name -> [timeMs, lowestPrice]
    static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("snipe_market.json"); }

    public static void load() {
        try {
            if (Files.exists(path())) data = new Gson().fromJson(Files.readString(path()),
                new TypeToken<Map<String, List<double[]>>>() {}.getType());
        } catch (Exception e) { e.printStackTrace(); }
        if (data == null) data = new HashMap<>();
    }
    public static void save() { try { Files.writeString(path(), new Gson().toJson(data)); } catch (Exception e) { e.printStackTrace(); } }

    public static void record(String name, double price) {
        List<double[]> l = data.computeIfAbsent(name.toLowerCase(), k -> new ArrayList<>());
        long now = System.currentTimeMillis();
        l.add(new double[]{now, price});
        long cutoff = now - 7L * 24 * 3600_000;
        l.removeIf(s -> s[0] < cutoff);
    }

    public record Stats(double low, double avg, double median, double high, int n) {}

    /** Stats over every item whose name contains `name`, within the last windowMs. */
    public static Stats stats(String name, long windowMs) {
        long cutoff = System.currentTimeMillis() - windowMs;
        List<Double> v = new ArrayList<>();
        for (var e : data.entrySet())
            if (e.getKey().contains(name.toLowerCase()))
                for (double[] s : e.getValue()) if (s[0] >= cutoff) v.add(s[1]);
        if (v.isEmpty()) return null;
        Collections.sort(v);
        double sum = 0; for (double d : v) sum += d;
        return new Stats(v.get(0), sum / v.size(), v.get(v.size() / 2), v.get(v.size() - 1), v.size());
    }
}
