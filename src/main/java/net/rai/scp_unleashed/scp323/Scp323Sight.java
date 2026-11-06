package net.rai.scp_unleashed.scp323;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.antimemetic.AntiMemetic;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.scp035.Scp035Sight;
import net.rai.scp_unleashed.scp268.Scp268;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import org.jetbrains.annotations.Nullable;

public final class Scp323Sight {
    private Scp323Sight() {
    }

    public static boolean hidesFace(LivingEntity Target) {
        return Target.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN) || AntiMemetic.isProtected(Target);
    }

    public static boolean wearsSkull(LivingEntity Target) {
        return Scp323SkullItem.is(Target.getItemBySlot(EquipmentSlot.HEAD));
    }

    public static boolean isExempt(Player User) {
        return !User.isAlive() || User.isCreative() || User.isSpectator();
    }

    public static boolean isDrawable(Player User) {
        return Targeting.isTargetable(User) && !isExempt(User) && !hidesFace(User) && !wearsSkull(User) && !Scp323Bond.isBound(User) && !Scp323Watch.isWatching(User) && !User.isSleeping()
                && !Scp268.isNeglected(User);
    }

    public static boolean isPerson(LivingEntity Target) {
        return ControlBodies.isPersonLike(Target);
    }

    public static boolean isDrawable(Mob Person) {
        return Person.isAlive() && Targeting.isTargetable(Person) && isPerson(Person) && !Person.isNoAi() && Person.getControllingPassenger() == null && !hidesFace(Person) && !wearsSkull(Person)
                && !Scp323Bond.isBound(Person);
    }

    public static boolean clear(Level World, Vec3 From, Vec3 To) {
        return Scp035Sight.clear(World, From, To);
    }

    @Nullable
    public static Vec3 lockerWindow(Level World, Vec3 Eye, StorageContainerBlockEntity Locker, double Range) {
        return Scp035Sight.lockerWindow(World, Eye, Locker, Range);
    }

    public static Vec3 lockerFront(StorageContainerBlockEntity Locker) {
        return Scp035Sight.lockerFront(Locker);
    }
}
