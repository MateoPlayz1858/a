package com.example.jumpreset;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = JumpReset.MODID, name = "JumpReset", version = "1.0", clientSideOnly = true)
public class JumpReset {
    public static final String MODID = "jumpreset";
    private static final String HOOK_NAME = "jumpreset_hook";

    private final Minecraft mc = Minecraft.getMinecraft();
    private KeyBinding toggleKey;

    private boolean enabled = true;

    // true while our packet listener is attached to the current connection
    private volatile boolean hooked = false;
    // set on the main thread right after a knockback packet has been applied
    private boolean velocityApplied = false;
    // used only by the fallback path
    private int lastHurtTime = 0;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        toggleKey = new KeyBinding("Toggle Jump Reset", Keyboard.KEY_J, "JumpReset");
        ClientRegistry.registerKeyBinding(toggleKey);
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
    }

    @SubscribeEvent
    public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent e) {
        try {
            if (e.manager.channel().pipeline().get(HOOK_NAME) == null) {
                e.manager.channel().pipeline().addBefore("packet_handler", HOOK_NAME, new VelocityHook());
            }
            hooked = true;
        } catch (Throwable t) {
            hooked = false;
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        hooked = false;
        velocityApplied = false;
    }

    private class VelocityHook extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ctx.fireChannelRead(msg);
            try {
                if (msg instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity v = (S12PacketEntityVelocity) msg;
                    EntityPlayerSP p = mc.thePlayer;
                    // Only knockback aimed at us that actually pushes sideways.
                    if (p != null
                            && v.getEntityID() == p.getEntityId()
                            && (v.getMotionX() != 0 || v.getMotionZ() != 0)) {
                        mc.addScheduledTask(new Runnable() {
                            @Override
                            public void run() {
                                velocityApplied = true;
                            }
                        });
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent e) {
        if (!(e.entity instanceof EntityPlayerSP)) return;
        EntityPlayerSP p = (EntityPlayerSP) e.entity;

        boolean fire = false;
        if (hooked) {
            if (velocityApplied) {
                velocityApplied = false;
                fire = p.hurtTime > 0;
            }
            lastHurtTime = p.hurtTime;
        } else {
            int hurt = p.hurtTime;
            if (hurt > lastHurtTime) fire = true;
            lastHurtTime = hurt;
        }

        if (!enabled || !fire) return;
        if (p.isBurning()) return; // no jump resets while on fire
        if (canJump(p)) {
            p.jump();
        }
    }

    // Ground only: no jumping while airborne.
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
