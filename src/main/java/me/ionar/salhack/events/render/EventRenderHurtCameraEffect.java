package me.ionar.salhack.events.render;

import me.ionar.salhack.events.MinecraftEvent;

public class EventRenderHurtCameraEffect extends MinecraftEvent {
    public float Ticks;

    public EventRenderHurtCameraEffect(float ticks) {
        super();
        Ticks = ticks;
    }
}
