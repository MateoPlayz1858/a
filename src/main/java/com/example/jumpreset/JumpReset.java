package com.example.jumpreset;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
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

    private static final int MIN_DELAY = 0;
    private static final int MAX_DELAY = 2;

    private final Minecraft mc = Minecraft.getMinecraft();
    private KeyBinding toggleKey;
    private KeyBinding delayUpKey;
    private KeyBinding delayDownKey;

    private boolean enabled = true;
    private int delayTicks = 1; // 1 tick = 50 ms

    private boolean pending = false;
    private int delayLeft = 0;
    private int lastHurtTime = 0;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        toggleKey = new KeyBinding("Toggle Jump Reset", Keyboard.KEY_J, "JumpReset");
        delayUpKey = new KeyBinding("Delay +", Keyboard.KEY_RBRACKET, "JumpReset");
        delayDownKey = new KeyBinding("Delay -", Keyboard.KEY_LBRACKET, "JumpReset");
        ClientRegistry.registerKeyBinding(toggleKey);
        ClientRegistry.registerKeyBinding(delayUpKey);
        ClientRegistry.registerKeyBinding(delayDownKey);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void say(String msg) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText("\u00a77[\u00a7bJumpReset\u00a77] " + msg));
        }
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        if (toggleKey.isPressed()) {
            enabled = !enabled;
            say(enabled ? "\u00a7aON" : "\u00a7cOFF");
        }
        if (delayUpKey.isPressed()) {
            if (delayTicks < MAX_DELAY) delayTicks++;
            say("\u00a7fDelay: \u00a7b" + delayTicks + " tick(s) \u00a77(" + (delayTicks * 50) + " ms)");
        }
        if (delayDownKey.isPressed()) {
            if (delayTicks > MIN_DELAY) delayTicks--;
            say("\u00a7fDelay: \u00a7b" + delayTicks + " tick(s) \u00a77(" + (delayTicks * 50) + " ms)");
        }
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent e) {
        if (!enabled) return;
        if (!(e.entity instanceof EntityPlayerSP)) return;

        EntityPlayerSP p = (EntityPlayerSP) e.entity;
        int hurt = p.hurtTime;

        // Fresh hit: only arm the jump if you were standing on the ground when it landed.
        if (hurt > lastHurtTime) {
            pending = canJump(p);
            delayLeft = delayTicks;
        }
        lastHurtTime = hurt;

        if (!pending) return;
        if (hurt == 0) { pending = false; return; }
        if (delayLeft > 0) { delayLeft--; return; }

        pending = false;
        if (!p.isInWater() && !p.isInLava() && !p.isOnLadder()) {
            p.jump();
        }
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
}
