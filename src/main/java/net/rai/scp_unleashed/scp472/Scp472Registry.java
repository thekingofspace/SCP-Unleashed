package net.rai.scp_unleashed.scp472;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.List;

public final class Scp472Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_472");
    public static final ResourceKey<DamageType> ConsumedDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp472_consumed"));
    public static final int Glow = 5;
    public static final float Hardness = 3.0f;
    public static final float Resistance = 1200.0f;
    public static final float StonePitch = 0.62f;

    public static final DeferredHolder<SoundEvent, SoundEvent> HeartThump = sound("scp472_heart_thump");
    public static final DeferredHolder<SoundEvent, SoundEvent> HeartBody = sound("scp472_heart_body");
    public static final DeferredHolder<SoundEvent, SoundEvent> HeartFlesh = sound("scp472_heart_flesh");
    public static final DeferredHolder<SoundEvent, SoundEvent> Whisper = sound("scp472_whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> WhisperLow = sound("scp472_whisper_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> WhisperAir = sound("scp472_whisper_air");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LubLayers = List.of(HeartThump, HeartBody, HeartFlesh);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DubLayers = List.of(HeartThump, HeartFlesh);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WhisperLayers = List.of(Whisper, WhisperLow, WhisperAir);

    public static final SoundType StoneSound = new SoundType(1.0f, StonePitch, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundEvents.AMETHYST_CLUSTER_STEP,
            SoundEvents.AMETHYST_CLUSTER_PLACE, SoundEvents.AMETHYST_CLUSTER_HIT, SoundEvents.AMETHYST_CLUSTER_FALL);

    public static final DeferredBlock<Scp472BloodstoneBlock> Bloodstone = BlockRegister.register("scp472_bloodstone",
            () -> new Scp472BloodstoneBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(Hardness, Resistance).sound(StoneSound)
                    .lightLevel(State -> Glow).noOcclusion().pushReaction(PushReaction.BLOCK).isValidSpawn((State, Getter, Pos, Type) -> false)
                    .isRedstoneConductor((State, Getter, Pos) -> false).isSuffocating((State, Getter, Pos) -> false).isViewBlocking((State, Getter, Pos) -> false)));
    public static final DeferredItem<Scp472BloodstoneItem> BloodstoneItem = ItemRegister.register("scp472_bloodstone",
            () -> new Scp472BloodstoneItem(Bloodstone.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp472BloodstoneBlockEntity>> BloodstoneEntity = BlockEntityTypeRegister.register("scp472_bloodstone",
            () -> BlockEntityType.Builder.of(Scp472BloodstoneBlockEntity::new, Bloodstone.get()).build(null));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Float>> Sanity = AttachmentRegister.register("scp472_sanity",
            () -> AttachmentType.builder(() -> Scp472Mind.Full).serialize(Codec.FLOAT).build());

    private Scp472Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
