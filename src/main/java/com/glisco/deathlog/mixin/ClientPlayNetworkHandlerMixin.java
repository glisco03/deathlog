package com.glisco.deathlog.mixin;

import com.glisco.deathlog.client.DeathLogClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin extends ClientCommonNetworkHandler {

    protected ClientPlayNetworkHandlerMixin(MinecraftClient client, ClientConnection connection, ClientConnectionState connectionState) {
        super(client, connection, connectionState);
    }

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "onDeathMessage", at = @At("HEAD"))
    private void onClientDeath(DeathMessageS2CPacket packet, CallbackInfo ci) {
        if (!RenderSystem.isOnRenderThread()) return;
        DeathLogClient.getClientStorage().store(packet.message(), this.client.player);

        if (DeathLogClient.CONFIG.screenshotsEnabled()) {
            ScreenshotRecorder.saveScreenshot(FabricLoader.getInstance().getGameDir().toFile(), this.client.getFramebuffer(), text -> {
                text = Text.literal("§7[§bDeathLog§7] ").append(((MutableText) text).formatted(Formatting.GRAY));
                this.client.player.sendMessage(text, false);
            });
        }
    }

    @Inject(method = "onHealthUpdate", at = @At("HEAD"))
    private void onLegacyClientDeath(HealthUpdateS2CPacket packet, CallbackInfo ci) {
        if (!DeathLogClient.CONFIG.useLegacyDeathDetection() || !RenderSystem.isOnRenderThread()) return;
        if (packet.getHealth() > 0 || this.client.player.isDead()) return;

        DeathLogClient.getClientStorage().store(Text.empty(), this.client.player);

        if (DeathLogClient.CONFIG.screenshotsEnabled()) {
            ScreenshotRecorder.saveScreenshot(FabricLoader.getInstance().getGameDir().toFile(), this.client.getFramebuffer(), text -> {
                text = Text.literal("§7[§bDeathLog§7] ").append(((MutableText) text).formatted(Formatting.GRAY));
                this.client.player.sendMessage(text, false);
            });
        }
    }

}
