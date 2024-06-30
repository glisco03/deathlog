package com.glisco.deathlog.storage;

import com.glisco.deathlog.client.DeathInfo;

import java.util.List;

public interface DirectDeathLogStorage extends DeathLogStorage {

    default List<DeathInfo> getDeathInfoList() {
        return getDeathInfoList(null);
    }

    default void delete(DeathInfo info) {
        delete(info, null);
    }

    default void restore(int index) {
        restore(index, null);
    }
}
