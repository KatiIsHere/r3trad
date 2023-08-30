package me.ionar.salhack.module.movement;

import me.ionar.salhack.events.MinecraftEvent.Era;
import me.ionar.salhack.events.network.EventNetworkPacketEvent;
import me.ionar.salhack.events.player.EventPlayerMotionUpdate;
import me.ionar.salhack.events.player.EventPlayerTravel;
import me.ionar.salhack.module.Module;
import me.ionar.salhack.module.Value;
import me.ionar.salhack.util.MathUtil;
import me.zero.alpine.fork.listener.EventHandler;
import me.zero.alpine.fork.listener.Listener;
import net.minecraft.network.play.client.CPacketPlayer;

public final class FlightModule extends Module {
    public final Value<Modes> Mode = new Value<Modes>("Mode", new String[]
            {"M"}, "Modes of the speed to use", Modes.Vanilla);
    public final Value<Float> Speed = new Value<Float>("Speed", new String[]
            {""}, "Speed to use", 1.0f, 0.0f, 10.0f, 1.0f);
    public final Value<Boolean> Glide = new Value<Boolean>("Glide", new String[]{""}, "Allows the glide speed under this to function.", false);
    public final Value<Boolean> GlideWhileMoving = new Value<Boolean>("GlideWhileMoving", new String[]{""}, "If no binds are pressed, should glide be enabled?", false);
    public final Value<Float> GlideSpeed = new Value<Float>("GlideSpeed", new String[]{"GlideSpeed"}, "Glide speed of going down", 0.0f, 0.0f, 10.0f, 1.0f);
    public final Value<Boolean> ElytraOnly = new Value<Boolean>("Elytra", new String[]{""}, "Only functions while on an elytra.", false);
    public final Value<Boolean> AntiFallDmg = new Value<Boolean>("AntiFallDmg", new String[]{""}, "Prevents you from taking fall damage while flying", false);
    public final Value<Boolean> AntiKick = new Value<Boolean>("AntiKick", new String[]{""}, "Prevents you from getting kicked while flying by vanilla anticheat", true);
    @EventHandler
    private final Listener<EventPlayerTravel> OnTravel = new Listener<>(event ->
    {
        if (mc.player == null)
            return;

        if (ElytraOnly.getValue() && !mc.player.isElytraFlying())
            return;

        if (Mode.getValue() == Modes.Creative) {
            mc.player.setVelocity(0, 0, 0);

            final double[] dir = MathUtil.directionSpeed(Speed.getValue());

            if (mc.player.movementInput.moveStrafe != 0 || mc.player.movementInput.moveForward != 0) {
                mc.player.motionX = dir[0];
                mc.player.motionZ = dir[1];
            }

            if (mc.player.movementInput.jump && !mc.player.isElytraFlying())
                mc.player.motionY = Speed.getValue();

            if (mc.player.movementInput.sneak)
                mc.player.motionY = -Speed.getValue();

            if (Glide.getValue() && (!GlideWhileMoving.getValue() || (mc.player.movementInput.moveStrafe != 0 || mc.player.movementInput.moveForward != 0))) {
                mc.player.motionY += -GlideSpeed.getValue();
            }

            event.cancel();

            mc.player.prevLimbSwingAmount = 0;
            mc.player.limbSwingAmount = 0;
            mc.player.limbSwing = 0;
        }
    });
    @EventHandler
    private final Listener<EventPlayerMotionUpdate> OnPlayerUpdate = new Listener<>(event ->
    {
        if (event.getEra() != Era.PRE)
            return;

        if (ElytraOnly.getValue() && !mc.player.isElytraFlying())
            return;

        if (Mode.getValue() == Modes.Vanilla) {
            mc.player.setVelocity(0, 0, 0);

            final double[] dir = MathUtil.directionSpeed(Speed.getValue());

            if (mc.player.movementInput.moveStrafe != 0 || mc.player.movementInput.moveForward != 0) {
                mc.player.motionX = dir[0];
                mc.player.motionZ = dir[1];
            }

            if (mc.gameSettings.keyBindJump.isKeyDown())
                mc.player.motionY = Speed.getValue();

            if (mc.gameSettings.keyBindSneak.isKeyDown())
                mc.player.motionY = -Speed.getValue();
        }

        if (AntiKick.getValue() && (mc.player.ticksExisted % 4) == 0)
            mc.player.motionY += -0.04;
    });
    @EventHandler
    private final Listener<EventNetworkPacketEvent> PacketEvent = new Listener<>(event ->
    {
        if (event.getPacket() instanceof CPacketPlayer) {
            if (!AntiFallDmg.getValue())
                return;

            if (mc.player.isElytraFlying())
                return;

            final CPacketPlayer packet = (CPacketPlayer) event.getPacket();

            if (mc.player.fallDistance > 3.8f) {
                packet.onGround = true;
                mc.player.fallDistance = 0.0f;
            }
        }
    });

    public FlightModule() {
        super("Flight", new String[]
                {"Flight"}, "Allows you to fly", "NONE", 0x24DB3E, ModuleType.MOVEMENT);
    }

    @Override
    public String getMetaData() {
        return String.valueOf(Mode.getValue());
    }

    public enum Modes {
        Vanilla,
        Creative,
    }
}
