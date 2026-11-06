package net.rai.scp_unleashed.scp001scarlet.throne;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

public final class ScarletThroneSpill {
    public static final int Margin = 4;
    public static final int MaxPulls = 64;
    private static int Spilled;

    private ScarletThroneSpill() {
    }

    public static int spilled() {
        return Spilled;
    }

    public static BlockPos spot(ServerLevel World, BlockPos Centre, BlockPos From) {
        double Dx = From.getX() - Centre.getX();
        double Dz = From.getZ() - Centre.getZ();
        double Length = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Length < 1.0E-3) {
            Dx = 1.0;
            Dz = 0.0;
            Length = 1.0;
        }
        double Reach = ScarletThrone.RimRadius + Margin;
        int X = Centre.getX() + (int) Math.round(Dx / Length * Reach);
        int Z = Centre.getZ() + (int) Math.round(Dz / Length * Reach);
        int Y = World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X, Z);
        return new BlockPos(X, Math.max(Y, World.getMinBuildHeight() + 1), Z);
    }

    public static List<ItemStack> empty(ServerLevel World, BlockPos Pos, BlockState Old) {
        BlockEntity Held = World.getBlockEntity(Pos);
        List<ItemStack> Stacks = new ArrayList<>();
        if (Held instanceof Container Box) {
            for (int Slot = 0; Slot < Box.getContainerSize(); Slot++) {
                ItemStack Stack = Box.removeItemNoUpdate(Slot);
                if (!Stack.isEmpty()) {
                    Stacks.add(Stack);
                }
            }
        } else if (Held instanceof LecternBlockEntity Lectern) {
            if (Lectern.hasBook()) {
                Stacks.add(Lectern.getBook().copy());
            }
            Lectern.clearContent();
        } else if (Held instanceof CampfireBlockEntity Fire) {
            for (ItemStack Each : Fire.getItems()) {
                if (!Each.isEmpty()) {
                    Stacks.add(Each.copy());
                }
            }
            Fire.clearContent();
        } else if (Held != null) {
            IItemHandler Handler = World.getCapability(Capabilities.ItemHandler.BLOCK, Pos, Old, Held, null);
            if (Handler != null) {
                for (int Slot = 0; Slot < Handler.getSlots(); Slot++) {
                    for (int Pull = 0; Pull < MaxPulls; Pull++) {
                        ItemStack Got = Handler.extractItem(Slot, 64, false);
                        if (Got.isEmpty()) {
                            break;
                        }
                        Stacks.add(Got);
                    }
                }
            }
        }
        for (ItemStack Each : Block.getDrops(Old, World, Pos, Held)) {
            if (!Each.isEmpty()) {
                Stacks.add(Each);
            }
        }
        return Stacks;
    }

    public static void spill(ServerLevel World, BlockPos Pos, BlockState Old) {
        List<ItemStack> Stacks = empty(World, Pos, Old);
        if (Stacks.isEmpty()) {
            return;
        }
        ScarletThroneSite Site = ScarletThroneSite.get(World.getServer());
        BlockPos Spot = Site.planned() ? spot(World, Site.centre(), Pos) : Pos.above();
        for (ItemStack Each : Stacks) {
            ItemEntity Drop = new ItemEntity(World, Spot.getX() + 0.5, Spot.getY() + 0.3, Spot.getZ() + 0.5, Each, 0.0, 0.05, 0.0);
            Drop.setUnlimitedLifetime();
            Drop.setInvulnerable(true);
            if (World.addFreshEntity(Drop)) {
                Spilled++;
            }
        }
    }
}
