package net.rai.scp_unleashed.scp5001.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import net.rai.scp_unleashed.archive.investigation.StartCondition;
import net.rai.scp_unleashed.notice.Notices;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record Scp5001FacilityCondition(int MinDistance, int MaxDistance, int Accuracy, int Arrival) implements StartCondition {
    public static final String TypeName = "scp5001_facility";
    public static final int NearDistance = 160;
    public static final int Reach = 6;
    public static final int DepthAccuracy = 4;
    public static final MapCodec<Scp5001FacilityCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(200, 20000).optionalFieldOf("min_distance", 900).forGetter(Scp5001FacilityCondition::MinDistance),
            Codec.intRange(200, 20000).optionalFieldOf("max_distance", 2400).forGetter(Scp5001FacilityCondition::MaxDistance),
            Codec.intRange(0, 256).optionalFieldOf("accuracy", 48).forGetter(Scp5001FacilityCondition::Accuracy),
            Codec.intRange(8, 256).optionalFieldOf("arrival", 64).forGetter(Scp5001FacilityCondition::Arrival)
    ).apply(Instance, Scp5001FacilityCondition::new));

    @Override
    public String getTypeName() {
        return TypeName;
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        MinecraftServer Server = Reader.server;
        ServerLevel World = Server.overworld();
        boolean Here = Reader.serverLevel().dimension() == Level.OVERWORLD;
        BlockPos Origin = Here ? Reader.blockPosition() : World.getSharedSpawnPos();
        Scp5001Sites Site = Scp5001Sites.ensure(Server, Origin, MinDistance, MaxDistance, Reader.getRandom());
        BlockPos Door = Site.door();
        CompoundTag State = Case.getState();
        State.putString("Dimension", LeadPlanner.dimensionOf(World));
        State.putInt("X", Door.getX());
        State.putInt("Y", Door.getY());
        State.putInt("Z", Door.getZ());
        Case.markDirty();
        return notes(Reader, Door, Here, Site.legacy());
    }

    private List<Component> notes(ServerPlayer Reader, BlockPos Door, boolean Here, boolean Legacy) {
        int ShownX = LeadPlanner.roughly(Door.getX(), Accuracy, Reader.getRandom());
        int ShownZ = LeadPlanner.roughly(Door.getZ(), Accuracy, Reader.getRandom());
        List<Component> Lines = new ArrayList<>();
        Lines.add(Component.translatable("archive.scp_unleashed.scp5001_world.notes.0"));
        Lines.add(Component.translatable("archive.scp_unleashed.notes.site", ShownX, ShownZ));
        Lines.add(Component.translatable("archive.scp_unleashed.notes.site.radius", Accuracy + Arrival));
        if (!Legacy) {
            Lines.add(Component.translatable("archive.scp_unleashed.scp5001_world.notes.depth", LeadPlanner.roughly(Door.getY(), DepthAccuracy, Reader.getRandom())));
        }
        Lines.add(Component.translatable("archive.scp_unleashed.scp5001_world.notes.1"));
        if (!Here) {
            Lines.add(Component.translatable("archive.scp_unleashed.notes.dimension", LeadPlanner.dimensionName(LeadPlanner.dimensionOf(Reader.server.overworld()))));
        }
        return Lines;
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        CompoundTag State = Case.getState();
        if (!LeadPlanner.inDimension(Reader, State.getString("Dimension"))) {
            return Optional.empty();
        }
        Scp5001Sites Site = Scp5001Sites.get(Reader.server);
        if (!Site.planned()) {
            return Optional.empty();
        }
        BlockPos Door = Site.door();
        double Distance = LeadPlanner.horizontalDistance(Reader, Door.getX(), Door.getZ());
        if (Site.legacy()) {
            if (Distance <= Arrival) {
                return Optional.of(new Finding(Door, false));
            }
        } else if (Reader.position().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(Site.target())) <= Reach) {
            return Optional.of(new Finding(Door, false));
        }
        if (Distance <= NearDistance && !State.getBoolean("Near")) {
            State.putBoolean("Near", true);
            Case.markDirty();
            Notices.lead(Reader, Case.getDocument(), Component.translatable("archive.scp_unleashed.scp5001_world.lead_near"), false);
            LeadPlanner.playTo(Reader, Scp5001WorldRegistry.EngineLow, LeadPlanner.towards(Reader, Door.getX() + 0.5, Door.getZ() + 0.5, 6.0), 0.5f, 0.6f);
        }
        return Optional.empty();
    }
}
