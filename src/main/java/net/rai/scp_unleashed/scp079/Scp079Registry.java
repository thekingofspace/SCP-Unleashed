package net.rai.scp_unleashed.scp079;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;

public final class Scp079Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_079");
    public static final ResourceKey<DamageType> AntiMemeticDamage = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp079_antimemetic"));

    public static final DeferredBlock<Scp079Block> Computer = BlockRegister.register("scp079_computer",
            () -> new Scp079Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).requiresCorrectToolForDrops().strength(5.0f, 3600000.0f).sound(SoundType.METAL)
                    .noOcclusion().pushReaction(PushReaction.BLOCK).isRedstoneConductor((State, World, Pos) -> false)
                    .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false).lightLevel(Scp079Block::lightOf)));

    public static final DeferredItem<Scp079ComputerItem> ComputerItem = ItemRegister.register("scp079_computer",
            () -> new Scp079ComputerItem(Computer.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<Scp079UpgradeItem> AnomalousUpgrade = ItemRegister.register("scp079_anomalous_upgrade",
            () -> new Scp079UpgradeItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp079BlockEntity>> ComputerEntity = BlockEntityTypeRegister.register("scp079_computer",
            () -> BlockEntityType.Builder.of(Scp079BlockEntity::new, Computer.get()).build(null));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Scp079Memory>> Memory = ComponentRegister.registerComponentType("scp079_memory",
            Builder -> Builder.persistent(Scp079Memory.MemoryCodec).networkSynchronized(Scp079Memory.MemoryStreamCodec));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Float>> Exposure = AttachmentRegister.register("scp079_exposure",
            () -> AttachmentType.builder(() -> 0.0f).serialize(Codec.FLOAT, Saved -> Saved > 0.0f).build());

    public static final DeferredHolder<SoundEvent, SoundEvent> Key = sound("scp079_key");
    public static final DeferredHolder<SoundEvent, SoundEvent> Boot = sound("scp079_boot");
    public static final DeferredHolder<SoundEvent, SoundEvent> BootWhine = sound("scp079_boot_whine");
    public static final DeferredHolder<SoundEvent, SoundEvent> Shutdown = sound("scp079_shutdown");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("scp079_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> Glitch = sound("scp079_glitch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Laugh = sound("scp079_laugh");
    public static final DeferredHolder<SoundEvent, SoundEvent> Refuse = sound("scp079_refuse");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pack = sound("scp079_pack");
    public static final DeferredHolder<SoundEvent, SoundEvent> Install = sound("scp079_install");
    public static final DeferredHolder<SoundEvent, SoundEvent> Signal = sound("scp079_signal");
    public static final DeferredHolder<SoundEvent, SoundEvent> Whisper = sound("scp079_whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> WhisperLow = sound("scp079_whisper_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpeakerLaugh = sound("scp079_speaker_laugh");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpeakerVoice = sound("scp079_speaker_voice");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpeakerStatic = sound("scp079_speaker_static");

    private Scp079Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        Scp079Stranger.register();
    }
}
