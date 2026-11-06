package net.rai.scp_unleashed.gas;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public class Gas {
    private final int Colour;
    private final boolean Toxic;
    private final GasRelease Release;
    private String DescriptionId;

    public Gas(int Colour, boolean Toxic, GasRelease Release) {
        this.Colour = Colour;
        this.Toxic = Toxic;
        this.Release = Release;
    }

    public int getColour() {
        return Colour;
    }

    public boolean isToxic() {
        return Toxic;
    }

    public ResourceLocation getId() {
        ResourceLocation Id = GasRegistry.Gases.getKey(this);
        return Id == null ? ResourceLocation.fromNamespaceAndPath("scp_unleashed", "unknown") : Id;
    }

    public String getDescriptionId() {
        if (DescriptionId == null) {
            DescriptionId = Util.makeDescriptionId("gas", getId());
        }
        return DescriptionId;
    }

    public Component getName() {
        return Component.translatable(getDescriptionId());
    }

    public void release(ServerLevel World, Vec3 Point, int Amount) {
        if (Amount > 0) {
            Release.release(World, Point, Amount);
        }
    }

    @FunctionalInterface
    public interface GasRelease {
        void release(ServerLevel World, Vec3 Point, int Amount);
    }
}
