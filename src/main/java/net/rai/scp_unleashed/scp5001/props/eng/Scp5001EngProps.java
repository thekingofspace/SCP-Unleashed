package net.rai.scp_unleashed.scp5001.props.eng;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Scp5001EngProps {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    private static final Map<String, DeferredBlock<Block>> PropBlocks = new LinkedHashMap<>();
    private static final List<DeferredItem<BlockItem>> PropItems = new ArrayList<>();

    static {
        for (Scp5001EngProp Entry : Scp5001EngCatalog.Entries) {
            DeferredBlock<Block> Made = BlockRegister.register(Entry.id(), () -> make(Entry));
            PropBlocks.put(Entry.id(), Made);
            PropItems.add(ItemRegister.register(Entry.id(), () -> new BlockItem(Made.get(), new Item.Properties())));
        }
    }

    private Scp5001EngProps() {
    }

    private static Block make(Scp5001EngProp Entry) {
        BlockBehaviour.Properties Props = BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(1.5f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.METAL)
                .noOcclusion().isViewBlocking((State, World, Pos) -> false).isSuffocating((State, World, Pos) -> false);
        int Glow = Entry.light();
        if (Glow > 0) {
            Props = Props.lightLevel(State -> Glow);
        }
        if (Entry.tall()) {
            return new Scp5001EngTallPropBlock(Props.pushReaction(PushReaction.BLOCK), Entry);
        }
        return new Scp5001EngPropBlock(Props, Entry);
    }

    public static Map<String, DeferredBlock<Block>> blocks() {
        return Collections.unmodifiableMap(PropBlocks);
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        ModEventBus.addListener(BuildCreativeModeTabContentsEvent.class, Scp5001EngProps::fillTab);
    }

    private static void fillTab(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            for (DeferredItem<BlockItem> Made : PropItems) {
                Event.accept(Made.get());
            }
        }
    }
}
