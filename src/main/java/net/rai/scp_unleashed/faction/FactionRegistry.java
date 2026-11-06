package net.rai.scp_unleashed.faction;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;

public final class FactionRegistry {
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<FactionBond>> Bond = AttachmentRegister.register("faction_bond",
            () -> AttachmentType.builder(() -> FactionBond.None).serialize(FactionBond.BondCodec, Saved -> Saved.bound()).build());
    private static boolean Ready;

    private FactionRegistry() {
    }

    public static synchronized void bootstrap(IEventBus ModEventBus) {
        if (Ready) {
            return;
        }
        Ready = true;
        AttachmentRegister.register(ModEventBus);
    }
}
