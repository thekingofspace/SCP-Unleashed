package net.rai.scp_unleashed.scp428;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.rai.scp_unleashed.guns.npc.NpcAmmoSource;
import net.rai.scp_unleashed.guns.npc.NpcGunner;

public final class Scp428Arms {
    public static final double BowRange = 18.0;
    public static final double CrossbowRange = 22.0;
    public static final double GunRange = 32.0;
    public static final int BowDraw = 20;
    public static final int CrossbowDraw = 25;
    public static final int GunAim = 8;
    public static final int BowRest = 30;
    public static final int CrossbowRest = 45;
    public static final int GunRest = 4;
    public static final float BowPower = 1.6f;
    public static final float CrossbowPower = 3.15f;
    public static final float Spread = 6.0f;
    public static final float GunAccuracy = 0.55f;
    public static final double ShootClose = 4.0;

    private Scp428Arms() {
    }

    public static boolean usable(Scp428Instance Body, ItemStack Weapon) {
        if (NpcGunner.isGun(Weapon)) {
            return !NpcGunner.dry(Body, Weapon);
        }
        if (!(Weapon.getItem() instanceof ProjectileWeaponItem Launcher)) {
            return false;
        }
        Scp428Record Entry = Body.record();
        return Entry != null && Entry.carried().stream().anyMatch(Launcher.getAllSupportedProjectiles());
    }

    public static double range(ItemStack Weapon) {
        if (NpcGunner.isGun(Weapon)) {
            return Math.min(GunRange, NpcGunner.range(Weapon));
        }
        return Weapon.getItem() instanceof CrossbowItem ? CrossbowRange : BowRange;
    }

    public static int draw(ItemStack Weapon) {
        if (NpcGunner.isGun(Weapon)) {
            return GunAim;
        }
        return Weapon.getItem() instanceof CrossbowItem ? CrossbowDraw : BowDraw;
    }

    public static int rest(ItemStack Weapon) {
        if (NpcGunner.isGun(Weapon)) {
            return GunRest;
        }
        return Weapon.getItem() instanceof CrossbowItem ? CrossbowRest : BowRest;
    }

    public static boolean clear(Scp428Instance Body, LivingEntity Target) {
        return NpcGunner.clearShot(Body, Target, Other -> Other instanceof Scp428Instance || Other instanceof Scp428Core);
    }

    public static boolean ready(Scp428Instance Body, ItemStack Weapon) {
        return !NpcGunner.isGun(Weapon) || NpcGunner.ready(Body, Weapon);
    }

    public static boolean fire(Scp428Instance Body, Scp428Record Entry, ItemStack Weapon, LivingEntity Target) {
        if (!(Body.level() instanceof ServerLevel World)) {
            return false;
        }
        Body.swing(InteractionHand.MAIN_HAND, true);
        if (NpcGunner.isGun(Weapon)) {
            NpcAmmoSource Ammo = (Cartridge, Wanted) -> Scp428Carry.take(Entry, Stack -> Stack.is(Cartridge), Wanted);
            return NpcGunner.fireAt(Body, Weapon, Target, Ammo, GunAccuracy);
        }
        if (!(Weapon.getItem() instanceof ProjectileWeaponItem Launcher)) {
            return false;
        }
        ItemStack Round = Scp428Carry.takeOne(Entry, Launcher.getAllSupportedProjectiles());
        if (Round == null) {
            return false;
        }
        boolean Crossbow = Weapon.getItem() instanceof CrossbowItem;
        float Power = Crossbow ? CrossbowPower : BowPower;
        float Wobble = Spread - World.getDifficulty().getId() * 1.5f;
        double Dx = Target.getX() - Body.getX();
        double Dz = Target.getZ() - Body.getZ();
        double Flat = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Round.is(Items.FIREWORK_ROCKET)) {
            FireworkRocketEntity Rocket = new FireworkRocketEntity(World, Round, Body, Body.getX(), Body.getEyeY() - 0.15, Body.getZ(), true);
            double Dy = Target.getY(0.5) - Rocket.getY();
            Rocket.shoot(Dx, Dy, Dz, Power * 0.5f, Wobble);
            World.addFreshEntity(Rocket);
        } else {
            AbstractArrow Shot = ProjectileUtil.getMobArrow(Body, Round, Power, Weapon);
            double Dy = Target.getY(0.3333) - Shot.getY();
            Shot.shoot(Dx, Dy + Flat * (Crossbow ? 0.05 : 0.2), Dz, Power, Wobble);
            Shot.pickup = Entry.owned() ? AbstractArrow.Pickup.ALLOWED : AbstractArrow.Pickup.DISALLOWED;
            World.addFreshEntity(Shot);
        }
        World.playSound(null, Body.getX(), Body.getY(), Body.getZ(), Crossbow ? SoundEvents.CROSSBOW_SHOOT : SoundEvents.ARROW_SHOOT, Body.getSoundSource(), 1.0f,
                1.0f / (World.random.nextFloat() * 0.4f + 1.2f) + (Weapon.getItem() instanceof BowItem ? 0.5f : 0.0f));
        return true;
    }
}
