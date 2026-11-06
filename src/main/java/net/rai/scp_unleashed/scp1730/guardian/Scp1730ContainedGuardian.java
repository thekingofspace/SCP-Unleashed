package net.rai.scp_unleashed.scp1730.guardian;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.notice.Notice;
import net.rai.scp_unleashed.notice.Notices;
import net.rai.scp_unleashed.scp1730.Scp1730Anchors;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.block.RealityFolderBlockEntity;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Guardian;
import net.rai.scp_unleashed.scp1730.network.Scp1730ShakePayload;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class Scp1730ContainedGuardian extends Scp1730Guardian {
    public static final String BoundKey = "BoundFolder";
    public static final String FreeKey = "Free";
    public static final double Margin = 1.0;
    public static final double Search = Scp1730Anchors.FolderRadius + 4.0;
    public static final int RetargetEvery = 10;
    public static final int RebindEvery = 5;
    public static final double NoticeRange = 96.0;
    public static final double ShakeRange = 32.0;
    public static final double BreakoutMin = 8.0;
    public static final double BreakoutMax = 24.0;
    public static final int BreakoutTries = 48;
    public static final int FloorAbove = 6;
    public static final int FloorBelow = 10;
    public static final double FreeLeash = 256.0;
    public static final double WanderMin = 6.0;
    public static final double WanderMax = 16.0;
    public static final int WanderTries = 12;
    public static final int WanderPause = 120;
    public static final int WanderSpread = 120;
    public static final double PathStep = 1.5;
    public static final SoundEvent BreachAlarm = SoundEvent.createFixedRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "speaker.facility.breach_alarm"), 64.0f);
    private static final Logger Log = LogUtils.getLogger();
    @Nullable
    private BlockPos Bound;
    private boolean Free;
    private int Outside;
    private int WanderCooldown = WanderPause;

    public Scp1730ContainedGuardian(EntityType<? extends Scp1730ContainedGuardian> Type, Level World) {
        super(Type, World);
    }

    @Nullable
    public BlockPos bound() {
        return Bound;
    }

    public boolean held() {
        return Bound != null && Outside == 0 && !Free;
    }

    public boolean free() {
        return Free;
    }

    @Override
    protected boolean showsBar() {
        return false;
    }

    @Override
    public boolean senseAnchor() {
        return Scp1730Anchors.thrown(this);
    }

    @Override
    protected double leash() {
        if (Free) {
            return FreeLeash;
        }
        return Bound != null ? Math.max(1.0, Scp1730Anchors.FolderRadius - Width * 0.5 - Margin) : Leash;
    }

    public void restore() {
        setHealth(getMaxHealth());
    }

    @Override
    protected void fell(ServerLevel Server) {
        Log.info("The recovered SCP-1730-2 was destroyed at {} in {}", blockPosition().toShortString(), Server.dimension().location());
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    public boolean prey(LivingEntity Each) {
        if (Each == this || !Each.isAlive() || Each instanceof Scp1730Guardian || Each instanceof ArmorStand || !Each.attackable()) {
            return false;
        }
        if (Each instanceof Player User && (User.isSpectator() || User.isCreative())) {
            return false;
        }
        return hasLineOfSight(Each);
    }

    @Override
    protected boolean swipeable(LivingEntity Each) {
        return prey(Each);
    }

    @Override
    protected boolean pushHurts(LivingEntity Each) {
        return prey(Each);
    }

    @Nullable
    @Override
    protected LivingEntity pickTarget(ServerLevel World) {
        LivingEntity Current = getTarget();
        if (tickCount % RetargetEvery != 0 && Current != null && Current.isAlive() && !Current.isRemoved() && Current.level() == World
                && Current.distanceToSqr(this) < ArenaReach * ArenaReach && withinReach(Current)) {
            return Current;
        }
        LivingEntity Best = null;
        double BestDistance = ArenaReach * ArenaReach;
        for (LivingEntity Each : World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(ArenaReach), this::prey)) {
            double Distance = Each.distanceToSqr(this);
            if (Distance < BestDistance && withinReach(Each)) {
                Best = Each;
                BestDistance = Distance;
            }
        }
        return Best;
    }

    private boolean withinReach(LivingEntity Each) {
        if (Bound == null || Free) {
            return true;
        }
        double Reach = leash() + PushReach + Width;
        double Dx = Each.getX() - (Bound.getX() + 0.5);
        double Dz = Each.getZ() - (Bound.getZ() + 0.5);
        return Dx * Dx + Dz * Dz <= Reach * Reach;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (Bound == null || Free) {
            return;
        }
        double Dx = Bound.getX() + 0.5 - getX();
        double Dz = Bound.getZ() + 0.5 - getZ();
        double Flat = Math.sqrt(Dx * Dx + Dz * Dz);
        double Limit = leash();
        if (Flat > Limit && Flat > 1.0e-3) {
            double Step = Math.min(Glide * 2.0, Flat - Limit);
            move(MoverType.SELF, new Vec3(Dx / Flat * Step, 0.0, Dz / Flat * Step));
            setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || isRemoved() || isDeadOrDying()) {
            return;
        }
        ServerLevel World = (ServerLevel) level();
        if (Free) {
            roam(World);
            return;
        }
        if (holding(World)) {
            Outside = 0;
            return;
        }
        Outside++;
        if (Outside >= Scp1730GuardianConfig.escapeTicks()) {
            escape(World);
        }
    }

    private void roam(ServerLevel World) {
        if ((tickCount + getId()) % RebindEvery == 0 && Scp1730Anchors.folded(World, getBoundingBox())) {
            BlockPos Found = findFolder(World);
            if (Found != null) {
                recontain(World, Found);
                return;
            }
        }
        if (getTarget() != null || --WanderCooldown > 0) {
            return;
        }
        WanderCooldown = WanderPause + random.nextInt(WanderSpread);
        Vec3 Spot = wanderSpot(World);
        if (Spot != null) {
            setHome(BlockPos.containing(Spot));
        }
    }

    private boolean holding(ServerLevel World) {
        if (Bound != null) {
            if (Bound.distToCenterSqr(position()) <= Search * Search && !World.isLoaded(Bound)) {
                return true;
            }
            if (covers(World, Bound)) {
                return true;
            }
        }
        BlockPos Found = findFolder(World);
        if (Found == null) {
            return false;
        }
        bind(Found);
        return true;
    }

    private boolean covers(ServerLevel World, BlockPos Master) {
        return World.isLoaded(Master) && World.getBlockEntity(Master) instanceof RealityFolderBlockEntity Folder && Folder.fieldUp()
                && Scp1730Anchors.gap(RealityFolderBlockEntity.fieldCentre(Master), getBoundingBox()) <= Scp1730Anchors.FolderRadius;
    }

    @Nullable
    private BlockPos findFolder(ServerLevel World) {
        AABB Box = getBoundingBox();
        int MinX = SectionPos.blockToSectionCoord(Box.minX - Search);
        int MaxX = SectionPos.blockToSectionCoord(Box.maxX + Search);
        int MinZ = SectionPos.blockToSectionCoord(Box.minZ - Search);
        int MaxZ = SectionPos.blockToSectionCoord(Box.maxZ + Search);
        BlockPos Best = null;
        double BestGap = Double.MAX_VALUE;
        for (int ChunkX = MinX; ChunkX <= MaxX; ChunkX++) {
            for (int ChunkZ = MinZ; ChunkZ <= MaxZ; ChunkZ++) {
                LevelChunk Chunk = World.getChunkSource().getChunkNow(ChunkX, ChunkZ);
                if (Chunk == null) {
                    continue;
                }
                for (BlockEntity Each : Chunk.getBlockEntities().values()) {
                    if (!(Each instanceof RealityFolderBlockEntity Folder) || Folder.isRemoved() || !Folder.fieldUp()) {
                        continue;
                    }
                    double Gap = Scp1730Anchors.gap(RealityFolderBlockEntity.fieldCentre(Folder.getBlockPos()), Box);
                    if (Gap <= Scp1730Anchors.FolderRadius && Gap < BestGap) {
                        Best = Folder.getBlockPos().immutable();
                        BestGap = Gap;
                    }
                }
            }
        }
        return Best;
    }

    private void bind(BlockPos Master) {
        Bound = Master.immutable();
        Free = false;
        Outside = 0;
        setHome(Master);
    }

    private void recontain(ServerLevel World, BlockPos Master) {
        bind(Master);
        Vec3 Centre = position().add(0.0, Height * 0.5, 0.0);
        World.sendParticles(Scp1730Registry.AnchorParticle.get(), Centre.x, Centre.y, Centre.z, 60, Width * 0.5, Height * 0.4, Width * 0.5, 0.02);
        World.playSound(null, Centre.x, Centre.y, Centre.z, Scp1730Registry.AnchorBurst.get(), SoundSource.HOSTILE, 2.0f, 0.5f);
        Notices.sendNear(World, Centre, NoticeRange, Notice.anomaly(DocumentClass.Keter, Component.translatable("notice.scp_unleashed.scp1730_2.recontained")));
        Log.info("The recovered SCP-1730-2 was held again at {} in {} by the Reality Folder at {}", blockPosition().toShortString(), World.dimension().location(),
                Master.toShortString());
    }

    private void escape(ServerLevel World) {
        Vec3 From = position();
        Vec3 Cell = Bound != null ? Vec3.atBottomCenterOf(Bound) : From;
        BlockPos Left = Bound;
        Vec3 Centre = From.add(0.0, Height * 0.5, 0.0);
        World.sendParticles(ParticleTypes.FLASH, Centre.x, Centre.y, Centre.z, 1, 0.0, 0.0, 0.0, 0.0);
        World.sendParticles(ParticleTypes.REVERSE_PORTAL, Centre.x, Centre.y, Centre.z, 220, Width * 0.35, Height * 0.4, Width * 0.35, 0.35);
        World.sendParticles(Scp1730Registry.GlyphParticle.get(), Centre.x, Centre.y, Centre.z, 48, Width * 0.45, Height * 0.4, Width * 0.45, 0.02);
        World.sendParticles(ParticleTypes.END_ROD, Centre.x, Centre.y, Centre.z, 30, Width * 0.3, Height * 0.35, Width * 0.3, 0.05);
        World.playSound(null, Centre.x, Centre.y, Centre.z, Scp1730GuardianRegistry.Phase.get(), SoundSource.HOSTILE, 3.0f, 0.8f + random.nextFloat() * 0.1f);
        World.playSound(null, Centre.x, Centre.y, Centre.z, Scp1730GuardianRegistry.PhaseFold.get(), SoundSource.HOSTILE, 2.5f, 0.7f);
        World.playSound(null, Centre.x, Centre.y, Centre.z, Scp1730Registry.GuardianChains.get(), SoundSource.HOSTILE, 2.0f, 0.5f);
        Vec3 To = breakout(World, From, Cell);
        Bound = null;
        Free = true;
        Outside = 0;
        WanderCooldown = WanderPause;
        if (To != null) {
            trail(World, Centre, To.add(0.0, Height * 0.5, 0.0));
            teleportTo(To.x, To.y, To.z);
            setDeltaMovement(Vec3.ZERO);
            Vec3 Arrival = To.add(0.0, Height * 0.5, 0.0);
            World.sendParticles(ParticleTypes.REVERSE_PORTAL, Arrival.x, Arrival.y, Arrival.z, 90, Width * 0.35, Height * 0.4, Width * 0.35, 0.1);
            World.sendParticles(Scp1730Registry.GlyphParticle.get(), Arrival.x, Arrival.y, Arrival.z, 24, Width * 0.45, Height * 0.4, Width * 0.45, 0.02);
            World.playSound(null, Arrival.x, Arrival.y, Arrival.z, Scp1730GuardianRegistry.PhaseFold.get(), SoundSource.HOSTILE, 2.0f, 1.1f);
        }
        setHome(blockPosition());
        World.playSound(null, Cell.x, Cell.y + 2.0, Cell.z, BreachAlarm, SoundSource.BLOCKS, 4.0f, 1.0f);
        Notices.sendNear(World, Cell, NoticeRange, Notice.anomaly(DocumentClass.Keter, Component.translatable("notice.scp_unleashed.scp1730_2.breach"))
                .withDetail(Component.translatable("message.scp_unleashed.scp1730_2.escaped")));
        for (ServerPlayer Watcher : World.players()) {
            if (Watcher.distanceToSqr(Centre) <= ShakeRange * ShakeRange || Watcher.distanceToSqr(position()) <= ShakeRange * ShakeRange) {
                PacketDistributor.sendToPlayer(Watcher, new Scp1730ShakePayload(0.35f, 14));
            }
        }
        Log.info("The recovered SCP-1730-2 slipped out of containment at {} in {} with {} of {} health; it was bound to {} and resurfaced at {}",
                BlockPos.containing(From).toShortString(), World.dimension().location(), String.format("%.1f", getHealth()), String.format("%.1f", getMaxHealth()),
                Left == null ? "nothing" : Left.toShortString(), blockPosition().toShortString());
    }

    private void trail(ServerLevel World, Vec3 From, Vec3 To) {
        Vec3 Span = To.subtract(From);
        int Steps = Math.max(1, Mth.ceil(Span.length() / 0.8));
        for (int Step = 0; Step <= Steps; Step++) {
            Vec3 At = From.add(Span.scale(Step / (double) Steps));
            World.sendParticles(Scp1730Registry.GlyphParticle.get(), At.x, At.y, At.z, 1, 0.15, 0.15, 0.15, 0.002);
        }
    }

    @Nullable
    public Vec3 breakout(ServerLevel World, Vec3 From, Vec3 Cell) {
        Vec3 Best = null;
        int BestScore = Integer.MIN_VALUE;
        for (int Attempt = 0; Attempt < BreakoutTries; Attempt++) {
            double Angle = random.nextDouble() * Math.PI * 2.0;
            double Reach = BreakoutMin + random.nextDouble() * (BreakoutMax - BreakoutMin);
            Vec3 Spot = floorNear(World, Mth.floor(From.x + Math.cos(Angle) * Reach), From.y, Mth.floor(From.z + Math.sin(Angle) * Reach));
            if (Spot == null) {
                continue;
            }
            int Score = score(World, From, Cell, Spot);
            if (Score > BestScore) {
                Best = Spot;
                BestScore = Score;
            }
        }
        if (Best != null || World.dimensionType().hasCeiling()) {
            return Best;
        }
        for (int Attempt = 0; Attempt < BreakoutTries; Attempt++) {
            double Angle = random.nextDouble() * Math.PI * 2.0;
            double Reach = BreakoutMin + random.nextDouble() * (BreakoutMax - BreakoutMin);
            int X = Mth.floor(From.x + Math.cos(Angle) * Reach);
            int Z = Mth.floor(From.z + Math.sin(Angle) * Reach);
            if (!World.isLoaded(new BlockPos(X, Mth.floor(From.y), Z))) {
                continue;
            }
            int Top = World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X, Z);
            Vec3 Spot = floorNear(World, X, Top, Z);
            if (Spot == null) {
                continue;
            }
            int Score = score(World, From, Cell, Spot);
            if (Score > BestScore) {
                Best = Spot;
                BestScore = Score;
            }
        }
        return Best;
    }

    private int score(ServerLevel World, Vec3 From, Vec3 Cell, Vec3 Spot) {
        AABB Box = getDimensions(getPose()).makeBoundingBox(Spot);
        int Score = 0;
        if (!Scp1730Anchors.folded(World, Box)) {
            Score += 8;
        }
        double Dx = Spot.x - Cell.x;
        double Dz = Spot.z - Cell.z;
        if (Dx * Dx + Dz * Dz > Scp1730Anchors.FolderRadius * Scp1730Anchors.FolderRadius) {
            Score += 2;
        }
        Vec3 Eye = From.add(0.0, Height * 0.5, 0.0);
        if (World.clip(new ClipContext(Eye, Spot.add(0.0, Height * 0.5, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() != HitResult.Type.MISS) {
            Score += 4;
        }
        int Open = 0;
        for (Direction Way : Direction.Plane.HORIZONTAL) {
            if (World.noCollision(this, Box.move(Way.getStepX() * 2.0, 0.0, Way.getStepZ() * 2.0))) {
                Open++;
            }
        }
        if (Open >= 2) {
            Score += 3;
        }
        return Score * 4 + Open;
    }

    @Nullable
    private Vec3 floorNear(ServerLevel World, int X, double Y, int Z) {
        int Base = Mth.floor(Y);
        for (int Lift = FloorAbove; Lift >= -FloorBelow; Lift--) {
            BlockPos Ground = new BlockPos(X, Base + Lift - 1, Z);
            if (!World.isLoaded(Ground) || !World.isPositionEntityTicking(Ground.above())) {
                continue;
            }
            BlockState Floor = World.getBlockState(Ground);
            if (!Floor.isFaceSturdy(World, Ground, Direction.UP) || !World.getFluidState(Ground.above()).isEmpty()) {
                continue;
            }
            Vec3 Spot = new Vec3(X + 0.5, Base + Lift + Hover, Z + 0.5);
            AABB Box = getDimensions(getPose()).makeBoundingBox(Spot);
            if (World.noCollision(this, Box) && !World.containsAnyLiquid(Box)) {
                return Spot;
            }
        }
        return null;
    }

    @Nullable
    private Vec3 wanderSpot(ServerLevel World) {
        Vec3 Here = position();
        for (int Attempt = 0; Attempt < WanderTries; Attempt++) {
            double Angle = random.nextDouble() * Math.PI * 2.0;
            double Reach = WanderMin + random.nextDouble() * (WanderMax - WanderMin);
            Vec3 Spot = floorNear(World, Mth.floor(Here.x + Math.cos(Angle) * Reach), Here.y - Hover, Mth.floor(Here.z + Math.sin(Angle) * Reach));
            if (Spot != null && clearRun(World, Here, Spot)) {
                return Spot;
            }
        }
        return null;
    }

    private boolean clearRun(ServerLevel World, Vec3 From, Vec3 To) {
        Vec3 Span = To.subtract(From);
        int Steps = Math.max(1, Mth.ceil(Span.horizontalDistance() / PathStep));
        AABB Start = getBoundingBox();
        for (int Step = 1; Step <= Steps; Step++) {
            Vec3 Offset = Span.scale(Step / (double) Steps);
            if (!World.noCollision(this, Start.move(Offset.x, Math.max(0.0, Offset.y), Offset.z))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        if (Bound != null) {
            Tag.put(BoundKey, NbtUtils.writeBlockPos(Bound));
        }
        Tag.putBoolean(FreeKey, Free);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Bound = NbtUtils.readBlockPos(Tag, BoundKey).orElse(null);
        Free = Tag.getBoolean(FreeKey);
    }
}
