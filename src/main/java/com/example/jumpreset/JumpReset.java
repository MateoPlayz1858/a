package com.example.jumpreset;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

/**
 * Jump Reset for Minecraft 1.8.9 (Forge 11.15.1.x)
 *
 * When you take a hit while standing on the ground, you jump on the exact tick the
 * knockback is applied. That is the whole point of a jump reset: you leave the ground
 * with your velocity intact instead of eating full ground-friction knockback.
 *
 * How the timing works:
 *  - The server sends the hurt status packet (S19) and the velocity packet (S12).
 *    Both are processed on the main thread, which sets hurtTime = maxHurtTime (10)
 *    and overwrites motionX/Y/Z with the knockback.
 *  - LivingUpdateEvent for the local player fires inside onLivingUpdate, i.e. in the
 *    SAME tick the packets were processed, before the player's movement is applied.
 *    So we jump there, not in a ClientTickEvent (which would be one tick late).
 */
@Mod(modid = JumpReset.MODID, name = "JumpReset", version = "1.0", clientSideOnly = true)
public class JumpReset {
    public static final String MODID = "jumpreset";

    private final Minecraft mc = Minecraft.getMinecraft();
    private KeyBinding toggleKey;

    private boolean enabled = true;
    private boolean jumpedThisHit = false;
    private int lastHurtTime = 0;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        toggleKey = new KeyBinding("Toggle Jump Reset", Keyboard.KEY_J, "JumpReset");
        ClientRegistry.registerKeyBinding(toggleKey);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        if (toggleKey.isPressed()) {
            enabled = !enabled;
            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new ChatComponentText(
                        "\u00a77[\u00a7bJumpReset\u00a77] " + (enabled ? "\u00a7aON" : "\u00a7cOFF")));
            }
        }
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent e) {
        if (!enabled) return;
        if (!(e.entity instanceof EntityPlayerSP)) return;

        EntityPlayerSP p = (EntityPlayerSP) e.entity;
        int hurt = p.hurtTime;

        // Detect a fresh hit: hurtTime jumped back up (it only counts down otherwise).
        if (hurt > lastHurtTime) {
            jumpedThisHit = false;
        }
        lastHurtTime = hurt;

        if (hurt == 0 || jumpedThisHit) return;
        // Only act in the first two ticks of the hit window (10 or 9 depending on
        // where in the tick the decrement happens).
        if (hurt < p.maxHurtTime - 1) return;

        if (!canJump(p)) return;

        p.jump();            // sets motionY = 0.42 (+ sprint boost) on this exact tick
        jumpedThisHit = true;
    }

    private boolean canJump(EntityPlayerSP p) {
        return p.onGround
                && !p.isInWater()
                && !p.isInLava()
                && !p.isOnLadder()
                && !p.isSneaking()
                && !p.capabilities.isFlying
                && p.ridingEntity == null
                && mc.currentScreen == null;
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Text e) {
        if (enabled) e.left.add("\u00a7bJumpReset: \u00a7aON");
    }
}
