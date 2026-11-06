package net.rai.scp_unleashed.rifttech;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;

public abstract class ApocalypticRiftEvent extends Event {
    private final ServerLevel World;
    private final Vec3 Centre;
    private final ApocalypticRiftEntity Rift;

    protected ApocalypticRiftEvent(ServerLevel World, Vec3 Centre, ApocalypticRiftEntity Rift) {
        this.World = World;
        this.Centre = Centre;
        this.Rift = Rift;
    }

    public ServerLevel getLevel() {
        return World;
    }

    public Vec3 getCentre() {
        return Centre;
    }

    public ApocalypticRiftEntity getRift() {
        return Rift;
    }

    public static class Formed extends ApocalypticRiftEvent {
        private final BlockPos Anchor;

        public Formed(ServerLevel World, Vec3 Centre, ApocalypticRiftEntity Rift, BlockPos Anchor) {
            super(World, Centre, Rift);
            this.Anchor = Anchor;
        }

        public BlockPos getAnchor() {
            return Anchor;
        }
    }

    public static class Collapsed extends ApocalypticRiftEvent {
        public Collapsed(ServerLevel World, Vec3 Centre, ApocalypticRiftEntity Rift) {
            super(World, Centre, Rift);
        }
    }
}
