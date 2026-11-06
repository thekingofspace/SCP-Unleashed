package net.rai.scp_unleashed.scp129;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.labware.LabOrganisms;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.ArrayList;
import java.util.List;

public final class Scp129Registry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<RecipeSerializer<?>> RecipeSerializerRegister = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_129");
    public static final ResourceKey<DamageType> SporeDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp129_spores"));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp129Infection.State>> Infection = AttachmentRegister.register("scp129_infection",
            () -> AttachmentType.builder(() -> Scp129Infection.State.Clean).serialize(Scp129Infection.State.StateCodec, Saved -> !Saved.isEmpty()).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp129Symptoms.Episode>> EpisodeState = AttachmentRegister.register("scp129_episode",
            () -> AttachmentType.builder(() -> Scp129Symptoms.Episode.Idle).build());

    public static final DeferredHolder<SoundEvent, SoundEvent> Cough = sound("scp129_cough");
    public static final DeferredHolder<SoundEvent, SoundEvent> CoughVoice = sound("scp129_cough_voice");
    public static final DeferredHolder<SoundEvent, SoundEvent> CoughLow = sound("scp129_cough_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Wheeze = sound("scp129_wheeze");
    public static final DeferredHolder<SoundEvent, SoundEvent> Seize = sound("scp129_seize");
    public static final DeferredHolder<SoundEvent, SoundEvent> Inject = sound("scp129_inject");
    public static final DeferredHolder<SoundEvent, SoundEvent> InjectClick = sound("scp129_inject_click");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CoughLayers = List.of(Cough, CoughVoice, CoughLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> InjectLayers = List.of(Inject, InjectClick);

    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> Spore = ParticleRegister.register("scp129_spore",
            () -> new ParticleType<ColorParticleOption>(false) {
                @Override
                public MapCodec<ColorParticleOption> codec() {
                    return ColorParticleOption.codec(this);
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, ColorParticleOption> streamCodec() {
                    return ColorParticleOption.streamCodec(this);
                }
            });

    public static final DeferredItem<Scp129SuppressantItem> Suppressant = ItemRegister.register("scp129_suppressant",
            () -> new Scp129SuppressantItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<Scp129SuppressantRecipe>> SuppressantSerializer = RecipeSerializerRegister.register("scp129_suppressant",
            () -> new SimpleCraftingRecipeSerializer<>(Scp129SuppressantRecipe::new));

    public static final List<Scp129Organism> Organisms = organisms();

    private Scp129Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    private static List<Scp129Organism> organisms() {
        List<Scp129Organism> Made = new ArrayList<>();
        for (Scp129Strain Strain : Scp129Strain.values()) {
            Made.add(new Scp129Organism(Strain));
        }
        return List.copyOf(Made);
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        RecipeSerializerRegister.register(ModEventBus);
        for (Scp129Organism Organism : Organisms) {
            LabOrganisms.register(Organism);
        }
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
