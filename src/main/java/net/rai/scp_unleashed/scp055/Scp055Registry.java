package net.rai.scp_unleashed.scp055;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

public final class Scp055Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_055");

    public static final DeferredHolder<SoundEvent, SoundEvent> Clasp = sound("scp055_clasp");
    public static final DeferredHolder<SoundEvent, SoundEvent> CasePlace = sound("scp055_case_place");
    public static final DeferredHolder<SoundEvent, SoundEvent> CaseBreak = sound("scp055_case_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> CaseStep = sound("scp055_case_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> CaseHit = sound("scp055_case_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> Onset = sound("scp055_onset");
    public static final DeferredHolder<SoundEvent, SoundEvent> OnsetLow = sound("scp055_onset_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Drone = sound("scp055_drone");
    public static final DeferredHolder<SoundEvent, SoundEvent> StaticHiss = sound("scp055_static_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> StaticCrackle = sound("scp055_static_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> StaticRush = sound("scp055_static_rush");
    public static final DeferredHolder<SoundEvent, SoundEvent> Wake = sound("scp055_wake");

    public static final SoundType CaseSound = new DeferredSoundType(1.0f, 1.0f, CaseBreak, CaseStep, CasePlace, CaseHit, CaseStep);

    public static final DeferredBlock<Scp055CaseBlock> Case = BlockRegister.register("scp055_case", () -> new Scp055CaseBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK).strength(0.6f, 6.0f).sound(CaseSound).noOcclusion().pushReaction(PushReaction.DESTROY)
            .isValidSpawn((State, World, Pos, Kind) -> false).isRedstoneConductor((State, World, Pos) -> false)
            .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false)));
    public static final DeferredItem<Scp055CaseItem> CaseItem = ItemRegister.register("scp055_case",
            () -> new Scp055CaseItem(Case.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    private Scp055Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
