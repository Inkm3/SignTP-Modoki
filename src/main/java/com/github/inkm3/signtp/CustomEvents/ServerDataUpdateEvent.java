package com.github.inkm3.signtp.CustomEvents;

import com.github.inkm3.signtp.FileEdit.ServerData;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ServerDataUpdateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final ServerData serveData;

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public ServerDataUpdateEvent(ServerData serveData) {
        this.serveData = serveData;
    }

    public ServerData getServeData() {
        return this.serveData;
    }
}
