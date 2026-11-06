package net.rai.scp_unleashed.raid;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;

public final class RaidRegistry {
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<RaidMark>> Mark = AttachmentRegister.register("raid_mark",
            () -> AttachmentType.builder(() -> RaidMark.None).serialize(RaidMark.MarkCodec, Saved -> Saved.marked()).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<RaidLoot>> Loot = AttachmentRegister.register("raid_loot",
            () -> AttachmentType.builder(() -> RaidLoot.None).serialize(RaidLoot.LootCodec, Saved -> !Saved.empty()).build());

    public static final DeferredHolder<SoundEvent, SoundEvent> Kindle = sound("raid_daevite_kindle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Warhorn = sound("raid_daevite_warhorn");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rout = sound("raid_daevite_rout");
    public static final DeferredHolder<SoundEvent, SoundEvent> Recall = sound("raid_daevite_recall");

    public static final String RuleName = "scpFactionRaids";
    public static GameRules.Key<GameRules.BooleanValue> Enabled;

    private RaidRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        AttachmentRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ModEventBus.addListener(FMLCommonSetupEvent.class, Event -> Event.enqueueWork(RaidRegistry::registerRules));
    }

    private static void registerRules() {
        if (Enabled == null) {
            Enabled = GameRules.register(RuleName, GameRules.Category.MOBS, GameRules.BooleanValue.create(true));
        }
    }

    public static boolean enabled(MinecraftServer Server) {
        if (Enabled == null) {
            return true;
        }
        GameRules.BooleanValue Rule = Server.getGameRules().getRule(Enabled);
        return Rule == null || Rule.get();
    }
}
