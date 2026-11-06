package net.rai.scp_unleashed.scp006;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp006VesselItem extends Item {
    public static final int Pour = FluidType.BUCKET_VOLUME;
    public static final int Pours = 10;
    public static final int Capacity = Pour * Pours;
    public static final int BarColour = 0xA8D8F2;
    public static final int Shards = 14;
    public static final String Key = "item.scp_unleashed.scp006_vessel.";

    public Scp006VesselItem(Properties Settings) {
        super(Settings);
    }

    public static int amount(ItemStack Stack) {
        return Mth.clamp(Stack.getOrDefault(Scp006Registry.Fill.get(), Capacity), 0, Capacity);
    }

    public static int poursLeft(ItemStack Stack) {
        return (amount(Stack) + Pour - 1) / Pour;
    }

    public static boolean take(ItemStack Stack, int Amount) {
        int Left = Math.max(0, amount(Stack) - Amount);
        Stack.set(Scp006Registry.Fill.get(), Left);
        return Left <= 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (amount(Stack) <= 0) {
            return InteractionResultHolder.fail(Stack);
        }
        BlockHitResult Hit = getPlayerPOVHitResult(World, User, ClipContext.Fluid.NONE);
        if (Hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(Stack);
        }
        BlockPos Clicked = Hit.getBlockPos();
        Direction Side = Hit.getDirection();
        BlockPos Beside = Clicked.relative(Side);
        if (!World.mayInteract(User, Clicked) || !User.mayUseItemAt(Beside, Side, Stack)) {
            return InteractionResultHolder.fail(Stack);
        }
        Scp006BucketItem Bucket = Scp006Registry.Bucket.get();
        BlockState Found = World.getBlockState(Clicked);
        BlockPos Spot = Found.getBlock() instanceof LiquidBlockContainer Container && Container.canPlaceLiquid(User, World, Clicked, Found, Scp006Registry.Water.get()) ? Clicked : Beside;
        if (!Bucket.emptyContents(User, World, Spot, Hit, Stack)) {
            return InteractionResultHolder.fail(Stack);
        }
        if (User instanceof ServerPlayer Server) {
            CriteriaTriggers.PLACED_BLOCK.trigger(Server, Spot, Stack);
        }
        User.awardStat(Stats.ITEM_USED.get(this));
        if (!World.isClientSide) {
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp006Registry.PourLayers) {
                World.playSound(null, User.getX(), User.getY() + 1.0, User.getZ(), Layer.get(), User.getSoundSource(), 0.7f, 0.95f + World.getRandom().nextFloat() * 0.1f);
            }
        }
        if (User.hasInfiniteMaterials()) {
            return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
        }
        if (take(Stack, Pour)) {
            shatter(World, User, Stack.copy());
            return InteractionResultHolder.sidedSuccess(ItemStack.EMPTY, World.isClientSide);
        }
        return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
    }

    public static void shatter(Level World, Player User, ItemStack Broken) {
        if (!(World instanceof ServerLevel Server)) {
            return;
        }
        Vec3 Look = User.getLookAngle();
        double X = User.getX() + Look.x * 0.45;
        double Y = User.getEyeY() - 0.35;
        double Z = User.getZ() + Look.z * 0.45;
        Server.playSound(null, X, Y, Z, SoundEvents.ITEM_BREAK, User.getSoundSource(), 0.9f, 0.8f + Server.getRandom().nextFloat() * 0.3f);
        Server.playSound(null, X, Y, Z, Scp006Registry.Crack.get(), User.getSoundSource(), 0.8f, 0.95f + Server.getRandom().nextFloat() * 0.1f);
        Server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Broken.copyWithCount(1)), X, Y, Z, Shards, 0.12, 0.1, 0.12, 0.08);
        Server.sendParticles(Scp006Registry.Glint.get(), X, Y, Z, 6, 0.2, 0.15, 0.2, 0.02);
        User.awardStat(Stats.ITEM_BROKEN.get(Broken.getItem()));
    }

    @Override
    public void inventoryTick(ItemStack Stack, Level World, Entity Holder, int Slot, boolean Selected) {
        if (World.isClientSide || amount(Stack) > 0) {
            return;
        }
        ItemStack Broken = Stack.copy();
        Stack.setCount(0);
        if (Holder instanceof Player User) {
            shatter(World, User, Broken);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack Stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack Stack) {
        return Math.round(13.0f * amount(Stack) / Capacity);
    }

    @Override
    public int getBarColor(ItemStack Stack) {
        return BarColour;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        Lines.add(Component.translatable(Key + "fill", poursLeft(Stack), Pours).withStyle(ChatFormatting.AQUA));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "detail").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "pipes").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "fragile").withStyle(ChatFormatting.GOLD));
        }
    }
}
