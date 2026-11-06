package net.rai.scp_unleashed.scp140.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

public class DaeviteGodSlainEvent extends Event {
    private final ServerLevel World;
    private final BlockPos Where;
    @Nullable
    private final LivingEntity Slayer;

    public DaeviteGodSlainEvent(ServerLevel World, BlockPos Where, @Nullable LivingEntity Slayer) {
        this.World = World;
        this.Where = Where.immutable();
        this.Slayer = Slayer;
    }

    public static void fire(ServerLevel World, BlockPos Where, @Nullable LivingEntity Slayer) {
        NeoForge.EVENT_BUS.post(new DaeviteGodSlainEvent(World, Where, Slayer));
    }

    public ServerLevel getLevel() {
        return World;
    }

    public BlockPos getWhere() {
        return Where;
    }

    @Nullable
    public LivingEntity getSlayer() {
        return Slayer;
    }
}
