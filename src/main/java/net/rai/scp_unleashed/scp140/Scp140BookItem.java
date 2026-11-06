package net.rai.scp_unleashed.scp140;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp140BookItem extends BlockItem {
    public static final String RestKey = "scp_unleashed_scp140_rest";
    public static final int RestInterval = 20;
    public static final int VoidMargin = 24;

    public Scp140BookItem(Block Placed, Properties Settings) {
        super(Placed, Settings);
    }

    public static boolean is(ItemStack Stack) {
        return !Stack.isEmpty() && Stack.is(Scp140Registry.BookItem.get());
    }

    public static void describe(List<Component> Lines) {
        Lines.add(Component.translatable("item.scp_unleashed.scp140_chronicle.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.scp140_chronicle.read").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scp140_chronicle.place").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scp140_chronicle.take").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scp140_chronicle.storage").withStyle(ChatFormatting.GOLD));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Held = User.getItemInHand(Hand);
        if (!World.isClientSide && User instanceof ServerPlayer Reader) {
            World.playSound(null, User.getX(), User.getY(), User.getZ(), Scp140Registry.PageRustle.get(), SoundSource.PLAYERS, 0.5f, 1.0f);
            Scp140Reading.open(Reader);
        }
        return InteractionResultHolder.sidedSuccess(Held, World.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        Player User = Context.getPlayer();
        if (User != null && !User.isSecondaryUseActive()) {
            return use(Context.getLevel(), User, Context.getHand()).getResult();
        }
        return super.useOn(Context);
    }

    @Override
    public boolean canBeHurtBy(ItemStack Stack, DamageSource Source) {
        return false;
    }

    @Override
    public int getEntityLifespan(ItemStack Stack, Level World) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack Stack, ItemEntity Dropped) {
        if (!(Dropped.level() instanceof ServerLevel World)) {
            return false;
        }
        CompoundTag Data = Dropped.getPersistentData();
        if (Dropped.onGround() && Dropped.tickCount % RestInterval == 0) {
            Data.putLong(RestKey, Dropped.blockPosition().asLong());
        }
        if (Dropped.getY() < World.getMinBuildHeight() - VoidMargin) {
            return rescue(World, Dropped, Data);
        }
        return false;
    }

    private static boolean rescue(ServerLevel World, ItemEntity Dropped, CompoundTag Data) {
        Dropped.setDeltaMovement(Vec3.ZERO);
        Dropped.fallDistance = 0.0f;
        if (Data.contains(RestKey)) {
            BlockPos Rest = BlockPos.of(Data.getLong(RestKey));
            Data.remove(RestKey);
            if (World.isLoaded(Rest) && !World.getBlockState(Rest.below()).isAir()) {
                Dropped.teleportTo(Rest.getX() + 0.5, Rest.getY() + 0.25, Rest.getZ() + 0.5);
                arrived(World, Dropped);
                return false;
            }
        }
        ServerLevel Home = World.getServer().overworld();
        BlockPos Spawn = Home.getSharedSpawnPos();
        BlockPos Ground = Home.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Spawn);
        ItemEntity Returned = new ItemEntity(Home, Ground.getX() + 0.5, Ground.getY() + 0.25, Ground.getZ() + 0.5, Dropped.getItem().copy());
        Returned.setDeltaMovement(Vec3.ZERO);
        Returned.setDefaultPickUpDelay();
        Dropped.discard();
        Home.addFreshEntity(Returned);
        arrived(Home, Returned);
        return true;
    }

    private static void arrived(ServerLevel World, ItemEntity Dropped) {
        World.sendParticles(Scp140Registry.Ink.get(), Dropped.getX(), Dropped.getY() + 0.2, Dropped.getZ(), 14, 0.2, 0.15, 0.2, 0.01);
        World.playSound(null, Dropped.getX(), Dropped.getY(), Dropped.getZ(), Scp140Registry.BookThump.get(), SoundSource.BLOCKS, 0.8f, 0.8f);
    }
}
