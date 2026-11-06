package net.rai.scp_unleashed.scp020;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.labware.LabContamination;
import net.rai.scp_unleashed.labware.LabExposure;
import net.rai.scp_unleashed.labware.LabOrganisms;
import net.rai.scp_unleashed.labware.LabVials;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Scp020Mold {
    public static final double Reach = 4.0;
    public static final int BreatheTicks = 20;
    public static final int SightChecks = 6;
    public static final int LocalRadius = 2;
    public static final int LocalCap = 14;
    public static final int CeilingChance = 6;
    public static final int CeilingReach = 12;
    public static final int SpreadInterval = 20;
    public static final int RandomTickArea = 4096;
    public static final double PlayerReach = 128.0;

    private Scp020Mold() {
    }

    public static boolean isSunlit(Level World, BlockPos Pos) {
        return World.isDay() && World.canSeeSky(Pos);
    }

    public static void tick(ServerLevel World) {
        if (World.getGameTime() % SpreadInterval != 0) {
            return;
        }
        int TickSpeed = World.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        if (TickSpeed <= 0 || Scp020Registry.spreadLimit(World.getGameRules()) <= 0) {
            return;
        }
        double Chance = 1.0 - Math.pow(1.0 - Math.min(1.0, TickSpeed / (double) RandomTickArea), SpreadInterval);
        RandomSource Random = World.getRandom();
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (long Key : Scp020Infection.tracked(World)) {
            LevelChunk Chunk = Scp020Infection.chunkAt(World, ChunkPos.getX(Key), ChunkPos.getZ(Key));
            Scp020Growth Colony = Scp020Infection.colonyIn(Chunk);
            if (Colony == null) {
                Scp020Infection.untrack(World, Key);
                continue;
            }
            if (!isTicking(World, Key)) {
                continue;
            }
            for (long Host : Colony.hostArray()) {
                if (Random.nextDouble() < Chance) {
                    trySpread(World, Cursor.set(Host).immutable(), Random);
                }
            }
        }
    }

    public static boolean isTicking(ServerLevel World, long Key) {
        if (!World.shouldTickBlocksAt(Key)) {
            return false;
        }
        if (World.getForcedChunks().contains(Key)) {
            return true;
        }
        double CentreX = ChunkPos.getX(Key) * 16.0 + 8.0;
        double CentreZ = ChunkPos.getZ(Key) * 16.0 + 8.0;
        for (ServerPlayer Watcher : World.players()) {
            double OffsetX = Watcher.getX() - CentreX;
            double OffsetZ = Watcher.getZ() - CentreZ;
            if (!Watcher.isSpectator() && OffsetX * OffsetX + OffsetZ * OffsetZ < PlayerReach * PlayerReach) {
                return true;
            }
        }
        return false;
    }

    public static void trySpread(ServerLevel World, BlockPos Host, RandomSource Random) {
        int Limit = Scp020Registry.spreadLimit(World.getGameRules());
        if (Limit <= 0 || Random.nextInt(100) >= Scp020Registry.spreadChance(World.getGameRules())) {
            return;
        }
        if (Scp020MoldCensus.get(World).faces() >= Limit) {
            return;
        }
        spreadFrom(World, Host, Random);
    }

    public static boolean spreadFrom(ServerLevel World, BlockPos Host, RandomSource Random) {
        int Faces = Scp020Infection.facesAt(World, Host);
        if (Faces == 0 || Scp020Infection.countNear(World, Host, LocalRadius) >= LocalCap) {
            return false;
        }
        for (Direction Side : shuffled(Direction.values(), Random)) {
            if (!Scp020Growth.hasFace(Faces, Side)) {
                continue;
            }
            if (!Scp020Infection.holds(World, Host, Side)) {
                Scp020Infection.cure(World, Host, Side);
                continue;
            }
            BlockPos Cell = Host.relative(Side);
            if (!World.getBlockState(Cell).isAir() || isSunlit(World, Cell)) {
                continue;
            }
            for (Direction Step : shuffled(Direction.values(), Random)) {
                if (Step.getAxis() == Side.getAxis() || Scp020Infection.has(World, Cell.relative(Step), Step.getOpposite())) {
                    continue;
                }
                if (tryInto(World, Cell.relative(Step), Step.getOpposite()) || tryInto(World, Host.relative(Step), Side) || tryInto(World, Host, Step)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean tryInto(ServerLevel World, BlockPos Host, Direction Side) {
        if (Scp020Infection.has(World, Host, Side) || !Scp020Infection.canTake(World, Host, Side) || isSunlit(World, Host.relative(Side))) {
            return false;
        }
        return Scp020Infection.infect(World, Host, Side);
    }

    private static List<Direction> shuffled(Direction[] Choices, RandomSource Random) {
        List<Direction> Order = new ArrayList<>(List.of(Choices));
        for (int Index = Order.size() - 1; Index > 0; Index--) {
            int Swap = Random.nextInt(Index + 1);
            Direction Held = Order.get(Index);
            Order.set(Index, Order.get(Swap));
            Order.set(Swap, Held);
        }
        return Order;
    }

    public static void seedCeiling(ServerLevel World, BlockPos Source, RandomSource Random) {
        int Limit = Scp020Registry.spreadLimit(World.getGameRules());
        if (Random.nextInt(CeilingChance) != 0 || Limit <= 0 || Scp020MoldCensus.get(World).faces() >= Limit) {
            return;
        }
        BlockPos.MutableBlockPos Cursor = Source.mutable();
        for (int Step = 1; Step <= CeilingReach; Step++) {
            Cursor.move(Direction.UP);
            if (Cursor.getY() >= World.getMaxBuildHeight() || !World.isLoaded(Cursor)) {
                return;
            }
            if (World.getBlockState(Cursor).isAir()) {
                continue;
            }
            BlockPos Host = Cursor.immutable();
            BlockPos Cell = Host.below();
            if (Cell.equals(Source) || isSunlit(World, Cell) || Scp020Infection.countNear(World, Host, LocalRadius) >= LocalCap) {
                return;
            }
            if (Scp020Infection.canTake(World, Host, Direction.DOWN)) {
                Scp020Infection.infect(World, Host, Direction.DOWN);
            }
            return;
        }
    }

    public static InteractionResult plant(UseOnContext Context, ItemStack Vial) {
        Player User = Context.getPlayer();
        Level World = Context.getLevel();
        if (User == null) {
            return InteractionResult.PASS;
        }
        BlockPos Host = Context.getClickedPos();
        Direction Side = Context.getClickedFace();
        boolean Takes = Scp020Infection.holds(World, Host, Side) && Scp020Infection.isOpen(World, Host, Side);
        if (!LabVials.isOpen(Vial)) {
            return Takes ? refuse(World, User, "message.scp_unleashed.labware.vial_unopened") : InteractionResult.PASS;
        }
        if (!Takes || !World.mayInteract(User, Host) || !User.mayUseItemAt(Host.relative(Side), Side, Vial)) {
            return refuse(World, User, "message.scp_unleashed.scp020.no_hold");
        }
        if (World instanceof ServerLevel Server) {
            Scp020Infection.infect(Server, Host, Side);
            Item Used = Vial.getItem();
            User.setItemInHand(Context.getHand(), ItemUtils.createFilledResult(Vial, User, LabVials.openVial(null)));
            User.awardStat(Stats.ITEM_USED.get(Used));
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp020.planted").withStyle(ChatFormatting.DARK_GREEN), true);
            Server.playSound(null, User.getX(), User.getEyeY() - 0.4, User.getZ(), Scp020Registry.Plant.get(), SoundSource.PLAYERS, 1.0f, 0.9f + Server.getRandom().nextFloat() * 0.15f);
            expose(Server, Scp020Infection.faceCentre(Host, Side));
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    private static InteractionResult refuse(Level World, Player User, String Key) {
        if (!World.isClientSide) {
            User.displayClientMessage(Component.translatable(Key).withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    public static void expose(ServerLevel World, Vec3 Point) {
        LabOrganisms.spread(World, Point, LabExposure.NothingIgnored, Scp020Organism.get(), Reach, null);
    }

    public static boolean disturb(ServerLevel World, BlockPos Host) {
        int Faces = Scp020Infection.facesAt(World, Host);
        for (Direction Side : Direction.values()) {
            if (Scp020Growth.hasFace(Faces, Side) && Scp020Infection.isOpen(World, Host, Side)) {
                expose(World, Scp020Infection.faceCentre(Host, Side));
                return true;
            }
        }
        return false;
    }

    public static void broken(ServerLevel World, BlockPos Host) {
        if (Scp020Infection.facesAt(World, Host) != 0) {
            disturb(World, Host);
        }
    }

    public static void breathe(LivingEntity User) {
        if (!User.isAlive() || InfectionImmunity.isImmune(User) || !(User.level() instanceof ServerLevel World)) {
            return;
        }
        if (Scp020Affliction.isProtected(User) && LabContamination.carries(User, Scp020Organism.Id)) {
            return;
        }
        Vec3 Eyes = User.getEyePosition();
        BlockPos Low = BlockPos.containing(Eyes.x - Reach - 1.0, Eyes.y - Reach - 1.0, Eyes.z - Reach - 1.0);
        BlockPos High = BlockPos.containing(Eyes.x + Reach + 1.0, Eyes.y + Reach + 1.0, Eyes.z + Reach + 1.0);
        List<Vec3> Points = new ArrayList<>();
        double Limit = Reach * Reach;
        Scp020Infection.forEachIn(World, Low, High, (Host, Side) -> {
            Vec3 Point = Scp020Infection.faceCentre(Host, Side);
            if (Point.distanceToSqr(Eyes) <= Limit && Scp020Infection.isOpen(World, Host, Side) && Scp020Infection.holds(World, Host, Side)) {
                Points.add(Point);
            }
        });
        if (Points.isEmpty()) {
            return;
        }
        Points.sort(Comparator.comparingDouble(Point -> Point.distanceToSqr(Eyes)));
        for (int Index = 0; Index < Math.min(SightChecks, Points.size()); Index++) {
            Vec3 Point = Points.get(Index);
            if (LabExposure.sees(World, Point, User, LabExposure.NothingIgnored)) {
                expose(World, Point);
                return;
            }
        }
    }
}
