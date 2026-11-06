package net.rai.scp_unleashed.scp207;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.storage.StorageBehaviours;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.List;

public final class Scp207Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_207");
    public static final ResourceKey<DamageType> ExposureDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp207_exposure"));
    public static final ResourceKey<DamageType> BurstDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp207_burst"));

    public static final DeferredHolder<SoundEvent, SoundEvent> CapPop = sound("scp207_cap_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> CapHiss = sound("scp207_cap_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> CapCrimp = sound("scp207_cap_crimp");
    public static final DeferredHolder<SoundEvent, SoundEvent> Fizz = sound("scp207_fizz");
    public static final DeferredHolder<SoundEvent, SoundEvent> FizzBed = sound("scp207_fizz_bed");
    public static final DeferredHolder<SoundEvent, SoundEvent> Gulp = sound("scp207_gulp");
    public static final DeferredHolder<SoundEvent, SoundEvent> Clink = sound("scp207_clink");
    public static final DeferredHolder<SoundEvent, SoundEvent> ClinkKnock = sound("scp207_clink_knock");
    public static final DeferredHolder<SoundEvent, SoundEvent> ClinkStep = sound("scp207_clink_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrateTake = sound("scp207_crate_take");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrateEmpty = sound("scp207_crate_empty");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrateKnock = sound("scp207_crate_knock");
    public static final DeferredHolder<SoundEvent, SoundEvent> BurstFizz = sound("scp207_burst_fizz");
    public static final DeferredHolder<SoundEvent, SoundEvent> BurstBubbles = sound("scp207_burst_bubbles");
    public static final DeferredHolder<SoundEvent, SoundEvent> TollSwell = sound("scp207_toll_swell");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> OpenLayers = List.of(CapPop, CapHiss, CapCrimp);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FizzLayers = List.of(Fizz, FizzBed);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ClinkLayers = List.of(Clink, ClinkKnock);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TakeLayers = List.of(CrateTake, Clink);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> EmptyLayers = List.of(CrateEmpty, CrateKnock);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BurstLayers = List.of(BurstFizz, BurstBubbles);

    public static final SoundType BottleSound = new DeferredSoundType(0.9f, 1.0f, Clink, ClinkStep, Clink, ClinkStep, ClinkStep);

    public static final DeferredBlock<Scp207BottlesBlock> Bottles = BlockRegister.register("scp207_bottles",
            () -> new Scp207BottlesBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).instabreak().noOcclusion().sound(BottleSound)
                    .pushReaction(PushReaction.DESTROY).isValidSpawn((State, World, Pos, Type) -> false).isRedstoneConductor((State, World, Pos) -> false)
                    .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false)));

    public static final DeferredBlock<Scp207CrateBlock> Crate = BlockRegister.register("scp207_crate",
            () -> new Scp207CrateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.0f, 1200.0f).sound(SoundType.WOOD)
                    .noOcclusion().pushReaction(PushReaction.BLOCK)));

    public static final DeferredItem<Scp207BottleItem> Bottle = ItemRegister.register("scp207_bottle",
            () -> new Scp207BottleItem(false, new Item.Properties().stacksTo(Scp207BottleItem.StackSize).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Scp207BottleItem> BottleOne = ItemRegister.register("scp207_1_bottle",
            () -> new Scp207BottleItem(true, new Item.Properties().stacksTo(Scp207BottleItem.StackSize).rarity(Rarity.RARE)));
    public static final DeferredItem<BlockItem> CrateItem = ItemRegister.register("scp207_crate",
            () -> new BlockItem(Crate.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp207CrateBlockEntity>> CrateEntity = BlockEntityTypeRegister.register("scp207_crate",
            () -> BlockEntityType.Builder.of(Scp207CrateBlockEntity::new, Crate.get()).build(null));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Scp207Ration.State>> RationData = ComponentRegister.registerComponentType("scp207_ration",
            Builder -> Builder.persistent(Scp207Ration.State.StateCodec).networkSynchronized(Scp207Ration.State.StateStreamCodec));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Scp207Rack>> RackData = ComponentRegister.registerComponentType("scp207_rack",
            Builder -> Builder.persistent(Scp207Rack.RackCodec).networkSynchronized(Scp207Rack.RackStreamCodec));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp207Toll.State>> Toll = AttachmentRegister.register("scp207_toll",
            () -> AttachmentType.builder(() -> Scp207Toll.State.Clear).serialize(Scp207Toll.State.StateCodec, Saved -> !Saved.isEmpty()).build());

    private Scp207Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void playLayers(Level World, Vec3 Where, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, SoundSource Source, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Where.x, Where.y, Where.z, Layer.get(), Source, Volume, Pitch);
        }
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
        StorageBehaviours.register(Scp207BottleItem::is, Scp207Shelf.Instance);
    }
}
