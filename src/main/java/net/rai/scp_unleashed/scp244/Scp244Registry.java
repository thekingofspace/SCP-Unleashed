package net.rai.scp_unleashed.scp244;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.List;

public final class Scp244Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_244");

    public static final DeferredHolder<SoundEvent, SoundEvent> LidLift = sound("scp244_lid_lift");
    public static final DeferredHolder<SoundEvent, SoundEvent> LidScrape = sound("scp244_lid_scrape");
    public static final DeferredHolder<SoundEvent, SoundEvent> LidClink = sound("scp244_lid_clink");
    public static final DeferredHolder<SoundEvent, SoundEvent> LidSeat = sound("scp244_lid_seat");
    public static final DeferredHolder<SoundEvent, SoundEvent> Release = sound("scp244_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> ReleaseGust = sound("scp244_release_gust");
    public static final DeferredHolder<SoundEvent, SoundEvent> Vent = sound("scp244_vent");
    public static final DeferredHolder<SoundEvent, SoundEvent> VentWind = sound("scp244_vent_wind");
    public static final DeferredHolder<SoundEvent, SoundEvent> Frost = sound("scp244_frost");
    public static final DeferredHolder<SoundEvent, SoundEvent> FrostGlint = sound("scp244_frost_glint");
    public static final DeferredHolder<SoundEvent, SoundEvent> Freeze = sound("scp244_freeze");

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Vapour = ParticleRegister.register("scp244_vapour", () -> new SimpleParticleType(false));

    public static final DeferredBlock<Scp244VaseBlock> VaseA = vase(Scp244Style.Amphora);
    public static final DeferredBlock<Scp244VaseBlock> VaseB = vase(Scp244Style.Urn);
    public static final DeferredBlock<Scp244VaseBlock> VaseC = vase(Scp244Style.GingerJar);

    public static final DeferredItem<BlockItem> VaseAItem = item(VaseA);
    public static final DeferredItem<BlockItem> VaseBItem = item(VaseB);
    public static final DeferredItem<BlockItem> VaseCItem = item(VaseC);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp244VaseBlockEntity>> VaseEntity = BlockEntityTypeRegister.register("scp244_vase",
            () -> BlockEntityType.Builder.of(Scp244VaseBlockEntity::new, VaseA.get(), VaseB.get(), VaseC.get()).build(null));

    private Scp244Registry() {
    }

    private static DeferredBlock<Scp244VaseBlock> vase(Scp244Style Style) {
        return BlockRegister.register(Style.id(), () -> new Scp244VaseBlock(Style, BlockBehaviour.Properties.of()
                .mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.BASEDRUM).strength(1.5f, 6.0f).sound(SoundType.DECORATED_POT)
                .noOcclusion().pushReaction(PushReaction.BLOCK).isValidSpawn((State, World, Pos, Kind) -> false)
                .isRedstoneConductor((State, World, Pos) -> false)));
    }

    private static DeferredItem<BlockItem> item(DeferredBlock<Scp244VaseBlock> Vase) {
        return ItemRegister.register(Vase.getId().getPath(), () -> new BlockItem(Vase.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static List<DeferredItem<BlockItem>> vaseItems() {
        return List.of(VaseAItem, VaseBItem, VaseCItem);
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
