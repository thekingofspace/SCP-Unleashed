package net.rai.scp_unleashed.scp335;

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
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.computer.ComputerImages;
import net.rai.scp_unleashed.computer.ComputerRegistry;

import java.util.List;

public final class Scp335Registry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceKey<DamageType> Absorbed = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp335_absorbed"));
    public static final ResourceLocation DocumentId = location("scp_335");

    public static final DeferredItem<Scp335DiskItem> Disk = ItemRegister.register("scp335_disk",
            () -> new Scp335DiskItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                    .component(ComputerRegistry.FloppyComponent.get(), Scp335Image.contents())));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Pixel = ParticleRegister.register("scp335_pixel", () -> new SimpleParticleType(true));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp335Episode>> Episode = AttachmentRegister.register("scp335_episode",
            () -> AttachmentType.builder(Scp335Episode::blank).serialize(Scp335Episode.EpisodeCodec).build());

    public static final DeferredHolder<SoundEvent, SoundEvent> DriveWhir = sound("scp335_drive_whir");
    public static final DeferredHolder<SoundEvent, SoundEvent> DriveHum = sound("scp335_drive_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> Static = sound("scp335_static");
    public static final DeferredHolder<SoundEvent, SoundEvent> StaticBits = sound("scp335_static_bits");
    public static final DeferredHolder<SoundEvent, SoundEvent> StaticCrackle = sound("scp335_static_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rush = sound("scp335_rush");
    public static final DeferredHolder<SoundEvent, SoundEvent> RushHum = sound("scp335_rush_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> Chatter = sound("scp335_chatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> Dissolve = sound("scp335_dissolve");
    public static final DeferredHolder<SoundEvent, SoundEvent> DissolveGlitch = sound("scp335_dissolve_glitch");
    public static final DeferredHolder<SoundEvent, SoundEvent> DissolveShimmer = sound("scp335_dissolve_shimmer");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DriveLayers = List.of(DriveWhir, DriveHum);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StaticLayers = List.of(Static, StaticBits, StaticCrackle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RushLayers = List.of(Rush, RushHum, StaticCrackle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DissolveLayers = List.of(Dissolve, DissolveGlitch, DissolveShimmer);

    private Scp335Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        ComputerImages.register(Scp335Image.Picture, Scp335Viewing::viewed);
    }
}
