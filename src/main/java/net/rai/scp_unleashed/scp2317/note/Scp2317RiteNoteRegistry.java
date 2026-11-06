package net.rai.scp_unleashed.scp2317.note;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
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
import net.rai.scp_unleashed.scp2317.Scp2317Registry;

import java.util.List;

public final class Scp2317RiteNoteRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredBlock<Scp2317RiteNoteBlock> FloorNote = BlockRegister.register("scp2317_rite_note",
            () -> new Scp2317RiteNoteBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).sound(SoundType.WOOL).strength(-1.0f, Scp2317Registry.SealedResistance)
                    .noLootTable().noCollission().noOcclusion().pushReaction(PushReaction.BLOCK).isValidSpawn((State, Getter, Pos, Type) -> false)));
    public static final DeferredItem<Scp2317RiteNoteItem> Note = ItemRegister.register("scp001_scarlet_rite_note",
            () -> new Scp2317RiteNoteItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<SoundEvent, SoundEvent> Take = sound("scp2317_rite_note_take");
    public static final DeferredHolder<SoundEvent, SoundEvent> TakeRustle = sound("scp2317_rite_note_rustle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Read = sound("scp2317_rite_note_read");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TakeLayers = List.of(Take, TakeRustle);

    private Scp2317RiteNoteRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(Scp2317Registry.location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
    }
}
