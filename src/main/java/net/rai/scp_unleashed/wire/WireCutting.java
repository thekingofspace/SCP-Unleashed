package net.rai.scp_unleashed.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;
import net.rai.scp_unleashed.registry.ModSounds;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class WireCutting {
    public static final double AimSlack = 1.5 / 16.0;
    public static final double ServerSlack = 6.0 / 16.0;
    private static final double LongestSpan = Arrays.stream(WireTier.values()).mapToDouble(WireTier::getMaxLength).max().orElse(64.0);

    private WireCutting() {
    }

    public record WireHit(BlockPos Owner, BlockPos Target, WireTier Tier, Vec3 From, Vec3 To, double HalfWidth, Vec3 Point, double Distance) {
    }

    @Nullable
    public static WireHit pick(Level World, Vec3 Eye, Vec3 Look, double Reach) {
        Vec3 Ray = Look.normalize().scale(Reach);
        int Radius = Mth.ceil((Reach + LongestSpan) / 16.0);
        int CentreX = SectionPos.blockToSectionCoord(Eye.x);
        int CentreZ = SectionPos.blockToSectionCoord(Eye.z);
        WireHit Best = null;
        for (int ChunkX = CentreX - Radius; ChunkX <= CentreX + Radius; ChunkX++) {
            for (int ChunkZ = CentreZ - Radius; ChunkZ <= CentreZ + Radius; ChunkZ++) {
                LevelChunk Chunk = World.getChunkSource().getChunkNow(ChunkX, ChunkZ);
                if (Chunk == null) {
                    continue;
                }
                for (BlockEntity Entity : Chunk.getBlockEntities().values()) {
                    if (!(Entity instanceof OutletBlockEntity Outlet) || Outlet.isRemoved()) {
                        continue;
                    }
                    for (WireLink Link : Outlet.getLinks()) {
                        if (!ownsSpan(World, Outlet.getBlockPos(), Link.Target())) {
                            continue;
                        }
                        WireHit Hit = test(Outlet, Link, Eye, Ray, AimSlack);
                        if (Hit != null && (Best == null || Hit.Distance() < Best.Distance())) {
                            Best = Hit;
                        }
                    }
                }
            }
        }
        return Best;
    }

    private static boolean ownsSpan(Level World, BlockPos Pos, BlockPos Target) {
        if (Pos.asLong() < Target.asLong() || !World.isLoaded(Target)) {
            return true;
        }
        return !(World.getBlockEntity(Target) instanceof OutletBlockEntity Other) || !Other.isLinkedTo(Pos);
    }

    @Nullable
    public static WireHit test(OutletBlockEntity Outlet, WireLink Link, Vec3 Eye, Vec3 Ray, double Slack) {
        Vec3 From = Outlet.attachFor(Link);
        Vec3 To = Link.targetAttach();
        double HalfWidth = Wires.halfWidth(Link.Tier(), Link.Slot(), Outlet.getBlockPos(), Link.Target());
        Vec3 Span = To.subtract(From);
        Vec3 Gap = Eye.subtract(From);
        double RayRay = Ray.dot(Ray);
        double SpanSpan = Span.dot(Span);
        double RaySpan = Ray.dot(Span);
        double RayGap = Ray.dot(Gap);
        double SpanGap = Span.dot(Gap);
        double Denominator = RayRay * SpanSpan - RaySpan * RaySpan;
        double Along = Denominator > 1.0E-9 ? Mth.clamp((RaySpan * SpanGap - SpanSpan * RayGap) / Denominator, 0.0, 1.0) : 0.0;
        double Across = SpanSpan > 1.0E-9 ? Mth.clamp((RaySpan * Along + SpanGap) / SpanSpan, 0.0, 1.0) : 0.0;
        Along = RayRay > 1.0E-9 ? Mth.clamp((RaySpan * Across - RayGap) / RayRay, 0.0, 1.0) : 0.0;
        Vec3 OnRay = Eye.add(Ray.scale(Along));
        Vec3 OnWire = From.add(Span.scale(Across));
        if (OnRay.distanceTo(OnWire) > HalfWidth + Slack) {
            return null;
        }
        return new WireHit(Outlet.getBlockPos(), Link.Target(), Link.Tier(), From, To, HalfWidth, OnWire, Math.max(0.0, Along * Math.sqrt(RayRay) - HalfWidth));
    }

    public static boolean cut(ServerLevel World, Player User, BlockPos Owner, BlockPos Target) {
        if (!World.isLoaded(Owner) || !(World.getBlockEntity(Owner) instanceof OutletBlockEntity Outlet)) {
            return false;
        }
        WireLink Link = Outlet.linkTo(Target);
        if (Link == null) {
            return false;
        }
        Vec3 Eye = User.getEyePosition();
        WireHit Hit = test(Outlet, Link, Eye, User.getViewVector(1.0f).scale(User.blockInteractionRange() + 1.0), ServerSlack);
        if (Hit == null) {
            return false;
        }
        int Coils = Wires.coilCost(Hit.From().distanceTo(Hit.To()));
        Outlet.removeLink(Target);
        if (World.isLoaded(Target) && World.getBlockEntity(Target) instanceof OutletBlockEntity Other) {
            Other.removeLink(Owner);
        }
        Wires.dropCoils(World, BlockPos.containing(Hit.Point()), Link.Tier(), Coils);
        Vec3 Point = Hit.Point();
        World.playSound(null, Point.x, Point.y, Point.z, ModSounds.WireCutterSnip.get(), SoundSource.PLAYERS, 1.0f, 0.94f + World.getRandom().nextFloat() * 0.12f);
        World.playSound(null, Point.x, Point.y, Point.z, ModSounds.WireDisconnect.get(), SoundSource.BLOCKS, 0.45f, 0.8f + World.getRandom().nextFloat() * 0.1f);
        World.gameEvent(User, GameEvent.SHEAR, Point);
        return true;
    }
}
