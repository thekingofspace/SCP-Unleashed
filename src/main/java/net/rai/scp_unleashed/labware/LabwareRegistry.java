package net.rai.scp_unleashed.labware;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public final class LabwareRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuTypeRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents DataComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> Sample = DataComponentRegister.registerComponentType("labware_sample",
            Builder -> Builder.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> Opened = DataComponentRegister.registerComponentType("labware_opened",
            Builder -> Builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LabSwabRecord>> Swabbed = DataComponentRegister.registerComponentType("labware_swab",
            Builder -> Builder.persistent(LabSwabRecord.RecordCodec).networkSynchronized(LabSwabRecord.RecordStream));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AssayRecord>> Assay = DataComponentRegister.registerComponentType("labware_assay",
            Builder -> Builder.persistent(AssayRecord.AssayCodec).networkSynchronized(AssayRecord.AssayStream));

    public static final DeferredBlock<LabPetriDishBlock> PetriDish = BlockRegister.register("lab_petri_dish", () -> new LabPetriDishBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.QUARTZ).strength(0.4f, 1.0f).sound(SoundType.GLASS).noOcclusion().pushReaction(PushReaction.DESTROY)
            .isValidSpawn((State, Getter, Pos, Kind) -> false)));
    public static final DeferredBlock<LabTubeHolderBlock> TubeHolder = BlockRegister.register("lab_tube_holder", () -> new LabTubeHolderBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(1.2f, 3.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<LabViralStorageBlock> ViralStorage = BlockRegister.register("lab_viral_storage", () -> new LabViralStorageBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY).requiresCorrectToolForDrops().strength(3.5f, 8.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<LabAnalyzerBlock> Analyzer = BlockRegister.register("lab_analyzer", () -> new LabAnalyzerBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5f, 6.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK)
            .lightLevel(State -> State.getValue(LabAnalyzerBlock.Running) ? LabAnalyzerBlock.ActiveLight : 0)));

    public static final DeferredItem<LabVialItem> Vial = ItemRegister.register("lab_vial", () -> new LabVialItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<LabSwabItem> Swab = ItemRegister.register("lab_swab", () -> new LabSwabItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<DisinfectingWipeItem> Wipe = ItemRegister.register("disinfecting_wipe", () -> new DisinfectingWipeItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<LabResultSlipItem> ResultSlip = ItemRegister.register("lab_result_slip", () -> new LabResultSlipItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<BlockItem> PetriDishItem = ItemRegister.registerSimpleBlockItem("lab_petri_dish", PetriDish);
    public static final DeferredItem<BlockItem> TubeHolderItem = ItemRegister.registerSimpleBlockItem("lab_tube_holder", TubeHolder);
    public static final DeferredItem<BlockItem> ViralStorageItem = ItemRegister.registerSimpleBlockItem("lab_viral_storage", ViralStorage, new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> AnalyzerItem = ItemRegister.registerSimpleBlockItem("lab_analyzer", Analyzer, new Item.Properties().rarity(Rarity.UNCOMMON));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LabPetriDishBlockEntity>> PetriDishEntity =
            BlockEntityTypeRegister.register("lab_petri_dish", () -> BlockEntityType.Builder.of(LabPetriDishBlockEntity::new, PetriDish.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LabTubeHolderBlockEntity>> TubeHolderEntity =
            BlockEntityTypeRegister.register("lab_tube_holder", () -> BlockEntityType.Builder.of(LabTubeHolderBlockEntity::new, TubeHolder.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LabViralStorageBlockEntity>> ViralStorageEntity =
            BlockEntityTypeRegister.register("lab_viral_storage", () -> BlockEntityType.Builder.of(LabViralStorageBlockEntity::new, ViralStorage.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LabAnalyzerBlockEntity>> AnalyzerEntity =
            BlockEntityTypeRegister.register("lab_analyzer", () -> BlockEntityType.Builder.of(LabAnalyzerBlockEntity::new, Analyzer.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<LabViralStorageMenu>> ViralStorageMenuType = MenuTypeRegister.register("lab_viral_storage",
            () -> new MenuType<>(LabViralStorageMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<LabAnalyzerMenu>> AnalyzerMenuType = MenuTypeRegister.register("lab_analyzer",
            () -> IMenuTypeExtension.create(LabAnalyzerMenu::new));

    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> VentPlume = ParticleRegister.register("labware_vent_plume",
            () -> new ParticleType<ColorParticleOption>(true) {
                @Override
                public MapCodec<ColorParticleOption> codec() {
                    return ColorParticleOption.codec(this);
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, ColorParticleOption> streamCodec() {
                    return ColorParticleOption.streamCodec(this);
                }
            });

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<List<ResourceLocation>>> Contamination = AttachmentRegister.register("labware_contamination",
            () -> AttachmentType.builder(() -> List.<ResourceLocation>of()).serialize(ResourceLocation.CODEC.listOf(), Saved -> !Saved.isEmpty()).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<LabContamination.Rinse>> Rinse = AttachmentRegister.register("labware_rinse",
            () -> AttachmentType.builder(() -> LabContamination.Rinse.Idle).build());

    private LabwareRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        MenuTypeRegister.register(ModEventBus);
        LabwareSounds.SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        DataComponentRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
    }
}
