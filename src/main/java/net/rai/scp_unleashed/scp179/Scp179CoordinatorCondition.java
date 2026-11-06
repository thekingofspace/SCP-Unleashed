package net.rai.scp_unleashed.scp179;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.InvestigationTracker;
import net.rai.scp_unleashed.archive.investigation.Investigations;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.List;
import java.util.Optional;

public record Scp179CoordinatorCondition(ResourceLocation Target) implements StartCondition {
    public static final String TypeName = "scp179_coordinator";
    public static final MapCodec<Scp179CoordinatorCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            ResourceLocation.CODEC.optionalFieldOf("target", Scp179Registry.TargetId).forGetter(Scp179CoordinatorCondition::Target)
    ).apply(Instance, Scp179CoordinatorCondition::new));

    @Override
    public String getTypeName() {
        return TypeName;
    }

    public static void give(ServerPlayer Reader, ItemStack Stack) {
        if (!Reader.getInventory().add(Stack)) {
            Reader.drop(Stack, false);
        }
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        give(Reader, SpaceCoordinatorItem.create(Target));
        Case.getState().putBoolean("Given", true);
        Case.markDirty();
        return List.of(
                Component.translatable("archive.scp_unleashed.notes.scp179_coordinator"),
                Component.translatable("archive.scp_unleashed.notes.scp179_coordinator.advice"));
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        if (!Case.getState().getBoolean("Given")) {
            give(Reader, SpaceCoordinatorItem.create(Target));
            Case.getState().putBoolean("Given", true);
            Case.markDirty();
        }
        Investigations.get(Reader.server).remove(Reader.getUUID(), Case);
        InvestigationTracker.markResolved(Reader, Case.getDocument(), Case.getId());
        return Optional.empty();
    }
}
