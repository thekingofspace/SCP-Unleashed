package net.rai.scp_unleashed.stasis;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class StasisPodItem extends Item {
    public static final int TicksPerSecond = 20;
    public static final int WarningTicks = 10 * TicksPerSecond;
    public static final int UrgentTicks = 3 * TicksPerSecond;
    public static final int RattleInterval = 20;
    public static final int UrgentRattleInterval = 8;
    public static final int FullBar = 13;
    public static final int WarningColour = 0xFF4A3A;
    public static final long BlinkMillis = 250L;
    private static final String Pip = "◆";
    private final FragmentRarity Grade;

    public StasisPodItem(FragmentRarity Grade, Properties Properties) {
        super(Properties);
        this.Grade = Grade;
    }

    public static int budget(FragmentRarity Grade) {
        return switch (Grade) {
            case Faint -> 2 * 60 * TicksPerSecond;
            case Wavering -> 5 * 60 * TicksPerSecond;
            case Volatile -> 15 * 60 * TicksPerSecond;
            case Severe -> 45 * 60 * TicksPerSecond;
            case Abyssal -> 120 * 60 * TicksPerSecond;
        };
    }

    public static boolean isPod(ItemStack Stack) {
        return Stack.getItem() instanceof StasisPodItem;
    }

    @Nullable
    public static StasisContents contents(ItemStack Stack) {
        return Stack.get(StasisRegistry.Contents.get());
    }

    public static boolean isOccupied(ItemStack Stack) {
        return isPod(Stack) && Stack.has(StasisRegistry.Contents.get());
    }

    public static boolean isExpired(ItemStack Stack, long Now) {
        StasisContents Held = isPod(Stack) ? contents(Stack) : null;
        return Held != null && Held.expired(Now);
    }

    public static long remaining(ItemStack Stack, long Now) {
        if (!(Stack.getItem() instanceof StasisPodItem Pod)) {
            return 0L;
        }
        StasisContents Held = contents(Stack);
        if (Held != null) {
            return Held.remaining(Now);
        }
        return Mth.clamp(Stack.getOrDefault(StasisRegistry.Charge.get(), Pod.totalTicks()), 0, Pod.totalTicks());
    }

    public static boolean isSpent(ItemStack Stack) {
        return isPod(Stack) && !isOccupied(Stack) && remaining(Stack, 0L) <= 0L;
    }

    public static String clock(long Ticks) {
        long Seconds = (Ticks + TicksPerSecond - 1) / TicksPerSecond;
        long Hours = Seconds / 3600L;
        long Minutes = Seconds % 3600L / 60L;
        long Rest = Seconds % 60L;
        return Hours > 0L ? String.format(Locale.ROOT, "%d:%02d:%02d", Hours, Minutes, Rest) : String.format(Locale.ROOT, "%d:%02d", Minutes, Rest);
    }

    public FragmentRarity getGrade() {
        return Grade;
    }

    public int totalTicks() {
        return budget(Grade);
    }

    public int getRgb() {
        return Grade.getColour() & 0xFFFFFF;
    }

    @Override
    public Component getName(ItemStack Stack) {
        return Component.translatable(getDescriptionId(Stack)).withStyle(Style.EMPTY.withColor(getRgb()));
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        ItemStack Stack = Context.getItemInHand();
        if (!isOccupied(Stack)) {
            return InteractionResult.PASS;
        }
        Level World = Context.getLevel();
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        return StasisPods.release(Context, Stack) ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    @Override
    public void inventoryTick(ItemStack Stack, Level World, Entity Holder, int Slot, boolean Selected) {
        if (!(World instanceof ServerLevel Server)) {
            return;
        }
        StasisContents Held = contents(Stack);
        if (Held == null) {
            return;
        }
        long Now = Server.getGameTime();
        if (Held.expired(Now)) {
            ItemStack Pod = Stack.copy();
            StasisPods.spend(Stack);
            StasisPods.breakOut(Server, Pod, Holder.position(), Holder);
            return;
        }
        StasisEffects.warn(Server, Holder.getX(), Holder.getEyeY() - 0.4, Holder.getZ(), Grade, Held.remaining(Now), Now);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack Stack, ItemEntity Dropped) {
        if (!(Dropped.level() instanceof ServerLevel Server)) {
            return false;
        }
        StasisContents Held = contents(Stack);
        if (Held == null) {
            return false;
        }
        long Now = Server.getGameTime();
        if (Held.expired(Now)) {
            ItemStack Pod = Stack.copy();
            Dropped.discard();
            StasisPods.breakOut(Server, Pod, Dropped.position(), null);
            return true;
        }
        if (StasisEffects.warn(Server, Dropped.getX(), Dropped.getY() + 0.2, Dropped.getZ(), Grade, Held.remaining(Now), Now) && Dropped.onGround()) {
            Vec3 Motion = Dropped.getDeltaMovement();
            Dropped.setDeltaMovement(Motion.x + (Server.random.nextDouble() - 0.5) * 0.08, 0.16, Motion.z + (Server.random.nextDouble() - 0.5) * 0.08);
        }
        return false;
    }

    @Override
    public void onDestroyed(ItemEntity Dropped, DamageSource Source) {
        ItemStack Stack = Dropped.getItem();
        if (Dropped.level() instanceof ServerLevel Server && isOccupied(Stack)) {
            StasisPods.breakOut(Server, Stack.copy(), Dropped.position(), null);
        }
    }

    @Override
    public int getEntityLifespan(ItemStack Stack, Level World) {
        return isOccupied(Stack) ? Integer.MAX_VALUE : super.getEntityLifespan(Stack, World);
    }

    @Override
    public boolean isBarVisible(ItemStack Stack) {
        return isOccupied(Stack) || remaining(Stack, 0L) < totalTicks();
    }

    @Override
    public int getBarWidth(ItemStack Stack) {
        long Left = remaining(Stack, StasisClock.client());
        return Math.round(FullBar * Mth.clamp((float) Left / totalTicks(), 0.0f, 1.0f));
    }

    @Override
    public int getBarColor(ItemStack Stack) {
        if (isOccupied(Stack) && remaining(Stack, StasisClock.client()) <= WarningTicks && System.currentTimeMillis() / BlinkMillis % 2L == 0L) {
            return WarningColour;
        }
        return getRgb();
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack OldStack, ItemStack NewStack, boolean SlotChanged) {
        return SlotChanged || !ItemStack.isSameItem(OldStack, NewStack) || isOccupied(OldStack) != isOccupied(NewStack);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Level World = Context.level();
        long Now = World != null ? World.getGameTime() : StasisClock.client();
        StasisContents Held = contents(Stack);
        Lines.add(Component.translatable("item.scp_unleashed.stasis_pod.tier", tierPips()).withStyle(ChatFormatting.DARK_GRAY));
        if (Held != null) {
            MutableComponent Name = Held.Label().copy().withStyle(ChatFormatting.WHITE);
            if (Held.isNamed()) {
                Name.append(Component.literal(" (").append(Held.Kind()).append(")").withStyle(ChatFormatting.GRAY));
            }
            Lines.add(Component.translatable("item.scp_unleashed.stasis_pod.holds", Name).withStyle(ChatFormatting.GRAY));
            if (Held.MaxHealth() > 0.0f) {
                Lines.add(Component.translatable("item.scp_unleashed.stasis_pod.health", Math.round(Held.Health()), Math.round(Held.MaxHealth()))
                        .withStyle(ChatFormatting.RED));
            }
            long Left = Held.remaining(Now);
            Lines.add(Component.translatable("item.scp_unleashed.stasis_pod.field_draining", clock(Left), clock(totalTicks()))
                    .withStyle(Left <= WarningTicks ? ChatFormatting.RED : ChatFormatting.AQUA));
            if (Left <= WarningTicks) {
                Lines.add(Component.translatable("item.scp_unleashed.stasis_pod.failing").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            }
        } else {
            long Left = remaining(Stack, Now);
            Lines.add(Component.translatable("item.scp_unleashed.stasis_pod.holds",
                    Component.translatable("item.scp_unleashed.stasis_pod.empty").withStyle(ChatFormatting.DARK_GRAY)).withStyle(ChatFormatting.GRAY));
            if (Left <= 0L) {
                Lines.add(Component.translatable("item.scp_unleashed.stasis_pod.spent").withStyle(ChatFormatting.RED));
            } else {
                Lines.add(Component.translatable("item.scp_unleashed.stasis_pod.field_paused", clock(Left), clock(totalTicks())).withStyle(ChatFormatting.GRAY));
            }
        }
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.stasis_pod.capture").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.stasis_pod.release").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.stasis_pod.drain").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("item.scp_unleashed.stasis_pod.breakout").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("item.scp_unleashed.stasis_pod.immune").withStyle(ChatFormatting.DARK_AQUA));
            if (Grade != FragmentRarity.Abyssal) {
                Lines.add(Component.translatable("item.scp_unleashed.stasis_pod.upgrade",
                        Component.translatable("item.scp_unleashed.unstable_fragment_" + FragmentRarity.byTier(Grade.getTier() + 1).getSerializedName()))
                        .withStyle(ChatFormatting.DARK_AQUA));
            }
        }
    }

    private Component tierPips() {
        int Count = FragmentRarity.values().length;
        MutableComponent Filled = Component.literal(Pip.repeat(Grade.getTier())).withStyle(Style.EMPTY.withColor(getRgb()));
        MutableComponent Empty = Component.literal(Pip.repeat(Count - Grade.getTier())).withStyle(ChatFormatting.DARK_GRAY);
        return Filled.append(Empty);
    }
}
