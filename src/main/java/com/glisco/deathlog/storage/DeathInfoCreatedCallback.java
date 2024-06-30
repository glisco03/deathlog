package com.glisco.deathlog.storage;

import com.glisco.deathlog.client.DeathInfo;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface DeathInfoCreatedCallback {

    Event<DeathInfoCreatedCallback> EVENT = EventFactory.createArrayBacked(DeathInfoCreatedCallback.class, listeners -> info -> {
        for (DeathInfoCreatedCallback listener : listeners) {
            listener.event(info);
        }
    });

    void event(DeathInfo info);
}
