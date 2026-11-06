package net.rai.scp_unleashed.guards;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record PatrolRoute(UUID RouteId, ResourceKey<Level> Dimension, BlockPos Manager, List<PatrolPoint> Points, boolean PingPong, GuardRules Rules,
                          Optional<UUID> Owner, String OwnerName) {
    public static final int MaxPoints = 32;
    public static final Codec<PatrolRoute> RouteCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            UUIDUtil.CODEC.fieldOf("RouteId").forGetter(PatrolRoute::RouteId),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("Dimension").forGetter(PatrolRoute::Dimension),
            BlockPos.CODEC.fieldOf("Manager").forGetter(PatrolRoute::Manager),
            PatrolPoint.PointCodec.listOf().fieldOf("Points").forGetter(PatrolRoute::Points),
            Codec.BOOL.optionalFieldOf("PingPong", false).forGetter(PatrolRoute::PingPong),
            GuardRules.RulesCodec.optionalFieldOf("Rules", GuardRules.Default).forGetter(PatrolRoute::Rules),
            UUIDUtil.CODEC.optionalFieldOf("Owner").forGetter(PatrolRoute::Owner),
            Codec.STRING.optionalFieldOf("OwnerName", "").forGetter(PatrolRoute::OwnerName)
    ).apply(Instance, PatrolRoute::new));
    public static final StreamCodec<ByteBuf, PatrolRoute> RouteStreamCodec = ByteBufCodecs.fromCodec(RouteCodec);

    public PatrolRoute {
        Manager = Manager.immutable();
        Points = List.copyOf(Points.size() > MaxPoints ? Points.subList(0, MaxPoints) : Points);
    }

    public boolean isEmpty() {
        return Points.isEmpty();
    }

    public boolean single() {
        return Points.size() == 1;
    }

    public int size() {
        return Points.size();
    }

    public PatrolPoint point(int Index) {
        return Points.get(Math.floorMod(Index, Points.size()));
    }

    public boolean isOwner(UUID Id) {
        return Owner.isPresent() && Owner.get().equals(Id);
    }

    public int indexOf(BlockPos Pos) {
        for (int Index = 0; Index < Points.size(); Index++) {
            if (Points.get(Index).Pos().equals(Pos)) {
                return Index;
            }
        }
        return -1;
    }
}
