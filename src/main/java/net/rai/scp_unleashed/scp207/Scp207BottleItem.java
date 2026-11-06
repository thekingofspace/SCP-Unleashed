package net.rai.scp_unleashed.scp207;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp207BottleItem extends Item {
    public static final int StackSize = 16;
    public static final int DrinkTicks = 32;
    public static final int SloshEvery = 6;
    public static final float SloshVolume = 0.3f;
    public static final String Key = "item.scp_unleashed.scp207_bottle.";
    public static final String OneKey = "item.scp_unleashed.scp207_1_bottle.";
    private final boolean One;

    public Scp207BottleItem(boolean One, Properties Properties) {
        super(Properties);
        this.One = One;
    }

    public boolean isOne() {
        return One;
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Scp207BottleItem;
    }

    public static boolean isOne(ItemStack Stack) {
        return Stack.getItem() instanceof Scp207BottleItem Bottle && Bottle.One;
    }

    public static ItemStack stack(boolean One) {
        return new ItemStack(One ? Scp207Registry.BottleOne.get() : Scp207Registry.Bottle.get());
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
        return Scp207Registry.Gulp.get();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp207Registry.OpenLayers) {
            User.playSound(Layer.get(), 0.6f, (One ? 0.9f : 1.0f) + World.getRandom().nextFloat() * 0.1f);
        }
        return ItemUtils.startUsingInstantly(World, User, Hand);
    }

    @Override
    public void onUseTick(Level World, LivingEntity Holder, ItemStack Stack, int Remaining) {
        int Elapsed = DrinkTicks - Remaining;
        if (Elapsed > 6 && Remaining % SloshEvery == 0) {
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp207Registry.FizzLayers) {
                Holder.playSound(Layer.get(), SloshVolume, (One ? 0.85f : 1.0f) + World.getRandom().nextFloat() * 0.2f);
            }
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        if (!Context.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        return Scp207BottlesBlock.place(new BlockPlaceContext(Context), One);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack Stack, Level World, LivingEntity Drinker) {
        if (Drinker instanceof ServerPlayer Server) {
            CriteriaTriggers.CONSUME_ITEM.trigger(Server, Stack);
            Server.awardStat(Stats.ITEM_USED.get(this));
        }
        if (!World.isClientSide) {
            Scp207Toll.drink(Drinker, One);
            World.gameEvent(Drinker, GameEvent.DRINK, Drinker.position());
        }
        Stack.consume(1, Drinker);
        if (Stack.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }
        if (Drinker instanceof Player User && !User.hasInfiniteMaterials()) {
            ItemStack Empty = new ItemStack(Items.GLASS_BOTTLE);
            if (!User.getInventory().add(Empty)) {
                User.drop(Empty, false);
            }
        }
        return Stack;
    }

    @Override
    public Component getName(ItemStack Stack) {
        if (Scp207Rack.isRack(Stack)) {
            return Component.translatable(Key + "rack", Scp207Rack.of(Stack).size());
        }
        return super.getName(Stack);
    }

    @Override
    public void inventoryTick(ItemStack Stack, Level World, Entity Holder, int Slot, boolean Selected) {
        if (World.isClientSide || !(Holder instanceof Player User) || !Scp207Rack.isRack(Stack)) {
            return;
        }
        List<ItemStack> Parts = Scp207Rack.of(Stack).bottles();
        int Racks = Stack.getCount();
        Stack.remove(Scp207Registry.RackData.get());
        for (int Index = 0; Index < Racks; Index++) {
            for (ItemStack Part : Parts.subList(1, Parts.size())) {
                ItemHandlerHelper.giveItemToPlayer(User, Part.copy());
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        if (Scp207Rack.isRack(Stack)) {
            Scp207Rack Held = Scp207Rack.of(Stack);
            Lines.add(Component.translatable(Key + "rack.blue", Held.blueCount()).withStyle(ChatFormatting.GRAY));
            return;
        }
        String Prefix = One ? OneKey : Key;
        Lines.add(Component.translatable(Prefix + "tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + "drink").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "place").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "warning").withStyle(ChatFormatting.RED));
        }
    }
}
