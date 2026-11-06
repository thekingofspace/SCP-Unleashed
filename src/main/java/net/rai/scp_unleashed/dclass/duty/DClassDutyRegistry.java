package net.rai.scp_unleashed.dclass.duty;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public final class DClassDutyRegistry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final TagKey<Block> Cleanable = BlockTags.create(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "cleanable"));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<DClassDuty>> Duty = AttachmentRegister.register("dclass_duty",
            () -> AttachmentType.builder(() -> DClassDuty.Idle).serialize(DClassDuty.DutyCodec, DClassDuty::busy).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<DClassGaze.State>> Gaze = AttachmentRegister.register("dclass_gaze",
            () -> AttachmentType.builder(DClassGaze.State::new).build());

    public static final DeferredItem<DClassManagerItem> Manager = ItemRegister.register("dclass_manager",
            () -> new DClassManagerItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<MenuType<?>, MenuType<DClassManagerMenu>> ManagerMenuType = MenuRegister.register("dclass_manager",
            () -> IMenuTypeExtension.create(DClassManagerMenu::new));

    public static final DeferredHolder<SoundEvent, SoundEvent> Scrub = sound("dclass_scrub");
    public static final DeferredHolder<SoundEvent, SoundEvent> ScrubWet = sound("dclass_scrub_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> ScrubDrag = sound("dclass_scrub_drag");
    public static final DeferredHolder<SoundEvent, SoundEvent> ScrubDone = sound("dclass_scrub_done");
    public static final DeferredHolder<SoundEvent, SoundEvent> ScrubWring = sound("dclass_scrub_wring");
    public static final DeferredHolder<SoundEvent, SoundEvent> ManagerWrite = sound("dclass_manager_write");
    public static final DeferredHolder<SoundEvent, SoundEvent> ManagerTap = sound("dclass_manager_tap");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ScrubLayers = List.of(Scrub, ScrubWet, ScrubDrag);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ScrubDoneLayers = List.of(ScrubDone, ScrubWring);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ManagerLayers = List.of(ManagerWrite, ManagerTap);

    private DClassDutyRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        MenuRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
    }
}
