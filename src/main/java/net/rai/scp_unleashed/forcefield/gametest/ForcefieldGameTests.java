package net.rai.scp_unleashed.forcefield.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.forcefield.ForcefieldBarrierBlock;
import net.rai.scp_unleashed.forcefield.ForcefieldEmitterBlock;
import net.rai.scp_unleashed.forcefield.ForcefieldEmitterBlockEntity;
import net.rai.scp_unleashed.forcefield.ForcefieldPhase;
import net.rai.scp_unleashed.forcefield.ForcefieldRegistry;
import net.rai.scp_unleashed.forcefield.client.ForcefieldDrawPlan;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class ForcefieldGameTests {
    public static final String SmallArena = "gear_arena";
    public static final String Arena = "gun_perf_arena";
    public static final int Gap = 3;
    public static final int Floor = 1;
    public static final BlockPos Base = new BlockPos(8, Floor + 1, 8);

    private ForcefieldGameTests() {
    }

    @GameTest(template = SmallArena, timeoutTicks = 20, batch = "forcefield_render")
    public static void drawPlanKeepsFieldAfterScene(GameTestHelper Helper) {
        for (boolean Fabulous : new boolean[]{false, true}) {
            ForcefieldDrawPlan.Pass Shaders = ForcefieldDrawPlan.pass(true, Fabulous);
            Helper.assertTrue(Shaders == ForcefieldDrawPlan.Pass.Direct, "a shader pack must keep the block entity path, got " + Shaders);
            Helper.assertFalse(ForcefieldDrawPlan.deferred(Shaders), "the shader pack path must not be deferred");
            Helper.assertFalse(ForcefieldDrawPlan.stampsDepth(Shaders) || ForcefieldDrawPlan.flushesSceneFirst(Shaders) || ForcefieldDrawPlan.bindsParticleTarget(Shaders),
                    "the shader pack path must not change any vanilla state");
        }
        ForcefieldDrawPlan.Pass Fancy = ForcefieldDrawPlan.pass(false, false);
        Helper.assertTrue(Fancy == ForcefieldDrawPlan.Pass.AfterBlockEntities, "fast and fancy must draw after block entities, got " + Fancy);
        Helper.assertTrue(ForcefieldDrawPlan.deferred(Fancy) && ForcefieldDrawPlan.flushesSceneFirst(Fancy), "fancy must flush the entity and item batches before the field");
        Helper.assertFalse(ForcefieldDrawPlan.stampsDepth(Fancy) || ForcefieldDrawPlan.bindsParticleTarget(Fancy), "fancy must keep the field out of the depth buffer and the main target bound");
        ForcefieldDrawPlan.Pass Fabulous = ForcefieldDrawPlan.pass(false, true);
        Helper.assertTrue(Fabulous == ForcefieldDrawPlan.Pass.AfterParticles, "fabulous must draw in the particle layer, got " + Fabulous);
        Helper.assertTrue(ForcefieldDrawPlan.deferred(Fabulous) && ForcefieldDrawPlan.stampsDepth(Fabulous) && ForcefieldDrawPlan.bindsParticleTarget(Fabulous),
                "fabulous must stamp the sheet depth into the particle layer");
        Helper.assertFalse(ForcefieldDrawPlan.flushesSceneFirst(Fabulous), "fabulous has already flushed every batch before particles");
        Helper.assertFalse(ForcefieldDrawPlan.SheetCulls || ForcefieldDrawPlan.GlowCulls || ForcefieldDrawPlan.StampCulls, "both faces of the field must be drawn");
        Helper.assertFalse(ForcefieldDrawPlan.SheetWritesDepth || ForcefieldDrawPlan.GlowWritesDepth, "the colour pass must not write depth");
        Helper.assertTrue(ForcefieldDrawPlan.StampWritesDepth && !ForcefieldDrawPlan.StampWritesColour, "the stamp must write depth only");
        Helper.assertTrue(ForcefieldDrawPlan.GlowKeepsAlpha, "the additive glow must leave layer alpha alone");
        List<Double> Distances = new ArrayList<>(List.of(1.0, 81.0, 16.0, 4.0));
        Distances.sort(ForcefieldDrawPlan.FarFirst);
        Helper.assertTrue(Distances.equals(List.of(81.0, 16.0, 4.0, 1.0)), "fields must be drawn far to near, got " + Distances);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "forcefield_smoke")
    public static void emitterPairRaisesAndDropsField(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        for (int X = 5; X <= 11; X++) {
            for (int Z = 5; Z <= 11; Z++) {
                Helper.setBlock(new BlockPos(X, Floor, Z), Blocks.STONE);
                for (int Y = Floor + 1; Y <= Floor + Gap + 3; Y++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Blocks.AIR);
                }
            }
        }
        BlockState Bottom = ForcefieldRegistry.Emitter.get().defaultBlockState().setValue(ForcefieldEmitterBlock.HalfPart, Half.BOTTOM).setValue(ForcefieldEmitterBlock.Axis, Direction.Axis.X);
        BlockState Top = Bottom.setValue(ForcefieldEmitterBlock.HalfPart, Half.TOP);
        Helper.setBlock(Base, Bottom);
        Helper.setBlock(Base.above(Gap + 1), Top);
        BlockPos Absolute = Helper.absolutePos(Base);
        Helper.assertTrue(World.getBlockEntity(Absolute) instanceof ForcefieldEmitterBlockEntity, "the bottom emitter has no block entity");
        ForcefieldEmitterBlockEntity Emitter = (ForcefieldEmitterBlockEntity) World.getBlockEntity(Absolute);
        boolean[] Powering = {true};
        int Supply = ForcefieldEmitterBlockEntity.columnDraw(ForcefieldEmitterBlockEntity.MaxGap) * 2;
        Helper.onEachTick(() -> {
            if (Powering[0] && !Emitter.isRemoved()) {
                Emitter.observeNetwork(Supply);
                Emitter.insertPower(Supply);
            }
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Emitter.getPhase() == ForcefieldPhase.Online, "field is " + Emitter.getPhase() + " with gap " + Emitter.getGap()))
                .thenExecute(() -> {
                    Helper.assertTrue(Emitter.getGap() == Gap && Emitter.getHeight() == Gap, "expected a " + Gap + " cell field, got gap " + Emitter.getGap() + " height " + Emitter.getHeight());
                    for (int Step = 1; Step <= Gap; Step++) {
                        BlockPos Cell = Absolute.above(Step);
                        BlockState State = World.getBlockState(Cell);
                        Helper.assertTrue(State.is(ForcefieldRegistry.Barrier.get()), "cell " + Step + " is " + State);
                        Helper.assertTrue(State.getValue(ForcefieldBarrierBlock.Axis) == Direction.Axis.X, "cell " + Step + " lost the emitter axis");
                        Helper.assertTrue(ForcefieldBarrierBlock.owner(World, Cell) == Emitter, "cell " + Step + " does not find its emitter");
                        Helper.assertTrue(State.getRenderShape() == RenderShape.INVISIBLE, "barrier cells must stay invisible to the chunk mesh");
                        Helper.assertTrue(State.getVisualShape(World, Cell, CollisionContext.empty()).isEmpty(), "barrier cells must not have a visual shape");
                        VoxelShape Solid = State.getCollisionShape(World, Cell);
                        AABB Box = Solid.bounds();
                        Helper.assertTrue(Math.abs(Box.getZsize() - ForcefieldBarrierBlock.Thickness / 16.0) < 1.0E-6 && Box.getXsize() == 1.0, "barrier collision is " + Box);
                        Helper.assertTrue(Emitter.renderBox().contains(Vec3.atCenterOf(Cell)), "render box " + Emitter.renderBox() + " misses cell " + Step);
                    }
                    Powering[0] = false;
                })
                .thenWaitUntil(() -> Helper.assertTrue(Emitter.getPhase() == ForcefieldPhase.Offline, "field stayed " + Emitter.getPhase() + " without power"))
                .thenExecute(() -> {
                    for (int Step = 1; Step <= Gap; Step++) {
                        BlockState State = World.getBlockState(Absolute.above(Step));
                        Helper.assertTrue(State.isAir(), "cell " + Step + " was left as " + State);
                    }
                    Helper.assertTrue(Emitter.getHeight() == 0, "height stayed " + Emitter.getHeight());
                })
                .thenSucceed();
    }
}
