package com.glisco.deathlog.client.gui;

import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class DeathLogToast extends SystemToast {

    public DeathLogToast(Type type, Text title, @Nullable Text description) {
        super(type, title, description);
    }

}
