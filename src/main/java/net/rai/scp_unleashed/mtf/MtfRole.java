package net.rai.scp_unleashed.mtf;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.gear.GearRegistry;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.scp127.Scp127Registry;
import net.rai.scp_unleashed.scp148.Scp148Registry;

public enum MtfRole {
    Sidearm("sidearm", 6.0, 14.0, false),
    Pointman("pointman", 3.0, 8.0, false),
    Rifleman("rifleman", 8.0, 20.0, false),
    Marksman("marksman", 16.0, 36.0, false),
    Leader("leader", 8.0, 18.0, true),
    Bearer("bearer", 7.0, 18.0, true);

    private final String Key;
    private final double Near;
    private final double Far;
    private final boolean Anomalous;

    MtfRole(String Key, double Near, double Far, boolean Anomalous) {
        this.Key = Key;
        this.Near = Near;
        this.Far = Far;
        this.Anomalous = Anomalous;
    }

    public String key() {
        return Key;
    }

    public double near() {
        return Near;
    }

    public double far() {
        return Far;
    }

    public boolean anomalous() {
        return Anomalous;
    }

    public Component displayName() {
        return Component.translatable("mtf.scp_unleashed.role." + Key);
    }

    public ItemStack weapon() {
        return switch (this) {
            case Sidearm -> GunItem.loaded(GunKind.Pistol);
            case Pointman -> GunItem.loaded(GunKind.Shotgun);
            case Rifleman, Leader -> GunItem.loaded(GunKind.AssaultRifle);
            case Marksman -> GunItem.loaded(GunKind.SniperRifle);
            case Bearer -> new ItemStack(Scp127Registry.LivingGun.get());
        };
    }

    public ItemStack helmet() {
        return this == Leader ? new ItemStack(Scp148Registry.Helmet.get()) : new ItemStack(GearRegistry.MtfHelmet.get());
    }

    public static MtfRole byIndex(int Index) {
        MtfRole[] All = values();
        return All[Math.floorMod(Index, All.length)];
    }
}
