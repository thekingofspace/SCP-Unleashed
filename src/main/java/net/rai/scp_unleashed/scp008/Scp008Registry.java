package net.rai.scp_unleashed.scp008;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.labware.LabOrganisms;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.List;

public final class Scp008Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_008");

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> Infection = AttachmentRegister.register("scp008_infection",
            () -> AttachmentType.builder(() -> Boolean.FALSE).serialize(Codec.BOOL, Saved -> Saved).build());

    public static final DeferredHolder<SoundEvent, SoundEvent> ZombieAmbient = sound("scp008_zombie_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZombieHurt = sound("scp008_zombie_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZombieDeath = sound("scp008_zombie_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZombieStep = sound("scp008_zombie_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZombieRise = sound("scp008_zombie_rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZombieRiseLow = sound("scp008_zombie_rise_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZombieCollapse = sound("scp008_zombie_collapse");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZombieCollapseDust = sound("scp008_zombie_collapse_dust");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Risen = List.of(ZombieRise, ZombieRiseLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Collapsed = List.of(ZombieCollapse, ZombieCollapseDust);

    public static final DeferredHolder<EntityType<?>, EntityType<Scp008Zombie>> ZombieType = EntityTypeRegister.register("scp008_zombie",
            () -> EntityType.Builder.<Scp008Zombie>of(Scp008Zombie::new, MobCategory.MONSTER).sized(0.6f, 1.95f).eyeHeight(1.74f).clientTrackingRange(8)
                    .build("scp008_zombie"));

    public static final DeferredItem<DeferredSpawnEggItem> ZombieEgg = ItemRegister.register("scp008_zombie_spawn_egg",
            () -> new DeferredSpawnEggItem(ZombieType, 0x6E7F62, 0xB9C9A8, new Item.Properties().rarity(Rarity.RARE)));

    private Scp008Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        LabOrganisms.register(new Scp008Organism());
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
