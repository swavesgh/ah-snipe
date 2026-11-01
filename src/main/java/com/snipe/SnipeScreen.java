package com.snipe;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class SnipeScreen extends Screen {
    static int selected = -1, listPage = 0, window = 1; // window: 0 hour, 1 24h, 2 7d
    static final long[] WINDOWS = {3600_000L, 86400_000L, 7 * 86400_000L};
    TextFieldWidget nameField, priceField;
    int x0, y0;

    public SnipeScreen() { super(Text.literal("AH Sniper")); }

    @Override protected void init() {
        x0 = width / 2 - 200; y0 = height / 2 - 120;
        addDrawableChild(ButtonWidget.builder(Text.literal(Sniper.running && Sniper.buy ? "Stop" : "Start sniping"), b -> {
            if (Sniper.running) Sniper.stop(); else Sniper.start(true); rebuild(); }).dimensions(x0, y0, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(Sniper.running && !Sniper.buy ? "Stop" : "Track only"), b -> {
            if (Sniper.running) Sniper.stop(); else Sniper.start(false); rebuild(); }).dimensions(x0 + 105, y0, 90, 20).build());
        String[] w = {"Past hour", "Past 24h", "Past 7 days"};
        for (int i = 0; i < 3; i++) { int idx = i;
            addDrawableChild(ButtonWidget.builder(Text.literal(w[i]), b -> window = idx).dimensions(x0 + 205 + i * 66, y0, 64, 20).build()); }

        var t = Config.I.targets;
        for (int i = 0; i < 8; i++) {
            int idx = listPage * 8 + i; if (idx >= t.size()) break;
            var tg = t.get(idx);
            addDrawableChild(ButtonWidget.builder(Text.literal((idx == selected ? "> " : "") + tg.name + " (" + Config.fmt(tg.maxPrice) + ")"),
                b -> { selected = idx; rebuild(); }).dimensions(x0, y0 + 28 + i * 20, 195, 18).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { if (listPage > 0) { listPage--; rebuild(); } }).dimensions(x0, y0 + 192, 20, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { if ((listPage + 1) * 8 < t.size()) { listPage++; rebuild(); } }).dimensions(x0 + 24, y0 + 192, 20, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Remove selected"), b -> {
            if (selected >= 0 && selected < t.size()) { t.remove(selected); selected = -1; Config.save(); rebuild(); } }).dimensions(x0 + 48, y0 + 192, 100, 18).build());

        nameField = new TextFieldWidget(textRenderer, x0 + 205, y0 + 150, 195, 18, Text.literal("Item"));
        nameField.setPlaceholder(Text.literal("Item name"));
        priceField = new TextFieldWidget(textRenderer, x0 + 205, y0 + 172, 100, 18, Text.literal("Price"));
        priceField.setPlaceholder(Text.literal("Snipe under (e.g. 16.8m)"));
        addDrawableChild(nameField); addDrawableChild(priceField);
        addDrawableChild(ButtonWidget.builder(Text.literal("Add to snipes"), b -> {
            double p = Config.parseUser(priceField.getText());
            if (!nameField.getText().isBlank() && p > 0) {
                t.add(new Config.Target(nameField.getText().trim(), p)); selected = t.size() - 1; Config.save(); rebuild();
            } }).dimensions(x0 + 310, y0 + 172, 90, 18).build());
    }

    void rebuild() { clearChildren(); init(); }

    @Override public void render(DrawContext c, int mx, int my, float delta) {
        c.fill(x0 - 8, y0 - 8, x0 + 408, y0 + 218, 0xE0101018);
        super.render(c, mx, my, delta);
        int tx = x0 + 205, ty = y0 + 32;
        var t = Config.I.targets;
        String status = Sniper.running ? (Sniper.buy ? "SNIPING" : "TRACKING") : "idle";
        c.drawTextWithShadow(textRenderer, "Status: " + status, tx, ty, Sniper.running ? 0xFF55FF55 : 0xFFAAAAAA);
        if (selected >= 0 && selected < t.size()) {
            var tg = t.get(selected);
            c.drawTextWithShadow(textRenderer, tg.name, tx, ty + 16, 0xFF55FFFF);
            c.drawTextWithShadow(textRenderer, "Snipe under: " + Config.fmt(tg.maxPrice), tx, ty + 28, 0xFFFFFFFF);
            var s = Market.stats(tg.name, WINDOWS[window]);
            if (s == null) c.drawTextWithShadow(textRenderer, "No market data yet - run Track only", tx, ty + 44, 0xFFFFAA00);
            else {
                c.drawTextWithShadow(textRenderer, "Lowest:  " + Config.fmt(s.low()), tx, ty + 44, 0xFF55FF55);
                c.drawTextWithShadow(textRenderer, "Average: " + Config.fmt(s.avg()), tx, ty + 56, 0xFFFFFF55);
                c.drawTextWithShadow(textRenderer, "Median:  " + Config.fmt(s.median()), tx, ty + 68, 0xFFFFFFFF);
                c.drawTextWithShadow(textRenderer, "Highest: " + Config.fmt(s.high()), tx, ty + 80, 0xFFFFFFFF);
                c.drawTextWithShadow(textRenderer, "Samples: " + s.n(), tx, ty + 92, 0xFFAAAAAA);
            }
        } else c.drawTextWithShadow(textRenderer, "Select a snipe to see market stats", tx, ty + 16, 0xFFAAAAAA);
    }

    @Override public boolean shouldPause() { return false; }
}
