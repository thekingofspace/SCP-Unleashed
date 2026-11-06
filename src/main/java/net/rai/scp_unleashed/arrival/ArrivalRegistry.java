package net.rai.scp_unleashed.arrival;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;

public final class ArrivalRegistry {
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ArrivalSeen>> Seen = AttachmentRegister.register("arrival_seen",
            () -> AttachmentType.builder(() -> ArrivalSeen.Fresh).serialize(ArrivalSeen.SeenCodec, Saved -> !Saved.empty()).copyOnDeath().build());

    private ArrivalRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    public static void register(IEventBus ModEventBus) {
        AttachmentRegister.register(ModEventBus);
    }
}
