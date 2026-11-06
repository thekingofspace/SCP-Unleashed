package net.rai.scp_unleashed.scp109;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp109CanteenItem extends Item {
    public static final int DrinkTicks = 32;
    public static final int PourCooldown = 8;
    public static final int BottleVolume = 250;
    public static final int SloshEvery = 4;
    public static final float SloshVolume = 0.35f;
    public static final String Key = "item.scp_unleashed.scp109_canteen.";

    public Scp109CanteenItem(Properties Properties) {
        super(Properties);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.is(Scp109Registry.Canteen.get());
    }

    @Override
    public int getUseDuration(ItemStack Stack, LivingEntity Holder) {
        return DrinkTicks;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack Stack) {
        return UseAnim.DRINK;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return Scp109Registry.Drink.get();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        InteractionHand Other = Hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack Held = User.getItemInHand(Other);
        if (Held.is(Items.GLASS_BOTTLE)) {
            if (!World.isClientSide) {
                fillBottle(World, User, Other, Held, User.position());
                User.awardStat(Stats.ITEM_USED.get(this));
                User.getCooldowns().addCooldown(this, PourCooldown);
            }
            return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
        }
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp109Registry.CapLayers) {
            User.playSound(Layer.get(), 0.5f, 0.95f + World.getRandom().nextFloat() * 0.1f);
        }
        return ItemUtils.startUsingInstantly(World, User, Hand);
    }

    @Override
    public void onUseTick(Level World, LivingEntity Holder, ItemStack Stack, int Remaining) {
        int Elapsed = DrinkTicks - Remaining;
        if (Elapsed > DrinkTicks * 0.21875f && Remaining % SloshEvery == 0) {
            Holder.playSound(Scp109Registry.Slosh.get(), SloshVolume, 0.9f + World.getRandom().nextFloat() * 0.2f);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack Stack, Level World, LivingEntity Drinker) {
        if (Drinker instanceof ServerPlayer Server) {
            CriteriaTriggers.CONSUME_ITEM.trigger(Server, Stack);
            Server.awardStat(Stats.ITEM_USED.get(this));
        }
        if (!World.isClientSide) {
            refresh(Drinker);
            World.gameEvent(Drinker, GameEvent.DRINK, Drinker.position());
        }
        return Stack;
    }

    public static void refresh(LivingEntity Drinker) {
        Level World = Drinker.level();
        boolean Burning = Drinker.isOnFire();
        if (Burning) {
            Drinker.clearFire();
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp109Registry.DouseLayers) {
                World.playSound(null, Drinker.getX(), Drinker.getY(), Drinker.getZ(), Layer.get(), SoundSource.PLAYERS, 0.8f, 0.95f + World.getRandom().nextFloat() * 0.1f);
            }
        }
        if (Drinker instanceof Player User) {
            User.getFoodData().setExhaustion(0.0f);
        }
        if (World instanceof ServerLevel Server) {
            Vec3 Look = Drinker.getLookAngle();
            double X = Drinker.getX() + Look.x * 0.3;
            double Y = Drinker.getEyeY() - 0.2;
            double Z = Drinker.getZ() + Look.z * 0.3;
            Server.sendParticles(ParticleTypes.SPLASH, X, Y, Z, 5, 0.12, 0.04, 0.12, 0.05);
            if (Burning) {
                Server.sendParticles(ParticleTypes.CLOUD, Drinker.getX(), Drinker.getY() + Drinker.getBbHeight() * 0.5, Drinker.getZ(), 8, 0.25, 0.4, 0.25, 0.02);
            }
        }
    }

    public static void fillBottle(Level World, Player User, InteractionHand BottleHand, ItemStack Bottle, Vec3 Where) {
        ItemStack Filled = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        User.setItemInHand(BottleHand, ItemUtils.createFilledResult(Bottle, User, Filled));
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp109Registry.BottleLayers) {
            World.playSound(null, Where.x, Where.y, Where.z, Layer.get(), SoundSource.PLAYERS, 0.8f, 0.95f + World.getRandom().nextFloat() * 0.1f);
        }
        World.gameEvent(User, GameEvent.FLUID_PICKUP, Where);
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        BlockState State = World.getBlockState(Pos);
        boolean Empty = State.is(Blocks.CAULDRON);
        boolean Partial = State.is(Blocks.WATER_CAULDRON) && State.getValue(LayeredCauldronBlock.LEVEL) < LayeredCauldronBlock.MAX_FILL_LEVEL;
        if (!Empty && !Partial) {
            return InteractionResult.PASS;
        }
        if (!World.isClientSide) {
            World.setBlockAndUpdate(Pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL));
            Player User = Context.getPlayer();
            if (User != null) {
                User.awardStat(Stats.FILL_CAULDRON);
                User.awardStat(Stats.ITEM_USED.get(this));
                User.getCooldowns().addCooldown(this, PourCooldown);
            }
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp109Registry.PourLayers) {
                World.playSound(null, Pos, Layer.get(), SoundSource.BLOCKS, 0.9f, 0.95f + World.getRandom().nextFloat() * 0.1f);
            }
            World.gameEvent(User, GameEvent.FLUID_PLACE, Pos);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "drink").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "cauldron").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "bottle").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "tap").withStyle(ChatFormatting.GOLD));
        }
    }
}
