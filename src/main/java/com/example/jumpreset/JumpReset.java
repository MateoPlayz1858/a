package com.example.jumpreset;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

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

        if (hurt > lastHurtTime) {
            jumpedThisHit = false;
        }
        lastHurtTime = hurt;

        if (hurt == 0 || jumpedThisHit) return;
        if (hurt < p.maxHurtTime - 1) return;

        if (!canJump(p)) return;

        p.jump();
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
