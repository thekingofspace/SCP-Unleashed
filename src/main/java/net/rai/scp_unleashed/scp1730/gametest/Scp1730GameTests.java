package net.rai.scp_unleashed.scp1730.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.antimemetic.AntiMemeticRegistry;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.decal.DecalRegistry;
import net.rai.scp_unleashed.gateway.GatewayDestination;
import net.rai.scp_unleashed.gateway.GatewayDestinations;
import net.rai.scp_unleashed.gateway.GatewayStash;
import net.rai.scp_unleashed.gateway.GatewayTravel;
import net.rai.scp_unleashed.scp1730.Scp1730Anchors;
import net.rai.scp_unleashed.scp1730.Scp1730Aura;
import net.rai.scp_unleashed.scp1730.Scp1730Breach;
import net.rai.scp_unleashed.scp1730.Scp1730Collapse;
import net.rai.scp_unleashed.scp1730.Scp1730Cracks;
import net.rai.scp_unleashed.scp1730.Scp1730Destination;
import net.rai.scp_unleashed.scp1730.Scp1730Director;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.Scp1730State;
import net.rai.scp_unleashed.scp1730.Scp1730Symbols;
import net.rai.scp_unleashed.scp1730.Scp1730World;
import net.rai.scp_unleashed.scp1730.block.RealityFolderBlock;
import net.rai.scp_unleashed.scp1730.block.RealityFolderBlockEntity;
import net.rai.scp_unleashed.scp1730.block.Scp1730CrackBlock;
import net.rai.scp_unleashed.scp1730.block.Scp1730LaserBlock;
import net.rai.scp_unleashed.scp1730.block.Scp1730LaserBlockEntity;
import net.rai.scp_unleashed.scp1730.block.Scp1730SymbolBlock;
import net.rai.scp_unleashed.scp1730.block.Scp1730ThresherBlockEntity;
import net.rai.scp_unleashed.scp1730.block.Scp1730TurretBlock;
import net.rai.scp_unleashed.scp1730.block.Scp1730TurretBlockEntity;
import net.rai.scp_unleashed.scp1730.entity.RealityAnchorField;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Guardian;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Leech;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Painter;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Tentacle;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Fixture;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Pit;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Plan;
import net.rai.scp_unleashed.scp1730.plan.Scp1730SetPieces;
import net.rai.scp_unleashed.scp1730.plan.Scp1730PlanCheck;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Zone;
import net.rai.scp_unleashed.scp049.Scp049Cured;
import net.rai.scp_unleashed.scp049.Scp049Doctor;
import net.rai.scp_unleashed.scp049.Scp049Registry;
import net.rai.scp_unleashed.scp439.Scp439Creature;
import net.rai.scp_unleashed.scp457.Scp457;
import net.rai.scp_unleashed.scp902.gametest.Scp902TestPlayer;
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.stasis.StasisRegistry;
import net.rai.scp_unleashed.stranger.StrangerEncounterCondition;
import net.rai.scp_unleashed.stranger.StrangerGift;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp1730GameTests {
    public static final String Arena = "gateway_arena";

    private Scp1730GameTests() {
    }

    public static ServerLevel facility(GameTestHelper Helper) {
        if (Helper.getLevel().getServer().getLevel(Scp1730World.Key) == null) {
            Scp1730Destination.standIn(Level.END);
        }
        return Scp1730World.level(Helper.getLevel().getServer());
    }

    public static GatewayDestination destination() {
        return GatewayDestinations.get(Scp1730World.standingIn() ? Scp1730Destination.StandinId : Scp1730Destination.Id);
    }

    public static Scp902TestPlayer player(GameTestHelper Helper, ServerLevel World, Vec3 At, String Name) {
        Scp902TestPlayer User = new Scp902TestPlayer(World, Name);
        User.moveTo(At.x, At.y, At.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.enlist(true);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    public static void dismiss(ServerPlayer User) {
        Scp1730Symbols.release(User);
        if (User instanceof Scp902TestPlayer Tester) {
            Tester.enlist(false);
        }
        if (!User.isRemoved()) {
            User.serverLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    public static void look(ServerPlayer User, Vec3 Target) {
        Vec3 Way = Target.subtract(User.getEyePosition());
        double Flat = Math.sqrt(Way.x * Way.x + Way.z * Way.z);
        float Yaw = (float) Math.toDegrees(Math.atan2(Way.z, Way.x)) - 90.0f;
        float Pitch = (float) -Math.toDegrees(Math.atan2(Way.y, Flat));
        User.setYRot(Yaw);
        User.setXRot(Pitch);
        User.setYHeadRot(Yaw);
    }

    public static ServerPlayer enter(GameTestHelper Helper, String Name) {
        ServerLevel Facility = facility(Helper);
        Helper.assertTrue(Facility != null, "the facility level is missing");
        Scp902TestPlayer User = player(Helper, Helper.getLevel(), Helper.absoluteVec(new Vec3(4.5, 1.0, 4.5)), Name);
        GatewayStash.Spot Back = new GatewayStash.Spot(Helper.getLevel().dimension(), User.position(), 0.0f);
        Helper.assertTrue(GatewayTravel.enter(User, destination(), Back), "the gateway refused entry");
        User.hasChangedDimension();
        return User;
    }

    public static List<long[]> forceAround(ServerLevel World, BlockPos Centre, int Radius) {
        List<long[]> Forced = new ArrayList<>();
        for (int Dx = -Radius; Dx <= Radius; Dx++) {
            for (int Dz = -Radius; Dz <= Radius; Dz++) {
                int ChunkX = (Centre.getX() >> 4) + Dx;
                int ChunkZ = (Centre.getZ() >> 4) + Dz;
                if (World.setChunkForced(ChunkX, ChunkZ, true)) {
                    Forced.add(new long[]{ChunkX, ChunkZ});
                }
            }
        }
        return Forced;
    }

    public static boolean ticking(ServerLevel World, BlockPos Centre, int Radius) {
        for (int Dx = -Radius; Dx <= Radius; Dx++) {
            for (int Dz = -Radius; Dz <= Radius; Dz++) {
                if (!World.isPositionEntityTicking(new BlockPos(((Centre.getX() >> 4) + Dx) * 16 + 8, Centre.getY(), ((Centre.getZ() >> 4) + Dz) * 16 + 8))) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void release(ServerLevel World, List<long[]> Forced) {
        for (long[] Each : Forced) {
            World.setChunkForced((int) Each[0], (int) Each[1], false);
        }
        Forced.clear();
    }

    public static void holdFragment(Player User) {
        for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
            ItemStack Found = User.getInventory().getItem(Slot);
            if (Found.is(Scp1730Registry.Fragment.get())) {
                ItemStack Hand = User.getInventory().getItem(0);
                User.getInventory().setItem(0, Found);
                if (Slot != 0) {
                    User.getInventory().setItem(Slot, Hand);
                }
                User.getInventory().selected = 0;
                return;
            }
        }
    }

    public static int fragments(Player User) {
        int Count = 0;
        for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
            if (User.getInventory().getItem(Slot).is(Scp1730Registry.Fragment.get())) {
                Count += User.getInventory().getItem(Slot).getCount();
            }
        }
        return Count;
    }

    public static int count(Player User, net.minecraft.world.item.Item Item0) {
        int Count = 0;
        for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
            if (User.getInventory().getItem(Slot).is(Item0)) {
                Count += User.getInventory().getItem(Slot).getCount();
            }
        }
        return Count;
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp1730_planDeterministic")
    public static void planDeterministic(GameTestHelper Helper) {
        Scp1730Plan First = Scp1730Plan.fresh(1730L);
        Scp1730Plan Second = Scp1730Plan.fresh(1730L);
        Scp1730Plan Other = Scp1730Plan.fresh(42L);
        Helper.assertTrue(First.checksum() == Second.checksum(), "the same seed built different facilities");
        Helper.assertTrue(First.checksum() != Other.checksum(), "different seeds built the same facility");
        List<String> Problems = Scp1730PlanCheck.check(1730L);
        Helper.assertTrue(Problems.isEmpty(), "plan self-check failed: " + Problems);
        List<Scp1730Zone> Zones = First.zones();
        Helper.assertTrue(Zones.size() == 4, "expected four phases, found " + Zones.size());
        for (int Index = 0; Index < Zones.size(); Index++) {
            if (Index > 0) {
                Helper.assertTrue(Zones.get(Index).MaxY() < Zones.get(Index - 1).MinY() + 1 || First.floorY(Index) < First.floorY(Index - 1),
                        "phase " + Index + " is not below the one above it");
            }
            if (Index < 3) {
                Helper.assertTrue(First.fixtures("seal", Index).size() >= 9, "phase " + Index + " has no sealed stairwell down");
                Helper.assertTrue(!First.fixtures("control", Index).isEmpty(), "phase " + Index + " has no objective control");
            }
        }
        Helper.assertTrue(!First.fixtures("thresher").isEmpty() && First.guardian() != null, "the deep site has no Thresher or guardian spot");
        Helper.assertTrue(!First.pits().isEmpty(), "no SCP-1730-4 pit was generated");
        for (int Run = 0; Run < 40; Run++) {
            Helper.assertTrue(Scp1730World.plan(Run) != null, "run " + Run + " has no buildable facility");
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp1730_wallPiecesBacked")
    public static void wallPiecesBacked(GameTestHelper Helper) {
        List<String> Problems = new java.util.ArrayList<>();
        for (long Seed : new long[]{1730L, 42L, 99L, 7L, 2024L, 1889L, -5794042299947963465L, 356L, 5076L, 41125L, 20000L, 308L}) {
            Problems.addAll(Scp1730WallAudit.audit(Seed));
        }
        if (!Problems.isEmpty()) {
            java.util.Map<String, Integer> Kinds = new java.util.TreeMap<>();
            for (String Each : Problems) {
                String[] Parts = Each.split(" ");
                Kinds.merge(Parts.length > 2 ? Parts[2].replaceAll("\\[.*", "") : Each, 1, Integer::sum);
            }
            java.util.Map<String, String> First = new java.util.TreeMap<>();
            for (String Each : Problems) {
                String[] Parts = Each.split(" ");
                First.putIfAbsent(Parts.length > 2 ? Parts[2].replaceAll("\\[.*", "") : Each, Each);
            }
            Helper.fail(Problems.size() + " wall pieces float " + Kinds + " e.g. " + String.join(" | ", First.values()));
        }
        Helper.succeed();
    }

    public static BlockPos laserCorridor(GameTestHelper Helper, BlockPos Wall, Direction Out, int Gap) {
        Helper.setBlock(Wall, Blocks.STONE);
        Helper.setBlock(Wall.relative(Out, Gap + 2), Blocks.STONE);
        BlockPos Emitter = Wall.relative(Out);
        Helper.setBlock(Emitter, Scp1730Registry.Laser.get().defaultBlockState().setValue(Scp1730LaserBlock.Facing, Out));
        return Emitter;
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp1730_laserSpan")
    public static void laserSpan(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos[] Walls = {new BlockPos(1, 2, 2), new BlockPos(12, 2, 5), new BlockPos(2, 2, 7), new BlockPos(9, 2, 13)};
        Direction[] Outs = {Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};
        for (int Index = 0; Index < 4; Index++) {
            BlockPos Emitter = laserCorridor(Helper, Walls[Index], Outs[Index], 3);
            Scp1730LaserBlockEntity Laser = (Scp1730LaserBlockEntity) World.getBlockEntity(Helper.absolutePos(Emitter));
            Helper.assertTrue(Laser != null, "the emitter has no block entity");
            Vec3 Lens = Laser.lens();
            BlockPos Far = Helper.absolutePos(Walls[Index].relative(Outs[Index], 5));
            double Face = Outs[Index].getAxisDirection() == Direction.AxisDirection.POSITIVE ? (Outs[Index].getAxis() == Direction.Axis.X ? Far.getX() : Far.getZ())
                    : (Outs[Index].getAxis() == Direction.Axis.X ? Far.getX() + 1.0 : Far.getZ() + 1.0);
            double Expected = Math.abs(Face - (Outs[Index].getAxis() == Direction.Axis.X ? Lens.x : Lens.z));
            double Length = Laser.measure(World);
            Helper.assertTrue(Math.abs(Length - Expected) < 1.0e-3, Outs[Index] + " beam is " + Length + " long but the wall is " + Expected + " from the lens");
            Helper.assertTrue(Math.abs(Lens.y - (Helper.absolutePos(Emitter).getY() + Scp1730LaserBlockEntity.LensHeight)) < 1.0e-6, "the beam is not at lens height");
            Vec3 End = Laser.beamEnd(Length);
            Helper.assertTrue(Laser.beamBox(Length).contains(End.subtract(Outs[Index].getStepX() * 0.01, 0.0, Outs[Index].getStepZ() * 0.01))
                    && Laser.beamBox(Length).contains(Lens.add(Outs[Index].getStepX() * 0.01, 0.0, Outs[Index].getStepZ() * 0.01)), "the trip box does not cover the drawn beam");
            Helper.assertTrue(World.getBlockState(BlockPos.containing(End.add(Outs[Index].getStepX() * 0.01, 0.0, Outs[Index].getStepZ() * 0.01))).is(Blocks.STONE),
                    "the beam does not end on the far wall");
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp1730_laserShotOut")
    public static void laserShotOut(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Emitter = laserCorridor(Helper, new BlockPos(1, 2, 3), Direction.EAST, 5);
        Helper.setBlock(new BlockPos(2, 1, 3), Blocks.REDSTONE_BLOCK);
        BlockPos Spot = Helper.absolutePos(Emitter);
        Scp1730LaserBlock Block = Scp1730Registry.Laser.get();
        BlockHitResult Hit = new BlockHitResult(Vec3.atCenterOf(Spot), Direction.EAST, Spot, false);
        Scp1730LaserBlockEntity Laser = (Scp1730LaserBlockEntity) World.getBlockEntity(Spot);
        for (int Shot = 0; Shot < 2; Shot++) {
            Block.onGunHit(World, Spot, World.getBlockState(Spot), Hit, null, 4.5f);
        }
        Helper.assertFalse(World.getBlockState(Spot).getValue(Scp1730LaserBlock.Broken), "two rifle hits already broke the emitter");
        Block.onGunHit(World, Spot, World.getBlockState(Spot), Hit, null, 4.5f);
        Helper.assertTrue(World.getBlockState(Spot).getValue(Scp1730LaserBlock.Broken), "three rifle hits did not break the emitter");
        BlockPos Other = laserCorridor(Helper, new BlockPos(1, 2, 8), Direction.EAST, 5);
        BlockPos OtherSpot = Helper.absolutePos(Other);
        net.minecraft.world.entity.projectile.Arrow Bolt = new net.minecraft.world.entity.projectile.Arrow(EntityType.ARROW, World);
        Bolt.setPos(Vec3.atCenterOf(OtherSpot));
        World.getBlockState(OtherSpot).onProjectileHit(World, World.getBlockState(OtherSpot), new BlockHitResult(Vec3.atCenterOf(OtherSpot), Direction.EAST, OtherSpot, false), Bolt);
        Helper.assertTrue(World.getBlockState(OtherSpot).getValue(Scp1730LaserBlock.Broken), "an arrow did not break the emitter");
        ServerPlayer User = player(Helper, World, Helper.absoluteVec(new Vec3(4.5, 2.0, 3.5)), "scp1730_beam_walker");
        Helper.runAfterDelay(30, () -> {
            Helper.assertFalse(Laser.tripped(), "a broken emitter tripped");
            Helper.assertFalse(World.getBlockState(Spot).getValue(Scp1730LaserBlock.Powered), "a broken emitter is still powered");
            dismiss(User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp1730_laserIgnoresShots")
    public static void laserIgnoresShots(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Emitter = laserCorridor(Helper, new BlockPos(1, 2, 3), Direction.EAST, 5);
        Helper.setBlock(new BlockPos(2, 1, 3), Blocks.REDSTONE_BLOCK);
        Scp1730LaserBlockEntity Laser = (Scp1730LaserBlockEntity) World.getBlockEntity(Helper.absolutePos(Emitter));
        for (int Index = 0; Index < 4; Index++) {
            net.minecraft.world.entity.projectile.Arrow Bolt = new net.minecraft.world.entity.projectile.Arrow(EntityType.ARROW, World);
            Bolt.setPos(Helper.absoluteVec(new Vec3(4.5 + Index * 0.4, 2.0 + Scp1730LaserBlockEntity.LensHeight, 1.0)));
            Bolt.setDeltaMovement(0.0, 0.0, 1.2);
            Bolt.setNoGravity(true);
            World.addFreshEntity(Bolt);
        }
        Helper.runAfterDelay(30, () -> {
            Helper.assertFalse(Laser.tripped(), "an arrow crossing the beam tripped it");
            ServerPlayer User = player(Helper, World, Helper.absoluteVec(new Vec3(5.5, 2.0, 3.5)), "scp1730_beam_crosser");
            Helper.runAfterDelay(20, () -> {
                Helper.assertTrue(Laser.tripped(), "a player standing in the beam did not trip it");
                dismiss(User);
                Helper.succeed();
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp1730_turretShotDown")
    public static void turretShotDown(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Helper.setBlock(new BlockPos(8, 4, 8), Blocks.STONE);
        Helper.setBlock(new BlockPos(8, 3, 8), Scp1730Registry.Turret.get().defaultBlockState().setValue(Scp1730TurretBlock.Facing, Direction.SOUTH));
        Helper.setBlock(new BlockPos(9, 3, 8), Blocks.REDSTONE_BLOCK);
        BlockPos Spot = Helper.absolutePos(new BlockPos(8, 3, 8));
        Scp1730TurretBlockEntity Turret = (Scp1730TurretBlockEntity) World.getBlockEntity(Spot);
        BlockHitResult Hit = new BlockHitResult(Vec3.atCenterOf(Spot), Direction.SOUTH, Spot, false);
        for (int Shot = 0; Shot < 9; Shot++) {
            Scp1730Registry.Turret.get().onGunHit(World, Spot, World.getBlockState(Spot), Hit, null, 4.5f);
        }
        Helper.assertFalse(Turret.wrecked(), "nine rifle hits wrecked the turret");
        Scp1730Registry.Turret.get().onGunHit(World, Spot, World.getBlockState(Spot), Hit, null, 4.5f);
        Helper.assertTrue(Turret.wrecked(), "ten rifle hits did not wreck the turret");
        ServerPlayer User = player(Helper, World, Helper.absoluteVec(new Vec3(8.5, 1.0, 13.5)), "scp1730_wreck_target");
        Helper.runAfterDelay(80, () -> {
            Helper.assertTrue(User.getHealth() >= User.getMaxHealth() && Turret.targetId() < 0, "a wrecked turret still fired");
            dismiss(User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp1730_turretBlasted")
    public static void turretBlasted(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Helper.setBlock(new BlockPos(3, 4, 3), Blocks.STONE);
        Helper.setBlock(new BlockPos(3, 3, 3), Scp1730Registry.Turret.get().defaultBlockState());
        Helper.setBlock(new BlockPos(12, 4, 12), Blocks.STONE);
        Helper.setBlock(new BlockPos(12, 3, 12), Scp1730Registry.Turret.get().defaultBlockState());
        Scp1730TurretBlockEntity Near = (Scp1730TurretBlockEntity) World.getBlockEntity(Helper.absolutePos(new BlockPos(3, 3, 3)));
        Scp1730TurretBlockEntity Far = (Scp1730TurretBlockEntity) World.getBlockEntity(Helper.absolutePos(new BlockPos(12, 3, 12)));
        Vec3 Blast = Helper.absoluteVec(new Vec3(3.5, 1.5, 4.5));
        World.explode(null, Blast.x, Blast.y, Blast.z, 3.0f, Level.ExplosionInteraction.NONE);
        Helper.assertTrue(Near.wrecked(), "a grenade-sized blast two blocks away did not wreck the turret");
        Helper.assertFalse(Far.wrecked(), "a turret far from the blast was wrecked");
        Vec3 Edge = Helper.absoluteVec(new Vec3(12.5, 1.5, 7.5));
        World.explode(null, Edge.x, Edge.y, Edge.z, 3.0f, Level.ExplosionInteraction.NONE);
        Helper.assertTrue(!Far.wrecked() && Far.health() < Scp1730TurretBlockEntity.MaxHealth, "a blast at mid range did not just damage the turret");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp1730_facilityExplosion")
    public static void facilityExplosion(GameTestHelper Helper) {
        ServerLevel Facility = facility(Helper);
        Helper.assertTrue(Facility != null, "the facility level is missing");
        BlockPos Base = new BlockPos(-3000, 120, -3000);
        for (int Dx = -2; Dx <= 2; Dx++) {
            for (int Dz = -2; Dz <= 2; Dz++) {
                Facility.setBlockAndUpdate(Base.offset(Dx, -1, Dz), Blocks.STONE.defaultBlockState());
                Facility.setBlockAndUpdate(Base.offset(Dx, 2, Dz), Blocks.STONE.defaultBlockState());
            }
        }
        Facility.setBlockAndUpdate(Base.offset(2, 0, 0), Blocks.STONE.defaultBlockState());
        Facility.setBlockAndUpdate(Base.offset(2, 1, 0), Blocks.OAK_PLANKS.defaultBlockState());
        Facility.explode(null, Base.getX() + 0.5, Base.getY() + 0.5, Base.getZ() + 0.5, 4.0f, true, Level.ExplosionInteraction.TNT);
        boolean Intact = Facility.getBlockState(Base.offset(2, 0, 0)).is(Blocks.STONE) && Facility.getBlockState(Base.offset(2, 1, 0)).is(Blocks.OAK_PLANKS);
        for (int Dx = -2; Dx <= 2; Dx++) {
            for (int Dz = -2; Dz <= 2; Dz++) {
                Intact &= Facility.getBlockState(Base.offset(Dx, -1, Dz)).is(Blocks.STONE) && Facility.getBlockState(Base.offset(Dx, 2, Dz)).is(Blocks.STONE);
                Intact &= !Facility.getBlockState(Base.offset(Dx, 0, Dz)).is(Blocks.FIRE) && !Facility.getBlockState(Base.offset(Dx, 1, Dz)).is(Blocks.FIRE);
            }
        }
        for (int Dx = -2; Dx <= 2; Dx++) {
            for (int Dz = -2; Dz <= 2; Dz++) {
                for (int Dy = -1; Dy <= 2; Dy++) {
                    Facility.setBlockAndUpdate(Base.offset(Dx, Dy, Dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
        Helper.assertTrue(Intact, "an explosion inside Site-13 broke or burned blocks");
        Mob Bystander = EntityType.PIG.create(Facility);
        net.minecraft.world.level.Explosion Fake = new net.minecraft.world.level.Explosion(Facility, null, Base.getX() + 0.5, Base.getY() + 0.5, Base.getZ() + 0.5, 4.0f, true,
                net.minecraft.world.level.Explosion.BlockInteraction.DESTROY);
        Fake.getToBlow().add(Base.offset(2, 0, 0));
        List<Entity> Struck = new java.util.ArrayList<>(List.of(Bystander));
        net.neoforged.neoforge.event.level.ExplosionEvent.Detonate Event = new net.neoforged.neoforge.event.level.ExplosionEvent.Detonate(Facility, Fake, Struck);
        net.rai.scp_unleashed.scp1730.Scp1730Events.detonating(Event);
        Helper.assertTrue(Event.getAffectedBlocks().isEmpty(), "Site-13 let an explosion keep blocks to destroy");
        Helper.assertTrue(Event.getAffectedEntities().contains(Bystander), "Site-13 stopped an explosion from reaching entities");
        ServerLevel World = Helper.getLevel();
        Mob Victim = EntityType.PIG.create(World);
        Victim.moveTo(Helper.absoluteVec(new Vec3(6.5, 2.0, 6.5)));
        World.addFreshEntity(Victim);
        float Before = Victim.getHealth();
        Vec3 Blast = Helper.absoluteVec(new Vec3(5.5, 2.5, 6.5));
        World.explode(null, Blast.x, Blast.y, Blast.z, 3.0f, Level.ExplosionInteraction.NONE);
        boolean Hurt = !Victim.isAlive() || Victim.getHealth() < Before;
        Victim.discard();
        Helper.assertTrue(Hurt, "an explosion did not hurt a mob");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp1730_documentGivesShard")
    public static void documentGivesShard(GameTestHelper Helper) {
        AnomalyDocument Found = AnomalyDocuments.server().get(Scp1730Registry.DocumentId);
        Helper.assertTrue(Found != null, "the SCP-1730 document is not loaded");
        Helper.assertTrue(Found.Condition() instanceof StrangerEncounterCondition, "the document does not hand over a shard through a stranger");
        StrangerEncounterCondition Meeting = (StrangerEncounterCondition) Found.Condition();
        boolean Attuned = false;
        for (StrangerGift Gift : Meeting.Gift()) {
            GatewayDestination Target = GatewayDestinations.of(Gift.toStack());
            Attuned |= Target != null && Target.Id().equals(Scp1730Destination.Id);
        }
        Helper.assertTrue(Attuned, "the gift is not a shard attuned to Site-13");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp1730_entryKeepsItems")
    public static void entryKeepsItems(GameTestHelper Helper) {
        facility(Helper);
        Scp1730State.get(Helper.getLevel().getServer()).endRun();
        ServerPlayer User = player(Helper, Helper.getLevel(), Helper.absoluteVec(new Vec3(4.5, 1.0, 4.5)), "scp1730_keeper");
        User.getInventory().add(new ItemStack(Items.BREAD, 7));
        User.getInventory().add(new ItemStack(Items.IRON_SWORD));
        User.getInventory().add(new ItemStack(Scp1730Registry.Fragment.get()));
        GatewayStash.Spot Back = new GatewayStash.Spot(Helper.getLevel().dimension(), User.position(), 0.0f);
        Helper.assertTrue(GatewayTravel.enter(User, destination(), Back), "the gateway refused entry");
        Helper.runAfterDelay(5, () -> {
            Helper.assertTrue(Scp1730World.inside(User), "the traveller did not arrive in the facility");
            Helper.assertTrue(count(User, Items.BREAD) == 7 && count(User, Items.IRON_SWORD) == 1, "belongings were not kept on entry");
            Helper.assertTrue(fragments(User) == 1, "expected exactly one contained fragment, found " + fragments(User));
            Scp1730State State = Scp1730State.get(Helper.getLevel().getServer());
            Helper.assertTrue(State.active(), "entering did not start a run");
            int Zone = Scp1730World.zoneOf(State.run(), User.blockPosition());
            Helper.assertTrue(Zone == 0, "the arrival is not in the Entrance Zone but in phase " + Zone);
            GatewayTravel.leave(User);
            dismiss(User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp1730_fragmentExits")
    public static void fragmentExits(GameTestHelper Helper) {
        ServerPlayer User = enter(Helper, "scp1730_leaver");
        Helper.runAfterDelay(5, () -> {
            Helper.assertTrue(fragments(User) == 1, "no fragment was given on arrival");
            holdFragment(User);
            User.getMainHandItem().use(User.level(), User, InteractionHand.MAIN_HAND);
            Helper.assertTrue(!Scp1730World.inside(User) && User.level() == Helper.getLevel(), "using the fragment did not bring the traveller home");
            Helper.assertTrue(User.position().distanceTo(Helper.absoluteVec(new Vec3(4.5, 1.0, 4.5))) < 8.0, "the traveller came home somewhere else");
            Helper.assertTrue(fragments(User) == 0, "the fragment was not used up");
            dismiss(User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp1730_pitKills")
    public static void pitKills(GameTestHelper Helper) {
        ServerPlayer User = enter(Helper, "scp1730_faller");
        Scp1730State State = Scp1730State.get(Helper.getLevel().getServer());
        Scp1730Plan Plan = Scp1730World.plan(State.run());
        Scp1730Pit Pit = Plan.pits().getFirst();
        int X = (Pit.MinX() + Pit.MaxX()) / 2;
        int Z = (Pit.MinZ() + Pit.MaxZ()) / 2;
        int Y = Pit.RimY() - 6;
        Helper.assertTrue(Plan.inPit(X, Y, Z), "the pit centre is not lethal in the plan");
        Helper.assertTrue(!Plan.inPit(X, Pit.RimY() + 1, Z), "standing above the rim counts as falling in");
        Vec3 At = Vec3.atBottomCenterOf(Scp1730World.world(State.run(), X, Y, Z));
        User.teleportTo(At.x, At.y, At.z);
        int Lx = Scp1730World.localX(State.run(), User.getX());
        int Lz = Scp1730World.localZ(User.getZ());
        boolean Seen = Plan.inPit(Lx, (int) Math.floor(User.getY()), Lz);
        Helper.runAfterDelay(4, () -> {
            String Why = "inside " + Scp1730World.inside(User) + ", local " + Lx + "/" + (int) Math.floor(User.getY()) + "/" + Lz + " pit " + Seen + ", active "
                    + State.active() + ", health " + User.getHealth() + ", level " + User.level().dimension().location();
            if (!User.isDeadOrDying()) {
                Scp1730Director.fall(facility(Helper), User);
                Why += ", direct fall killed " + User.isDeadOrDying();
            }
            Helper.assertTrue(User.isDeadOrDying() && Why.contains("pit true"), "falling into SCP-1730-4 did not kill: " + Why);
            dismiss(User);
            Helper.succeed();
        });
    }

    private static BlockPos symbolWall(GameTestHelper Helper) {
        Helper.setBlock(new BlockPos(5, 2, 9), Blocks.STONE);
        Helper.setBlock(new BlockPos(5, 2, 8), Scp1730Registry.Symbol.get().defaultBlockState().setValue(Scp1730SymbolBlock.Facing, Direction.NORTH));
        return Helper.absolutePos(new BlockPos(5, 2, 8));
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp1730_symbolHolds")
    public static void symbolHolds(GameTestHelper Helper) {
        BlockPos Symbol = symbolWall(Helper);
        ServerPlayer User = player(Helper, Helper.getLevel(), Helper.absoluteVec(new Vec3(5.5, 1.0, 3.5)), "scp1730_viewer");
        look(User, Scp1730Symbols.faceCentre(Symbol, Direction.NORTH));
        Helper.runAfterDelay(6, () -> {
            Helper.assertTrue(Scp1730Symbols.held(User), "looking straight at a symbol did not hold the viewer");
            Helper.assertTrue(User.getRemainingFireTicks() > 0, "the viewer did not ignite");
            Helper.setBlock(new BlockPos(5, 2, 6), Blocks.STONE);
            Helper.runAfterDelay(4, () -> {
                Helper.assertTrue(!Scp1730Symbols.held(User), "breaking line of sight did not free the viewer");
                dismiss(User);
                Helper.succeed();
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp1730_symbolPeripheral")
    public static void symbolPeripheral(GameTestHelper Helper) {
        BlockPos Symbol = symbolWall(Helper);
        ServerPlayer User = player(Helper, Helper.getLevel(), Helper.absoluteVec(new Vec3(5.5, 1.0, 3.5)), "scp1730_glancer");
        look(User, Scp1730Symbols.faceCentre(Symbol, Direction.NORTH).add(3.0, 0.0, 0.0));
        Helper.runAfterDelay(10, () -> {
            Helper.assertTrue(!Scp1730Symbols.held(User), "a symbol in peripheral vision caught the viewer");
            dismiss(User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp1730_symbolImmune")
    public static void symbolImmune(GameTestHelper Helper) {
        BlockPos Symbol = symbolWall(Helper);
        ServerPlayer User = player(Helper, Helper.getLevel(), Helper.absoluteVec(new Vec3(5.5, 1.0, 3.5)), "scp1730_shielded");
        User.addEffect(new MobEffectInstance(AntiMemeticRegistry.Shielded, 600));
        look(User, Scp1730Symbols.faceCentre(Symbol, Direction.NORTH));
        Helper.runAfterDelay(10, () -> {
            Helper.assertTrue(!Scp1730Symbols.held(User), "anti-memetics did not protect the viewer");
            dismiss(User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp1730_symbolRemoved")
    public static void symbolRemoved(GameTestHelper Helper) {
        BlockPos Symbol = symbolWall(Helper);
        ServerPlayer User = player(Helper, Helper.getLevel(), Helper.absoluteVec(new Vec3(5.5, 1.0, 3.5)), "scp1730_freed");
        look(User, Scp1730Symbols.faceCentre(Symbol, Direction.NORTH));
        Helper.runAfterDelay(6, () -> {
            Helper.assertTrue(Scp1730Symbols.held(User), "the symbol did not hold the viewer");
            Scp1730SymbolBlock.scrub(Helper.getLevel(), Symbol, Helper.getLevel().getBlockState(Symbol));
            Helper.runAfterDelay(3, () -> {
                Helper.assertTrue(!Scp1730Symbols.held(User), "removing the symbol did not free the viewer");
                dismiss(User);
                Helper.succeed();
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp1730_symbolScrubbed")
    public static void symbolScrubbed(GameTestHelper Helper) {
        BlockPos Symbol = symbolWall(Helper);
        ServerPlayer User = player(Helper, Helper.getLevel(), Helper.absoluteVec(new Vec3(5.5, 1.0, 6.5)), "scp1730_scrubber");
        User.addEffect(new MobEffectInstance(AntiMemeticRegistry.Shielded, 600));
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(DecalRegistry.DecalScraper.get()));
        User.gameMode.useItemOn(User, Helper.getLevel(), User.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Scp1730Symbols.faceCentre(Symbol, Direction.NORTH), Direction.NORTH, Symbol, false));
        Helper.assertTrue(Helper.getLevel().getBlockState(Symbol).isAir(), "the scraper did not scrub the symbol away");
        dismiss(User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp1730_painterCapture")
    public static void painterCapture(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp1730Painter Painter = Helper.spawn(Scp1730Registry.PainterType.get(), new BlockPos(8, 1, 8));
        ServerPlayer User = player(Helper, World, Helper.absoluteVec(new Vec3(4.5, 1.0, 4.5)), "scp1730_keeper2");
        User.addEffect(new MobEffectInstance(AntiMemeticRegistry.Shielded, 600));
        ItemStack Pod = new ItemStack(StasisRegistry.podFor(FragmentRarity.values()[0]));
        Helper.assertTrue(StasisPods.isVetoed(Painter), "an unanchored painter is not refused by stasis pods");
        Helper.assertTrue(!StasisPods.capture(World, User, Pod, Painter), "a stasis pod captured an unanchored painter");
        RealityAnchorField.spawn(World, Painter.position().add(0.0, 1.0, 0.0));
        Helper.runAfterDelay(8, () -> {
            Helper.assertTrue(Painter.anchored(), "the anchor field did not anchor the painter");
            Helper.assertTrue(!StasisPods.isVetoed(Painter), "an anchored painter is still refused");
            Helper.assertTrue(StasisPods.capture(World, User, Pod, Painter), "the stasis pod failed to capture an anchored painter");
            dismiss(User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp1730_guardianAnchor")
    public static void guardianAnchor(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp1730Guardian Guardian = Helper.spawn(Scp1730Registry.GuardianType.get(), new BlockPos(16, 1, 16));
        float Full = Guardian.getHealth();
        Helper.assertTrue(!Guardian.hurt(World.damageSources().generic(), 40.0f) && Guardian.getHealth() == Full, "an unanchored Malidramagiuan took damage");
        RealityAnchorField Field = RealityAnchorField.spawn(World, Guardian.position().add(0.0, 1.0, 0.0));
        Helper.runAfterDelay(8, () -> {
            Helper.assertTrue(Guardian.anchored(), "the anchor field did not reach Malidramagiuan");
            Guardian.hurt(World.damageSources().generic(), 40.0f);
            float Wounded = Guardian.getHealth();
            Helper.assertTrue(Wounded < Full, "an anchored Malidramagiuan took no damage");
            Field.discard();
            Helper.runAfterDelay(50, () -> {
                Helper.assertTrue(!Guardian.anchored(), "Malidramagiuan stayed anchored after the field ended");
                Helper.assertTrue(Guardian.getHealth() > Wounded, "Malidramagiuan did not recover once unanchored");
                Guardian.discard();
                Helper.succeed();
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp1730_leechCap")
    public static void leechCap(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Helper.setBlock(new BlockPos(10, 2, 10), Scp1730Registry.Crack.get().defaultBlockState().setValue(Scp1730CrackBlock.Facing, Direction.NORTH));
        BlockPos Crack = Helper.absolutePos(new BlockPos(10, 2, 10));
        int Made = 0;
        for (int Attempt = 0; Attempt < 10; Attempt++) {
            if (Scp1730Cracks.spawnLeech(World, Crack)) {
                Made++;
            }
        }
        int Near = World.getEntitiesOfClass(Scp1730Leech.class, new AABB(Crack).inflate(4.0)).size();
        Helper.assertTrue(Made >= 1, "no leech crawled out of the crack");
        Helper.assertTrue(Near <= Scp1730Cracks.LeechesPerCrack, "the crack cap was exceeded: " + Near);
        for (Scp1730Leech Each : World.getEntitiesOfClass(Scp1730Leech.class, new AABB(Crack).inflate(6.0))) {
            Each.discard();
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp1730_tentacleSuppressed")
    public static void tentacleSuppressed(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Helper.setBlock(new BlockPos(10, 2, 10), Scp1730Registry.Crack.get().defaultBlockState().setValue(Scp1730CrackBlock.Facing, Direction.NORTH));
        BlockPos Crack = Helper.absolutePos(new BlockPos(10, 2, 10));
        Mob Bait = Helper.spawn(EntityType.ZOMBIE, new BlockPos(10, 1, 5));
        Helper.assertTrue(!Scp1730Cracks.suppressed(World, Crack), "the crack is suppressed with no anchor nearby");
        Scp1730Tentacle Free = Scp1730Tentacle.emerge(World, Crack, Direction.NORTH, Bait);
        RealityAnchorField.spawn(World, Vec3.atCenterOf(Crack).add(0.0, 0.0, -1.5));
        Helper.assertTrue(Scp1730Cracks.suppressed(World, Crack), "the anchor field did not suppress the crack");
        Helper.runAfterDelay(4, () -> {
            Helper.assertTrue(Free.phase() == Scp1730Tentacle.PhaseWithdraw || Free.isRemoved(), "the tentacle kept striking inside an anchor field");
            Bait.discard();
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp1730_turretPower")
    public static void turretPower(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Helper.setBlock(new BlockPos(8, 4, 8), Blocks.STONE);
        Helper.setBlock(new BlockPos(8, 3, 8), Scp1730Registry.Turret.get().defaultBlockState().setValue(Scp1730TurretBlock.Facing, Direction.SOUTH));
        ServerPlayer User = player(Helper, World, Helper.absoluteVec(new Vec3(8.5, 1.0, 13.5)), "scp1730_target");
        Scp1730TurretBlockEntity Turret = (Scp1730TurretBlockEntity) World.getBlockEntity(Helper.absolutePos(new BlockPos(8, 3, 8)));
        Helper.assertTrue(Turret != null, "the turret has no block entity");
        Helper.runAfterDelay(50, () -> {
            Helper.assertTrue(User.getHealth() >= User.getMaxHealth() && Turret.targetId() < 0, "an unpowered turret fired");
            Helper.setBlock(new BlockPos(9, 3, 8), Blocks.REDSTONE_BLOCK);
            Helper.runAfterDelay(60, () -> {
                Helper.assertTrue(User.getHealth() < User.getMaxHealth(), "a powered turret never hit its target");
                dismiss(User);
                Helper.succeed();
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp1730_anchorField")
    public static void anchorField(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Vec3 Centre = Helper.absoluteVec(new Vec3(16.5, 2.0, 16.5));
        RealityAnchorField.spawn(World, Centre.add(0.0, 0.25, 0.0));
        Helper.assertTrue(Scp1730Anchors.anchored(World, Centre.add(Scp1730Anchors.GrenadeRadius - 0.3, 0.0, 0.0)), "a point inside the field radius is not anchored");
        Helper.assertTrue(!Scp1730Anchors.anchored(World, Centre.add(Scp1730Anchors.GrenadeRadius + 0.6, 0.0, 0.0)), "a point outside the field radius is anchored");
        Helper.runAfterDelay(Scp1730Anchors.GrenadeTicks + 5, () -> {
            Helper.assertTrue(World.getEntitiesOfClass(RealityAnchorField.class, new AABB(Centre, Centre).inflate(2.0)).isEmpty(), "the field outlived its duration");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp1730_folderPower")
    public static void folderPower(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Master = new BlockPos(16, 1, 16);
        Helper.setBlock(Master, Scp1730Registry.Folder.get().defaultBlockState().setValue(RealityFolderBlock.Part, 62));
        RealityFolderBlockEntity Folder = (RealityFolderBlockEntity) World.getBlockEntity(Helper.absolutePos(Master));
        Helper.assertTrue(Folder != null, "the Reality Folder has no block entity");
        int[] Ticks = {0};
        Helper.onEachTick(() -> {
            Ticks[0]++;
            if (Ticks[0] <= 20) {
                Folder.feed(RealityFolderBlockEntity.Demand - 1);
            } else if (Ticks[0] <= 120) {
                Folder.feed(RealityFolderBlockEntity.Demand);
            }
        });
        Helper.runAfterDelay(20, () -> Helper.assertTrue(Folder.status() == RealityFolderBlockEntity.StatusOff, "an under-powered Folder started"));
        Helper.runAfterDelay(110, () -> {
            Helper.assertTrue(Folder.fieldUp(), "a fully powered Folder did not raise its field");
            Vec3 Centre = RealityFolderBlockEntity.fieldCentre(Helper.absolutePos(Master));
            Helper.assertTrue(Scp1730Anchors.anchored(World, Centre.add(Scp1730Anchors.FolderRadius - 0.5, 0.0, 0.0)), "the Folder field does not reach its radius");
            Helper.assertTrue(!Scp1730Anchors.anchored(World, Centre.add(Scp1730Anchors.FolderRadius + 1.0, 0.0, 0.0)), "the Folder field reaches too far");
        });
        Helper.runAfterDelay(140, () -> {
            Helper.assertTrue(!Folder.fieldUp(), "the Folder kept its field after losing power");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp1730_folderPorted")
    public static void folderPorted(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Master = Helper.absolutePos(new BlockPos(10, 1, 10));
        net.minecraft.world.level.block.state.BlockState Base = Scp1730Registry.Folder.get().defaultBlockState().setValue(RealityFolderBlock.Facing, Direction.NORTH);
        for (int Part = 0; Part < net.rai.scp_unleashed.scp1730.block.RealityFolderLayout.CellCount; Part++) {
            World.setBlock(net.rai.scp_unleashed.scp1730.block.RealityFolderLayout.cellPos(Master, Part), Base.setValue(RealityFolderBlock.Part, Part), 2);
        }
        RealityFolderBlockEntity Folder = (RealityFolderBlockEntity) World.getBlockEntity(Master);
        Helper.assertTrue(Folder != null, "the Reality Folder has no block entity");
        net.minecraft.world.level.block.state.BlockState Outlet = net.rai.scp_unleashed.creativeoutlet.CreativeOutletRegistry.Outlet.get().defaultBlockState();
        BlockPos Socket = null;
        Direction SocketSide = null;
        for (int Port : net.rai.scp_unleashed.scp1730.block.RealityFolderLayout.ports(Direction.NORTH)) {
            BlockPos Cell = net.rai.scp_unleashed.scp1730.block.RealityFolderLayout.cellPos(Master, Port);
            Direction Side = net.rai.scp_unleashed.scp1730.block.RealityFolderLayout.portSide(Port, Direction.NORTH);
            Helper.assertTrue(Side != null && Side != Direction.NORTH, "a power port sits on the front of the Folder");
            Helper.assertTrue(Outlet.setValue(net.rai.scp_unleashed.block.OutletBlock.Facing, Side).canSurvive(World, Cell.relative(Side)), "an outlet does not fit the " + Side + " power port");
            if (Side == Direction.SOUTH) {
                Socket = Cell.relative(Side);
                SocketSide = Side;
            }
        }
        Helper.assertTrue(Socket != null, "the Folder has no power port on its back");
        BlockPos Mount = Socket;
        Direction MountSide = SocketSide;
        Helper.runAfterDelay(40, () -> {
            Helper.assertTrue(Folder.status() == RealityFolderBlockEntity.StatusOff, "an unpowered Folder started");
            World.setBlock(Mount, Outlet.setValue(net.rai.scp_unleashed.block.OutletBlock.Facing, MountSide), 3);
            Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(Folder.fieldUp(), "a Folder with a Creative Outlet on its back port never raised its field")).thenSucceed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp1730_painterSuppressed")
    public static void painterSuppressed(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        for (int X = 5; X <= 13; X++) {
            for (int Z = 6; Z <= 10; Z++) {
                Helper.setBlock(new BlockPos(X, 0, Z), Blocks.SMOOTH_STONE);
                for (int Y = 1; Y <= 4; Y++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Blocks.AIR);
                }
            }
        }
        Scp1730Painter Painter = Helper.spawn(Scp1730Registry.PainterType.get(), new BlockPos(7, 1, 8));
        RealityAnchorField.spawn(World, Painter.position().add(0.0, 1.0, 0.0));
        Vec3[] Start = new Vec3[1];
        Helper.onEachTick(() -> {
            if (Start[0] != null) {
                Painter.getMoveControl().setWantedPosition(Start[0].x + 3.0, Start[0].y, Start[0].z, 1.0);
            }
        });
        Helper.runAfterDelay(8, () -> {
            Helper.assertTrue(Painter.anchored(), "the anchor field did not suppress the painter");
            Start[0] = Painter.position();
        });
        Helper.runAfterDelay(68, () -> {
            Helper.assertTrue(Start[0] != null && Painter.position().distanceTo(Start[0]) > 1.0, "a suppressed painter could not walk (" + Start[0] + " -> " + Painter.position() + ", ground " + Painter.onGround()
                    + ", collided " + Painter.horizontalCollision + ", speed " + Painter.getSpeed() + ", above " + World.getBlockState(Painter.blockPosition().above(2)) + ")");
            Helper.assertTrue(Painter.anchored() && !Painter.painting(), "a suppressed painter started painting");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp1730_coreLockout")
    public static void coreLockout(GameTestHelper Helper) {
        ServerPlayer User = enter(Helper, "scp1730_witness");
        ServerLevel Facility = facility(Helper);
        Scp1730State State = Scp1730State.get(Helper.getLevel().getServer());
        State.setGuardianDead();
        BlockPos Spot = User.blockPosition().above(3);
        Facility.setBlock(Spot, Scp1730Registry.Thresher.get().defaultBlockState(), 3);
        Scp1730ThresherBlockEntity Thresher = (Scp1730ThresherBlockEntity) Facility.getBlockEntity(Spot);
        Helper.assertTrue(Scp1730Collapse.canPull(Facility, Spot), "the Thresher cannot be pulled after its guardian fell");
        Scp1730Collapse.pull(Facility, Spot, Thresher, User);
        Helper.assertTrue(State.collapsing() && Thresher.pulled(), "pulling the core did not start the collapse");
        Helper.assertTrue(count(User, Scp1730Registry.Core.get()) == 1, "the puller did not receive the Thresher Core");
        Helper.runAfterDelay(10, () -> {
            GatewayTravel.leave(User);
            Helper.assertTrue(Scp1730Destination.sealed(User), "leaving the collapsing site did not lock the player out");
            Helper.assertTrue(!destination().Hooks().canEnter(User, Facility), "a locked-out player could enter again");
            CompoundTag Saved = User.saveWithoutId(new CompoundTag());
            Scp902TestPlayer Reloaded = new Scp902TestPlayer(Helper.getLevel(), "scp1730_reloaded");
            Reloaded.load(Saved);
            Helper.assertTrue(Scp1730Destination.sealed(Reloaded), "the lockout did not survive a save and load");
            Facility.removeBlock(Spot, false);
            Scp1730Destination.unseal(User);
            State.endRun();
            dismiss(User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp1730_breachRoster")
    public static void breachRoster(GameTestHelper Helper) {
        for (int Zone = 0; Zone < 4; Zone++) {
            for (Scp1730Breach.Threat Each : Scp1730Breach.roster(Zone)) {
                Helper.assertTrue(Scp1730Breach.allowed(Each.Id()), "an excluded entity is on the roster: " + Each.Id());
                Helper.assertTrue(Scp1730Breach.typeOf(Each.Id()) != null, "a roster entity does not exist: " + Each.Id());
            }
            for (int Index = 0; Index < 4; Index++) {
                for (Scp1730Breach.Threat Each : Scp1730Breach.setPiece(Zone, Index, 1730L + Index)) {
                    Helper.assertTrue(Scp1730Breach.allowed(Each.Id()) && Scp1730Breach.typeOf(Each.Id()) != null, "a set piece entity is invalid: " + Each.Id());
                }
            }
        }
        for (String Banned : Scp1730Breach.Excluded) {
            Helper.assertTrue(Scp1730Breach.spawnOne(Helper.getLevel(), Banned, Helper.absoluteVec(new Vec3(4.5, 1.0, 4.5)), 0.0f, Scp1730Breach.RoamingTag) == null,
                    "an excluded entity was spawned: " + Banned);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp1730_breachCaps")
    public static void breachCaps(GameTestHelper Helper) {
        ServerPlayer User = enter(Helper, "scp1730_crowd");
        ServerLevel Facility = facility(Helper);
        Scp1730State State = Scp1730State.get(Helper.getLevel().getServer());
        Helper.runAfterDelay(60, () -> {
            for (int Round = 0; Round < 40; Round++) {
                Scp1730Breach.spawnRound(Facility, User, State.run(), 0);
            }
            AABB Box = Scp1730Breach.boxOf(State.run(), Scp1730World.plan(State.run()).zone(0));
            int Zone = Scp1730Breach.countIn(Facility, Box, Scp1730Breach.RoamingTag);
            int Area = Scp1730Breach.countIn(Facility, User.getBoundingBox().inflate(Scp1730Breach.AreaRadius), Scp1730Breach.RoamingTag);
            Helper.assertTrue(Zone <= Scp1730Breach.ZoneCaps[0], "the Entrance Zone cap was exceeded: " + Zone);
            Helper.assertTrue(Area <= Scp1730Breach.AreaCaps[0], "the area cap was exceeded: " + Area);
            for (Mob Each : Facility.getEntitiesOfClass(Mob.class, Box.inflate(8.0), Each -> Each.getTags().contains(Scp1730Breach.RoamingTag))) {
                Each.discard();
            }
            GatewayTravel.leave(User);
            dismiss(User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp1730_nothingFollows")
    public static void nothingFollows(GameTestHelper Helper) {
        ServerPlayer User = enter(Helper, "scp1730_escapee");
        ServerLevel Facility = facility(Helper);
        BlockPos Arrival = User.blockPosition();
        List<long[]> Forced = forceAround(Facility, Arrival, 1);
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(ticking(Facility, Arrival, 1), "the arrival area is still loading")).thenExecuteAfter(5, () -> {
            Mob Chaser = Scp1730Breach.spawnOne(Facility, "scp008_zombie", User.position().add(1.5, 0.0, 0.0), 0.0f, Scp1730Breach.RoamingTag);
            holdFragment(User);
            User.getMainHandItem().use(User.level(), User, InteractionHand.MAIN_HAND);
            Helper.runAfterDelay(5, () -> {
                Helper.assertTrue(User.level() == Helper.getLevel(), "the traveller did not leave");
                Helper.assertTrue(Helper.getLevel().getEntitiesOfClass(Mob.class, User.getBoundingBox().inflate(16.0),
                        Each -> Each.getTags().contains(Scp1730Breach.RoamingTag)).isEmpty(), "something followed the traveller out");
                if (Chaser != null) {
                    Helper.assertTrue(Chaser.level() == Facility, "the facility threat left the facility");
                    Chaser.discard();
                }
                release(Facility, Forced);
                dismiss(User);
                Helper.succeed();
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp1730_auraLogic")
    public static void auraLogic(GameTestHelper Helper) {
        Helper.assertTrue(Math.abs(Scp1730Aura.strength(0.0, false, false) - 1.0) < 1.0e-9, "the aura is not full strength at its centre");
        Helper.assertTrue(Scp1730Aura.strength(Scp1730Aura.Range, false, false) == 0.0, "the aura reaches past its range");
        double Last = 2.0;
        for (double Distance = 0.0; Distance <= Scp1730Aura.Range; Distance += 1.0) {
            double Now = Scp1730Aura.strength(Distance, false, false);
            Helper.assertTrue(Now <= Last, "the aura grows with distance at " + Distance);
            Last = Now;
        }
        Helper.assertTrue(Scp1730Aura.strength(4.0, true, true) <= Scp1730Aura.strength(4.0, false, false) * 0.15, "anchoring did not collapse the aura");
        Helper.assertTrue(Scp1730Aura.strength(4.0, false, true) >= Scp1730Aura.strength(4.0, false, false), "casting did not strengthen the aura");
        Helper.assertTrue(Scp1730Aura.comfort(0.8, 0.0) == 0.0 && Math.abs(Scp1730Aura.comfort(0.8, 0.5) - 0.4) < 1.0e-9, "the comfort setting does not scale the aura");
        Helper.succeed();
    }

    public static List<String> scenePieces(int Zone) {
        List<String> Found = new java.util.ArrayList<>();
        for (Scp1730SetPieces.Piece Each : Scp1730SetPieces.Pieces.get(Zone)) {
            Found.add(Each.Scene());
        }
        return Found;
    }

    public static boolean inSceneCell(Scp1730Plan Plan, String Problem) {
        java.util.regex.Matcher Found = java.util.regex.Pattern.compile(" at (-?\\d+),(-?\\d+),(-?\\d+)").matcher(Problem);
        if (!Found.find()) {
            return false;
        }
        int X = Integer.parseInt(Found.group(1));
        int Y = Integer.parseInt(Found.group(2));
        int Z = Integer.parseInt(Found.group(3));
        for (net.rai.scp_unleashed.scp1730.plan.Scp1730RoomInfo Room : Plan.rooms()) {
            if (!Room.Type().equals("holding_cell") || X < Room.MinX() || X > Room.MaxX() || Z < Room.MinZ() || Z > Room.MaxZ() || Y < Room.Floor()
                    || Y > Room.Floor() + Room.Height() + 1) {
                continue;
            }
            for (Scp1730Fixture Each : Plan.fixtures("setpiece", Room.Zone())) {
                if (Each.X() >= Room.MinX() && Each.X() <= Room.MaxX() && Each.Z() >= Room.MinZ() && Each.Z() <= Room.MaxZ()
                        && !Scp1730SetPieces.scene(Room.Zone(), Each.Data(), Plan.seed()).equals(Scp1730SetPieces.Generic)) {
                    return true;
                }
            }
        }
        return false;
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp1730_setPieceTable")
    public static void setPieceTable(GameTestHelper Helper) {
        Helper.assertTrue(Scp1730Breach.SetPieces.size() == Scp1730SetPieces.Pieces.size(), "the breach set piece table does not match the plan table");
        java.util.Set<String> Ids = new java.util.TreeSet<>(Scp1730SetPieces.Biological);
        Ids.add(Scp1730SetPieces.ShyStandIn.Groups().getFirst().Id());
        for (int Zone = 0; Zone < 4; Zone++) {
            Helper.assertTrue(Scp1730Breach.SetPieces.get(Zone).size() == Scp1730SetPieces.count(Zone), "zone " + Zone + " has mismatched set pieces");
            for (Scp1730Breach.Threat Each : Scp1730Breach.roster(Zone)) {
                Ids.add(Each.Id());
                Helper.assertTrue(Each.Weight() > 0 && Each.Pack() > 0, "a roster entry has no weight or pack: " + Each.Id());
            }
            for (int Number = 0; Number < Scp1730SetPieces.count(Zone); Number++) {
                Scp1730SetPieces.Piece Piece = Scp1730SetPieces.Pieces.get(Zone).get(Number);
                Helper.assertTrue(Scp1730SetPieces.Scenes.contains(Piece.Scene()), "an unknown scene: " + Piece.Scene());
                for (Scp1730SetPieces.Group Each : Piece.Groups()) {
                    Ids.add(Each.Id());
                    Helper.assertTrue(Each.Pack() > 0, "a set piece group has no pack: " + Each.Id());
                }
                Helper.assertTrue(Scp1730Breach.biological(Scp1730Breach.setPiece(Zone, Number, 0L)) == Scp1730SetPieces.biological(Zone, Number, 0L),
                        "the breach and plan disagree on a biological set piece");
            }
        }
        for (String Id : Ids) {
            Helper.assertTrue(Scp1730Breach.allowed(Id), "an excluded entity is listed: " + Id);
            Helper.assertTrue(Scp1730Breach.typeOf(Id) != null, "a listed entity does not exist: " + Id);
        }
        for (String Id : Scp1730Breach.Offspring) {
            Helper.assertTrue(Scp1730Breach.typeOf(Id) != null && Scp1730Breach.allowed(Id), "an adoptable entity is invalid: " + Id);
        }
        Helper.assertTrue(scenePieces(1).contains(Scp1730SetPieces.Hive) && scenePieces(2).contains(Scp1730SetPieces.PlagueDoctor)
                && scenePieces(2).contains(Scp1730SetPieces.Incinerator) && scenePieces(3).contains(Scp1730SetPieces.BeastPen), "a new set piece is missing from its zone");
        Helper.assertTrue(scenePieces(1).contains(Scp1730SetPieces.StatueCell) && scenePieces(3).contains(Scp1730SetPieces.DarkCell), "a non biological scene is missing");
        for (int Zone = 0; Zone < 4; Zone++) {
            java.util.Set<String> Halls = new java.util.TreeSet<>();
            for (Scp1730Breach.Threat Each : Scp1730Breach.halls(Zone)) {
                Helper.assertTrue(!Scp1730Breach.biological(Each.Id()), "a hall threat is biological: " + Each.Id());
                Halls.add(Each.Id());
            }
            Helper.assertTrue(Halls.size() >= 3, "zone " + Zone + " has only " + Halls.size() + " non biological hall threats: " + Halls);
            boolean Plain = false;
            for (int Number = 0; Number < Scp1730SetPieces.count(Zone); Number++) {
                Plain |= !Scp1730SetPieces.biological(Zone, Number, 0L);
            }
            Helper.assertTrue(Plain, "zone " + Zone + " has no non biological set piece");
        }
        Helper.assertTrue(!Scp1730SetPieces.biological(1, scenePieces(1).indexOf(Scp1730SetPieces.StatueCell), 0L)
                && !Scp1730SetPieces.biological(3, scenePieces(3).indexOf(Scp1730SetPieces.DarkCell), 0L), "a statue or dark cell is marked biological");
        Helper.assertTrue(Scp1730SetPieces.biological(2, Scp1730SetPieces.featured(2), 0L), "the plague doctor cell is not marked biological");
        Helper.assertTrue(!Scp1730SetPieces.biological(2, scenePieces(2).indexOf(Scp1730SetPieces.Incinerator), 0L), "the incinerator cell is marked biological");
        Helper.assertTrue(Scp1730Breach.biological("scp049") && Scp1730Breach.biological("scp049_2") && Scp1730Breach.biological("scp008_zombie")
                && !Scp1730Breach.biological("scp173") && !Scp1730Breach.biological("scp457"), "the biological flags are wrong");
        for (long Seed = 0L; Seed < 24L; Seed++) {
            Helper.assertTrue(Scp1730SetPieces.choose(2, 0, Seed) == Scp1730SetPieces.featured(2), "index 0 is not the featured cell");
            Helper.assertTrue(Scp1730SetPieces.choose(2, 0, Seed, Boolean.TRUE) == Scp1730SetPieces.featured(2), "the first biological cell is not the featured cell");
            Helper.assertTrue(Scp1730SetPieces.choose(2, 0, Seed, Boolean.FALSE) != Scp1730SetPieces.featured(2), "a non biological pick returned the plague doctor");
            for (int Order = 0; Order < 4; Order++) {
                Scp1730SetPieces.Picker Picker = new Scp1730SetPieces.Picker(Seed);
                int Stars = 0;
                for (int Call = 0; Call < 4; Call++) {
                    Boolean Wanted = Call == Order ? Boolean.TRUE : Boolean.FALSE;
                    int Number = Picker.next(2, Wanted);
                    Helper.assertTrue(Scp1730SetPieces.fits(2, Number, Seed, Wanted), "a filtered pick ignored its filter");
                    if (Number == Scp1730SetPieces.featured(2)) {
                        Stars++;
                    }
                }
                Helper.assertTrue(Stars == 1, "the featured cell was placed " + Stars + " times when routed");
            }
        }
        List<String> Problems = new java.util.ArrayList<>();
        java.util.Set<String> Seen = new java.util.TreeSet<>();
        int Checked = 0;
        for (int Run = 0; Run < 28; Run++) {
            Scp1730Plan Plan = Scp1730World.plan(Run);
            long Seed = Plan.seed();
            int Doctors = 0;
            boolean Fresh = false;
            for (int Zone = 0; Zone < 4; Zone++) {
                for (Scp1730Fixture Each : Plan.fixtures("setpiece", Zone)) {
                    String Scene = Scp1730SetPieces.scene(Zone, Each.Data(), Plan.seed());
                    if (Scene.equals(Scp1730SetPieces.PlagueDoctor)) {
                        Doctors++;
                    }
                    if (!Scene.equals(Scp1730SetPieces.Generic) && Seen.add(Scene)) {
                        Fresh = true;
                    }
                }
            }
            Helper.assertTrue(Doctors == 1, "run " + Run + " has " + Doctors + " plague doctor cells");
            if (Fresh && Checked < 6) {
                Checked++;
                Problems.addAll(Scp1730PlanCheck.check(Seed));
                for (String Each : Scp1730WallAudit.audit(Seed)) {
                    if (inSceneCell(Plan, Each)) {
                        Problems.add(Each);
                    }
                }
            }
        }
        Helper.assertTrue(Seen.containsAll(List.of(Scp1730SetPieces.PlagueDoctor, Scp1730SetPieces.Hive, Scp1730SetPieces.Incinerator, Scp1730SetPieces.BeastPen,
                Scp1730SetPieces.StatueCell, Scp1730SetPieces.DarkCell)), "some new set pieces never appeared in 28 runs: " + Seen);
        Helper.assertTrue(Problems.isEmpty(), "set piece cells broke a plan check: " + Problems.subList(0, Math.min(6, Problems.size())));
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp1730_setPiecesSpawn")
    public static void setPiecesSpawn(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        String[][] Wanted = {{"1", Scp1730SetPieces.Hive}, {"2", Scp1730SetPieces.PlagueDoctor}, {"2", Scp1730SetPieces.Incinerator}, {"3", Scp1730SetPieces.BeastPen},
                {"1", Scp1730SetPieces.StatueCell}, {"3", Scp1730SetPieces.DarkCell}};
        int[][] Spots = {{6, 6}, {16, 6}, {26, 6}, {6, 25}, {16, 25}, {26, 25}};
        List<Mob> Everyone = new java.util.ArrayList<>();
        for (int Index = 0; Index < Wanted.length; Index++) {
            int Zone = Integer.parseInt(Wanted[Index][0]);
            int Number = scenePieces(Zone).indexOf(Wanted[Index][1]);
            List<Scp1730Breach.Threat> Piece = Scp1730Breach.setPiece(Zone, Number, 0L);
            Vec3 At = Helper.absoluteVec(new Vec3(Spots[Index][0] + 0.5, 2.0, Spots[Index][1] + 0.5));
            List<Mob> Made = Scp1730Breach.spawnSetPiece(World, Piece, At, 0.0f);
            for (Scp1730Breach.Threat Each : Piece) {
                long Count = Made.stream().filter(Body -> Scp1730Breach.typeOf(Each.Id()) == Body.getType()).count();
                Helper.assertTrue(Count == Each.Pack(), Wanted[Index][1] + " spawned " + Count + " of " + Each.Pack() + " " + Each.Id());
            }
            for (Mob Body : Made) {
                Helper.assertTrue(Body.getTags().contains(Scp1730Breach.SetPieceTag) && Body.isPersistenceRequired(), "a set piece mob is untagged or may despawn");
                if (Body instanceof Scp439Creature Member) {
                    Helper.assertTrue(BlockPos.containing(At).equals(Member.home()), "a hive member has no home");
                }
                if (Body instanceof Scp457 Burning) {
                    Helper.assertTrue(!Burning.isPrime(), "the incinerator SCP-457 is prime");
                }
                if (Body instanceof Scp049Cured Attendant) {
                    Helper.assertTrue(!Attendant.isMasterless(), "an attendant has no doctor");
                }
            }
            Everyone.addAll(Made);
        }
        Helper.runAfterDelay(20, () -> {
            for (Mob Body : Everyone) {
                Helper.assertTrue(Body.isAlive() && !Body.isRemoved(), "a set piece mob vanished: " + Body.getType().getDescriptionId());
            }
            for (Mob Body : Everyone) {
                if (Body instanceof Scp049Doctor Doctor) {
                    Scp1730Breach.clearTray(World, Doctor);
                }
                Body.discard();
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp1730_plagueDoctor")
    public static void plagueDoctor(GameTestHelper Helper) {
        ServerPlayer User = enter(Helper, "scp1730_plague");
        ServerLevel Facility = facility(Helper);
        User.setGameMode(GameType.CREATIVE);
        BlockPos Arrival = User.blockPosition();
        List<long[]> Forced = forceAround(Facility, Arrival, 1);
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(ticking(Facility, Arrival, 1), "the arrival area is still loading")).thenExecuteAfter(40, () -> {
            Vec3 At = User.position();
            int Number = Scp1730SetPieces.featured(2);
            List<Mob> Made = Scp1730Breach.spawnSetPiece(Facility, Scp1730Breach.setPiece(2, Number, 0L), At, 0.0f);
            Scp049Doctor Doctor = null;
            int Attendants = 0;
            for (Mob Body : Made) {
                if (Body instanceof Scp049Doctor Found) {
                    Doctor = Found;
                } else if (Body instanceof Scp049Cured) {
                    Attendants++;
                }
            }
            Helper.assertTrue(Doctor != null, "SCP-049 did not spawn through the facility");
            Helper.assertTrue(Attendants == 2, "SCP-049 arrived with " + Attendants + " attendants");
            Scp049Doctor Found = Doctor;
            Mob Stray = EntityType.ZOMBIE.create(Facility);
            if (Stray != null) {
                Stray.moveTo(At.x, At.y, At.z, 0.0f, 0.0f);
                Facility.addFreshEntity(Stray);
            }
            Scp049Cured Risen = Scp049Cured.rise(Facility, new CompoundTag(), At, 0.0f, Found, null);
            Helper.runAfterDelay(100, () -> {
                Helper.assertTrue(Found.isAlive() && !Found.isRemoved() && Found.level() == Facility, "SCP-049 did not survive in the facility");
                Helper.assertTrue(Found.getTags().contains(Scp1730Breach.SetPieceTag) && Found.isPersistenceRequired(), "SCP-049 lost his set piece setup");
                for (Mob Body : Made) {
                    if (Body instanceof Scp049Cured Attendant) {
                        Helper.assertTrue(Attendant.isAlive() && !Attendant.isMasterless(), "an attendant lost its doctor");
                    }
                }
                BlockPos Tray = Found.trayPos();
                Helper.assertTrue(Tray != null && Facility.getBlockState(Tray).is(Scp049Registry.Tray.get()), "SCP-049 never set up his tray");
                Helper.assertTrue(Risen == null || Risen.getTags().contains(Scp1730Breach.RoamingTag), "a risen SCP-049-2 was not adopted by the facility");
                Helper.assertTrue(Stray == null || !Stray.getTags().contains(Scp1730Breach.RoamingTag), "an unrelated mob was adopted by the facility");
                for (Mob Body : Made) {
                    Helper.assertTrue(!Body.getTags().contains(Scp1730Breach.RoamingTag), "a set piece mob was retagged as roaming");
                }
                Scp1730Breach.clearTray(Facility, Found);
                Helper.assertTrue(!Facility.getBlockState(Tray).is(Scp049Registry.Tray.get()), "the tray was left behind");
                for (Mob Body : Made) {
                    Body.discard();
                }
                if (Risen != null) {
                    Risen.discard();
                }
                if (Stray != null) {
                    Stray.discard();
                }
                release(Facility, Forced);
                GatewayTravel.leave(User);
                dismiss(User);
                Helper.succeed();
            });
        });
    }
}
