package net.rai.scp_unleashed.transferpod;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record TransferPodContent(ResourceLocation Id, String LabelKey, int Colour, Release Releaser, boolean Listed) {
    public TransferPodContent(ResourceLocation Id, String LabelKey, int Colour, Release Releaser) {
        this(Id, LabelKey, Colour, Releaser, true);
    }

    @FunctionalInterface
    public interface Release {
        int release(ServerLevel World, Vec3 Where, float Yaw, @Nullable Player User);
    }

    public Component label() {
        return Component.translatable(LabelKey);
    }

    public int rgb() {
        return Colour & 0xFFFFFF;
    }
}
