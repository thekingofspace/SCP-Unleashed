package net.rai.scp_unleashed.guns;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public enum GunKind {
    Pistol("pistol", "handgun_rounds", "semi_auto", new GunStats().damage(5.0F).interval(4).magazine(12, 32).spread(2.4F, 0.35F)
            .bloom(0.9F, 3.2F, 0.3F).range(64.0F, 24.0F, 56.0F, 0.6F).recoil(2.4F, 0.8F).aim(0.86F, 4).knockback(0.12F)),
    Revolver("revolver", "handgun_rounds", "double_action", new GunStats().damage(12.0F).interval(12).magazine(6, 52).spread(2.8F, 0.25F)
            .bloom(1.6F, 4.0F, 0.35F).range(80.0F, 32.0F, 72.0F, 0.7F).recoil(6.0F, 1.5F).aim(0.84F, 5).knockback(0.35F)),
    AssaultRifle("assault_rifle", "rifle_rounds", "full_auto", new GunStats().damage(4.5F).interval(2).automatic().magazine(30, 46).spread(3.4F, 0.7F)
            .bloom(0.32F, 4.5F, 0.4F).range(96.0F, 40.0F, 90.0F, 0.6F).recoil(1.15F, 0.55F).aim(0.8F, 6).knockback(0.05F).longGun()),
    Shotgun("shotgun", "shotgun_shells", "pump_action", new GunStats().damage(2.6F).pellets(9, 5.5F).interval(17).magazine(6, 64).spread(1.5F, 0.6F)
            .bloom(1.0F, 2.0F, 0.3F).range(36.0F, 6.0F, 26.0F, 0.25F).recoil(7.0F, 1.8F).aim(0.88F, 6).knockback(0.6F).longGun()),
    SniperRifle("sniper_rifle", "sniper_rounds", "bolt_action", new GunStats().damage(30.0F).interval(30).magazine(5, 64).spread(8.0F, 0.0F)
            .bloom(2.0F, 4.0F, 0.3F).movement(4.0F, 6.0F).range(256.0F, 128.0F, 256.0F, 0.85F).headshot(2.0F).recoil(9.0F, 1.2F).aim(0.2F, 8).scoped()
            .knockback(0.45F).longGun()),
    ParticleDisruptor("particle_disruptor", null, "particle_beam", new GunStats().damage(150.0F).interval(40).charges(3, 18).spread(0.0F, 0.0F)
            .bloom(0.0F, 0.0F, 1.0F).movement(0.0F, 0.0F).range(128.0F, 128.0F, 128.0F, 1.0F).headshot(1.0F).recoil(12.0F, 2.0F).aim(0.8F, 7)
            .piercing().knockback(1.6F).longGun()),
    DoubleBarrel("double_barrel", "shotgun_shells", "break_action", new GunStats().damage(2.9F).pellets(9, 6.0F).interval(5).magazine(2, 34).spread(1.6F, 0.7F)
            .bloom(1.4F, 3.0F, 0.3F).range(30.0F, 5.0F, 22.0F, 0.25F).recoil(9.0F, 2.2F).aim(0.9F, 5).knockback(0.75F).longGun()),
    Spiker("spiker", "nails", "pneumatic", new GunStats().damage(3.0F).interval(2).automatic().magazine(40, 36).spread(3.0F, 1.2F)
            .bloom(0.25F, 3.0F, 0.45F).range(20.0F, 6.0F, 16.0F, 0.4F).recoil(0.8F, 0.6F).aim(0.9F, 4).knockback(0.04F).quiet()),
    Ultralaser("ultralaser", "ultralaser_cell", "ion_beam", new GunStats().damage(800.0F).interval(20).magazine(1, 70).spread(0.0F, 0.0F)
            .bloom(0.0F, 0.0F, 1.0F).movement(0.0F, 0.0F).range(96.0F, 96.0F, 96.0F, 1.0F).headshot(1.0F).recoil(16.0F, 3.0F).aim(0.78F, 10)
            .piercing().knockback(2.5F).longGun()),
    Scp127("scp127_living_gun", null, "full_auto", new GunStats().damage(8.0F).interval(2).automatic().magazine(30, 40).spread(2.6F, 0.5F)
            .bloom(0.28F, 3.6F, 0.45F).range(72.0F, 28.0F, 60.0F, 0.7F).recoil(0.9F, 0.45F).aim(0.86F, 4).knockback(0.06F).longGun()),
    MicroHid("micro_hid", null, "tesla_beam", new GunStats().damage(16.0F).interval(4).spread(0.0F, 0.0F).bloom(0.0F, 0.0F, 1.0F).movement(0.0F, 0.0F)
            .range(22.0F, 22.0F, 22.0F, 1.0F).headshot(1.0F).recoil(0.0F, 0.0F).aim(1.0F, 8).knockback(0.12F).quiet().longGun().heavy()),
    CombatShotgun("combat_shotgun", "shotgun_shells", "semi_auto", new GunStats().damage(2.2F).pellets(8, 4.5F).interval(7).magazine(8, 56).spread(1.6F, 0.6F)
            .bloom(1.2F, 3.2F, 0.3F).range(40.0F, 7.0F, 28.0F, 0.25F).recoil(5.2F, 1.5F).aim(0.88F, 6).knockback(0.45F).longGun()),
    MachineGun("machine_gun", "rifle_rounds", "full_auto", new GunStats().damage(6.0F).interval(2).automatic().magazine(100, 100).spread(5.0F, 1.1F)
            .bloom(0.5F, 5.5F, 0.1F).movement(3.0F, 6.0F).range(96.0F, 36.0F, 80.0F, 0.6F).recoil(1.8F, 0.9F).aim(0.82F, 10).knockback(0.08F).longGun()
            .braced(0.5F, 0.6F, 0.55F).heat(1.0F, 40.0F, 0.1F, 50).encumber(0.12F)),
    Flamethrower("flamethrower", "fuel_canister", "flame_stream", new GunStats().damage(2.5F).interval(2).automatic().magazine(100, 50).spread(0.0F, 0.0F)
            .bloom(0.0F, 0.0F, 1.0F).movement(0.0F, 0.0F).range(8.5F, 4.0F, 8.5F, 0.55F).headshot(1.0F).recoil(0.12F, 0.2F).aim(0.92F, 6).knockback(0.0F)
            .quiet().longGun()),
    ScarletRifle("scarlet_rifle", null, "scarlet_cell", new GunStats().damage(7.5F).interval(2).automatic().magazine(60, 70).spread(2.2F, 0.35F)
            .bloom(0.26F, 3.2F, 0.5F).range(110.0F, 48.0F, 100.0F, 0.7F).recoil(0.9F, 0.45F).aim(0.8F, 6).knockback(0.06F).longGun());

    private final String Name;
    @Nullable
    private final String AmmoName;
    private final String Action;
    private final GunStats Stats;

    GunKind(String Name, @Nullable String AmmoName, String Action, GunStats Stats) {
        this.Name = Name;
        this.AmmoName = AmmoName;
        this.Action = Action;
        this.Stats = Stats;
    }

    public String getName() {
        return Name;
    }

    public String getAction() {
        return Action;
    }

    public GunStats getStats() {
        return Stats;
    }

    public boolean usesAmmo() {
        return AmmoName != null;
    }

    public Item getAmmo() {
        if (AmmoName == null) {
            return Items.AIR;
        }
        return GunsRegistry.ammoItem(AmmoName);
    }

    public static GunKind byIndex(int Index) {
        GunKind[] All = values();
        return All[Math.floorMod(Index, All.length)];
    }
}
