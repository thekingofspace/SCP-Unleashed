package net.rai.scp_unleashed.scp4845.block;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class Scp4845SealedBlock {
    public static final float Resistance = 3600000.0f;

    private Scp4845SealedBlock() {
    }

    public static BlockBehaviour.Properties sealed(MapColor Colour) {
        return BlockBehaviour.Properties.of().mapColor(Colour).strength(-1.0f, Resistance).noLootTable().pushReaction(PushReaction.BLOCK)
                .isValidSpawn((State, World, Pos, Kind) -> false);
    }
}
