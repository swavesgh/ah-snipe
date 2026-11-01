package com.snipe;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import java.util.*;
import java.util.regex.*;

public class Sniper {
    enum State { OPEN, SCAN, CONFIRM, WAIT }
    public static boolean running = false;
    public static boolean buy = true;   // false = track prices only
    static State state = State.OPEN;
    static int cooldown, wait, page;
    static Map<String, Double> scanLow = new HashMap<>();

    public static void start(boolean buyMode) { buy = buyMode; running = true; state = State.OPEN; cooldown = 0; msg(buyMode ? "Sniping started" : "Tracking started"); }
    public static void stop() { running = false; msg("Stopped"); }

    static void msg(String s) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.sendMessage(Text.literal("[Sniper] " + s), false);
    }

    static double listingPrice(ItemStack st) {
        LoreComponent lore = st.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT);
        Pattern p = Pattern.compile(Config.I.priceRegex);
        for (Text line : lore.lines()) {
            Matcher m = p.matcher(line.getString());
            if (m.find()) {
                try {
                    double v = Double.parseDouble(m.group(1).replace(",", "")) * Config.suffix(m.groupCount() >= 2 && m.group(2) != null ? m.group(2) : "");
                    return Config.I.pricePerItem ? v / Math.max(1, st.getCount()) : v;
                } catch (Exception ignored) {}
            }
        }
        return -1;
    }

    static int findButton(GenericContainerScreenHandler h, String contains) {
        int size = h.getRows() * 9;
        for (int i = 0; i < size; i++) {
            ItemStack s = h.getSlot(i).getStack();
            if (!s.isEmpty() && s.getName().getString().toLowerCase().contains(contains.toLowerCase())) return i;
        }
        return -1;
    }

    static void click(MinecraftClient mc, GenericContainerScreenHandler h, int slot) {
        mc.interactionManager.clickSlot(h.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
        cooldown = Config.I.delayTicks;
    }

    public static void tick(MinecraftClient mc) {
        if (!running || mc.player == null || mc.getNetworkHandler() == null) return;
        if (cooldown > 0) { cooldown--; return; }

        switch (state) {
            case OPEN -> {
                scanLow.clear(); page = 0; wait = 0;
                mc.getNetworkHandler().sendChatCommand(Config.I.command);
                state = State.SCAN; cooldown = Config.I.delayTicks * 2;
            }
            case SCAN -> {
                if (!(mc.currentScreen instanceof GenericContainerScreen gs)) {
                    if (++wait > 80) { state = State.OPEN; }
                    return;
                }
                wait = 0;
                var h = gs.getScreenHandler();
                int size = h.getRows() * 9;
                for (int i = 0; i < size; i++) {
                    ItemStack st = h.getSlot(i).getStack();
                    if (st.isEmpty()) continue;
                    double unit = listingPrice(st);
                    if (unit < 0) continue;
                    String name = st.getName().getString();
                    scanLow.merge(name, unit, Math::min);
                    if (!buy) continue;
                    for (Config.Target t : Config.I.targets) {
                        if (name.toLowerCase().contains(t.name.toLowerCase()) && unit <= t.maxPrice) {
                            msg("Buying " + name + " for " + Config.fmt(unit) + " (limit " + Config.fmt(t.maxPrice) + ")");
                            click(mc, h, i);
                            state = State.CONFIRM; wait = 0;
                            return;
                        }
                    }
                }
                int next = findButton(h, Config.I.nextName);
                if (next >= 0 && page < Config.I.maxPages) { page++; click(mc, h, next); cooldown += 4; return; }
                // finished this scan
                scanLow.forEach(Market::record);
                mc.player.closeHandledScreen();
                state = State.WAIT; cooldown = Config.I.refreshTicks;
            }
            case CONFIRM -> {
                if (mc.currentScreen instanceof GenericContainerScreen gs) {
                    int c = findButton(gs.getScreenHandler(), Config.I.confirmName);
                    if (c >= 0) { click(mc, gs.getScreenHandler(), c); state = State.SCAN; cooldown += 8; return; }
                }
                if (++wait > 20) { state = State.SCAN; } // no confirm screen: assume instant buy
            }
            case WAIT -> state = State.OPEN;
        }
    }
}
