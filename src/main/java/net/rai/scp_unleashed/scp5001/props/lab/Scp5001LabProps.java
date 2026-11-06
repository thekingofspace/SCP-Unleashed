package net.rai.scp_unleashed.scp5001.props.lab;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class Scp5001LabProps {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    private static final Map<Scp5001LabProp, DeferredBlock<Block>> Blocks = new EnumMap<>(Scp5001LabProp.class);
    private static final List<DeferredItem<BlockItem>> Items = new ArrayList<>();

    static {
        for (Scp5001LabProp Entry : Scp5001LabProp.values()) {
            DeferredBlock<Block> Made = BlockRegister.register(Entry.id(), () -> make(Entry));
            Blocks.put(Entry, Made);
            Items.add(ItemRegister.registerSimpleBlockItem(Entry.id(), Made));
        }
    }

    private Scp5001LabProps() {
    }

    private static Block make(Scp5001LabProp Entry) {
        BlockBehaviour.Properties Props = BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(1.5f, 6.0f).requiresCorrectToolForDrops()
                .noOcclusion().sound(sound(Entry)).isViewBlocking((State, World, Pos) -> false).isSuffocating((State, World, Pos) -> false)
                .isRedstoneConductor((State, World, Pos) -> false).isValidSpawn((State, World, Pos, Kind) -> false);
        int Glow = Entry.light();
        if (Glow > 0) {
            Props = Props.lightLevel(State -> Glow);
        }
        if (Entry.kind() == Scp5001LabProp.Kind.FloorTall) {
            return new Scp5001LabTallPropBlock(Entry, Props.pushReaction(PushReaction.BLOCK));
        }
        return new Scp5001LabPropBlock(Entry, Props);
    }

    private static SoundType sound(Scp5001LabProp Entry) {
        return switch (Entry.sound()) {
            case Glass -> SoundType.GLASS;
            case Stone -> SoundType.CALCITE;
            case Metal -> SoundType.METAL;
        };
    }

    public static Map<Scp5001LabProp, DeferredBlock<Block>> blocks() {
        return Collections.unmodifiableMap(Blocks);
    }

    public static List<DeferredItem<BlockItem>> items() {
        return Collections.unmodifiableList(Items);
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
    }
}
