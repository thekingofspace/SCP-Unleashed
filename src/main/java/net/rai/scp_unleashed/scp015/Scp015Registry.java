package net.rai.scp_unleashed.scp015;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

public final class Scp015Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final int DefaultSegmentLimit = 20000;
    public static GameRules.Key<GameRules.IntegerValue> SegmentLimit;
    public static final TagKey<Block> Resistant = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp015_resistant"));

    public static final DeferredBlock<Scp015PipeBlock> Pipe = BlockRegister.register("scp015_pipe", () -> new Scp015PipeBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(2.0f, 6.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK)));
    public static final DeferredItem<BlockItem> PipeItem = ItemRegister.register("scp015_pipe",
            () -> new BlockItem(Pipe.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<SoundEvent, SoundEvent> Creak = sound("scp015_pipe_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> Clank = sound("scp015_pipe_clank");
    public static final DeferredHolder<SoundEvent, SoundEvent> Groan = sound("scp015_pipe_groan");
    public static final DeferredHolder<SoundEvent, SoundEvent> GroanCreak = sound("scp015_pipe_groan_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> Drip = sound("scp015_pipe_drip");
    public static final DeferredHolder<SoundEvent, SoundEvent> Strain = sound("scp015_pipe_strain");
    public static final DeferredHolder<SoundEvent, SoundEvent> StrainClang = sound("scp015_pipe_strain_clang");

    private Scp015Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ModEventBus.addListener(FMLCommonSetupEvent.class, Event -> Event.enqueueWork(Scp015Registry::registerRules));
    }

    private static void registerRules() {
        if (SegmentLimit == null) {
            SegmentLimit = GameRules.register("scp015SegmentLimit", GameRules.Category.UPDATES, GameRules.IntegerValue.create(DefaultSegmentLimit));
        }
    }

    public static int segmentLimit(GameRules Rules) {
        if (SegmentLimit == null) {
            return DefaultSegmentLimit;
        }
        GameRules.IntegerValue Rule = Rules.getRule(SegmentLimit);
        return Rule == null ? DefaultSegmentLimit : Rule.get();
    }
}
