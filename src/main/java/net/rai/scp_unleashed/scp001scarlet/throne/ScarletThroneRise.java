package net.rai.scp_unleashed.scp001scarlet.throne;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp001gate.Scp001GateSites;
import net.rai.scp_unleashed.scp001scarlet.ScarletMeter;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKingRegistry;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletRite;
import net.rai.scp_unleashed.scp5001.world.Scp5001Sites;
import net.rai.scp_unleashed.scp999.Scp999Blob;
import net.rai.scp_unleashed.scp999.Scp999Registry;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class ScarletThroneRise {
    public static final long BudgetNanos = 8_000_000L;
    public static final double FrontSpeed = 0.7;
    public static final double FrontLead = 3.0;
    public static final int DaisTicks = 24;
    public static final int PillarStagger = 12;
    public static final int RestTicks = 16;
    public static final int FlashTicks = 40;
    public static final int RoofDepth = 24;
    public static final int Below = 8;
    public static final int FootingReach = 4;
    public static final int SampleStep = 6;
    public static final double LiquidShare = 0.15;
    public static final int Crack = 0;
    public static final int Dais = 1;
    public static final int Rise = 2;
    public static final int Rest = 3;
    public static final int Flash = 4;
    public static final DustParticleOptions Crimson = new DustParticleOptions(new Vector3f(0.86f, 0.08f, 0.1f), 2.2f);
    public static final DustParticleOptions Ember = new DustParticleOptions(new Vector3f(1.0f, 0.42f, 0.25f), 1.2f);
    public static int SpawnOverride = -1;
    private static int[][] Order;
    @Nullable
    private static Job Current;

    public enum Refusal { None, Footing, Liquid, Limit, Buried, Spawn, Claimed }

    public record Survey(Refusal Reason, BlockPos Throne) {
        public boolean fine() {
            return Reason == Refusal.None;
        }
    }

    private static final class Job {
        final BlockPos Centre;
        final long Seed;
        final List<List<ScarletThrone.Piece>> Groups = new ArrayList<>();
        final int[] Placed = new int[ScarletThrone.RuinGroup + 1];
        int Column;
        int Elapsed;

        Job(BlockPos Centre, long Seed) {
            this.Centre = Centre;
            this.Seed = Seed;
            for (int Group = 0; Group <= ScarletThrone.RuinGroup; Group++) {
                Groups.add(new ArrayList<>());
            }
            for (ScarletThrone.Piece Each : ScarletThrone.plan(Seed).All()) {
                Groups.get(Each.Group()).add(Each);
            }
            for (List<ScarletThrone.Piece> Each : Groups) {
                Each.sort(Comparator.comparingInt(ScarletThrone.Piece::Y));
            }
        }
    }

    private ScarletThroneRise() {
    }

    public static synchronized int[][] order() {
        if (Order == null) {
            List<int[]> Cells = new ArrayList<>();
            int Reach = ScarletThrone.RimRadius;
            for (int Dx = -Reach; Dx <= Reach; Dx++) {
                for (int Dz = -Reach; Dz <= Reach; Dz++) {
                    if (Dx * Dx + Dz * Dz <= Reach * Reach) {
                        Cells.add(new int[]{Dx, Dz, Dx * Dx + Dz * Dz});
                    }
                }
            }
            Cells.sort(Comparator.comparingInt(Each -> Each[2]));
            Order = Cells.toArray(new int[0][]);
        }
        return Order;
    }

    public static int spawnRadius(MinecraftServer Server) {
        if (SpawnOverride >= 0) {
            return SpawnOverride;
        }
        return Server.isDedicatedServer() ? Server.getSpawnProtectionRadius() : 0;
    }

    public static Survey survey(ServerLevel World, Vec3 Where) {
        BlockPos Feet = BlockPos.containing(Where);
        if (!World.getFluidState(Feet).isEmpty() || !World.getFluidState(Feet.above()).isEmpty()) {
            return new Survey(Refusal.Liquid, Feet);
        }
        BlockPos Ground = null;
        for (int Drop = 0; Drop <= FootingReach; Drop++) {
            BlockPos At = Feet.below(Drop);
            BlockState Here = World.getBlockState(At);
            if (!Here.getFluidState().isEmpty()) {
                return new Survey(Refusal.Liquid, Feet);
            }
            if (Drop > 0 && !Here.getCollisionShape(World, At).isEmpty()) {
                Ground = At;
                break;
            }
        }
        if (Ground == null) {
            return new Survey(Refusal.Footing, Feet);
        }
        BlockPos Throne = new BlockPos(Feet.getX(), Ground.getY() + 1 - ScarletThrone.DaisHeight, Feet.getZ());
        if (Throne.getY() - Below <= World.getMinBuildHeight() || Throne.getY() + ScarletThrone.ClearAbove + 2 >= World.getMaxBuildHeight()) {
            return new Survey(Refusal.Limit, Throne);
        }
        int Sky = World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Feet.getX(), Feet.getZ());
        if (Sky - (Ground.getY() + 1) > RoofDepth) {
            return new Survey(Refusal.Buried, Throne);
        }
        int Radius = spawnRadius(World.getServer());
        if (Radius > 0 && World.dimension() == Level.OVERWORLD) {
            BlockPos Spawn = World.getSharedSpawnPos();
            int Gap = Math.max(Math.abs(Throne.getX() - Spawn.getX()), Math.abs(Throne.getZ() - Spawn.getZ()));
            if (Gap <= Radius + ScarletThrone.RimRadius) {
                return new Survey(Refusal.Spawn, Throne);
            }
        }
        if (wet(World, Throne)) {
            return new Survey(Refusal.Liquid, Throne);
        }
        if (claimed(World, Throne)) {
            return new Survey(Refusal.Claimed, Throne);
        }
        return new Survey(Refusal.None, Throne);
    }

    private static boolean wet(ServerLevel World, BlockPos Throne) {
        int Wet = 0;
        int Total = 0;
        int Reach = ScarletThrone.FlatRadius;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Dx = -Reach; Dx <= Reach; Dx += SampleStep) {
            for (int Dz = -Reach; Dz <= Reach; Dz += SampleStep) {
                if (Dx * Dx + Dz * Dz > Reach * Reach) {
                    continue;
                }
                int X = Throne.getX() + Dx;
                int Z = Throne.getZ() + Dz;
                int Top = World.getHeight(Heightmap.Types.WORLD_SURFACE, X, Z) - 1;
                Total++;
                if (!World.getFluidState(Cursor.set(X, Top, Z)).isEmpty()) {
                    Wet++;
                }
            }
        }
        return Total > 0 && Wet > Total * LiquidShare;
    }

    public static boolean foreign(BlockState State) {
        ResourceLocation Key = BuiltInRegistries.BLOCK.getKey(State.getBlock());
        if (!Scp_unleashed.ModId.equals(Key.getNamespace())) {
            return false;
        }
        String Path = Key.getPath();
        if (Path.startsWith("scp001_scarlet") || Path.startsWith("scarlet_") || Path.endsWith("_ore") || Path.startsWith("lunar_") || Path.startsWith("moon_")) {
            return false;
        }
        return !State.is(Tags.Blocks.ORES);
    }

    public static boolean claimed(ServerLevel World, BlockPos Throne) {
        MinecraftServer Server = World.getServer();
        int Reach = ScarletThrone.RimRadius + 1;
        int Low = Throne.getY() - Below;
        int High = Throne.getY() + ScarletThrone.ClearAbove;
        BoundingBox Box = new BoundingBox(Throne.getX() - Reach, Low, Throne.getZ() - Reach, Throne.getX() + Reach, High, Throne.getZ() + Reach);
        Scp5001Sites Vault = Scp5001Sites.get(Server);
        if (Vault.planned() && Vault.bounds().intersects(Box)) {
            return true;
        }
        Scp001GateSites Gate = Scp001GateSites.get(Server);
        var Structures = World.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (int Cx = (Throne.getX() - Reach) >> 4; Cx <= (Throne.getX() + Reach) >> 4; Cx++) {
            for (int Cz = (Throne.getZ() - Reach) >> 4; Cz <= (Throne.getZ() + Reach) >> 4; Cz++) {
                ChunkPos Pos = new ChunkPos(Cx, Cz);
                if (Gate.planned() && Gate.covers(Pos)) {
                    return true;
                }
                LevelChunk Chunk = World.getChunk(Cx, Cz);
                for (Structure Each : Chunk.getAllReferences().keySet()) {
                    ResourceLocation Key = Structures.getKey(Each);
                    if (Key != null && Scp_unleashed.ModId.equals(Key.getNamespace()) && !Chunk.getAllReferences().get(Each).isEmpty()) {
                        return true;
                    }
                }
                if (foreignIn(World, Chunk, Throne, Low, High)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean foreignIn(ServerLevel World, LevelChunk Chunk, BlockPos Throne, int Low, int High) {
        LevelChunkSection[] Sections = Chunk.getSections();
        double Limit = (ScarletThrone.RimRadius + 0.5) * (ScarletThrone.RimRadius + 0.5);
        for (int Index = 0; Index < Sections.length; Index++) {
            LevelChunkSection Section = Sections[Index];
            int Base = World.getSectionYFromSectionIndex(Index) << 4;
            if (Section.hasOnlyAir() || Base + 15 < Low || Base > High || !Section.getStates().maybeHas(ScarletThroneRise::foreign)) {
                continue;
            }
            for (int Y = 0; Y < 16; Y++) {
                if (Base + Y < Low || Base + Y > High) {
                    continue;
                }
                for (int Z = 0; Z < 16; Z++) {
                    for (int X = 0; X < 16; X++) {
                        double Dx = Chunk.getPos().getMinBlockX() + X - Throne.getX();
                        double Dz = Chunk.getPos().getMinBlockZ() + Z - Throne.getZ();
                        if (Dx * Dx + Dz * Dz <= Limit && foreign(Section.getBlockState(X, Y, Z))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean active(MinecraftServer Server) {
        return ScarletThroneSite.get(Server).rising();
    }

    public static void start(ServerLevel World, BlockPos Throne, Scp999Blob Blob, @Nullable ServerPlayer Leader, long XkTick) {
        MinecraftServer Server = World.getServer();
        ScarletState State = ScarletState.get(Server);
        ScarletThroneSite.claim(Server, Throne);
        ScarletThroneSite Site = ScarletThroneSite.get(Server);
        Site.beginRising(Blob.getUUID(), Leader == null ? null : Leader.getUUID(), XkTick);
        State.throne(Throne);
        State.phase(ScarletState.Phase.Rite, World.getGameTime());
        hold(Blob);
        ChunkPos Home = new ChunkPos(Throne);
        World.setChunkForced(Home.x, Home.z, true);
        Current = new Job(Throne, Site.seed());
        World.playSound(null, Throne, ScarletKingRegistry.ThroneCrack.get(), SoundSource.HOSTILE, 8.0f, 1.0f);
        World.playSound(null, Throne, ScarletKingRegistry.Rumble.get(), SoundSource.HOSTILE, 8.0f, 0.5f);
        ScarletMeter.syncAll(Server);
    }

    public static void hold(Scp999Blob Blob) {
        Blob.setNoAi(true);
        Blob.setNoGravity(true);
        Blob.setInvulnerable(true);
        Blob.setGlowingTag(true);
        Blob.setDeltaMovement(Vec3.ZERO);
        Blob.setPersistenceRequired();
    }

    public static void forget() {
        Current = null;
    }

    public static void tick(MinecraftServer Server) {
        ScarletThroneSite Site = ScarletThroneSite.get(Server);
        if (!Site.rising() || !Site.planned()) {
            Current = null;
            return;
        }
        ServerLevel World = Server.overworld();
        if (Current == null || !Current.Centre.equals(Site.centre())) {
            Current = new Job(Site.centre(), Site.seed());
            if (Site.stage() > Crack) {
                Current.Column = order().length;
            }
        }
        Job Work = Current;
        long Start = System.nanoTime();
        Work.Elapsed++;
        switch (Site.stage()) {
            case Crack -> crack(World, Site, Work, Start);
            case Dais -> group(World, Site, Work, Start, ScarletThrone.DaisGroup, 0, DaisTicks / 2, Rise);
            case Rise -> pillars(World, Site, Work, Start);
            case Rest -> rest(World, Site, Work, Start);
            default -> flash(World, Site, Work);
        }
    }

    private static void advance(ScarletThroneSite Site, Job Work, int Next) {
        Site.stage(Next);
        Work.Elapsed = 0;
    }

    private static void crack(ServerLevel World, ScarletThroneSite Site, Job Work, long Start) {
        int[][] Cells = order();
        double Front = Work.Elapsed * FrontSpeed + FrontLead;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        BlockPos Centre = Work.Centre;
        while (Work.Column < Cells.length && System.nanoTime() - Start < BudgetNanos) {
            int[] Cell = Cells[Work.Column];
            if (Cell[2] > Front * Front) {
                break;
            }
            int X = Centre.getX() + Cell[0];
            int Z = Centre.getZ() + Cell[1];
            LevelChunk Chunk = World.getChunk(X >> 4, Z >> 4);
            ScarletThroneEmitter.column(World, Chunk, Centre, Work.Seed, X, Z, Cursor);
            Work.Column++;
        }
        double Reached = Work.Column >= Cells.length ? ScarletThrone.RimRadius : Math.sqrt(Cells[Work.Column][2]);
        if (Work.Elapsed % 3 == 0) {
            int Points = 10 + (int) (Reached * 0.8);
            for (int Point = 0; Point < Points; Point++) {
                double Angle = World.random.nextDouble() * Mth.TWO_PI;
                double X = Centre.getX() + 0.5 + Math.sin(Angle) * Reached;
                double Z = Centre.getZ() + 0.5 - Math.cos(Angle) * Reached;
                double Y = World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(X), (int) Math.floor(Z));
                BlockState Under = World.getBlockState(BlockPos.containing(X, Y - 1, Z));
                if (!Under.isAir()) {
                    World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Under), X, Y + 0.1, Z, 6, 0.4, 0.1, 0.4, 0.1);
                }
                World.sendParticles(Crimson, X, Y + 0.3, Z, 2, 0.3, 0.2, 0.3, 0.0);
                if (World.random.nextInt(5) == 0) {
                    World.sendParticles(ParticleTypes.LAVA, X, Y + 0.1, Z, 1, 0.2, 0.0, 0.2, 0.0);
                }
            }
        }
        if (Work.Elapsed % 20 == 1) {
            double Angle = World.random.nextDouble() * Mth.TWO_PI;
            World.playSound(null, Centre.getX() + Math.sin(Angle) * Reached, Centre.getY(), Centre.getZ() - Math.cos(Angle) * Reached, ScarletKingRegistry.ThroneCrack.get(),
                    SoundSource.HOSTILE, 6.0f, 0.9f + World.random.nextFloat() * 0.2f);
        }
        glow(World, Work);
        if (Work.Elapsed % 10 == 0) {
            unbury(World, Centre);
        }
        if (Work.Column >= Cells.length) {
            unbury(World, Centre);
            advance(Site, Work, Dais);
        }
    }

    private static boolean placeUpTo(ServerLevel World, Job Work, int Group, int Layer, long Start) {
        List<ScarletThrone.Piece> Pieces = Work.Groups.get(Group);
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        while (Work.Placed[Group] < Pieces.size() && System.nanoTime() - Start < BudgetNanos) {
            ScarletThrone.Piece Each = Pieces.get(Work.Placed[Group]);
            if (Each.Y() > Layer) {
                break;
            }
            ScarletThroneEmitter.piece(World, Work.Centre, Each, Cursor);
            Work.Placed[Group]++;
        }
        return Work.Placed[Group] >= Pieces.size();
    }

    private static void group(ServerLevel World, ScarletThroneSite Site, Job Work, long Start, int Group, int Delay, int LayerTicks, int Next) {
        int Layer = Math.max(-1, (Work.Elapsed - Delay) / Math.max(1, LayerTicks));
        if (Work.Elapsed % 6 == 1) {
            World.playSound(null, Work.Centre, ScarletKingRegistry.ThroneRise.get(), SoundSource.HOSTILE, 5.0f, 0.9f + World.random.nextFloat() * 0.15f);
        }
        boolean Done = placeUpTo(World, Work, Group, Layer, Start);
        Vec3 Middle = Vec3.atBottomCenterOf(Work.Centre).add(0.0, Math.max(0, Layer) + 1.0, 0.0);
        World.sendParticles(Crimson, Middle.x, Middle.y, Middle.z, 16, ScarletThrone.DaisRadius * 0.5, 0.2, ScarletThrone.DaisRadius * 0.5, 0.0);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, Middle.x, Middle.y, Middle.z, 6, ScarletThrone.DaisRadius * 0.5, 0.2, ScarletThrone.DaisRadius * 0.5, 0.01);
        glow(World, Work);
        unbury(World, Work.Centre);
        if (Done && Work.Elapsed >= DaisTicks) {
            advance(Site, Work, Next);
        }
    }

    private static void pillars(ServerLevel World, ScarletThroneSite Site, Job Work, long Start) {
        boolean All = true;
        for (int Index = 0; Index < ScarletThrone.Pillars; Index++) {
            int Group = Index + 1;
            int Began = Index * PillarStagger;
            if (Work.Elapsed < Began) {
                All = false;
                continue;
            }
            int Layer = Work.Elapsed - Began;
            int[] Offset = ScarletThrone.pillarOffset(Index);
            double X = Work.Centre.getX() + Offset[0] + 0.5;
            double Z = Work.Centre.getZ() + Offset[1] + 0.5;
            if (Layer == 0) {
                World.playSound(null, X, Work.Centre.getY(), Z, ScarletKingRegistry.ThroneRise.get(), SoundSource.HOSTILE, 7.0f, 0.95f + World.random.nextFloat() * 0.1f);
            }
            boolean Done = placeUpTo(World, Work, Group, Layer, Start);
            if (!Done) {
                All = false;
                double Y = Work.Centre.getY() + Layer + 0.5;
                World.sendParticles(Crimson, X, Y, Z, 8, ScarletThrone.PillarWidth * 0.5, 0.3, ScarletThrone.PillarWidth * 0.5, 0.0);
                World.sendParticles(ParticleTypes.LARGE_SMOKE, X, Work.Centre.getY() + 0.5, Z, 4, ScarletThrone.PillarWidth, 0.2, ScarletThrone.PillarWidth, 0.02);
                World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()), X, Y, Z, 6,
                        ScarletThrone.PillarWidth * 0.6, 0.2, ScarletThrone.PillarWidth * 0.6, 0.1);
            }
        }
        glow(World, Work);
        if (Work.Elapsed % 4 == 0) {
            unbury(World, Work.Centre);
        }
        if (All) {
            unbury(World, Work.Centre);
            advance(Site, Work, Rest);
        }
    }

    private static void rest(ServerLevel World, ScarletThroneSite Site, Job Work, long Start) {
        boolean Seats = placeUpTo(World, Work, ScarletThrone.ThroneGroup, Work.Elapsed / 2, Start);
        boolean Ruins = placeUpTo(World, Work, ScarletThrone.RuinGroup, Work.Elapsed / 2, Start);
        if (Work.Elapsed % 4 == 1) {
            for (int Index = 0; Index < ScarletThrone.Pillars; Index++) {
                double Angle = ScarletThrone.angle(Index) + Mth.PI / ScarletThrone.Pillars;
                double X = Work.Centre.getX() + 0.5 + Math.sin(Angle) * ScarletThrone.ThroneRadius;
                double Z = Work.Centre.getZ() + 0.5 - Math.cos(Angle) * ScarletThrone.ThroneRadius;
                World.sendParticles(Crimson, X, Work.Centre.getY() + 2.0, Z, 10, 1.2, 1.0, 1.2, 0.0);
            }
        }
        glow(World, Work);
        if (Seats && Ruins && Work.Elapsed >= RestTicks) {
            unbury(World, Work.Centre);
            advance(Site, Work, Flash);
            World.playSound(null, Work.Centre, ScarletKingRegistry.ThroneDone.get(), SoundSource.HOSTILE, 10.0f, 1.0f);
            Vec3 Top = Vec3.atBottomCenterOf(ScarletThrone.dais(World, Work.Centre));
            World.sendParticles(Crimson, Top.x, Top.y + 1.0, Top.z, 200, 3.0, 6.0, 3.0, 0.05);
            World.sendParticles(ParticleTypes.END_ROD, Top.x, Top.y + 1.0, Top.z, 60, 1.0, 4.0, 1.0, 0.08);
            World.sendParticles(ParticleTypes.FLASH, Top.x, Top.y + 2.0, Top.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void glow(ServerLevel World, Job Work) {
        Vec3 Top = Vec3.atBottomCenterOf(ScarletThrone.dais(World, Work.Centre));
        if (Work.Elapsed % 2 == 0) {
            World.sendParticles(Crimson, Top.x, Top.y + 0.5, Top.z, 6, 0.6, 1.2, 0.6, 0.02);
            World.sendParticles(Ember, Top.x, Top.y + 2.0, Top.z, 3, 0.3, 2.5, 0.3, 0.05);
        }
    }

    private static void flash(ServerLevel World, ScarletThroneSite Site, Job Work) {
        glow(World, Work);
        if (Work.Elapsed < FlashTicks) {
            return;
        }
        finish(World, Site, Work.Centre);
    }

    private static void finish(ServerLevel World, ScarletThroneSite Site, BlockPos Throne) {
        UUID Id = Site.blob();
        UUID LeaderId = Site.leader();
        long XkTick = Site.xkTick();
        Site.endRising();
        Current = null;
        Entity Found = Id == null ? null : World.getEntity(Id);
        ChunkPos Home = new ChunkPos(Throne);
        Scp999Blob Blob = Found instanceof Scp999Blob Each && Each.isAlive() ? Each : null;
        if (Blob == null) {
            Vec3 At = Vec3.atBottomCenterOf(ScarletThrone.dais(World, Throne));
            Blob = Scp999Registry.Blob.get().spawn(World, BlockPos.containing(At), MobSpawnType.EVENT);
        }
        World.setChunkForced(Home.x, Home.z, false);
        if (Blob == null) {
            ScarletState.get(World.getServer()).phase(ScarletState.Phase.Xk2, XkTick);
            ScarletMeter.syncAll(World.getServer());
            return;
        }
        hold(Blob);
        ServerPlayer Leader = LeaderId == null ? null : World.getServer().getPlayerList().getPlayer(LeaderId);
        ScarletRite.begin(World, Throne, Blob, Leader, XkTick);
        ScarletMeter.syncAll(World.getServer());
    }

    public static void unbury(ServerLevel World, BlockPos Centre) {
        AABB Area = new AABB(Centre).inflate(ScarletThrone.RimRadius + 1.0, ScarletThrone.ClearAbove, ScarletThrone.RimRadius + 1.0);
        for (LivingEntity Each : World.getEntitiesOfClass(LivingEntity.class, Area, Each -> Each.isAlive() && !Each.isSpectator() && !Each.isNoGravity())) {
            if (World.noCollision(Each, Each.getBoundingBox().deflate(0.02))) {
                continue;
            }
            for (int Lift = 1; Lift <= 8; Lift++) {
                AABB Moved = Each.getBoundingBox().move(0.0, Lift, 0.0).deflate(0.02);
                if (World.noCollision(Each, Moved)) {
                    Each.teleportTo(Each.getX(), Math.floor(Each.getY()) + Lift, Each.getZ());
                    Each.resetFallDistance();
                    break;
                }
            }
        }
    }
}
