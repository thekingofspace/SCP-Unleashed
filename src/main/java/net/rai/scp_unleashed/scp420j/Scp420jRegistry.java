package net.rai.scp_unleashed.scp420j;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.List;

public final class Scp420jRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_420_j");

    public static final DeferredBlock<Scp420jHempBlock> Hemp = BlockRegister.register("scp420j_hemp",
            () -> new Scp420jHempBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak()
                    .sound(SoundType.CROP).pushReaction(PushReaction.DESTROY)));

    public static final DeferredItem<Scp420jSeedsItem> Seeds = ItemRegister.register("scp420j_seeds",
            () -> new Scp420jSeedsItem(Hemp.get(), new Item.Properties()));
    public static final DeferredItem<Scp420jLeafItem> Leaf = ItemRegister.register("scp420j_leaf",
            () -> new Scp420jLeafItem(new Item.Properties()));
    public static final DeferredItem<Scp420jJointItem> Joint = ItemRegister.register("scp420j_joint",
            () -> new Scp420jJointItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<SoundEvent, SoundEvent> Mandeville = SoundEventRegister.register("scp420j_mandeville",
            () -> SoundEvent.createFixedRangeEvent(location("scp420j_mandeville"), Scp420jFloat.SongRange));
    public static final DeferredHolder<SoundEvent, SoundEvent> Flick = sound("scp420j_flick");
    public static final DeferredHolder<SoundEvent, SoundEvent> Ember = sound("scp420j_ember");
    public static final DeferredHolder<SoundEvent, SoundEvent> Puff = sound("scp420j_puff");
    public static final DeferredHolder<SoundEvent, SoundEvent> PuffSoft = sound("scp420j_puff_soft");
    public static final DeferredHolder<SoundEvent, SoundEvent> Sigh = sound("scp420j_sigh");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LightLayers = List.of(Flick, Ember);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PuffLayers = List.of(Puff, PuffSoft);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Ring = ParticleRegister.register("scp420j_ring", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Note = ParticleRegister.register("scp420j_note", () -> new SimpleParticleType(false));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp420jFloat.State>> Floating = AttachmentRegister.register("scp420j_float",
            () -> AttachmentType.builder(() -> Scp420jFloat.State.Clear).serialize(Scp420jFloat.State.StateCodec, Saved -> !Saved.isEmpty()).build());

    private Scp420jRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void playLayers(Level World, Vec3 Where, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Where.x, Where.y, Where.z, Layer.get(), SoundSource.PLAYERS, Volume, Pitch);
        }
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
