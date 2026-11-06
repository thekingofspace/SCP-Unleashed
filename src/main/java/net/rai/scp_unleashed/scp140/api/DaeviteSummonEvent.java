package net.rai.scp_unleashed.scp140.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

public class DaeviteSummonEvent extends Event {
    private final ServerLevel World;
    private final BlockPos Altar;
    @Nullable
    private Entity God;

    public DaeviteSummonEvent(ServerLevel World, BlockPos Altar) {
        this.World = World;
        this.Altar = Altar.immutable();
    }

    @Nullable
    public static Entity fire(ServerLevel World, BlockPos Altar) {
        return NeoForge.EVENT_BUS.post(new DaeviteSummonEvent(World, Altar)).getGod();
    }

    public ServerLevel getLevel() {
        return World;
    }

    public BlockPos getAltar() {
        return Altar;
    }

    @Nullable
    public Entity getGod() {
        return God;
    }

    public void answer(Entity God) {
        this.God = God;
    }

    public boolean isAnswered() {
        return God != null;
    }
}
