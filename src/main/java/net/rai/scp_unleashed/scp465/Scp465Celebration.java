package net.rai.scp_unleashed.scp465;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.stranger.StrangerEncounters;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Scp465Celebration {
    public static final String Messages = "message.scp_unleashed.scp465.";
    public static final int PeopleBase = 3;
    public static final int PeopleArea = 15;
    public static final int MinPeople = 6;
    public static final int MaxPeople = 30;
    public static final int PeoplePerClown = 7;
    public static final int MaxClowns = 4;
    public static final double RingStart = 2.0;
    public static final double RingStep = 1.25;
    public static final double RingSpacing = 1.15;
    public static final int MaxRings = 6;
    public static final double SpawnJitter = 0.6;
    public static final double PanelHeight = 1.0 / 16.0;
    public static final int WanderTries = 6;
    public static final int GuardInterval = 5;
    public static final int GuardHold = 30;
    public static final double GuardPush = 0.34;
    public static final double GuardLift = 0.12;
    public static final int CheerInterval = 22;
    public static final int CelebrateInterval = 160;
    public static final double WalkSpeed = 0.75;
    public static final int WeightDance = 40;
    public static final int WeightSnack = 15;
    public static final int WeightChat = 20;
    public static final int WeightWander = 10;
    public static final int WeightCelebrate = 15;

    public enum Outcome {
        Ignored,
        Refused,
        Opened,
        Closed
    }

    private Scp465Celebration() {
    }

    public static boolean canUse(@Nullable LivingEntity User) {
        return User == null || !(User instanceof FakePlayer) && !User.isSpectator() && User.isAlive();
    }

    public static Outcome open(ServerLevel World, BlockPos BoxPos, @Nullable LivingEntity Opener) {
        BlockState State = World.getBlockState(BoxPos);
        if (!(State.getBlock() instanceof Scp465BoxBlock) || Scp465BoxBlock.isOpen(State) || !canUse(Opener)) {
            return Outcome.Ignored;
        }
        if (!(World.getBlockEntity(BoxPos) instanceof Scp465BoxBlockEntity Carton)) {
            return Outcome.Ignored;
        }
        Scp465Room.Scan Found = Scp465Room.scan(World, BoxPos);
        Scp465Parties Data = Scp465Parties.get(World);
        if (Found.fits()) {
            for (Scp465Party Other : Data.all()) {
                if (Other.overlaps(Found.Cells())) {
                    Found = Found.withResult(Scp465Room.Verdict.Taken);
                    break;
                }
            }
        }
        if (!Found.fits()) {
            refuse(World, BoxPos, Opener, Found.Result());
            return Outcome.Refused;
        }
        Scp465Layout.Plan Plan = Scp465Layout.plan(Found, BoxPos);
        long Now = World.getGameTime();
        BlockPos Low = Found.Low();
        int[] Cells = packed(Found.Cells(), Low);
        int[] Kept = packed(Found.Kept(), Low);
        Set<UUID> Celebrants = new LinkedHashSet<>();
        AABB Bounds = new AABB(Low.getX(), Low.getY(), Low.getZ(), Found.High().getX() + 1.0, Found.High().getY() + 1.0, Found.High().getZ() + 1.0);
        for (LivingEntity Present : World.getEntitiesOfClass(LivingEntity.class, Bounds.inflate(0.5), Scp465Celebration::personLike)) {
            BlockPos Feet = BlockPos.containing(Present.getX(), Present.getY() + 0.2, Present.getZ());
            if (Found.Cells().contains(Feet.asLong()) || Found.Cells().contains(BlockPos.containing(Present.getEyePosition()).asLong())) {
                Celebrants.add(Present.getUUID());
            }
        }
        if (Opener instanceof Player) {
            Celebrants.add(Opener.getUUID());
        }
        Set<Integer> Skipped = new HashSet<>();
        for (int Packed : Kept) {
            Skipped.add(Packed);
        }
        int[] Restore = Arrays.stream(Cells).filter(Packed -> !Skipped.contains(Packed)).toArray();
        Scp465Snapshot Snapshot = Scp465Snapshot.capture(World, Low, Restore);
        Scp465Snapshot.clear(World, Low, Restore);
        for (Map.Entry<BlockPos, BlockState> Entry : Plan.Placed().entrySet()) {
            World.setBlock(Entry.getKey(), Entry.getValue(), Scp465Snapshot.QuietFlags);
        }
        int Id = Data.nextId();
        Scp465Party Party = new Scp465Party(Id, BoxPos, Low, Cells, Kept, Found.FloorY(), Snapshot, Celebrants, Plan.Booth(), Plan.BoothFacing(), Plan.DjStand(),
                Plan.Bar(), Plan.BarFront(), Plan.Dance(), Plan.Floor(), Now, Now + Scp465Party.CheerTicks);
        Data.add(Party);
        nudge(World, Party);
        spawnGuests(World, Party, Plan.FloorArea());
        World.setBlock(BoxPos, State.setValue(Scp465BoxBlock.Open, true), Block.UPDATE_ALL);
        if (World.getBlockEntity(BoxPos) instanceof Scp465BoxBlockEntity Bound) {
            Bound.bind(Id, Party.musicStart(), Plan.Booth(), Party.low(), Party.high(), Celebrants.size());
        } else {
            Carton.bind(Id, Party.musicStart(), Plan.Booth(), Party.low(), Party.high(), Celebrants.size());
        }
        Vec3 Mouth = Vec3.atCenterOf(BoxPos);
        Scp465Effects.play(World, Mouth, Scp465Registry.OpenLayers, 1.0f, 1.0f);
        Scp465Effects.play(World, Mouth, Scp465Registry.BurstLayers, 1.1f, 1.0f);
        World.gameEvent(Opener, GameEvent.CONTAINER_OPEN, BoxPos);
        Data.setDirty();
        return Outcome.Opened;
    }

    private static int[] packed(Iterable<Long> Cells, BlockPos Low) {
        List<Integer> Found = new ArrayList<>();
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (long Packed : Cells) {
            Cursor.set(Packed);
            Found.add(Scp465Snapshot.packOf(Low, Cursor));
        }
        Found.sort(Integer::compare);
        return Found.stream().mapToInt(Integer::intValue).toArray();
    }

    public static boolean personLike(LivingEntity Target) {
        if (!Target.isAlive() || Target instanceof Scp465Guest) {
            return false;
        }
        if (Target instanceof Player User) {
            return !User.isSpectator() && !(User instanceof FakePlayer);
        }
        return ControlBodies.isPersonLike(Target);
    }

    private static void refuse(ServerLevel World, BlockPos BoxPos, @Nullable LivingEntity Opener, Scp465Room.Verdict Why) {
        Scp465Effects.play(World, Vec3.atCenterOf(BoxPos), Scp465Registry.RattleLayers, 0.9f, 1.0f);
        if (Opener instanceof Player User) {
            String Key = switch (Why) {
                case TooBig -> "refuse.big";
                case TooSmall -> "refuse.small";
                case Taken -> "refuse.taken";
                default -> "refuse.open";
            };
            Scp465Effects.hint(User, Messages + Key);
        }
    }

    public static int people(int Area) {
        return Mth.clamp(Math.round(PeopleBase + Area / (float) PeopleArea), MinPeople, MaxPeople);
    }

    public static int clowns(int People) {
        return Mth.clamp(People / PeoplePerClown, 1, MaxClowns);
    }

    public static int guestCount(int People) {
        return People - 1 - clowns(People);
    }

    private static void spawnGuests(ServerLevel World, Scp465Party Party, int Area) {
        RandomSource Random = World.getRandom();
        int People = people(Area);
        int Clowns = clowns(People);
        List<String> Names = Scp465Names.pick(guestCount(People), Party.names(), Random);
        Party.names().addAll(Names);
        Set<BlockPos> Occupied = occupied(World, Party);
        List<BlockPos> Spawns = spread(Party, Clowns + Names.size(), Occupied, Random);
        Vec3 Centre = celebrantCentre(World, Party);
        List<Vec3> Ring = ring(Party, Centre, Spawns.size(), Occupied);
        Scp465Guest Dj = Scp465Registry.Guest.get().create(World);
        if (Dj != null) {
            Vec3 Stand = Vec3.atBottomCenterOf(Party.djStand());
            float Yaw = Party.boothFacing().toYRot();
            Dj.moveTo(Stand.x, Stand.y, Stand.z, Yaw, 0.0f);
            Dj.setYHeadRot(Yaw);
            Dj.setYBodyRot(Yaw);
            Dj.setup(Scp465Guest.KindDj, 0, Party.id(), Party.musicStart());
            Dj.setCustomName(Component.translatable(Scp465Names.DjKey));
            Dj.setCustomNameVisible(true);
            Dj.finalizeSpawn(World, World.getCurrentDifficultyAt(Party.djStand()), MobSpawnType.EVENT, null);
            Dj.setPersistenceRequired();
            assignDj(Party, Dj);
            World.addFreshEntity(Dj);
            Party.addGuest(Dj.getUUID(), true);
            Scp465Effects.confetti(World, Stand.add(0.0, 1.0, 0.0), 24, 0.35, 0.12);
        }
        for (int Index = 0; Index < Spawns.size(); Index++) {
            Scp465Guest Guest = Scp465Registry.Guest.get().create(World);
            if (Guest == null) {
                continue;
            }
            BlockPos Cell = Spawns.get(Index);
            Vec3 Spawn = standOn(Party, Cell);
            Guest.moveTo(Spawn.x, Spawn.y, Spawn.z, Random.nextFloat() * 360.0f, 0.0f);
            if (Index < Clowns) {
                Guest.setup(Scp465Guest.KindClown, Index % Scp465Names.ClownLooks, Party.id(), Party.musicStart());
            } else {
                int Order = Index - Clowns;
                String Name = Names.get(Order);
                Guest.setup(Scp465Guest.KindGuest, Scp465Names.lookFor(Name, Order), Party.id(), Party.musicStart());
                Guest.setCustomName(Scp465Names.name(Name));
                Guest.setCustomNameVisible(true);
            }
            Guest.finalizeSpawn(World, World.getCurrentDifficultyAt(Cell), MobSpawnType.EVENT, null);
            Guest.setPersistenceRequired();
            Vec3 Target = Index < Ring.size() ? Ring.get(Index) : Spawn;
            Guest.assign("cheer", Target, null, Centre, Scp465Guest.ActionCheer, Party.musicStart());
            World.addFreshEntity(Guest);
            Party.addGuest(Guest.getUUID(), false);
            Scp465Effects.confetti(World, Spawn.add(0.0, 1.0, 0.0), 24, 0.35, 0.12);
        }
    }

    public static Vec3 standOn(Scp465Party Party, BlockPos Cell) {
        return Vec3.atBottomCenterOf(Cell).add(0.0, Party.dance().contains(Cell) ? PanelHeight : 0.0, 0.0);
    }

    public static Set<BlockPos> occupied(ServerLevel World, Scp465Party Party) {
        Set<BlockPos> Found = new HashSet<>();
        Found.add(Party.djStand());
        for (UUID Id : Party.celebrants()) {
            Entity Present = World.getEntity(Id);
            if (Present != null && Party.inArea(Present)) {
                Found.add(BlockPos.containing(Present.getX(), Party.floorY(), Present.getZ()));
            }
        }
        return Found;
    }

    public static List<BlockPos> spread(Scp465Party Party, int Count, Set<BlockPos> Occupied, RandomSource Random) {
        List<BlockPos> Options = new ArrayList<>();
        for (BlockPos Cell : Party.floor()) {
            if (!Occupied.contains(Cell)) {
                Options.add(Cell);
            }
        }
        List<BlockPos> Picked = new ArrayList<>();
        if (Options.isEmpty() || Count <= 0) {
            return Picked;
        }
        double[] Nearest = new double[Options.size()];
        Arrays.fill(Nearest, Double.MAX_VALUE);
        int Next = Random.nextInt(Options.size());
        while (Picked.size() < Math.min(Count, Options.size())) {
            BlockPos Chosen = Options.get(Next);
            Picked.add(Chosen);
            Nearest[Next] = -1.0;
            int Best = -1;
            double Farthest = -1.0;
            for (int Index = 0; Index < Options.size(); Index++) {
                if (Nearest[Index] < 0.0) {
                    continue;
                }
                BlockPos Cell = Options.get(Index);
                double Dx = Cell.getX() - Chosen.getX();
                double Dz = Cell.getZ() - Chosen.getZ();
                Nearest[Index] = Math.min(Nearest[Index], Math.sqrt(Dx * Dx + Dz * Dz));
                double Score = Nearest[Index] + Random.nextDouble() * SpawnJitter;
                if (Score > Farthest) {
                    Farthest = Score;
                    Best = Index;
                }
            }
            if (Best < 0) {
                break;
            }
            Next = Best;
        }
        return Picked;
    }

    public static Set<BlockPos> spots(List<Scp465Guest> Guests, @Nullable Scp465Guest Skipped) {
        Set<BlockPos> Taken = new HashSet<>();
        for (Scp465Guest Other : Guests) {
            if (Other != Skipped && Other.spot() != null) {
                Taken.add(BlockPos.containing(Other.spot()));
            }
        }
        return Taken;
    }

    private static void assignDj(Scp465Party Party, Scp465Guest Dj) {
        Vec3 Stand = Vec3.atBottomCenterOf(Party.djStand());
        Vec3 Deck = Vec3.atBottomCenterOf(Party.booth()).add(Vec3.atLowerCornerOf(Party.boothFacing().getNormal()).scale(2.0));
        Dj.assign("dj", Stand, null, Deck.add(0.0, 1.0, 0.0), Scp465Guest.ActionDj, Long.MAX_VALUE);
    }

    public static Vec3 celebrantCentre(ServerLevel World, Scp465Party Party) {
        double X = 0.0;
        double Y = 0.0;
        double Z = 0.0;
        int Count = 0;
        for (UUID Id : Party.celebrants()) {
            Entity Found = World.getEntity(Id);
            if (Found != null && Party.inArea(Found)) {
                X += Found.getX();
                Y += Found.getY();
                Z += Found.getZ();
                Count++;
            }
        }
        if (Count == 0) {
            return Vec3.atBottomCenterOf(Party.boxPos());
        }
        return new Vec3(X / Count, Y / Count, Z / Count);
    }

    public static List<Vec3> ring(Scp465Party Party, Vec3 Centre, int Count, Set<BlockPos> Taken) {
        List<Vec3> Spots = new ArrayList<>();
        Set<BlockPos> Used = new HashSet<>(Taken);
        double Radius = RingStart;
        int Placed = 0;
        for (int Round = 0; Round < MaxRings && Placed < Count; Round++) {
            int Room = Math.max(1, (int) Math.floor(Mth.TWO_PI * Radius / RingSpacing));
            int Here = Math.min(Room, Count - Placed);
            for (int Index = 0; Index < Here; Index++) {
                double Angle = Index * Mth.TWO_PI / Here + 0.4 + Round * 0.5;
                Vec3 Wanted = Centre.add(Math.cos(Angle) * Radius, 0.0, Math.sin(Angle) * Radius);
                BlockPos Cell = nearestFloor(Party, Wanted, Used);
                if (Cell != null) {
                    Used.add(Cell);
                    Spots.add(Vec3.atBottomCenterOf(Cell));
                }
            }
            Placed += Here;
            Radius += RingStep;
        }
        return Spots;
    }

    @Nullable
    public static BlockPos nearestFloor(Scp465Party Party, Vec3 Wanted, Set<BlockPos> Used) {
        BlockPos Best = null;
        double Closest = Double.MAX_VALUE;
        for (BlockPos Cell : Party.floor()) {
            if (Used.contains(Cell) || Cell.equals(Party.djStand())) {
                continue;
            }
            double Dx = Cell.getX() + 0.5 - Wanted.x;
            double Dz = Cell.getZ() + 0.5 - Wanted.z;
            double Distance = Dx * Dx + Dz * Dz;
            if (Distance < Closest) {
                Closest = Distance;
                Best = Cell;
            }
        }
        return Best;
    }

    public static Outcome closeBy(ServerLevel World, BlockPos BoxPos, Player User) {
        if (!canUse(User)) {
            return Outcome.Ignored;
        }
        BlockState State = World.getBlockState(BoxPos);
        if (!Scp465BoxBlock.isOpen(State)) {
            return Outcome.Ignored;
        }
        Scp465Party Party = World.getBlockEntity(BoxPos) instanceof Scp465BoxBlockEntity Carton ? Scp465Parties.find(World, Carton.partyId()) : null;
        if (Party == null) {
            World.setBlock(BoxPos, State.setValue(Scp465BoxBlock.Open, false), Block.UPDATE_ALL);
            Scp465Effects.play(World, Vec3.atCenterOf(BoxPos), Scp465Registry.CloseLayers, 0.9f, 1.0f);
            return Outcome.Closed;
        }
        if (!Party.isCelebrant(User)) {
            Scp465Effects.hint(User, Messages + "not_yours");
            Scp465Effects.play(World, Vec3.atCenterOf(BoxPos), Scp465Registry.RattleLayers, 0.6f, 1.2f);
            return Outcome.Refused;
        }
        close(World, Party, User);
        return Outcome.Closed;
    }

    public static void close(ServerLevel World, Scp465Party Party, @Nullable Entity Closer) {
        Scp465Parties Data = Scp465Parties.get(World);
        List<ServerPlayer> Present = new ArrayList<>();
        for (ServerPlayer User : World.players()) {
            if (!User.isSpectator() && !(User instanceof FakePlayer) && Party.inArea(User)) {
                Present.add(User);
            }
        }
        for (UUID Id : Party.guests()) {
            if (World.getEntity(Id) instanceof Scp465Guest Guest) {
                vanish(World, Guest);
            }
        }
        Data.remove(Party.id());
        dropForeign(World, Party);
        Party.snapshot().restore(World, Party.low(), Party.restoreCells());
        nudge(World, Party);
        BlockPos BoxPos = Party.boxPos();
        BlockState State = World.getBlockState(BoxPos);
        if (State.getBlock() instanceof Scp465BoxBlock) {
            if (State.getValue(Scp465BoxBlock.Open)) {
                World.setBlock(BoxPos, State.setValue(Scp465BoxBlock.Open, false), Block.UPDATE_ALL);
            }
            if (World.getBlockEntity(BoxPos) instanceof Scp465BoxBlockEntity Carton) {
                Carton.unbind();
            }
            Scp465Effects.play(World, Vec3.atCenterOf(BoxPos), Scp465Registry.CloseLayers, 1.0f, 1.0f);
        }
        for (ServerPlayer User : Present) {
            StrangerEncounters.give(User, new ItemStack(Scp465Registry.Letter.get()));
        }
        World.gameEvent(Closer, GameEvent.CONTAINER_CLOSE, BoxPos);
        Data.setDirty();
    }

    private static void dropForeign(ServerLevel World, Scp465Party Party) {
        for (int Packed : Party.restoreCells()) {
            BlockPos Pos = Scp465Snapshot.unpack(Party.low(), Packed);
            BlockState State = World.getBlockState(Pos);
            if (State.isAir() || Scp465PartyBlock.isPartyBlock(State)) {
                continue;
            }
            if (State.getBlock() instanceof LiquidBlock) {
                World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Scp465Snapshot.QuietFlags);
                continue;
            }
            World.destroyBlock(Pos, true);
        }
    }

    public static void nudge(ServerLevel World, Scp465Party Party) {
        for (LivingEntity Stuck : World.getEntitiesOfClass(LivingEntity.class, Party.bounds().inflate(0.5))) {
            if (World.noCollision(Stuck, Stuck.getBoundingBox().deflate(0.01))) {
                continue;
            }
            Vec3 From = Stuck.position();
            List<BlockPos> Options = new ArrayList<>(Party.floor());
            Options.sort(Comparator.comparingDouble(Cell -> Vec3.atBottomCenterOf(Cell).distanceToSqr(From)));
            for (BlockPos Cell : Options) {
                Vec3 Landing = Vec3.atBottomCenterOf(Cell);
                AABB Footprint = Stuck.getDimensions(Stuck.getPose()).makeBoundingBox(Landing).deflate(0.01);
                if (World.noCollision(Stuck, Footprint)) {
                    Stuck.teleportTo(Landing.x, Landing.y, Landing.z);
                    break;
                }
            }
        }
    }

    public static void vanish(ServerLevel World, Scp465Guest Guest) {
        Vec3 At = Guest.position().add(0.0, 0.9, 0.0);
        Scp465Effects.dust(World, At, 10);
        Scp465Effects.confetti(World, At, 16, 0.3, 0.05);
        Scp465Effects.play(World, At, Scp465Registry.VanishLayers, 0.55f, 1.0f);
        Guest.discard();
    }

    public static void tick(ServerLevel World) {
        Scp465Parties Data = Scp465Parties.peek(World);
        if (Data == null || Data.isEmpty()) {
            return;
        }
        long Now = World.getGameTime();
        for (Scp465Party Party : Data.snapshotList()) {
            if (!World.isLoaded(Party.boxPos())) {
                continue;
            }
            if (!intact(World, Party)) {
                close(World, Party, null);
                continue;
            }
            tickParty(World, Party, Now);
        }
    }

    public static boolean intact(ServerLevel World, Scp465Party Party) {
        BlockState State = World.getBlockState(Party.boxPos());
        return Scp465BoxBlock.isOpen(State) && World.getBlockEntity(Party.boxPos()) instanceof Scp465BoxBlockEntity Carton && Carton.partyId() == Party.id();
    }

    public static List<Scp465Guest> guests(ServerLevel World, Scp465Party Party) {
        List<Scp465Guest> Found = new ArrayList<>();
        for (UUID Id : Party.guests()) {
            if (World.getEntity(Id) instanceof Scp465Guest Guest && Guest.isAlive()) {
                Found.add(Guest);
            }
        }
        return Found;
    }

    public static void tickParty(ServerLevel World, Scp465Party Party, long Now) {
        List<Scp465Guest> Guests = guests(World, Party);
        RandomSource Random = World.getRandom();
        boolean Cheering = Now < Party.musicStart();
        if (Cheering) {
            if (Now % CheerInterval == 0 && !Guests.isEmpty()) {
                Scp465Guest Loud = Guests.get(Random.nextInt(Guests.size()));
                Scp465Effects.play(World, Loud.getEyePosition(), Scp465Registry.CheerLayers, 0.9f, 0.9f + Random.nextFloat() * 0.3f);
                Scp465Effects.confetti(World, celebrantCentre(World, Party).add(0.0, 2.2, 0.0), 10, 0.8, 0.05);
            }
        }
        for (Scp465Guest Guest : Guests) {
            if (Guest.isDj()) {
                if (!"dj".equals(Guest.taskName())) {
                    assignDj(Party, Guest);
                }
                continue;
            }
            if (Cheering) {
                continue;
            }
            if (Now >= Guest.taskUntil() || "cheer".equals(Guest.taskName()) && Now >= Party.musicStart()) {
                choose(World, Party, Guest, Guests, Now, Random);
            }
        }
        if (Now % GuardInterval == 0) {
            guard(World, Party, Guests, Now);
        }
        if (!Cheering && Now % CelebrateInterval == 0 && !Guests.isEmpty()) {
            List<Scp465Guest> Free = Guests.stream().filter(Guest -> !Guest.isDj() && !"guard".equals(Guest.taskName())).toList();
            if (!Free.isEmpty()) {
                Scp465Guest Chosen = Free.get(Random.nextInt(Free.size()));
                celebrate(World, Party, Chosen, spots(Guests, Chosen), Now, Random);
            }
        }
    }

    private static void choose(ServerLevel World, Scp465Party Party, Scp465Guest Guest, List<Scp465Guest> Guests, long Now, RandomSource Random) {
        Guest.holdCup(false);
        int Total = WeightDance + (Party.barFront().isEmpty() ? 0 : WeightSnack) + (Guests.size() > 2 ? WeightChat : 0) + WeightWander + WeightCelebrate;
        int Roll = Random.nextInt(Total);
        if ((Roll -= WeightDance) < 0) {
            dance(Party, Guest, Guests, Now, Random);
            return;
        }
        Set<BlockPos> Taken = spots(Guests, Guest);
        if (!Party.barFront().isEmpty() && (Roll -= WeightSnack) < 0) {
            BlockPos Front = Party.barFront().get(Random.nextInt(Party.barFront().size()));
            BlockPos Cell = Taken.contains(Front) ? nearestFloor(Party, Vec3.atBottomCenterOf(Front), Taken) : Front;
            Vec3 Spot = Vec3.atBottomCenterOf(Cell == null ? Front : Cell);
            BlockPos Food = Party.bar().stream().min(Comparator.comparingDouble(Top -> Top.distSqr(Front))).orElse(Front.above());
            Guest.holdCup(true);
            Guest.assign("snack", Spot, null, Vec3.atCenterOf(Food), Scp465Guest.ActionDrink, Now + 100 + Random.nextInt(60));
            return;
        }
        if (Guests.size() > 2 && (Roll -= WeightChat) < 0) {
            List<Scp465Guest> Others = Guests.stream().filter(Other -> Other != Guest && !Other.isDj()).toList();
            if (!Others.isEmpty()) {
                Scp465Guest Partner = Others.get(Random.nextInt(Others.size()));
                double Angle = Random.nextDouble() * Mth.TWO_PI;
                Vec3 Near = Partner.position().add(Math.cos(Angle) * 1.4, 0.0, Math.sin(Angle) * 1.4);
                Set<BlockPos> Avoid = new HashSet<>(Taken);
                Avoid.add(BlockPos.containing(Partner.getX(), Party.floorY(), Partner.getZ()));
                BlockPos Cell = nearestFloor(Party, Near, Avoid);
                Guest.holdCup(Random.nextBoolean());
                Guest.assign("chat", Cell == null ? Near : Vec3.atBottomCenterOf(Cell), Partner.getUUID(), null, Scp465Guest.ActionChat, Now + 120 + Random.nextInt(120));
                return;
            }
        }
        if ((Roll -= WeightWander) < 0 || !celebrate(World, Party, Guest, Taken, Now, Random)) {
            BlockPos Cell = Party.floor().get(Random.nextInt(Party.floor().size()));
            for (int Try = 1; Try < WanderTries && Taken.contains(Cell); Try++) {
                Cell = Party.floor().get(Random.nextInt(Party.floor().size()));
            }
            Guest.assign("wander", Vec3.atBottomCenterOf(Cell), null, null, Scp465Guest.ActionNone, Now + 90 + Random.nextInt(60));
        }
    }

    private static void dance(Scp465Party Party, Scp465Guest Guest, List<Scp465Guest> Guests, long Now, RandomSource Random) {
        List<BlockPos> Choices = Party.dance().isEmpty() ? Party.floor() : Party.dance();
        Set<BlockPos> Taken = spots(Guests, Guest);
        List<BlockPos> Open = Choices.stream().filter(Cell -> !Taken.contains(Cell)).toList();
        BlockPos Cell = (Open.isEmpty() ? Choices : Open).get(Random.nextInt((Open.isEmpty() ? Choices : Open).size()));
        byte Move = Scp465Guest.DanceActions[Random.nextInt(Scp465Guest.DanceActions.length)];
        Vec3 Facing = Vec3.atBottomCenterOf(Party.booth());
        Guest.assign("dance", Vec3.atBottomCenterOf(Cell), null, Random.nextInt(3) == 0 ? null : Facing.add(0.0, 1.5, 0.0), Move, Now + 160 + Random.nextInt(160));
    }

    private static boolean celebrate(ServerLevel World, Scp465Party Party, Scp465Guest Guest, Set<BlockPos> Taken, long Now, RandomSource Random) {
        List<Entity> Here = new ArrayList<>();
        for (UUID Id : Party.celebrants()) {
            Entity Found = World.getEntity(Id);
            if (Found != null && Found.isAlive() && Party.inArea(Found)) {
                Here.add(Found);
            }
        }
        if (Here.isEmpty()) {
            return false;
        }
        Entity Target = Here.get(Random.nextInt(Here.size()));
        double Angle = Random.nextDouble() * Mth.TWO_PI;
        Vec3 Near = Target.position().add(Math.cos(Angle) * 1.6, 0.0, Math.sin(Angle) * 1.6);
        Set<BlockPos> Avoid = new HashSet<>(Taken);
        Avoid.add(BlockPos.containing(Target.getX(), Party.floorY(), Target.getZ()));
        BlockPos Cell = nearestFloor(Party, Near, Avoid);
        Guest.holdCup(false);
        Guest.assign("celebrate", Cell == null ? Near : Vec3.atBottomCenterOf(Cell), Target.getUUID(), null, Scp465Guest.ActionCheer, Now + 80);
        return true;
    }

    public static List<LivingEntity> invaders(ServerLevel World, Scp465Party Party) {
        return World.getEntitiesOfClass(LivingEntity.class, Party.bounds().inflate(1.0), Target -> personLike(Target) && !Party.isCelebrant(Target) && Party.inArea(Target));
    }

    public static double flatDistance(Vec3 First, Vec3 Second) {
        double Dx = First.x - Second.x;
        double Dz = First.z - Second.z;
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    public static void guard(ServerLevel World, Scp465Party Party, List<Scp465Guest> Guests, long Now) {
        List<LivingEntity> Invaders = invaders(World, Party);
        Vec3 Booth = Party.boothCentre();
        Map<UUID, UUID> Bouncers = Party.bouncers();
        Set<UUID> Watching = new HashSet<>();
        for (LivingEntity Invader : Invaders) {
            if (flatDistance(Invader.position(), Booth) <= Scp465Party.WatchRadius) {
                Watching.add(Invader.getUUID());
            }
        }
        Bouncers.entrySet().removeIf(Entry -> !Watching.contains(Entry.getValue()));
        for (LivingEntity Invader : Invaders) {
            if (!Watching.contains(Invader.getUUID())) {
                continue;
            }
            Vec3 Away = new Vec3(Invader.getX() - Booth.x, 0.0, Invader.getZ() - Booth.z);
            if (Away.lengthSqr() < 1.0e-4) {
                Away = Vec3.atLowerCornerOf(Party.boothFacing().getNormal());
            }
            Away = Away.normalize();
            Scp465Guest Bouncer = null;
            for (Scp465Guest Guest : Guests) {
                if (Invader.getUUID().equals(Bouncers.get(Guest.getUUID()))) {
                    Bouncer = Guest;
                }
            }
            if (Bouncer == null) {
                double Closest = Double.MAX_VALUE;
                for (Scp465Guest Guest : Guests) {
                    if (Guest.isDj() || Bouncers.containsKey(Guest.getUUID())) {
                        continue;
                    }
                    double Distance = Guest.distanceToSqr(Invader);
                    if (Distance < Closest) {
                        Closest = Distance;
                        Bouncer = Guest;
                    }
                }
                if (Bouncer != null) {
                    Bouncers.put(Bouncer.getUUID(), Invader.getUUID());
                }
            }
            if (Bouncer != null) {
                Vec3 Wanted = Booth.add(Away.scale(Scp465Party.GuardRadius - 1.2));
                BlockPos Cell = nearestFloor(Party, Wanted, Set.of());
                Bouncer.holdCup(false);
                Bouncer.assign("guard", Cell == null ? Wanted : Vec3.atBottomCenterOf(Cell), Invader.getUUID(), null, Scp465Guest.ActionGuard, Now + GuardHold);
            }
            if (flatDistance(Invader.position(), Booth) < Scp465Party.GuardRadius) {
                push(Invader, Away);
            }
        }
    }

    public static void push(LivingEntity Invader, Vec3 Away) {
        Vec3 Motion = Invader.getDeltaMovement();
        double Along = Motion.x * Away.x + Motion.z * Away.z;
        double Extra = Math.max(0.0, GuardPush - Along);
        Invader.setDeltaMovement(Motion.add(Away.x * Extra, Invader.onGround() ? GuardLift : 0.0, Away.z * Extra));
        Invader.hasImpulse = true;
        Invader.hurtMarked = true;
    }

    public static boolean serve(ServerLevel World, BlockPos Pos, Player User, Scp465Decor Kind) {
        if (!canUse(User)) {
            return false;
        }
        Scp465Party Party = Scp465Parties.containing(World, Pos);
        if (Party == null) {
            Scp465Effects.hint(User, Messages + "snack_empty");
            return false;
        }
        if (!Party.isCelebrant(User)) {
            Scp465Effects.hint(User, Messages + "snack_refused");
            Scp465Effects.play(World, Vec3.atCenterOf(Pos), Scp465Registry.Refuse, 0.8f, 1.0f);
            for (Scp465Guest Guest : guests(World, Party)) {
                if (!Guest.isDj() && Guest.distanceToSqr(User) < 36.0) {
                    Guest.flash(Scp465Guest.ActionGuard, User.getUUID(), 30);
                    break;
                }
            }
            return false;
        }
        if (Party.served(User.getUUID()) >= Scp465Party.Servings) {
            Scp465Effects.hint(User, Messages + "snack_plenty");
            return false;
        }
        ItemStack Given = switch (Kind) {
            case PunchBowl -> new ItemStack(Scp465Registry.Punch.get());
            case SnackBowl -> new ItemStack(Scp465Registry.Chips.get());
            default -> new ItemStack(Scp465Registry.Cupcake.get());
        };
        Party.serve(User.getUUID());
        Scp465Parties.get(World).setDirty();
        if (User instanceof ServerPlayer Served) {
            StrangerEncounters.give(Served, Given);
        } else if (!User.getInventory().add(Given)) {
            User.drop(Given, false);
        }
        Scp465Effects.play(World, Vec3.atCenterOf(Pos), Scp465Registry.Serve, 0.8f, 1.0f);
        return true;
    }

    public static InteractionResult interact(ServerLevel World, Scp465Guest Guest, Player User) {
        Scp465Party Party = Scp465Parties.find(World, Guest.partyId());
        if (Party == null || !canUse(User)) {
            return InteractionResult.PASS;
        }
        if (Party.isCelebrant(User)) {
            friendly(World, Guest, User);
            return InteractionResult.SUCCESS;
        }
        disintegrate(World, Guest, User);
        return InteractionResult.SUCCESS;
    }

    private static void friendly(ServerLevel World, Scp465Guest Guest, Player User) {
        RandomSource Random = World.getRandom();
        Vec3 Between = Guest.getEyePosition().add(User.getEyePosition()).scale(0.5);
        switch (Random.nextInt(3)) {
            case 0 -> {
                Guest.flash(Scp465Guest.ActionHighFive, User.getUUID(), 14);
                Scp465Effects.play(World, Between, Scp465Registry.Clap, 0.9f, 1.0f);
                Scp465Effects.confetti(World, Between, 8, 0.15, 0.08);
            }
            case 1 -> {
                Guest.flash(Scp465Guest.ActionCheer, User.getUUID(), 30);
                Scp465Effects.play(World, Guest.getEyePosition(), Scp465Registry.CheerLayers, 0.9f, 1.1f);
            }
            default -> {
                Guest.flash(Scp465Guest.ActionWave, User.getUUID(), 20);
                Scp465Effects.play(World, Between, Scp465Registry.Popper, 0.9f, 1.0f);
                Scp465Effects.confetti(World, Between.add(0.0, 0.3, 0.0), 30, 0.25, 0.18);
                Scp465Effects.streamers(World, Between.add(0.0, 0.3, 0.0), 4, 0.2, 0.1);
            }
        }
    }

    public static Holder<DamageType> confettiType(ServerLevel World) {
        return World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(Scp465Registry.ConfettiDamage);
    }

    public static void disintegrate(ServerLevel World, Scp465Guest Guest, Player User) {
        Guest.flash(Scp465Guest.ActionWave, User.getUUID(), 24);
        Vec3 At = User.position().add(0.0, 0.9, 0.0);
        boolean Hit = User.hurt(new DamageSource(confettiType(World), Guest), Float.MAX_VALUE);
        if (Hit) {
            Scp465Effects.play(World, At, Scp465Registry.DisintegrateLayers, 1.0f, 1.0f);
            Scp465Effects.confetti(World, At, 90, 0.35, 0.22);
            Scp465Effects.streamers(World, At, 10, 0.3, 0.12);
            Scp465Effects.dust(World, At, 14);
        } else {
            shrugOff(World, Guest, User);
        }
    }

    public static void shrugOff(ServerLevel World, Scp465Guest Guest, Entity Attacker) {
        Guest.flash(Scp465Guest.ActionLaugh, Attacker.getUUID(), 18);
        if (Guest.isClown()) {
            Scp465Effects.play(World, Guest.getEyePosition(), Scp465Registry.HonkLayers, 0.9f, 1.0f);
        } else {
            Scp465Effects.play(World, Guest.getEyePosition(), Scp465Registry.Laugh, 0.8f, Guest.isDj() ? 0.9f : 1.05f);
        }
        Scp465Effects.confetti(World, Guest.getEyePosition(), 6, 0.2, 0.05);
    }

    public static void useLetter(ServerPlayer User, ItemStack Stack) {
        Stack.shrink(1);
        StrangerEncounters.give(User, new ItemStack(Scp465Registry.Chocolate.get()));
    }
}
