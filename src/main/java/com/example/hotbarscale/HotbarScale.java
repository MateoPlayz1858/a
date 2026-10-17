package com.example.hotbarscale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = "hotbarscale", name = "HotbarScale", version = "1.0", clientSideOnly = true)
public class HotbarScale {
    private static final float MIN = 0.5f;
    private static final float MAX = 2.0f;
    private static final float STEP = 0.1f;

    private final Minecraft mc = Minecraft.getMinecraft();
    private Configuration config;
    private Property scaleProp;
    private float scale = 1.0f;
    private boolean pushed = false;

    private KeyBinding upKey;
    private KeyBinding downKey;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        config = new Configuration(e.getSuggestedConfigurationFile());
        scaleProp = config.get("general", "scale", 1.0, "Hotbar scale (0.5 - 2.0)", MIN, MAX);
        scale = clamp((float) scaleProp.getDouble());
        config.save();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        upKey = new KeyBinding("Hotbar Scale +", Keyboard.KEY_EQUALS, "HotbarScale");
        downKey = new KeyBinding("Hotbar Scale -", Keyboard.KEY_MINUS, "HotbarScale");
        ClientRegistry.registerKeyBinding(upKey);
        ClientRegistry.registerKeyBinding(downKey);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private float clamp(float v) {
        return Math.max(MIN, Math.min(MAX, v));
    }

    private void setScale(float v) {
        scale = clamp(Math.round(v * 10f) / 10f);
        scaleProp.set((double) scale);
        config.save();
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(
                    "\u00a77[\u00a7bHotbarScale\u00a77] \u00a7f" + scale + "x"));
        }
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        if (upKey.isPressed()) setScale(scale + STEP);
        if (downKey.isPressed()) setScale(scale - STEP);
    }

    // Runs last, so if another mod cancelled the hotbar we don't touch the matrix.
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onPre(RenderGameOverlayEvent.Pre e) {
        if (e.type != RenderGameOverlayEvent.ElementType.HOTBAR) return;
        if (e.isCanceled() || scale == 1.0f) return;

        // Scale around the bottom-center of the screen, where the hotbar sits.
        float cx = e.resolution.getScaledWidth() / 2.0f;
        float bottom = e.resolution.getScaledHeight();
        GlStateManager.pushMatrix();
        GlStateManager.translate(cx, bottom, 0.0f);
        GlStateManager.scale(scale, scale, 1.0f);
        GlStateManager.translate(-cx, -bottom, 0.0f);
        pushed = true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPost(RenderGameOverlayEvent.Post e) {
        if (e.type != RenderGameOverlayEvent.ElementType.HOTBAR) return;
        if (pushed) {
            GlStateManager.popMatrix();
            pushed = false;
        }
    }
}
