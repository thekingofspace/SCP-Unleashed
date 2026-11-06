package net.rai.scp_unleashed.dclass.duty;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MilkBucketItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.UseAnim;
import net.rai.scp_unleashed.antimemetic.AntiMemeticPillsItem;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.guards.GuardDuty;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public final class DClassDuties {
    public static final byte ActionScrubbing = 4;
    public static final double HandBackRange = 16.0;
    public static final String MessagePrefix = "message.scp_unleashed.dclass_manager.";

    public enum Verdict {
        Accepted,
        Empty,
        Unusable,
        Full,
        NotHungry,
        Unavailable
    }

    private DClassDuties() {
    }

    public static DClassDuty of(@Nullable Entity Body) {
        if (!(Body instanceof DClass Subject)) {
            return DClassDuty.Idle;
        }
        DClassDuty Found = Subject.getExistingDataOrNull(DClassDutyRegistry.Duty.get());
        return Found == null ? DClassDuty.Idle : Found;
    }

    public static void set(DClass Subject, DClassDuty Duty) {
        if (Duty.idle()) {
            if (Subject.hasData(DClassDutyRegistry.Duty.get())) {
                Subject.removeData(DClassDutyRegistry.Duty.get());
            }
        } else {
            GuardDuty.yieldTo(Subject, Duty);
            Subject.setData(DClassDutyRegistry.Duty.get(), Duty);
        }
    }

    public static boolean busy(@Nullable Entity Body) {
        return of(Body).busy();
    }

    public static boolean hasRole(@Nullable Entity Body) {
        return of(Body).Role().active();
    }

    public static boolean hasTask(@Nullable Entity Body) {
        return of(Body).Task().active();
    }

    public static DClassRole role(@Nullable Entity Body) {
        return of(Body).Role();
    }

    public static DClassTask task(@Nullable Entity Body) {
        return of(Body).Task();
    }

    @Nullable
    public static BlockPos home(@Nullable Entity Body) {
        return of(Body).Home().orElse(null);
    }

    public static boolean clear(@Nullable Entity Body) {
        if (!(Body instanceof DClass Subject) || !busy(Subject)) {
            return false;
        }
        set(Subject, DClassDuty.Idle);
        settle(Subject);
        return true;
    }

    public static boolean clearTask(@Nullable Entity Body) {
        if (!(Body instanceof DClass Subject) || !hasTask(Subject)) {
            return false;
        }
        set(Subject, of(Subject).withoutTask());
        settle(Subject);
        return true;
    }

    public static boolean clearRole(@Nullable Entity Body) {
        if (!(Body instanceof DClass Subject) || !hasRole(Subject)) {
            return false;
        }
        set(Subject, of(Subject).withoutRole());
        settle(Subject);
        return true;
    }

    private static void settle(DClass Subject) {
        if (Subject.action() == ActionScrubbing) {
            Subject.setAction(DClassLike.ActionNone);
        }
    }

    public static boolean available(@Nullable DClass Subject) {
        return Subject != null && Subject.isAlive() && !Subject.isRemoved() && !Subject.isSpecimen() && !Subject.isPuppet() && !Subject.isUndead();
    }

    public static boolean assignRole(DClass Subject, DClassRole Role) {
        if (!available(Subject)) {
            return false;
        }
        DClassDuty Current = of(Subject);
        set(Subject, Current.withoutTask().withRole(Role, Role.homed() ? Subject.blockPosition() : null));
        settle(Subject);
        if (Role.active()) {
            Subject.wake();
        }
        return true;
    }

    public static boolean orderFollow(DClass Subject, Player Leader) {
        if (!available(Subject)) {
            return false;
        }
        set(Subject, of(Subject).withTask(DClassTask.FollowMe, null, Leader.getUUID(), ItemStack.EMPTY, Subject.level().getGameTime()));
        settle(Subject);
        Subject.wake();
        return true;
    }

    public static boolean orderClean(DClass Subject, @Nullable Player Issuer) {
        if (!available(Subject)) {
            return false;
        }
        set(Subject, of(Subject).withTask(DClassTask.CleanArea, Subject.blockPosition(), Issuer == null ? null : Issuer.getUUID(), ItemStack.EMPTY, Subject.level().getGameTime()));
        settle(Subject);
        Subject.wake();
        return true;
    }

    public static Verdict orderEat(DClass Subject, @Nullable Player Issuer, ItemStack Offered) {
        if (!available(Subject)) {
            return Verdict.Unavailable;
        }
        if (Offered.isEmpty()) {
            return Verdict.Empty;
        }
        if (!consumable(Subject, Offered)) {
            return Verdict.Unusable;
        }
        FoodProperties Food = Offered.getFoodProperties(Subject);
        if (Food != null && !Food.canAlwaysEat() && !Subject.food().needsFood()) {
            return Verdict.NotHungry;
        }
        ItemStack One = Offered.copyWithCount(1);
        if (!Subject.roomFor(One)) {
            return Verdict.Full;
        }
        ItemStack Left = Subject.insert(One.copy());
        if (!Left.isEmpty()) {
            return Verdict.Full;
        }
        set(Subject, of(Subject).withTask(DClassTask.EatThis, null, Issuer == null ? null : Issuer.getUUID(), One, Subject.level().getGameTime()));
        settle(Subject);
        Subject.wake();
        return Verdict.Accepted;
    }

    public static boolean consumable(LivingEntity Eater, ItemStack Stack) {
        if (Stack.isEmpty()) {
            return false;
        }
        if (Stack.getItem() instanceof AntiMemeticPillsItem && AntiMemeticPillsItem.pills(Stack) <= 0) {
            return false;
        }
        UseAnim Kind = Stack.getUseAnimation();
        return (Kind == UseAnim.EAT || Kind == UseAnim.DRINK) && Stack.getUseDuration(Eater) > 0;
    }

    public static boolean emptyContainer(DClass Subject, ItemStack Held) {
        ItemStack Container;
        if (Held.getItem() instanceof PotionItem && Held.getUseAnimation() == UseAnim.DRINK) {
            Container = new ItemStack(Items.GLASS_BOTTLE);
        } else if (Held.getItem() instanceof MilkBucketItem) {
            Container = new ItemStack(Items.BUCKET);
        } else {
            return false;
        }
        Held.shrink(1);
        if (Held.isEmpty()) {
            Subject.setItemSlot(EquipmentSlot.MAINHAND, Container);
            return true;
        }
        ItemStack Left = Subject.insert(Container);
        if (!Left.isEmpty()) {
            Subject.spawnAtLocation(Left, 0.5f);
        }
        return true;
    }

    @Nullable
    public static Player issuer(DClass Subject) {
        Optional<UUID> Id = of(Subject).Issuer();
        if (Id.isEmpty()) {
            return null;
        }
        Player Found = Subject.level().getPlayerByUUID(Id.get());
        return Found != null && Found.isAlive() && !Found.isSpectator() ? Found : null;
    }

    @Nullable
    public static Player leader(DClass Subject) {
        return of(Subject).Task() == DClassTask.FollowMe ? issuer(Subject) : null;
    }

    public static void handBack(DClass Subject, ItemStack Stack, @Nullable Player Issuer) {
        if (Stack.isEmpty()) {
            return;
        }
        if (Issuer != null && Issuer.distanceToSqr(Subject) <= HandBackRange * HandBackRange) {
            Issuer.getInventory().placeItemBackInInventory(Stack);
            return;
        }
        Subject.spawnAtLocation(Stack, 0.5f);
    }

    public static void tell(@Nullable Player Viewer, String Key, Object... Arguments) {
        if (Viewer != null) {
            Viewer.displayClientMessage(Component.translatable(MessagePrefix + Key, Arguments).withStyle(ChatFormatting.GOLD), true);
        }
    }

    public static void warn(@Nullable Player Viewer, String Key, Object... Arguments) {
        if (Viewer != null) {
            Viewer.displayClientMessage(Component.translatable(MessagePrefix + Key, Arguments).withStyle(ChatFormatting.RED), true);
        }
    }
}
