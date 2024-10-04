package com.github.inkm3.signtp.CustomEvents;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class SignTPEnableEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
