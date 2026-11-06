package net.rai.scp_unleashed.scp5001.props.ctrl;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

import java.util.EnumMap;
import java.util.Map;

public final class Scp5001CtrlProps {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final Map<Scp5001CtrlProp, DeferredBlock<Block>> Blocks = new EnumMap<>(Scp5001CtrlProp.class);
    public static final Map<Scp5001CtrlProp, DeferredItem<BlockItem>> Items = new EnumMap<>(Scp5001CtrlProp.class);

    static {
        for (Scp5001CtrlProp Entry : Scp5001CtrlProp.values()) {
            DeferredBlock<Block> Made = BlockRegister.register(Entry.id(), () -> create(Entry));
            Blocks.put(Entry, Made);
            Items.put(Entry, ItemRegister.registerSimpleBlockItem(Entry.id(), Made));
        }
    }

    private Scp5001CtrlProps() {
    }

    private static Block create(Scp5001CtrlProp Entry) {
        int Light = Entry.light();
        BlockBehaviour.Properties Props = BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(1.5f, 6.0f).sound(SoundType.METAL)
                .noOcclusion().lightLevel(State -> Light).isValidSpawn((State, World, Pos, Type) -> false).isRedstoneConductor((State, World, Pos) -> false)
                .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false);
        if (Entry.kind() == Scp5001CtrlProp.Kind.FloorTall) {
            return new Scp5001CtrlTallBlock(Props.pushReaction(PushReaction.BLOCK), Entry);
        }
        return new Scp5001CtrlBlock(Props, Entry);
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        ModEventBus.addListener(Scp5001CtrlProps::fillTabs);
    }

    private static void fillTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() != ModCreativeTabs.ScpBlocks.getKey()) {
            return;
        }
        for (DeferredItem<BlockItem> Item : Items.values()) {
            Event.accept(Item.get());
        }
    }
}
