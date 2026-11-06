package net.rai.scp_unleashed.radio;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public final class RadioRegistry {
    public static final String RadioId = "handheld_radio";
    public static final String TowerId = "radio_booster_tower";
    public static final String MastId = "radio_mast";
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> Channel = ComponentRegister.registerComponentType("radio_channel",
            Builder -> Builder.persistent(Codec.intRange(RadioCarry.MinChannel, RadioCarry.MaxChannel)).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> Switched = ComponentRegister.registerComponentType("radio_on",
            Builder -> Builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    public static final DeferredBlock<RadioTowerBlock> Tower = BlockRegister.register(TowerId, () -> new RadioTowerBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(4.0f, 6.0f).sound(SoundType.METAL).pushReaction(PushReaction.BLOCK).noOcclusion()
            .isRedstoneConductor((State, World, Pos) -> false).isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false)
            .lightLevel(State -> State.getValue(RadioTowerBlock.Powered) ? RadioTowerBlock.PoweredLight : 0)));
    public static final DeferredBlock<RadioMastBlock> Mast = BlockRegister.register(MastId, () -> new RadioMastBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.0f, 6.0f).sound(SoundType.CHAIN).pushReaction(PushReaction.DESTROY).noOcclusion()
            .isRedstoneConductor((State, World, Pos) -> false).isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false)
            .lightLevel(State -> State.getValue(RadioMastBlock.Lit) ? RadioMastBlock.LampLight : 0)));
    public static final DeferredItem<BlockItem> TowerItem = ItemRegister.register(TowerId, () -> new BlockItem(Tower.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<BlockItem> MastItem = ItemRegister.register(MastId, () -> new BlockItem(Mast.get(), new Item.Properties()));
    public static final DeferredItem<HandheldRadioItem> Radio = ItemRegister.register(RadioId, () -> new HandheldRadioItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadioTowerBlockEntity>> TowerEntity = BlockEntityRegister.register(TowerId,
            () -> BlockEntityType.Builder.of(RadioTowerBlockEntity::new, Tower.get()).build(null));

    public static final DeferredHolder<SoundEvent, SoundEvent> Click = sound("radio_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hiss = sound("radio_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Beep = sound("radio_beep");
    public static final DeferredHolder<SoundEvent, SoundEvent> Crackle = sound("radio_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Dial = sound("radio_dial");
    public static final DeferredHolder<SoundEvent, SoundEvent> BleedClank = sound("radio_bleed_clank");
    public static final DeferredHolder<SoundEvent, SoundEvent> BleedClankShiver = sound("radio_bleed_clank_shiver");
    public static final DeferredHolder<SoundEvent, SoundEvent> BleedPulse = sound("radio_bleed_pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> BleedPulseBody = sound("radio_bleed_pulse_body");
    public static final DeferredHolder<SoundEvent, SoundEvent> BleedWhisper = sound("radio_bleed_whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> BleedRefrain = sound("radio_bleed_refrain");
    public static final DeferredHolder<SoundEvent, SoundEvent> BleedRefrainBell = sound("radio_bleed_refrain_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> Corrupt = sound("radio_corrupt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CorruptCrackle = sound("radio_corrupt_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> CorruptBlip = sound("radio_corrupt_blip");
    public static final DeferredHolder<SoundEvent, SoundEvent> Glitch = sound("radio_glitch");
    public static final DeferredHolder<SoundEvent, SoundEvent> TowerStart = sound("radio_tower_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> TowerStop = sound("radio_tower_stop");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SwitchOnLayers = List.of(Click, Beep);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SwitchOffLayers = List.of(Click, Hiss);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> KeyLayers = List.of(Click, Beep);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> UnkeyLayers = List.of(Hiss, Click);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> MessageLayers = List.of(Beep, Crackle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StaticLayers = List.of(CorruptCrackle, Hiss);

    private RadioRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
    }
}
