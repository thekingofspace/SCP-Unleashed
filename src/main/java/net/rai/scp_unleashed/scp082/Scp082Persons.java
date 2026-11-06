package net.rai.scp_unleashed.scp082;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.scp049.Scp049Hold;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;

public final class Scp082Persons {
    public static final float PersonMeal = 45.0f;
    public static final float VillagerMeal = 38.0f;
    public static final float RawMeatSnack = 10.0f;
    public static final float RawFishSnack = 6.0f;
    public static final float CookedSnack = 8.0f;
    public static final float FleshSnack = 5.0f;
    public static final float OtherShare = 0.8f;
    public static final double MaxVictimHeight = 2.3;

    public enum Kind {
        Player,
        Person,
        Villager,
        None
    }

    private Scp082Persons() {
    }

    public static boolean sparesPlayers(Entity Viewer) {
        return Viewer.level().getDifficulty() == Difficulty.PEACEFUL;
    }

    public static Kind kindOf(@Nullable Entity Found) {
        if (!(Found instanceof LivingEntity Body) || Body instanceof Scp082Fernand) {
            return Kind.None;
        }
        if (Body instanceof Player) {
            return Kind.Player;
        }
        if (ControlBodies.isPersonLike(Body)) {
            return Kind.Person;
        }
        if (Body instanceof AbstractVillager) {
            return Kind.Villager;
        }
        return Kind.None;
    }

    public static boolean isPrey(@Nullable Entity Found, Scp082Fernand Hunter) {
        if (!(Found instanceof LivingEntity Body) || !Body.isAlive() || Body.isRemoved() || Body.isDeadOrDying() || Body.isSpectator() || !Targeting.isTargetable(Body)) {
            return false;
        }
        Kind Type = kindOf(Body);
        if (Type == Kind.None || Body.isBaby()) {
            return false;
        }
        if (Body instanceof Player User && (User.isCreative() || sparesPlayers(Hunter) || Scp268.isNeglected(User))) {
            return false;
        }
        if (Scp049Hold.isHeld(Body) || Scp082Hold.isHeld(Body) && !Scp082Hold.heldBy(Body, Hunter)) {
            return false;
        }
        return Body.getVehicle() == null || Body.getVehicle() == Hunter || !(Body.getVehicle() instanceof Scp082Fernand);
    }

    public static boolean fitsHand(LivingEntity Body) {
        return Body.getBbHeight() <= MaxVictimHeight && Body.getBbWidth() <= 1.2f;
    }

    public static boolean isNoticeable(LivingEntity Target) {
        return !Target.isInvisible() || Target.hasEffect(MobEffects.GLOWING);
    }

    public static float mealOf(Kind Type) {
        return Type == Kind.Villager ? VillagerMeal : PersonMeal;
    }

    public static boolean isFood(ItemStack Stack) {
        return !Stack.isEmpty() && Stack.has(DataComponents.FOOD);
    }

    public static float snackOf(ItemStack Stack) {
        if (Stack.is(Tags.Items.FOODS_RAW_MEAT)) {
            return RawMeatSnack;
        }
        if (Stack.is(Tags.Items.FOODS_RAW_FISH)) {
            return RawFishSnack;
        }
        if (Stack.is(Tags.Items.FOODS_COOKED_MEAT) || Stack.is(Tags.Items.FOODS_COOKED_FISH)) {
            return CookedSnack;
        }
        if (Stack.is(Items.ROTTEN_FLESH)) {
            return FleshSnack;
        }
        FoodProperties Food = Stack.get(DataComponents.FOOD);
        return Food == null ? 0.0f : Math.max(1.0f, Food.nutrition() * OtherShare);
    }
}
