package net.rai.scp_unleashed.ultralaser;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.AmmoItem;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunsRegistry;

public final class UltralaserRegistry {
    public static final String CellName = "ultralaser_cell";
    public static final int CellStack = 16;
    public static final float NoteRange = 48.0F;
    public static final float PrimingRange = 48.0F;
    public static final float PowerRange = 32.0F;
    public static final float BlastRange = 160.0F;
    public static final float ImpactRange = 96.0F;
    public static final float VentRange = 24.0F;
    public static final float SeatRange = 16.0F;

    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final DeferredItem<GunItem> Ultralaser = ItemRegister.register("ultralaser", () -> new UltralaserItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
            .fireResistant().component(GunsRegistry.LoadedAmmo.get(), 0)));
    public static final DeferredItem<AmmoItem> Cell = ItemRegister.register(CellName, () -> new AmmoItem(new Item.Properties().stacksTo(CellStack).rarity(Rarity.EPIC)));

    public static final DeferredHolder<SoundEvent, SoundEvent> Priming = fixedSound("ultralaser_priming", PrimingRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> Bell = fixedSound("ultralaser_note_bell", NoteRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> PowerUp = fixedSound("ultralaser_power_up", PowerRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> PowerDown = fixedSound("ultralaser_power_down", PowerRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> BlastBoom = fixedSound("ultralaser_blast_boom", BlastRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> BlastThunder = fixedSound("ultralaser_blast_thunder", BlastRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> BlastFirework = fixedSound("ultralaser_blast_firework", BlastRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> BlastExplode = fixedSound("ultralaser_blast_explode", BlastRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> BlastBeacon = fixedSound("ultralaser_blast_beacon", ImpactRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> BlastTwinkle = fixedSound("ultralaser_blast_twinkle", ImpactRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> Impact = fixedSound("ultralaser_impact", ImpactRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> Vent = fixedSound("ultralaser_vent", VentRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> VentGust = fixedSound("ultralaser_vent_gust", VentRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> Seat = fixedSound("ultralaser_seat", SeatRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> SeatChime = fixedSound("ultralaser_seat_chime", SeatRange);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MoteTeal = ParticleRegister.register("ultralaser_mote_teal", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MotePink = ParticleRegister.register("ultralaser_mote_pink", () -> new SimpleParticleType(true));

    public static final ResourceKey<DamageType> UltralaserDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "ultralaser"));

    static {
        GunsRegistry.adoptGun(GunKind.Ultralaser, Ultralaser);
        GunsRegistry.adoptAmmo(CellName, Cell);
    }

    private UltralaserRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> fixedSound(String Name, float Range) {
        return SoundRegister.register(Name, () -> SoundEvent.createFixedRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name), Range));
    }
}
