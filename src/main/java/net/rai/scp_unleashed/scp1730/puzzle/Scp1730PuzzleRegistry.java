package net.rai.scp_unleashed.scp1730.puzzle;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730CableJunctionBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730ClueBoardBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730GeneratorBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730HatchBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730JunctionBoxBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730LockdownPanelBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730PickupBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730PylonBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730SluiceBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730SwitchBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730TerminalBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730ValveBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730VentGrateBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Scp1730PuzzleRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    private static final List<DeferredItem<? extends Item>> BlockItems = new ArrayList<>();
    private static final List<DeferredItem<? extends Item>> Items = new ArrayList<>();

    public static final DeferredBlock<Scp1730LockdownPanelBlock> LockdownPanel = wall("scp1730_lockdown_panel", Scp1730LockdownPanelBlock::new, 4);
    public static final DeferredBlock<Scp1730JunctionBoxBlock> JunctionBox = wall("scp1730_junction_box", Scp1730JunctionBoxBlock::new, 2);
    public static final DeferredBlock<Scp1730TerminalBlock> Terminal = wall("scp1730_terminal", Scp1730TerminalBlock::new, 5);
    public static final DeferredBlock<Scp1730ClueBoardBlock> ClueBoard = wall("scp1730_clue_board", Scp1730ClueBoardBlock::new, 0);
    public static final DeferredBlock<Scp1730SwitchBlock> Switch = wall("scp1730_switch", Scp1730SwitchBlock::new, 2);
    public static final DeferredBlock<Scp1730ValveBlock> Valve = wall("scp1730_valve", Scp1730ValveBlock::new, 0);
    public static final DeferredBlock<Scp1730CableJunctionBlock> CableJunction = wall("scp1730_cable_junction", Scp1730CableJunctionBlock::new, 0);
    public static final DeferredBlock<Scp1730HatchBlock> DisposalHatch = wall("scp1730_disposal_hatch", Scp1730HatchBlock::new, 2);
    public static final DeferredBlock<Scp1730SluiceBlock> SluiceWheel = wall("scp1730_sluice_wheel", Scp1730SluiceBlock::new, 0);
    public static final DeferredBlock<Scp1730VentGrateBlock> VentGrate = block("scp1730_vent_grate",
            () -> new Scp1730VentGrateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f).requiresCorrectToolForDrops().noOcclusion()
                    .sound(SoundType.METAL).pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<Scp1730GeneratorBlock> Generator = block("scp1730_emergency_generator",
            () -> new Scp1730GeneratorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 8.0f).requiresCorrectToolForDrops().noOcclusion()
                    .sound(SoundType.NETHERITE_BLOCK).pushReaction(PushReaction.BLOCK).lightLevel(State -> State.getValue(Scp1730GeneratorBlock.Running) ? 7 : 1)));
    public static final DeferredBlock<Scp1730PylonBlock> Pylon = block("scp1730_glyph_pylon",
            () -> new Scp1730PylonBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(5.0f, 8.0f).requiresCorrectToolForDrops().noOcclusion()
                    .sound(SoundType.DEEPSLATE).pushReaction(PushReaction.BLOCK).lightLevel(State -> State.getValue(Scp1730PylonBlock.Lit) ? 9 : 3)));
    public static final DeferredBlock<Scp1730PickupBlock> Pickup = block("scp1730_pickup",
            () -> new Scp1730PickupBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).noCollission().noOcclusion().instabreak().noLootTable()
                    .sound(SoundType.METAL).pushReaction(PushReaction.DESTROY)));

    public static final DeferredItem<Scp1730PuzzleItem> Fuse = item("scp1730_fuse", 16, Rarity.COMMON);
    public static final DeferredItem<Scp1730PuzzleItem> OverrideCard = item("scp1730_override_card", 1, Rarity.UNCOMMON);
    public static final DeferredItem<Scp1730PuzzleItem> DutyKey = item("scp1730_duty_key", 1, Rarity.UNCOMMON);
    public static final DeferredItem<Scp1730PuzzleItem> SpliceKit = item("scp1730_splice_kit", 16, Rarity.COMMON);
    public static final DeferredItem<Scp1730PuzzleItem> SampleCanister = item("scp1730_sample_canister", 1, Rarity.UNCOMMON);
    public static final DeferredItem<Scp1730PuzzleItem> FuelCanister = item("scp1730_fuel_canister", 16, Rarity.COMMON);

    public static final DeferredHolder<SoundEvent, SoundEvent> Pa = sound("scp1730_puzzle_pa");
    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp1730_puzzle_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> FuseSeat = sound("scp1730_puzzle_fuse");
    public static final DeferredHolder<SoundEvent, SoundEvent> ValveTurn = sound("scp1730_puzzle_valve");
    public static final DeferredHolder<SoundEvent, SoundEvent> Refuse = sound("scp1730_puzzle_refuse");
    public static final DeferredHolder<SoundEvent, SoundEvent> VentOpen = sound("scp1730_puzzle_vent");
    public static final DeferredHolder<SoundEvent, SoundEvent> FanStop = sound("scp1730_puzzle_fan");
    public static final DeferredHolder<SoundEvent, SoundEvent> Spark = sound("scp1730_puzzle_spark");
    public static final DeferredHolder<SoundEvent, SoundEvent> GeneratorStart = sound("scp1730_puzzle_generator");
    public static final DeferredHolder<SoundEvent, SoundEvent> Beep = sound("scp1730_puzzle_beep");
    public static final DeferredHolder<SoundEvent, SoundEvent> Crank = sound("scp1730_puzzle_crank");
    public static final DeferredHolder<SoundEvent, SoundEvent> Wrong = sound("scp1730_puzzle_wrong");

    private Scp1730PuzzleRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static <B extends Block> DeferredBlock<B> wall(String Name, Function<BlockBehaviour.Properties, B> Maker, int Glow) {
        return block(Name, () -> Maker.apply(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f).requiresCorrectToolForDrops().noOcclusion()
                .sound(SoundType.METAL).pushReaction(PushReaction.BLOCK).lightLevel(State -> Glow)));
    }

    private static <B extends Block> DeferredBlock<B> block(String Name, Supplier<B> Maker) {
        DeferredBlock<B> Made = BlockRegister.register(Name, Maker);
        BlockItems.add(ItemRegister.registerSimpleBlockItem(Name, Made));
        return Made;
    }

    private static DeferredItem<Scp1730PuzzleItem> item(String Name, int Stack, Rarity Kind) {
        DeferredItem<Scp1730PuzzleItem> Made = ItemRegister.register(Name, () -> new Scp1730PuzzleItem(new Item.Properties().stacksTo(Stack).rarity(Kind)));
        Items.add(Made);
        return Made;
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static List<DeferredItem<? extends Item>> blockItems() {
        return Collections.unmodifiableList(BlockItems);
    }

    public static List<DeferredItem<? extends Item>> items() {
        return Collections.unmodifiableList(Items);
    }

    public static Item itemFor(Scp1730PickupBlock.Kind Kind) {
        return switch (Kind) {
            case Fuse -> Fuse.get();
            case OverrideCard -> OverrideCard.get();
            case DutyKey -> DutyKey.get();
            case SpliceKit -> SpliceKit.get();
            case Sample -> SampleCanister.get();
            case Fuel -> FuelCanister.get();
        };
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
    }
}
