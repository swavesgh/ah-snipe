package com.snipe;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class SnipeMod implements ClientModInitializer {
    public static KeyBinding openKey;

    @Override public void onInitializeClient() {
        Config.load(); Market.load();
        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of("snipe", "main"));
        openKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.snipe.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, cat));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.wasPressed()) mc.setScreen(new SnipeScreen());
            Sniper.tick(mc);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> { Config.save(); Market.save(); });
    }
}
