package net.rai.scp_unleashed.scp5001.props.hab;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class Scp5001HabProps {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    private static final Map<Scp5001HabCatalog, DeferredBlock<Block>> Made = new EnumMap<>(Scp5001HabCatalog.class);
    private static final List<DeferredItem<? extends Item>> Placeables = new ArrayList<>();

    static {
        for (Scp5001HabCatalog Entry : Scp5001HabCatalog.values()) {
            DeferredBlock<Block> Registered = BlockRegister.register(Entry.id(), () -> make(Entry));
            Made.put(Entry, Registered);
            if (Entry.kind() == Scp5001HabCatalog.Kind.FloorTall) {
                Placeables.add(ItemRegister.register(Entry.id(), () -> new DoubleHighBlockItem(Registered.get(), new Item.Properties())));
            } else {
                Placeables.add(ItemRegister.register(Entry.id(), () -> new BlockItem(Registered.get(), new Item.Properties())));
            }
        }
    }

    private Scp5001HabProps() {
    }

    private static Block make(Scp5001HabCatalog Entry) {
        BlockBehaviour.Properties Props = BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(1.5f, 6.0f).requiresCorrectToolForDrops().noOcclusion()
                .sound(SoundType.METAL).isValidSpawn((State, World, Pos, Spawned) -> false);
        int Glow = Entry.glow();
        if (Glow > 0) {
            Props = Props.lightLevel(State -> Glow);
        }
        if (Entry.kind() == Scp5001HabCatalog.Kind.FloorTall) {
            return new Scp5001HabTallBlock(Props.pushReaction(PushReaction.DESTROY), Entry);
        }
        return new Scp5001HabPropBlock(Props, Entry);
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        ModEventBus.addListener(BuildCreativeModeTabContentsEvent.class, Scp5001HabProps::tabs);
    }

    private static void tabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() != ModCreativeTabs.ScpBlocks.getKey()) {
            return;
        }
        for (DeferredItem<? extends Item> Entry : Placeables) {
            Event.accept(Entry.get());
        }
    }

    public static Map<Scp5001HabCatalog, DeferredBlock<Block>> blocks() {
        return Collections.unmodifiableMap(Made);
    }

    public static List<DeferredItem<? extends Item>> items() {
        return Collections.unmodifiableList(Placeables);
    }
}
