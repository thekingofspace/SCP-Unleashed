package net.rai.scp_unleashed.scp1762;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class Scp1762Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_1762");

    public static final DeferredHolder<SoundEvent, SoundEvent> LidOpen = sound("scp1762_lid_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> LidOpenRustle = sound("scp1762_lid_open_rustle");
    public static final DeferredHolder<SoundEvent, SoundEvent> LidClose = sound("scp1762_lid_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> LidCloseThump = sound("scp1762_lid_close_thump");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rattle = sound("scp1762_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> RattleTap = sound("scp1762_rattle_tap");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rustle = sound("scp1762_rustle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Flap = sound("scp1762_flap");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlapPaper = sound("scp1762_flap_paper");
    public static final DeferredHolder<SoundEvent, SoundEvent> Roar = sound("scp1762_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> RoarChirp = sound("scp1762_roar_chirp");
    public static final DeferredHolder<SoundEvent, SoundEvent> Chirp = sound("scp1762_chirp");
    public static final DeferredHolder<SoundEvent, SoundEvent> Slither = sound("scp1762_slither");
    public static final DeferredHolder<SoundEvent, SoundEvent> Scurry = sound("scp1762_scurry");
    public static final DeferredHolder<SoundEvent, SoundEvent> Startle = sound("scp1762_startle");
    public static final DeferredHolder<SoundEvent, SoundEvent> StartlePaper = sound("scp1762_startle_paper");
    public static final DeferredHolder<SoundEvent, SoundEvent> Home = sound("scp1762_home");
    public static final DeferredHolder<SoundEvent, SoundEvent> Smoke = sound("scp1762_smoke");
    public static final DeferredHolder<SoundEvent, SoundEvent> SmokeSigh = sound("scp1762_smoke_sigh");
    public static final DeferredHolder<SoundEvent, SoundEvent> Puff = sound("scp1762_puff");
    public static final DeferredHolder<SoundEvent, SoundEvent> Note = sound("scp1762_note");
    public static final DeferredHolder<SoundEvent, SoundEvent> NoteChime = sound("scp1762_note_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> NoteRead = sound("scp1762_note_read");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoxStep = sound("scp1762_box_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoxPlace = sound("scp1762_box_place");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoxBreak = sound("scp1762_box_break");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> OpenLayers = List.of(LidOpen, LidOpenRustle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CloseLayers = List.of(LidClose, LidCloseThump);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RattleLayers = List.of(Rattle, RattleTap);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FlapLayers = List.of(Flap, FlapPaper);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RoarLayers = List.of(Roar, RoarChirp);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StartleLayers = List.of(Startle, StartlePaper);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SmokeLayers = List.of(Smoke, SmokeSigh);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> NoteLayers = List.of(Note, NoteChime);

    public static final DeferredSoundType Cardboard = new DeferredSoundType(0.8f, 1.1f, BoxBreak, BoxStep, BoxPlace, BoxStep, BoxStep);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> Stage = ComponentRegister.registerComponentType("scp1762_stage",
            Builder -> Builder.persistent(Codec.intRange(0, Scp1762Stage.Last)).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> NoteLine = ComponentRegister.registerComponentType("scp1762_note",
            Builder -> Builder.persistent(Codec.intRange(0, Scp1762NoteItem.Notes - 1)).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredBlock<Scp1762BoxBlock> Box = BlockRegister.register("scp1762_box",
            () -> new Scp1762BoxBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(0.5f, 6.0f)
                    .sound(Cardboard).noOcclusion().forceSolidOn().pushReaction(PushReaction.BLOCK).isValidSpawn((State, Getter, Pos, Type) -> false)
                    .isRedstoneConductor((State, Getter, Pos) -> false).isSuffocating((State, Getter, Pos) -> false).isViewBlocking((State, Getter, Pos) -> false)));
    public static final DeferredItem<Scp1762BoxItem> BoxItem = ItemRegister.register("scp1762_box",
            () -> new Scp1762BoxItem(Box.get(), new Item.Properties().stacksTo(1).rarity(Rarity.RARE).component(Stage.get(), 0)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp1762BoxBlockEntity>> BoxEntity = BlockEntityTypeRegister.register("scp1762_box",
            () -> BlockEntityType.Builder.of(Scp1762BoxBlockEntity::new, Box.get()).build(null));
    public static final DeferredItem<Scp1762NoteItem> NoteItem = ItemRegister.register("scp1762_note",
            () -> new Scp1762NoteItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).component(NoteLine.get(), 0)));

    private static final Map<Scp1762Kind, DeferredHolder<EntityType<?>, EntityType<Scp1762Dragon>>> Dragons = new EnumMap<>(Scp1762Kind.class);

    static {
        for (Scp1762Kind Kind : Scp1762Kind.values()) {
            Dragons.put(Kind, EntityTypeRegister.register(Kind.id(), () -> EntityType.Builder.<Scp1762Dragon>of((Type, World) -> new Scp1762Dragon(Type, World, Kind), MobCategory.MISC)
                    .sized(Kind.Width, Kind.Height)
                    .eyeHeight(Kind.Height * 0.7f)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build(Kind.id())));
        }
    }

    private Scp1762Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static EntityType<Scp1762Dragon> dragon(Scp1762Kind Kind) {
        return Dragons.get(Kind).get();
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
