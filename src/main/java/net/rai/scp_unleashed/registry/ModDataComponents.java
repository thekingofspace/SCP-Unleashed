package net.rai.scp_unleashed.registry;

import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents DataComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> WireStart = DataComponentRegister.registerComponentType("wire_start",
            Builder -> Builder.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Direction>> WireStartFace = DataComponentRegister.registerComponentType("wire_start_face",
            Builder -> Builder.persistent(Direction.CODEC).networkSynchronized(Direction.STREAM_CODEC));
}
