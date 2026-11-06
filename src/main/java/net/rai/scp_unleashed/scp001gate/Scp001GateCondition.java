package net.rai.scp_unleashed.scp001gate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import net.rai.scp_unleashed.archive.investigation.StartCondition;
import net.rai.scp_unleashed.notice.Notices;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record Scp001GateCondition(int MinDistance, int MaxDistance, int Accuracy, int Sighting) implements StartCondition {
    public static final String TypeName = "scp001_gate";
    public static final int NearDistance = 320;
    public static final int SightHeight = 30;
    public static final MapCodec<Scp001GateCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(200, 20000).optionalFieldOf("min_distance", 1500).forGetter(Scp001GateCondition::MinDistance),
            Codec.intRange(200, 20000).optionalFieldOf("max_distance", 3000).forGetter(Scp001GateCondition::MaxDistance),
            Codec.intRange(0, 256).optionalFieldOf("accuracy", 64).forGetter(Scp001GateCondition::Accuracy),
            Codec.intRange(16, 256).optionalFieldOf("sighting", 120).forGetter(Scp001GateCondition::Sighting)
    ).apply(Instance, Scp001GateCondition::new));

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
        Scp001GateSites Site = Scp001GateSites.ensure(Server, Origin, MinDistance, MaxDistance, Reader.getRandom());
        BlockPos Centre = Site.centre();
        CompoundTag State = Case.getState();
        State.putString("Dimension", LeadPlanner.dimensionOf(World));
        State.putInt("X", Centre.getX());
        State.putInt("Z", Centre.getZ());
        Case.markDirty();
        int ShownX = LeadPlanner.roughly(Centre.getX(), Accuracy, Reader.getRandom());
        int ShownZ = LeadPlanner.roughly(Centre.getZ(), Accuracy, Reader.getRandom());
        List<Component> Lines = new ArrayList<>();
        Lines.add(Component.translatable("archive.scp_unleashed.scp001_gate.notes.0"));
        Lines.add(Component.translatable("archive.scp_unleashed.notes.site", ShownX, ShownZ));
        Lines.add(Component.translatable("archive.scp_unleashed.notes.site.radius", Accuracy + Sighting));
        Lines.add(Component.translatable("archive.scp_unleashed.scp001_gate.notes.1"));
        if (!Here) {
            Lines.add(Component.translatable("archive.scp_unleashed.notes.dimension", LeadPlanner.dimensionName(LeadPlanner.dimensionOf(World))));
        }
        return Lines;
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        CompoundTag State = Case.getState();
        if (!LeadPlanner.inDimension(Reader, State.getString("Dimension"))) {
            return Optional.empty();
        }
        Scp001GateSites Site = Scp001GateSites.get(Reader.server);
        if (!Site.planned()) {
            return Optional.empty();
        }
        BlockPos Centre = Site.centre();
        double Distance = LeadPlanner.horizontalDistance(Reader, Centre.getX(), Centre.getZ());
        if (Distance <= Sighting && sees(Reader, Site)) {
            return Optional.of(new Finding(Centre, false));
        }
        if (Distance <= NearDistance && !State.getBoolean("Near")) {
            State.putBoolean("Near", true);
            Case.markDirty();
            Notices.lead(Reader, Case.getDocument(), Component.translatable("archive.scp_unleashed.scp001_gate.lead_near"), false);
            LeadPlanner.playTo(Reader, Scp001GateRegistry.Crackle, LeadPlanner.towards(Reader, Centre.getX() + 0.5, Centre.getZ() + 0.5, 6.0), 0.8f, 0.8f);
        }
        return Optional.empty();
    }

    public static boolean sees(ServerPlayer Reader, Scp001GateSites Site) {
        BlockPos Centre = Site.centre();
        ServerLevel World = Reader.serverLevel();
        if (!World.isLoaded(Centre) || !Site.done(Site.centreChunk().toLong())) {
            return false;
        }
        Vec3 Eye = Reader.getEyePosition();
        for (int Lift : new int[]{SightHeight, 12, 40}) {
            Vec3 Target = new Vec3(Centre.getX() + 0.5, Centre.getY() + Lift, Centre.getZ() + 0.5);
            HitResult Hit = World.clip(new ClipContext(Eye, Target, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Reader));
            if (Hit.getType() == HitResult.Type.MISS || Hit.getLocation().distanceTo(Target) < 14.0) {
                return true;
            }
        }
        return false;
    }
}
