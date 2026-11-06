package net.rai.scp_unleashed.security;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunsRegistry;
import org.jetbrains.annotations.Nullable;

public enum TurretRound {
    Handgun("handgun_rounds", GunKind.Pistol, 5.0f, 1, 0.0f, 1.0f, 6, 0.9f),
    Rifle("rifle_rounds", GunKind.AssaultRifle, 4.5f, 1, 0.0f, 1.4f, 4, 0.85f),
    Buckshot("shotgun_shells", GunKind.DoubleBarrel, 2.6f, 8, 5.0f, 1.0f, 18, 0.9f),
    Sniper("sniper_rounds", GunKind.SniperRifle, 28.0f, 1, 0.0f, 0.2f, 32, 0.95f),
    Nails("nails", GunKind.Spiker, 2.5f, 1, 0.0f, 2.0f, 4, 1.0f);

    private final String AmmoName;
    private final GunKind Kind;
    private final float Damage;
    private final int Pellets;
    private final float PelletSpread;
    private final float Spread;
    private final int Interval;
    private final float Volume;

    TurretRound(String AmmoName, GunKind Kind, float Damage, int Pellets, float PelletSpread, float Spread, int Interval, float Volume) {
        this.AmmoName = AmmoName;
        this.Kind = Kind;
        this.Damage = Damage;
        this.Pellets = Pellets;
        this.PelletSpread = PelletSpread;
        this.Spread = Spread;
        this.Interval = Interval;
        this.Volume = Volume;
    }

    public Item getItem() {
        Item Found = GunsRegistry.ammoItem(AmmoName);
        return Found == null ? Items.AIR : Found;
    }

    public GunKind getKind() {
        return Kind;
    }

    public float getDamage() {
        return Damage;
    }

    public int getPellets() {
        return Pellets;
    }

    public float getPelletSpread() {
        return PelletSpread;
    }

    public float getSpread() {
        return Spread;
    }

    public int getInterval() {
        return Interval;
    }

    public float getVolume() {
        return Volume;
    }

    public float perSecond() {
        return Damage * Pellets * 20.0f / Interval;
    }

    @Nullable
    public static TurretRound of(ItemStack Stack) {
        if (Stack.isEmpty()) {
            return null;
        }
        for (TurretRound Round : values()) {
            Item Ammo = Round.getItem();
            if (Ammo != Items.AIR && Stack.is(Ammo)) {
                return Round;
            }
        }
        return null;
    }

    public static TurretRound byIndex(int Index) {
        TurretRound[] All = values();
        return All[Math.floorMod(Index, All.length)];
    }
}
