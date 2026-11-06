package net.rai.scp_unleashed.scp001day.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.rai.scp_unleashed.scp001day.plan.Scp001DayDressing;

public final class Scp001DayExtras {
    public static final int Shops = 12;
    public static final String ShopPrefix = "sign.scp_unleashed.scp001_day.shop.";
    public static final String RotationKey = "ItemRotation";

    private Scp001DayExtras() {
    }

    public static void place(WorldGenLevel Level, ChunkAccess Chunk, Scp001DayDressing.Extra Item) {
        BlockPos Pos = new BlockPos(Item.X(), Item.Y(), Item.Z());
        if (!Chunk.getBlockState(Pos).isAir()) {
            return;
        }
        if (Item.Kind() == 0) {
            if (Chunk.getBlockState(Pos.below()).isAir()) {
                return;
            }
            Level.addFreshEntity(frame(Level, Pos, Item));
        } else if (Item.Kind() == 1) {
            Direction Facing = Direction.from2DDataValue(Item.Facing() == 0 ? 2 : (Item.Facing() == 1 ? 3 : (Item.Facing() == 2 ? 0 : 1)));
            if (Chunk.getBlockState(Pos.relative(Facing.getOpposite())).isAir()) {
                return;
            }
            Level.setBlock(Pos, Blocks.BIRCH_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Facing), 2);
            if (Level.getBlockEntity(Pos) instanceof SignBlockEntity Sign) {
                SignText Text = new SignText().setMessage(1, Component.translatable(ShopPrefix + Math.floorMod(Item.Salt(), Shops))).setColor(DyeColor.GRAY);
                CompoundTag Data = new CompoundTag();
                SignText.DIRECT_CODEC.encodeStart(Level.registryAccess().createSerializationContext(NbtOps.INSTANCE), Text).result().ifPresent(Encoded -> Data.put("front_text", Encoded));
                Data.putBoolean("is_waxed", true);
                Sign.loadWithComponents(Data, Level.registryAccess());
            }
        } else if (Item.Kind() == 2) {
            if (Chunk.getBlockState(Pos.below()).isAir()) {
                return;
            }
            Level.setBlock(Pos, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 2);
            RandomizableContainer.setBlockEntityLootTable(Level, RandomSource.create(Scp001DayChunkGenerator.seedOf(Item.X(), Item.Y(), Item.Z())), Pos, Scp001DayChunkGenerator.CampLoot);
        }
    }

    public static ItemFrame frame(WorldGenLevel Level, BlockPos Pos, Scp001DayDressing.Extra Item) {
        ItemFrame Frame = new ItemFrame(Level.getLevel(), Pos, Direction.UP);
        Frame.setInvisible(true);
        Frame.setSilent(true);
        Frame.setItem(leftBehind(Item.Salt()), false);
        CompoundTag Data = new CompoundTag();
        Frame.addAdditionalSaveData(Data);
        Data.putByte(RotationKey, (byte) rotation(Item));
        Frame.readAdditionalSaveData(Data);
        Frame.setSilent(false);
        return Frame;
    }

    public static int rotation(Scp001DayDressing.Extra Item) {
        return Math.floorMod(Item.Facing() * 2, 8);
    }

    public static ItemStack leftBehind(int Salt) {
        return switch (Math.floorMod(Salt, 4)) {
            case 0 -> new ItemStack(Items.LEATHER_BOOTS);
            case 1 -> new ItemStack(Items.BUNDLE);
            case 2 -> new ItemStack(Items.CLOCK);
            default -> new ItemStack(Items.BOOK);
        };
    }
}
