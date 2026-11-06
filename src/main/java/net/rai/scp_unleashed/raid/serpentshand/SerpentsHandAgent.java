package net.rai.scp_unleashed.raid.serpentshand;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.gear.GearRegistry;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.raid.hostile.HostileFaction;
import net.rai.scp_unleashed.raid.hostile.HostileLevels;
import net.rai.scp_unleashed.raid.hostile.HostileRaider;

public class SerpentsHandAgent extends HostileRaider {
    public static final double[] Health = {18.0, 22.0, 26.0, 30.0, 34.0, 38.0};
    public static final double Speed = 0.31;
    public static final GunKind[][] Guns = {
            {GunKind.Pistol},
            {GunKind.Pistol, GunKind.Revolver},
            {GunKind.Revolver, GunKind.DoubleBarrel},
            {GunKind.Revolver, GunKind.Spiker, GunKind.DoubleBarrel},
            {GunKind.Spiker, GunKind.AssaultRifle, GunKind.Revolver},
            {GunKind.AssaultRifle, GunKind.Spiker, GunKind.Revolver}
    };

    public SerpentsHandAgent(EntityType<? extends SerpentsHandAgent> Type, Level World) {
        super(Type, World);
    }

    @Override
    public HostileFaction faction() {
        return SerpentsHandFaction.Instance;
    }

    @Override
    protected void addSeekGoals() {
        goalSelector.addGoal(2, new SerpentCaptureGoal(this));
    }

    public static GunKind gunFor(int Level, int Index) {
        GunKind[] Choices = Guns[HostileLevels.index(Level)];
        return Choices[Math.floorMod(Index, Choices.length)];
    }

    @Override
    protected void dress(int Level, int Index, RandomSource Random) {
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(GearRegistry.SerpentsHandHood.get()));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(GearRegistry.SerpentsHandJacket.get()));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(GearRegistry.SerpentsHandTrousers.get()));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(GearRegistry.SerpentsHandBoots.get()));
        setItemSlot(EquipmentSlot.MAINHAND, GunItem.loaded(gunFor(Level, Index)));
        base(Attributes.MOVEMENT_SPEED, Speed);
    }

    @Override
    protected double healthFor(int Level) {
        return HostileLevels.pick(Health, Level);
    }

    @Override
    protected DustParticleOptions mote() {
        return SerpentPods.Teal;
    }
}
