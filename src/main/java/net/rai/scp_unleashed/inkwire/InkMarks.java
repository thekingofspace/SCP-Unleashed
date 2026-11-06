package net.rai.scp_unleashed.inkwire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;
import net.rai.scp_unleashed.wire.WireCutting;
import net.rai.scp_unleashed.wire.WireLink;
import org.jetbrains.annotations.Nullable;

public final class InkMarks {
    public static final int InkColour = 0xFFB4DCF0;
    public static final int HideSwirls = 7;
    public static final int RevealSwirls = 3;
    public static final int RevealSpecks = 5;
    public static final double Spread = 0.09;

    private InkMarks() {
    }

    public enum Toggle {
        Hidden,
        Revealed,
        Missed
    }

    @Nullable
    private static BooleanProperty hiddenProperty(BlockState State) {
        if (!(State.getBlock() instanceof InkHideable Hideable)) {
            return null;
        }
        BooleanProperty Flag = Hideable.hiddenProperty();
        return State.hasProperty(Flag) ? Flag : null;
    }

    public static boolean canHide(BlockState State) {
        return hiddenProperty(State) != null;
    }

    public static boolean isHidden(BlockState State) {
        BooleanProperty Flag = hiddenProperty(State);
        return Flag != null && State.getValue(Flag);
    }

    public static boolean wireHidden(Level World, BlockPos Pos, WireLink Link) {
        if (Link.Hidden()) {
            return true;
        }
        if (!World.isLoaded(Link.Target()) || !(World.getBlockEntity(Link.Target()) instanceof OutletBlockEntity Other)) {
            return false;
        }
        WireLink Back = Other.linkTo(Pos);
        return Back != null && Back.Hidden();
    }

    public static void setWireHidden(Level World, BlockPos Owner, BlockPos Target, boolean Hide) {
        if (reach(World, Owner) instanceof OutletBlockEntity Outlet) {
            Outlet.setLinkHidden(Target, Hide);
        }
        if (reach(World, Target) instanceof OutletBlockEntity Other) {
            Other.setLinkHidden(Owner, Hide);
        }
    }

    @Nullable
    private static BlockEntity reach(Level World, BlockPos Pos) {
        if (!World.isLoaded(Pos)) {
            if (!(World instanceof ServerLevel Server)) {
                return null;
            }
            Server.getChunk(SectionPos.blockToSectionCoord(Pos.getX()), SectionPos.blockToSectionCoord(Pos.getZ()));
        }
        return World.getBlockEntity(Pos);
    }

    public static Toggle toggleWire(ServerLevel World, Player User, BlockPos Owner, BlockPos Target) {
        if (!World.isLoaded(Owner) || !(World.getBlockEntity(Owner) instanceof OutletBlockEntity Outlet)) {
            return Toggle.Missed;
        }
        WireLink Link = Outlet.linkTo(Target);
        if (Link == null) {
            return Toggle.Missed;
        }
        WireCutting.WireHit Hit = WireCutting.test(Outlet, Link, User.getEyePosition(), User.getViewVector(1.0f).scale(User.blockInteractionRange() + 1.0), WireCutting.ServerSlack);
        if (Hit == null) {
            return Toggle.Missed;
        }
        boolean Hide = !wireHidden(World, Owner, Link);
        setWireHidden(World, Owner, Target, Hide);
        effects(World, Hit.Point(), Hide);
        return Hide ? Toggle.Hidden : Toggle.Revealed;
    }

    public static Toggle toggleBlock(ServerLevel World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        BooleanProperty Flag = hiddenProperty(State);
        if (Flag == null) {
            return Toggle.Missed;
        }
        boolean Hide = !State.getValue(Flag);
        World.setBlock(Pos, State.setValue(Flag, Hide), Block.UPDATE_CLIENTS);
        effects(World, blockCentre(World, Pos, State), Hide);
        return Hide ? Toggle.Hidden : Toggle.Revealed;
    }

    public static Vec3 blockCentre(Level World, BlockPos Pos, BlockState State) {
        VoxelShape Shape = State.getShape(World, Pos);
        if (Shape.isEmpty()) {
            return Vec3.atCenterOf(Pos);
        }
        AABB Bounds = Shape.bounds();
        return Bounds.getCenter().add(Pos.getX(), Pos.getY(), Pos.getZ());
    }

    public static void effects(ServerLevel World, Vec3 Point, boolean Hide) {
        float Pitch = 0.94f + World.getRandom().nextFloat() * 0.12f;
        World.playSound(null, Point.x, Point.y, Point.z, (Hide ? InkwireRegistry.InkDab : InkwireRegistry.InkWipe).get(), SoundSource.PLAYERS, 0.8f, Pitch);
        World.playSound(null, Point.x, Point.y, Point.z, InkwireRegistry.InkShimmer.get(), SoundSource.PLAYERS, Hide ? 0.55f : 0.4f, Hide ? Pitch * 1.08f : Pitch * 0.84f);
        ColorParticleOption Swirl = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, InkColour);
        World.sendParticles(Swirl, Point.x, Point.y, Point.z, Hide ? HideSwirls : RevealSwirls, Spread, Spread, Spread, 0.0);
        if (!Hide) {
            World.sendParticles(ParticleTypes.WAX_OFF, Point.x, Point.y, Point.z, RevealSpecks, Spread, Spread, Spread, 0.6);
        }
    }
}
