package net.rai.scp_unleashed.z0002;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.z0002.network.Z0002ChannelPayload;
import net.rai.scp_unleashed.z0002.network.Z0002DomainPayload;
import net.rai.scp_unleashed.z0002.network.Z0002WarningPayload;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Z0002Domains {
    public static final int Radius = 20;
    public static final double TakeRange = 32.0;
    public static final int DomainTicks = 700;
    public static final int CrackLead = 40;
    public static final long DayTicks = 24000L;
    public static final double ArrivalRing = 12.0;
    public static final int MinRingSlots = 6;
    public static final int SlashCooldown = 15;
    public static final long RetainTicks = 12000L;
    public static final int GroundSearch = 8;
    public static final int OverlapGap = 2;
    public static final int RecoveryWait = 200;
    public static final int TicketDistance = 2;
    public static final int TitleIn = 10;
    public static final int TitleStay = 70;
    public static final int TitleOut = 25;
    public static final float OpenVolume = 4.0f;
    public static final float ShatterVolume = 4.0f;
    public static final int ShardBursts = 160;
    public static final int CrackBursts = 60;
    public static final long HintGap = 30L;
    public static final int SafeReach = 6;
    public static final int WindupTicks = 50;
    public static final int BuildTicks = 50;
    public static final int CollapseTicks = 50;
    public static final int MinBudget = 256;
    public static final float BudgetSlack = 1.6f;
    public static final int FizzleTicks = 200;
    public static final float HardHit = 4.0f;
    public static final int WarnEvery = 5;
    public static final float WindupVolume = 3.0f;
    public static final int SpiralPoints = 6;
    public static final double SpiralRadius = 3.2;
    public static final float RiseShare = 0.85f;
    public static final float SinkShare = 0.8f;
    public static final int ConstructEvery = 8;
    public static final double InwardPush = 1.2;
    private static final Map<UUID, Channel> Channels = new ConcurrentHashMap<>();
    private static final Map<UUID, Z0002Plan> Plans = new ConcurrentHashMap<>();
    private static final Set<UUID> Ticketed = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> Hinted = new ConcurrentHashMap<>();
    private static boolean Recovering;
    private static int RecoveryTicks;

    public static final class Channel {
        public final UUID Holder;
        public final ResourceKey<Level> Dimension;
        public final long Start;
        public final long End;
        public final InteractionHand Hand;
        public final boolean Forced;
        public final int Duration;
        public final Set<UUID> Warned = new HashSet<>();
        public boolean Broken;

        public Channel(UUID Holder, ResourceKey<Level> Dimension, long Start, long End, InteractionHand Hand, boolean Forced, int Duration) {
            this.Holder = Holder;
            this.Dimension = Dimension;
            this.Start = Start;
            this.End = End;
            this.Hand = Hand;
            this.Forced = Forced;
            this.Duration = Duration;
        }
    }

    public enum Outcome {
        Opened("opened"),
        Channelling("channelling"),
        Busy("busy"),
        Dormant("dormant"),
        Disabled("disabled"),
        Inside("inside"),
        Overlap("overlap"),
        Protected("protected"),
        Space("space");

        private final String Key;

        Outcome(String Key) {
            this.Key = Key;
        }

        public String key() {
            return Key;
        }
    }

    private Z0002Domains() {
    }

    public static long now(MinecraftServer Server) {
        return Server.overworld().getGameTime();
    }

    @Nullable
    public static Z0002Domain byId(MinecraftServer Server, UUID Id) {
        return Z0002World.get(Server).byId(Id);
    }

    @Nullable
    public static Z0002Domain memberOf(MinecraftServer Server, UUID Id) {
        for (Z0002Domain Each : Z0002World.get(Server).domains()) {
            if (Each.active() && Each.Members.containsKey(Id)) {
                return Each;
            }
        }
        return null;
    }

    @Nullable
    public static Z0002Domain ownedBy(MinecraftServer Server, UUID Id) {
        for (Z0002Domain Each : Z0002World.get(Server).domains()) {
            if (Each.active() && Each.Owner.equals(Id)) {
                return Each;
            }
        }
        return null;
    }

    @Nullable
    public static Z0002Domain domainAt(Level World, BlockPos Pos) {
        MinecraftServer Server = World.getServer();
        if (World.isClientSide || Server == null) {
            return null;
        }
        for (Z0002Domain Each : Z0002World.get(Server).domains()) {
            if (Each.active() && Each.Dimension == World.dimension() && Each.Region.contains(Pos)) {
                return Each;
            }
        }
        return null;
    }

    public static boolean bound(Entity Target) {
        MinecraftServer Server = Target.getServer();
        return Target instanceof Player && Server != null && !Target.level().isClientSide && memberOf(Server, Target.getUUID()) != null;
    }

    public static boolean shrineValid(ServerLevel World, Z0002Shrine Shrine) {
        UUID Id = Shrine.domainId();
        Z0002Domain Domain = Id == null ? null : byId(World.getServer(), Id);
        return Domain != null && Domain.active() && Domain.Dimension == World.dimension();
    }

    @Nullable
    public static ServerPlayer player(MinecraftServer Server, UUID Id) {
        ServerPlayer Listed = Server.getPlayerList().getPlayer(Id);
        if (Listed != null) {
            return Listed;
        }
        for (ServerLevel World : Server.getAllLevels()) {
            if (World.getPlayerByUUID(Id) instanceof ServerPlayer Found && !Found.isRemoved()) {
                return Found;
            }
        }
        return null;
    }

    public static long itemUntil(ItemStack Stack) {
        Long Until = Stack.get(Z0002Registry.DormantUntil.get());
        return Until == null ? 0L : Until;
    }

    public static boolean itemDormant(ItemStack Stack, long Now) {
        return itemUntil(Stack) > Now;
    }

    public static boolean dormant(MinecraftServer Server, Player User, ItemStack Stack, long Now) {
        return Z0002World.get(Server).dormantUntil(User.getUUID()) > Now || itemDormant(Stack, Now);
    }

    public static void use(ServerPlayer User, ItemStack Stack, InteractionHand Hand) {
        MinecraftServer Server = User.server;
        Z0002Domain Mine = ownedBy(Server, User.getUUID());
        if (Mine != null) {
            slash(User, Stack, Mine);
            return;
        }
        if (memberOf(Server, User.getUUID()) != null) {
            hint(User, "inside");
            return;
        }
        Outcome Result = begin(User, Hand, false, DomainTicks);
        if (Result != Outcome.Channelling) {
            hint(User, Result.key());
            if (Result == Outcome.Dormant) {
                User.serverLevel().playSound(null, User.getX(), User.getY(), User.getZ(), Z0002Registry.Dormant.get(), SoundSource.PLAYERS, 0.5f, 0.8f);
            }
        }
    }

    @Nullable
    public static Channel channelOf(UUID Holder) {
        return Channels.get(Holder);
    }

    public static Outcome begin(ServerPlayer User, InteractionHand Hand, boolean Forced, int Duration) {
        if (Channels.containsKey(User.getUUID())) {
            return Outcome.Busy;
        }
        ItemStack Stack = User.getItemInHand(Hand);
        Outcome Refused = check(User, Stack, Forced);
        if (Refused != null) {
            return Refused;
        }
        ServerLevel World = User.serverLevel();
        long Now = now(User.server);
        Channel Started = new Channel(User.getUUID(), World.dimension(), Now, Now + WindupTicks, Hand, Forced, Duration);
        Channels.put(User.getUUID(), Started);
        User.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, WindupTicks + 5, 3, false, false));
        play(World, User.position().add(0.0, 1.0, 0.0), Z0002Registry.WindupLayers, WindupVolume, 1.0f);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(User, new Z0002ChannelPayload(User.getId(), WindupTicks, Hand == InteractionHand.MAIN_HAND));
        warn(World, User, Started, Now);
        return Outcome.Channelling;
    }

    public static void hurt(ServerPlayer Victim, float Amount) {
        Channel Found = Channels.get(Victim.getUUID());
        if (Found != null && Amount >= HardHit) {
            Found.Broken = true;
        }
    }

    private static ItemStack finger(ServerPlayer Holder, InteractionHand Hand) {
        ItemStack Held = Holder.getItemInHand(Hand);
        if (Z0002FingerItem.is(Held)) {
            return Held;
        }
        ItemStack Other = Holder.getItemInHand(Hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        return Z0002FingerItem.is(Other) ? Other : ItemStack.EMPTY;
    }

    private static void tickChannels(MinecraftServer Server) {
        if (Channels.isEmpty()) {
            return;
        }
        long Now = now(Server);
        for (Channel Each : new ArrayList<>(Channels.values())) {
            ServerPlayer Holder = player(Server, Each.Holder);
            boolean Gone = Holder == null || !Holder.isAlive() || Holder.hasDisconnected() || Holder.level().dimension() != Each.Dimension;
            if (Gone || Each.Broken || !Each.Forced && finger(Holder, Each.Hand).isEmpty()) {
                fizzle(Server, Each, Gone ? null : Holder);
                continue;
            }
            ServerLevel World = Holder.serverLevel();
            spiral(World, Holder, Each, Now);
            if ((Now - Each.Start) % WarnEvery == 0) {
                warn(World, Holder, Each, Now);
            }
            if (Now < Each.End) {
                continue;
            }
            Channels.remove(Each.Holder);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(Holder, new Z0002ChannelPayload(Holder.getId(), 0, true));
            Outcome Result = open(Holder, finger(Holder, Each.Hand), Each.Forced, Each.Duration);
            if (Result != Outcome.Opened) {
                fizzleEffects(World, Holder);
                hint(Holder, Result.key());
            }
        }
    }

    public static void fizzle(MinecraftServer Server, Channel Each, @Nullable ServerPlayer Holder) {
        Channels.remove(Each.Holder);
        long Now = now(Server);
        Z0002World Data = Z0002World.get(Server);
        Data.setDormant(Each.Holder, Math.max(Data.dormantUntil(Each.Holder), Now + FizzleTicks));
        if (Holder == null) {
            return;
        }
        Holder.getCooldowns().addCooldown(Z0002Registry.Finger.get(), FizzleTicks);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(Holder, new Z0002ChannelPayload(Holder.getId(), 0, true));
        fizzleEffects(Holder.serverLevel(), Holder);
        hint(Holder, "fizzle");
    }

    private static void fizzleEffects(ServerLevel World, ServerPlayer Holder) {
        play(World, Holder.position().add(0.0, 1.2, 0.0), Z0002Registry.FizzleLayers, 1.0f, 1.0f);
        World.sendParticles(Z0002Registry.Mote.get(), Holder.getX(), Holder.getY() + 1.2, Holder.getZ(), 30, 0.6, 0.6, 0.6, 0.08);
    }

    private static void spiral(ServerLevel World, ServerPlayer Holder, Channel Each, long Now) {
        float Progress = Mth.clamp((Now - Each.Start) / (float) WindupTicks, 0.0f, 1.0f);
        double Radius = SpiralRadius * (1.0 - 0.75 * Progress) + 0.4;
        long Age = Now - Each.Start;
        for (int Index = 0; Index < SpiralPoints; Index++) {
            double Angle = Age * 0.45 + Index * Math.PI * 2.0 / SpiralPoints;
            double Height = 0.15 + ((Age + Index * 3) % 20) / 20.0 * 2.2;
            double X = Holder.getX() + Math.cos(Angle) * Radius;
            double Z = Holder.getZ() + Math.sin(Angle) * Radius;
            double InX = Holder.getX() - X;
            double InZ = Holder.getZ() - Z;
            double Length = Math.max(0.001, Math.sqrt(InX * InX + InZ * InZ));
            double SpeedX = InX / Length * 0.12 - Math.sin(Angle) * 0.1;
            double SpeedZ = InZ / Length * 0.12 + Math.cos(Angle) * 0.1;
            World.sendParticles(Z0002Registry.Mote.get(), X, Holder.getY() + Height, Z, 0, SpeedX, 0.02, SpeedZ, 1.0);
        }
    }

    private static void warn(ServerLevel World, ServerPlayer Holder, Channel Each, long Now) {
        int Left = (int) Math.max(1L, Each.End - Now);
        for (ServerPlayer Other : World.players()) {
            if (Other.isSpectator() || !Targeting.isTargetable(Other) || Other.distanceTo(Holder) > TakeRange || !Each.Warned.add(Other.getUUID())) {
                continue;
            }
            Other.connection.send(new ClientboundSetTitlesAnimationPacket(5, Left + BuildTicks + 10, 20));
            Other.connection.send(new ClientboundSetSubtitleTextPacket(Component.empty()));
            Other.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("title.scp_unleashed.z0002.title").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)));
            PacketDistributor.sendToPlayer(Other, new Z0002WarningPayload(Left + 10));
        }
    }

    @Nullable
    private static Outcome check(ServerPlayer User, ItemStack Stack, boolean Forced) {
        ServerLevel World = User.serverLevel();
        MinecraftServer Server = User.server;
        long Now = now(Server);
        if (!Forced) {
            if (!Z0002Registry.enabled(World.getGameRules())) {
                return Outcome.Disabled;
            }
            if (dormant(Server, User, Stack, Now)) {
                return Outcome.Dormant;
            }
        }
        if (memberOf(Server, User.getUUID()) != null || domainAt(World, User.blockPosition()) != null) {
            return Outcome.Inside;
        }
        if (!User.mayBuild()) {
            return Outcome.Protected;
        }
        Z0002Region Region = regionFor(User);
        if (Region.floorY() < World.getMinBuildHeight() || Region.topY() >= World.getMaxBuildHeight()) {
            return Outcome.Space;
        }
        for (Z0002Domain Other : Z0002World.get(Server).domains()) {
            if (Other.Dimension == World.dimension() && Other.Region.overlaps(Region, OverlapGap)
                    && (Other.active() || Z0002Build.showsDomain(World, Other.Region, new ChunkPos(Other.Region.Centre()), later(Z0002World.get(Server), Other)))) {
                return Outcome.Overlap;
            }
        }
        for (int X = Region.minX(); X <= Region.maxX(); X++) {
            for (int Z = Region.minZ(); Z <= Region.maxZ(); Z++) {
                BlockPos Column = new BlockPos(X, Region.floorY(), Z);
                if (Region.contains(Column) && !World.mayInteract(User, Column)) {
                    return Outcome.Protected;
                }
            }
        }
        return null;
    }

    public static Z0002Region regionFor(ServerPlayer User) {
        return new Z0002Region(new BlockPos(User.getBlockX(), floorFor(User.serverLevel(), User.blockPosition()), User.getBlockZ()), Radius);
    }

    public static boolean slash(ServerPlayer User, ItemStack Stack, Z0002Domain Domain) {
        if (Domain.Phase != Z0002Phase.Active || Domain.Cracked || User.level().dimension() != Domain.Dimension || !Domain.Region.holds(User.position(), User.getBbHeight())) {
            return false;
        }
        if (!Stack.isEmpty() && User.getCooldowns().isOnCooldown(Stack.getItem())) {
            return false;
        }
        ServerLevel World = User.serverLevel();
        Z0002Slash.launch(World, User, Domain);
        if (!Stack.isEmpty()) {
            User.getCooldowns().addCooldown(Stack.getItem(), SlashCooldown);
        }
        play(World, User.position().add(0.0, 1.2, 0.0), Z0002Registry.SlashLayers, 1.2f, 0.9f + World.random.nextFloat() * 0.2f);
        return true;
    }

    public static Outcome open(ServerPlayer User, ItemStack Stack, boolean Forced, int Duration) {
        Outcome Refused = check(User, Stack, Forced);
        if (Refused != null) {
            return Refused;
        }
        ServerLevel World = User.serverLevel();
        MinecraftServer Server = User.server;
        Z0002World Data = Z0002World.get(Server);
        long Now = now(Server);
        Z0002Region Region = regionFor(User);
        List<ServerPlayer> Taken = new ArrayList<>();
        Taken.add(User);
        for (ServerPlayer Other : World.players()) {
            if (Other == User || Other.isSpectator() || !Other.isAlive() || !Targeting.isTargetable(Other) || Other.distanceTo(User) > TakeRange || memberOf(Server, Other.getUUID()) != null
                    || Channels.containsKey(Other.getUUID())) {
                continue;
            }
            Taken.add(Other);
        }
        float HolderYaw = Math.round(User.getYRot() / 90.0f) * 90.0f;
        float Facing = Mth.wrapDegrees(HolderYaw + 180.0f);
        Z0002Domain Domain = new Z0002Domain(UUID.randomUUID(), World.dimension(), Region, User.getUUID(), User.getGameProfile().getName(), Now, Now + BuildTicks + Duration,
                Facing, User.position());
        Domain.Phase = Z0002Phase.Building;
        Domain.BuildEnd = Now + BuildTicks;
        Domain.Snapshot = Z0002Build.capture(World, Region);
        Z0002Build.captureEntities(World, Domain);
        List<LivingEntity> Mobs = World.getEntitiesOfClass(LivingEntity.class, Region.box().inflate(1.0),
                Each -> !(Each instanceof Player) && !(Each instanceof ArmorStand) && Each.isAlive() && Z0002Build.within(Region, Each));
        for (LivingEntity Creature : Mobs) {
            Domain.MobReturns.put(Creature.getUUID(), Creature.position());
        }
        int Slots = Math.max(MinRingSlots, Taken.size());
        double HolderAngle = Math.atan2(-Mth.cos(HolderYaw * Mth.DEG_TO_RAD), Mth.sin(HolderYaw * Mth.DEG_TO_RAD));
        Vec3 Middle = Region.floorCentre();
        List<Float> Yaws = new ArrayList<>();
        for (int Index = 0; Index < Taken.size(); Index++) {
            ServerPlayer Member = Taken.get(Index);
            double Angle = HolderAngle + Math.PI * 2.0 * Index / Slots;
            Vec3 Spot = new Vec3(Middle.x + Math.cos(Angle) * ArrivalRing, Middle.y, Middle.z + Math.sin(Angle) * ArrivalRing);
            Yaws.add(Index == 0 ? HolderYaw : (float) (Mth.atan2(Middle.z - Spot.z, Middle.x - Spot.x) * Mth.RAD_TO_DEG) - 90.0f);
            Domain.Members.put(Member.getUUID(), Z0002Spot.of(Member));
            Domain.Arrivals.put(Member.getUUID(), Spot);
        }
        Data.add(Domain);
        Z0002Plan Plan = Z0002Plan.of(Domain);
        Plans.put(Domain.Id, Plan);
        Plan.build(World, Domain, 0.0f, Integer.MAX_VALUE);
        ensureTickets(World, Domain);
        Vec3 ShrineSpot = Z0002Build.shrineSpot(Domain);
        Z0002Shrine Shrine = Z0002Registry.Shrine.get().create(World);
        if (Shrine != null) {
            Shrine.moveTo(ShrineSpot.x, ShrineSpot.y, ShrineSpot.z, Facing, 0.0f);
            Shrine.setYHeadRot(Facing);
            Shrine.bind(Domain.Id);
            Shrine.rise(World.getGameTime(), (int) (BuildTicks * RiseShare));
            World.addFreshEntity(Shrine);
            Domain.ShrineId = Shrine.getUUID();
        }
        for (int Index = 0; Index < Taken.size(); Index++) {
            ServerPlayer Member = Taken.get(Index);
            move(Member, World, Domain.Arrivals.get(Member.getUUID()), Yaws.get(Index), 0.0f);
            enter(Member, Domain);
        }
        play(World, Middle.add(0.0, 2.0, 0.0), Z0002Registry.OpenLayers, OpenVolume, 1.0f);
        play(World, Middle.add(0.0, 2.0, 0.0), Z0002Registry.ConstructLayers, OpenVolume, 0.8f);
        long Until = Domain.End + DayTicks;
        if (!Stack.isEmpty() && Z0002FingerItem.is(Stack)) {
            Stack.set(Z0002Registry.DormantUntil.get(), Until);
        }
        Data.setDormant(User.getUUID(), Math.max(Data.dormantUntil(User.getUUID()), Until));
        Data.touch();
        Z0002World.writeNow(Server);
        return Outcome.Opened;
    }

    public static Z0002Plan plan(Z0002Domain Domain) {
        return Plans.computeIfAbsent(Domain.Id, Id -> Z0002Plan.of(Domain));
    }

    private static int budget(Z0002Plan Plan, int Ticks) {
        return Math.max(MinBudget, (int) Math.ceil(Plan.size() / (double) Ticks * BudgetSlack));
    }

    private static void stepBuilding(MinecraftServer Server, ServerLevel World, Z0002Domain Domain, long Now) {
        Z0002Plan Plan = plan(Domain);
        float Progress = (Now - Domain.Start) / (float) BuildTicks;
        Plan.build(World, Domain, Progress, budget(Plan, BuildTicks));
        if ((Now - Domain.Start) % ConstructEvery == 0 && !Plan.built()) {
            Vec3 Middle = Domain.Region.floorCentre();
            play(World, Middle.add(0.0, 1.0 + Domain.Region.Radius() * Math.min(1.0f, Progress), 0.0), List.of(Z0002Registry.Construct), OpenVolume, 0.6f + 0.4f * Math.min(1.0f, Progress));
        }
        guard(Server, World, Domain);
        if (Plan.built() && Now >= Domain.BuildEnd) {
            complete(Server, World, Domain);
        }
    }

    public static void finishBuilding(MinecraftServer Server, Z0002Domain Domain) {
        ServerLevel World = Server.getLevel(Domain.Dimension);
        if (World == null || Domain.Phase != Z0002Phase.Building) {
            return;
        }
        plan(Domain).build(World, Domain, Float.MAX_VALUE, Integer.MAX_VALUE);
        if (Domain.ShrineId != null && World.getEntity(Domain.ShrineId) instanceof Z0002Shrine Shrine) {
            Shrine.rise(World.getGameTime(), 0);
        }
        complete(Server, World, Domain);
    }

    private static void complete(MinecraftServer Server, ServerLevel World, Z0002Domain Domain) {
        Domain.Phase = Z0002Phase.Active;
        List<LivingEntity> Mobs = new ArrayList<>();
        for (UUID Id : Domain.MobReturns.keySet()) {
            if (World.getEntity(Id) instanceof LivingEntity Creature && Creature.isAlive()) {
                Mobs.add(Creature);
            }
        }
        settleMobs(World, Domain, Mobs);
        for (UUID Id : Domain.Members.keySet()) {
            ServerPlayer Member = player(Server, Id);
            if (Member != null && Member.isAlive()) {
                title(Member);
            }
        }
        play(World, Domain.Region.floorCentre().add(0.0, 2.0, 0.0), List.of(Z0002Registry.OpenToll), OpenVolume, 0.8f);
        Z0002World.get(Server).touch();
    }

    public static int floorFor(ServerLevel World, BlockPos Feet) {
        for (int Depth = 1; Depth <= GroundSearch; Depth++) {
            BlockPos Below = Feet.below(Depth);
            if (!World.getBlockState(Below).getCollisionShape(World, Below).isEmpty()) {
                return Below.getY();
            }
        }
        return Feet.getY() - 1;
    }

    private static void settleMobs(ServerLevel World, Z0002Domain Domain, List<LivingEntity> Mobs) {
        Z0002Region Region = Domain.Region;
        Vec3 Middle = Region.floorCentre();
        for (LivingEntity Creature : Mobs) {
            if (Creature.isRemoved()) {
                continue;
            }
            Vec3 Floor = new Vec3(Creature.getX(), Middle.y, Creature.getZ());
            AABB Box = Creature.getDimensions(Creature.getPose()).makeBoundingBox(Floor);
            if (!Region.holds(Floor, Creature.getBbHeight()) || !World.noCollision(Creature, Box)) {
                double Angle = Math.atan2(Creature.getZ() - Middle.z, Creature.getX() - Middle.x);
                Floor = new Vec3(Middle.x + Math.cos(Angle) * (ArrivalRing - 2.0), Middle.y, Middle.z + Math.sin(Angle) * (ArrivalRing - 2.0));
            }
            Creature.teleportTo(Floor.x, Floor.y, Floor.z);
            Creature.setDeltaMovement(Vec3.ZERO);
            Creature.resetFallDistance();
        }
    }

    public static void move(ServerPlayer Member, ServerLevel World, Vec3 Spot, float Yaw, float Pitch) {
        Member.stopRiding();
        if (Member.isFallFlying()) {
            Member.stopFallFlying();
        }
        Member.teleportTo(World, Spot.x, Spot.y, Spot.z, Yaw, Pitch);
        Member.setDeltaMovement(Vec3.ZERO);
        Member.resetFallDistance();
    }

    private static void title(ServerPlayer Member) {
        Member.connection.send(new ClientboundSetTitlesAnimationPacket(0, TitleStay, TitleOut));
        Member.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("title.scp_unleashed.z0002.subtitle").withStyle(ChatFormatting.RED)));
        Member.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("title.scp_unleashed.z0002.title").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)));
    }

    public static void enter(ServerPlayer Member, Z0002Domain Domain) {
        BlockPos Centre = Domain.Region.Centre();
        PacketDistributor.sendToPlayer(Member, new Z0002DomainPayload(true, Centre.getX() + 0.5, Centre.getY() + 0.5, Centre.getZ() + 0.5, Domain.Region.Radius()));
    }

    public static void leave(ServerPlayer Member) {
        PacketDistributor.sendToPlayer(Member, new Z0002DomainPayload(false, 0.0, 0.0, 0.0, 0));
    }

    public static void play(ServerLevel World, Vec3 Where, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Where.x, Where.y, Where.z, Layer.get(), SoundSource.HOSTILE, Volume, Pitch);
        }
    }

    public static void hint(ServerPlayer User, String Key) {
        long Now = User.serverLevel().getGameTime();
        Long Last = Hinted.get(User.getUUID());
        if (Last != null && Now >= Last && Now - Last < HintGap) {
            return;
        }
        Hinted.put(User.getUUID(), Now);
        User.displayClientMessage(Component.translatable("message.scp_unleashed.z0002." + Key).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), true);
    }

    public static void ensureTickets(ServerLevel World, Z0002Domain Domain) {
        if (!Ticketed.add(Domain.Id)) {
            return;
        }
        for (ChunkPos Chunk : Domain.Region.chunks()) {
            World.getChunkSource().addRegionTicket(Z0002Registry.Ticket, Chunk, TicketDistance, Domain.Id, true);
        }
    }

    public static void releaseTickets(ServerLevel World, Z0002Domain Domain) {
        if (!Ticketed.remove(Domain.Id)) {
            return;
        }
        for (ChunkPos Chunk : Domain.Region.chunks()) {
            World.getChunkSource().removeRegionTicket(Z0002Registry.Ticket, Chunk, TicketDistance, Domain.Id, true);
        }
    }

    public static void tick(MinecraftServer Server) {
        tickChannels(Server);
        Z0002World Data = Z0002World.get(Server);
        if (Data.domains().isEmpty()) {
            Recovering = false;
            return;
        }
        long Now = now(Server);
        if (Recovering) {
            recover(Server, Data);
            return;
        }
        for (Z0002Domain Domain : new ArrayList<>(Data.domains())) {
            if (Domain.Closed) {
                if (Now - Domain.ClosedAt > RetainTicks || Now < Domain.ClosedAt) {
                    Data.remove(Domain);
                    Plans.remove(Domain.Id);
                }
                continue;
            }
            ServerLevel World = Server.getLevel(Domain.Dimension);
            if (World == null) {
                collapse(Server, Domain, false);
                continue;
            }
            ensureTickets(World, Domain);
            if (Domain.Phase == Z0002Phase.Building) {
                stepBuilding(Server, World, Domain, Now);
                if (Now >= Domain.End) {
                    beginCollapse(Server, Domain);
                }
            } else if (Domain.Phase == Z0002Phase.Active) {
                if (!Domain.Cracked && Now >= Domain.End - CrackLead) {
                    crack(World, Domain);
                }
                if (Now >= Domain.End) {
                    beginCollapse(Server, Domain);
                }
            } else {
                stepCollapse(Server, World, Domain, Now);
            }
            if (!Domain.Closed) {
                leash(Server, World, Domain);
            }
        }
    }

    public static void beginCollapse(MinecraftServer Server, Z0002Domain Domain) {
        if (Domain.Closed || Domain.Phase == Z0002Phase.Collapsing) {
            return;
        }
        ServerLevel World = Server.getLevel(Domain.Dimension);
        long Now = now(Server);
        Domain.Phase = Z0002Phase.Collapsing;
        Domain.CollapseStart = Now;
        Domain.End = Math.min(Domain.End, Now);
        Domain.Cracked = true;
        Z0002World.get(Server).touch();
        if (World == null) {
            return;
        }
        Vec3 Middle = Domain.Region.floorCentre();
        play(World, Middle.add(0.0, 4.0, 0.0), Z0002Registry.ShatterLayers, ShatterVolume, 1.0f);
        inward(World, Domain, ShardBursts, 1.0f);
        for (Entity Leftover : World.getEntities((Entity) null, Domain.Region.box().inflate(4.0), Each -> Each instanceof Z0002Slash)) {
            Leftover.discard();
        }
        if (Domain.ShrineId != null && World.getEntity(Domain.ShrineId) instanceof Z0002Shrine Shrine) {
            Shrine.sink(World.getGameTime(), (int) (CollapseTicks * SinkShare));
        }
        for (UUID Id : Domain.Members.keySet()) {
            ServerPlayer Member = player(Server, Id);
            if (Member != null && !Member.hasDisconnected()) {
                leave(Member);
            }
        }
        plan(Domain).Restored = 0;
    }

    private static void stepCollapse(MinecraftServer Server, ServerLevel World, Z0002Domain Domain, long Now) {
        float Progress = (Now - Domain.CollapseStart) / (float) CollapseTicks;
        if (Progress >= 1.0f) {
            collapse(Server, Domain, true);
            return;
        }
        Z0002Plan Plan = plan(Domain);
        Plan.restore(World, Domain, Math.min(Progress, 0.999f), budget(Plan, CollapseTicks));
        guard(Server, World, Domain);
        float Standing = 1.0f - Progress / Z0002Plan.ShellFall;
        if ((Now - Domain.CollapseStart) % 5 == 0 && Standing > 0.05f) {
            inward(World, Domain, CrackBursts / 3, Standing);
        }
    }

    private static void guard(MinecraftServer Server, ServerLevel World, Z0002Domain Domain) {
        for (LivingEntity Creature : World.getEntitiesOfClass(LivingEntity.class, Domain.Region.box().inflate(1.0), Each -> Each.isAlive() && !Each.isSpectator())) {
            Creature.resetFallDistance();
            if (World.noCollision(Creature, Creature.getBoundingBox().deflate(0.01))) {
                continue;
            }
            Vec3 Pad = Creature instanceof ServerPlayer ? Domain.Arrivals.get(Creature.getUUID()) : null;
            if (Creature instanceof ServerPlayer Member && Pad != null) {
                move(Member, World, Pad, Member.getYRot(), Member.getXRot());
            } else if (!(Creature instanceof Player)) {
                Vec3 Free = safeSpot(World, Creature, Creature.position());
                Creature.teleportTo(Free.x, Free.y, Free.z);
                Creature.setDeltaMovement(Vec3.ZERO);
            }
        }
    }

    private static void inward(ServerLevel World, Z0002Domain Domain, int Bursts, float Height) {
        RandomSource Random = World.random;
        Z0002Region Region = Domain.Region;
        double CentreX = Region.Centre().getX() + 0.5;
        double CentreY = Region.Centre().getY() + 0.5;
        double CentreZ = Region.Centre().getZ() + 0.5;
        double Reach = Region.Radius() - 0.6;
        for (int Index = 0; Index < Bursts; Index++) {
            double Yaw = Random.nextDouble() * Math.PI * 2.0;
            double Lift = Math.asin(Mth.clamp(Random.nextDouble() * Height, 0.0, 1.0));
            double X = CentreX + Math.cos(Yaw) * Math.cos(Lift) * Reach;
            double Y = CentreY + Math.sin(Lift) * Reach;
            double Z = CentreZ + Math.sin(Yaw) * Math.cos(Lift) * Reach;
            Vec3 Towards = new Vec3(CentreX - X, CentreY + 2.0 - Y, CentreZ - Z).normalize().scale(InwardPush);
            World.sendParticles(Z0002Registry.Shard.get(), X, Y, Z, 0, Towards.x, Towards.y, Towards.z, 1.0);
        }
    }

    public static void beginRecovery() {
        Recovering = true;
        RecoveryTicks = 0;
        Ticketed.clear();
    }

    public static boolean recovering() {
        return Recovering;
    }

    private static void recover(MinecraftServer Server, Z0002World Data) {
        RecoveryTicks++;
        boolean Ready = true;
        for (Z0002Domain Domain : Data.domains()) {
            ServerLevel World = Server.getLevel(Domain.Dimension);
            if (World == null) {
                continue;
            }
            ensureTickets(World, Domain);
            for (ChunkPos Chunk : Domain.Region.chunks()) {
                World.getChunk(Chunk.x, Chunk.z);
                if (!World.areEntitiesLoaded(Chunk.toLong())) {
                    Ready = false;
                }
            }
        }
        if (!Ready && RecoveryTicks < RecoveryWait) {
            return;
        }
        List<Z0002Domain> Ordered = new ArrayList<>(Data.domains());
        Ordered.sort((First, Second) -> Long.compare(First.Start, Second.Start));
        for (Z0002Domain Domain : Ordered) {
            ServerLevel World = Server.getLevel(Domain.Dimension);
            if (Domain.active()) {
                collapse(Server, Domain, true);
                continue;
            }
            if (World != null) {
                List<ChunkPos> Marked = new ArrayList<>();
                List<Z0002Region> Claimed = later(Data, Domain);
                for (ChunkPos Chunk : Domain.Region.chunks()) {
                    if (Z0002Build.showsDomain(World, Domain.Region, Chunk, Claimed)) {
                        Marked.add(Chunk);
                    }
                }
                if (!Marked.isEmpty()) {
                    Set<Long> Keys = new HashSet<>();
                    for (ChunkPos Chunk : Marked) {
                        Keys.add(Chunk.toLong());
                    }
                    Z0002Build.restore(World, Domain, Chunk -> Keys.contains(Chunk.toLong()));
                    Z0002Build.restoreEntities(World, Domain.Snapshot);
                }
                releaseTickets(World, Domain);
            }
        }
        Recovering = false;
        Data.touch();
        Z0002World.writeNow(Server);
    }

    private static List<Z0002Region> later(Z0002World Data, Z0002Domain Domain) {
        List<Z0002Region> Found = new ArrayList<>();
        for (Z0002Domain Other : Data.domains()) {
            if (Other != Domain && Other.Dimension == Domain.Dimension && (Other.active() || Other.Start > Domain.Start) && Other.Region.overlaps(Domain.Region, 0)) {
                Found.add(Other.Region);
            }
        }
        return Found;
    }

    private static void crack(ServerLevel World, Z0002Domain Domain) {
        Domain.Cracked = true;
        Z0002World.get(World.getServer()).touch();
        Vec3 Middle = Domain.Region.floorCentre();
        play(World, Middle.add(0.0, 4.0, 0.0), Z0002Registry.CrackLayers, ShatterVolume, 0.9f);
        shards(World, Domain, CrackBursts, 1, 0.02);
    }

    private static void shards(ServerLevel World, Z0002Domain Domain, int Bursts, int Count, double Speed) {
        RandomSource Random = World.random;
        Z0002Region Region = Domain.Region;
        for (int Index = 0; Index < Bursts; Index++) {
            double Yaw = Random.nextDouble() * Math.PI * 2.0;
            double Lift = Math.asin(Random.nextDouble());
            double Reach = Region.Radius() - 0.6;
            double X = Region.Centre().getX() + 0.5 + Math.cos(Yaw) * Math.cos(Lift) * Reach;
            double Y = Region.Centre().getY() + 0.5 + Math.sin(Lift) * Reach;
            double Z = Region.Centre().getZ() + 0.5 + Math.sin(Yaw) * Math.cos(Lift) * Reach;
            World.sendParticles(Z0002Registry.Shard.get(), X, Y, Z, Count, 0.4, 0.4, 0.4, Speed);
        }
    }

    private static void leash(MinecraftServer Server, ServerLevel World, Z0002Domain Domain) {
        for (UUID Id : new ArrayList<>(Domain.Members.keySet())) {
            ServerPlayer Member = player(Server, Id);
            if (Member == null || !Member.isAlive()) {
                continue;
            }
            if (Member.level().dimension() == Domain.Dimension && Domain.Region.holds(Member.position(), Member.getBbHeight())) {
                continue;
            }
            Vec3 Back = Domain.Arrivals.getOrDefault(Id, Domain.Region.floorCentre().add(ArrivalRing, 0.0, 0.0));
            Vec3 Middle = Domain.Region.floorCentre();
            float Yaw = (float) (Mth.atan2(Middle.z - Back.z, Middle.x - Back.x) * Mth.RAD_TO_DEG) - 90.0f;
            move(Member, World, Back, Yaw, 0.0f);
            hint(Member, "escape");
        }
    }

    public static void endSoon(MinecraftServer Server, Z0002Domain Domain) {
        long Now = now(Server);
        if (Domain.End > Now) {
            Domain.End = Now;
            Domain.Cracked = true;
            Z0002World.get(Server).touch();
        }
    }

    public static void collapse(MinecraftServer Server, Z0002Domain Domain, boolean Quiet) {
        if (Domain.Closed) {
            return;
        }
        Z0002World Data = Z0002World.get(Server);
        long Now = now(Server);
        ServerLevel World = Server.getLevel(Domain.Dimension);
        if (World != null) {
            Vec3 Middle = Domain.Region.floorCentre();
            if (!Quiet) {
                play(World, Middle.add(0.0, 4.0, 0.0), Z0002Registry.ShatterLayers, ShatterVolume, 1.0f);
                shards(World, Domain, ShardBursts, 3, 0.08);
            }
            for (Entity Leftover : World.getEntities((Entity) null, Domain.Region.box().inflate(4.0), Each -> Each instanceof Z0002Shrine || Each instanceof Z0002Slash)) {
                Leftover.discard();
            }
            if (Domain.ShrineId != null && World.getEntity(Domain.ShrineId) instanceof Z0002Shrine Shrine) {
                Shrine.discard();
            }
            Z0002Plan Plan = Plans.get(Domain.Id);
            if (Plan != null && Domain.Phase == Z0002Phase.Collapsing) {
                Plan.restore(World, Domain, Float.MAX_VALUE, Integer.MAX_VALUE);
            } else {
                Z0002Build.restore(World, Domain, null);
            }
            Z0002Build.restoreEntities(World, Domain.Snapshot);
            settleAfter(World, Domain);
            releaseTickets(World, Domain);
        }
        Domain.Closed = true;
        Domain.ClosedAt = Now;
        Plans.remove(Domain.Id);
        for (Map.Entry<UUID, Z0002Spot> Entry : Domain.Members.entrySet()) {
            ServerPlayer Member = player(Server, Entry.getKey());
            if (Member == null || Member.hasDisconnected()) {
                Data.setPendingReturn(Entry.getKey(), Entry.getValue());
                continue;
            }
            if (!Member.isAlive()) {
                continue;
            }
            returnPlayer(Server, Member, Entry.getValue());
        }
        long Until = Now + DayTicks;
        Data.setDormant(Domain.Owner, Until);
        ServerPlayer Holder = player(Server, Domain.Owner);
        if (Holder != null) {
            stamp(Holder, Now, Until);
        }
        Data.touch();
        Z0002World.writeNow(Server);
    }

    public static void stamp(ServerPlayer Holder, long Now, long Until) {
        Inventory Pack = Holder.getInventory();
        for (int Slot = 0; Slot < Pack.getContainerSize(); Slot++) {
            ItemStack Stack = Pack.getItem(Slot);
            if (Z0002FingerItem.is(Stack) && itemUntil(Stack) > Now) {
                Stack.set(Z0002Registry.DormantUntil.get(), Until);
            }
        }
    }

    private static void settleAfter(ServerLevel World, Z0002Domain Domain) {
        Set<UUID> Restored = new HashSet<>();
        for (CompoundTag Data : Domain.Snapshot.captured()) {
            if (Data.hasUUID("UUID")) {
                Restored.add(Data.getUUID("UUID"));
            }
        }
        for (Map.Entry<UUID, Vec3> Entry : Domain.MobReturns.entrySet()) {
            Entity Found = World.getEntity(Entry.getKey());
            if (Found instanceof LivingEntity Living && Living.isAlive()) {
                Vec3 Back = Entry.getValue();
                Living.teleportTo(Back.x, Back.y, Back.z);
                Living.setDeltaMovement(Vec3.ZERO);
                Living.resetFallDistance();
            }
        }
        Vec3 Anchor = Domain.Anchor;
        for (Entity Each : World.getEntities((Entity) null, Domain.Region.box().inflate(1.0), Each -> !(Each instanceof Player))) {
            if (Restored.contains(Each.getUUID()) || Domain.MobReturns.containsKey(Each.getUUID()) || !Z0002Build.within(Domain.Region, Each)) {
                continue;
            }
            if (!World.noCollision(Each, Each.getBoundingBox().deflate(0.01))) {
                Each.teleportTo(Anchor.x, Anchor.y, Anchor.z);
                Each.setDeltaMovement(Vec3.ZERO);
            }
        }
        for (Player Each : World.getEntitiesOfClass(Player.class, Domain.Region.box().inflate(1.0), Each -> !Domain.Members.containsKey(Each.getUUID()))) {
            if (Each instanceof ServerPlayer Other && !World.noCollision(Each, Each.getBoundingBox().deflate(0.01))) {
                move(Other, World, Anchor, Other.getYRot(), Other.getXRot());
            }
        }
    }

    public static void returnPlayer(MinecraftServer Server, ServerPlayer Member, Z0002Spot Spot) {
        ServerLevel Target = Server.getLevel(Spot.Dimension());
        if (Target == null) {
            Target = Server.overworld();
        }
        Vec3 Where = Spot.Pos();
        AABB Box = Member.getDimensions(Member.getPose()).makeBoundingBox(Where);
        Target.getChunkAt(BlockPos.containing(Where));
        if (!Target.noCollision(Member, Box.deflate(0.01))) {
            Where = safeSpot(Target, Member, Where);
        }
        move(Member, Target, Where, Spot.Yaw(), Spot.Pitch());
        leave(Member);
        Target.playSound(null, Where.x, Where.y, Where.z, Z0002Registry.Return.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
    }

    public static Vec3 safeSpot(ServerLevel World, Entity Who, Vec3 Near) {
        BlockPos Centre = BlockPos.containing(Near);
        for (int Reach = 0; Reach <= SafeReach; Reach++) {
            for (int Dy = 0; Dy <= SafeReach; Dy++) {
                for (int Dx = -Reach; Dx <= Reach; Dx++) {
                    for (int Dz = -Reach; Dz <= Reach; Dz++) {
                        if (Math.max(Math.abs(Dx), Math.abs(Dz)) != Reach) {
                            continue;
                        }
                        Vec3 Spot = Vec3.atBottomCenterOf(Centre.offset(Dx, Dy, Dz));
                        if (World.noCollision(Who, Who.getDimensions(Who.getPose()).makeBoundingBox(Spot).deflate(0.01))) {
                            return Spot;
                        }
                    }
                }
            }
        }
        return Vec3.atBottomCenterOf(World.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Centre));
    }

    public static void loggedIn(ServerPlayer User) {
        MinecraftServer Server = User.server;
        Z0002World Data = Z0002World.get(Server);
        Z0002Spot Pending = Data.pendingReturn(User.getUUID());
        if (Pending != null) {
            Data.clearPendingReturn(User.getUUID());
            returnPlayer(Server, User, Pending);
            return;
        }
        Z0002Domain In = memberOf(Server, User.getUUID());
        if (In != null) {
            if (In.Phase != Z0002Phase.Collapsing) {
                enter(User, In);
            }
            return;
        }
        Z0002Domain Around = domainAt(User.serverLevel(), User.blockPosition());
        if (Around != null) {
            Vec3 Middle = Around.Region.floorCentre();
            Vec3 Out = new Vec3(User.getX() - Middle.x, 0.0, User.getZ() - Middle.z);
            if (Out.lengthSqr() < 1.0E-4) {
                Out = new Vec3(1.0, 0.0, 0.0);
            }
            Out = Out.normalize().scale(Around.Region.Radius() + 2.0);
            BlockPos Column = BlockPos.containing(Middle.x + Out.x, Middle.y, Middle.z + Out.z);
            BlockPos Top = User.serverLevel().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Column);
            move(User, User.serverLevel(), Vec3.atBottomCenterOf(Top), User.getYRot(), User.getXRot());
        }
    }

    public static void loggedOut(ServerPlayer User) {
        Channel Winding = Channels.get(User.getUUID());
        if (Winding != null) {
            fizzle(User.server, Winding, null);
        }
        Z0002Domain Mine = ownedBy(User.server, User.getUUID());
        if (Mine != null) {
            endSoon(User.server, Mine);
        }
        Hinted.remove(User.getUUID());
    }

    public static void died(ServerPlayer User) {
        MinecraftServer Server = User.server;
        Channel Winding = Channels.get(User.getUUID());
        if (Winding != null) {
            fizzle(Server, Winding, null);
        }
        Z0002Domain In = memberOf(Server, User.getUUID());
        if (In == null) {
            return;
        }
        In.Members.remove(User.getUUID());
        In.Arrivals.remove(User.getUUID());
        if (In.Owner.equals(User.getUUID())) {
            endSoon(Server, In);
        }
        Z0002World.get(Server).touch();
        leave(User);
    }

    public static void stopped() {
        Channels.clear();
        Plans.clear();
        Ticketed.clear();
        Hinted.clear();
        Recovering = false;
    }
}
