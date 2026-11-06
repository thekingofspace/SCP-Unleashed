package net.rai.scp_unleashed.raid.underground;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.EventHooks;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.grenade.GrenadeRegistry;
import net.rai.scp_unleashed.guns.GunGlass;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidContext;
import net.rai.scp_unleashed.raid.RaidDirector;
import net.rai.scp_unleashed.raid.RaidGround;
import net.rai.scp_unleashed.raid.RaidStage;
import net.rai.scp_unleashed.raid.hostile.HostileRaider;
import net.rai.scp_unleashed.raid.hostile.HostileRegistry;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RaidBreach {
    private static final Logger Log = LogUtils.getLogger();
    public static final String Key = "Breach";
    public static final byte ModeNone = 0;
    public static final byte ModeShaft = 1;
    public static final byte ModeEntrance = 2;
    public static final byte PhasePlanned = 0;
    public static final byte PhasePlanting = 1;
    public static final byte PhaseArmed = 2;
    public static final byte PhaseBlasting = 3;
    public static final byte PhaseOpen = 4;
    public static final byte PhaseFailed = 5;
    public static final float Sturdy = 1200.0f;
    public static final float BlastPower = 3.0f;
    public static final TagKey<Block> MallProof = tag("scp3008_unbreakable");
    public static final TagKey<Block> CageProof = tag("scp682_unbreakable");
    public static final DustParticleOptions Blink = new DustParticleOptions(new Vector3f(1.0f, 0.12f, 0.08f), 1.1f);

    public record Plan(BlockPos Corner, int Floor, int Top, BlockPos Lip, BlockPos Room, int Cells) {
        public Vec3 axis() {
            return new Vec3(Corner.getX() + UndergroundRules.ShaftWidth * 0.5, Floor, Corner.getZ() + UndergroundRules.ShaftWidth * 0.5);
        }
    }

    private RaidBreach() {
    }

    private static TagKey<Block> tag(String Name) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name));
    }

    public static boolean guarded(ServerLevel World, BlockPos Pos, BlockState State, @Nullable LivingEntity Breacher) {
        if (State.getDestroySpeed(World, Pos) < 0.0f || State.hasBlockEntity() || State.getBlock().getExplosionResistance() >= Sturdy) {
            return true;
        }
        if (State.is(BlockTags.WITHER_IMMUNE) || State.is(BlockTags.DRAGON_IMMUNE) || State.is(GunGlass.Bulletproof) || State.is(MallProof) || State.is(CageProof)) {
            return true;
        }
        return Breacher != null && (!State.canEntityDestroy(World, Pos, Breacher) || !EventHooks.onEntityDestroyBlock(Breacher, Pos, State));
    }

    public static boolean natural(BlockState State) {
        return RaidSurvey.earthy(State) && !State.is(Tags.Blocks.ORES) || State.is(BlockTags.LEAVES) || State.is(BlockTags.REPLACEABLE) || State.is(BlockTags.FLOWERS) || State.is(BlockTags.SAPLINGS);
    }

    public static boolean allowed(ServerLevel World, @Nullable Entity Breacher) {
        return HostileRegistry.ruleOn(World.getServer(), HostileRegistry.BreachRule) && EventHooks.canEntityGrief(World, Breacher);
    }

    public static boolean blocking(ServerLevel World, BlockPos Pos, BlockState State) {
        return !State.getCollisionShape(World, Pos).isEmpty();
    }

    @Nullable
    public static BlockPos lip(ServerLevel World, BlockPos Corner, int Top) {
        int Width = UndergroundRules.ShaftWidth;
        BlockPos Best = null;
        int BestCost = Integer.MAX_VALUE;
        for (int Ring = 1; Ring <= 2; Ring++) {
            for (int Dx = -Ring; Dx < Width + Ring; Dx++) {
                for (int Dz = -Ring; Dz < Width + Ring; Dz++) {
                    if (Dx >= -Ring + 1 && Dx < Width + Ring - 1 && Dz >= -Ring + 1 && Dz < Width + Ring - 1) {
                        continue;
                    }
                    for (int Lift = -UndergroundRules.LipRise; Lift <= UndergroundRules.LipRise; Lift++) {
                        BlockPos Feet = new BlockPos(Corner.getX() + Dx, Top + 1 + Lift, Corner.getZ() + Dz);
                        int Cost = Ring * 10 + Math.abs(Lift) * 3 + (Dx >= 0 && Dx < Width || Dz >= 0 && Dz < Width ? 0 : 1);
                        if (Cost >= BestCost || !World.isPositionEntityTicking(Feet) || !RaidGround.standable(World, Feet) || !RaidSurvey.open(World, Feet)) {
                            continue;
                        }
                        Best = Feet;
                        BestCost = Cost;
                    }
                }
            }
            if (Best != null) {
                return Best;
            }
        }
        return Best;
    }

    @Nullable
    public static Plan window(ServerLevel World, BlockPos Corner, BlockPos Room, @Nullable LivingEntity Breacher, int Cap) {
        int Width = UndergroundRules.ShaftWidth;
        int Floor = Room.getY();
        int Top = Integer.MIN_VALUE;
        for (int Dx = 0; Dx < Width; Dx++) {
            for (int Dz = 0; Dz < Width; Dz++) {
                BlockPos Column = new BlockPos(Corner.getX() + Dx, Floor, Corner.getZ() + Dz);
                if (!World.isPositionEntityTicking(Column)) {
                    return null;
                }
                BlockPos Under = Column.below();
                BlockState Base = World.getBlockState(Under);
                if (!Base.isFaceSturdy(World, Under, Direction.UP) || !Base.getFluidState().isEmpty()) {
                    return null;
                }
                int ColumnTop = RaidSurvey.top(World, Column.getX(), Column.getZ());
                if (ColumnTop < Floor + 2) {
                    return null;
                }
                Top = Math.max(Top, ColumnTop);
            }
        }
        if (Top - Floor > UndergroundRules.MaxShaftDepth) {
            return null;
        }
        int Cells = 0;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Dx = 0; Dx < Width; Dx++) {
            for (int Dz = 0; Dz < Width; Dz++) {
                for (int Y = Floor; Y <= Top + 1; Y++) {
                    Cursor.set(Corner.getX() + Dx, Y, Corner.getZ() + Dz);
                    BlockState State = World.getBlockState(Cursor);
                    if (!State.getFluidState().isEmpty()) {
                        return null;
                    }
                    for (Direction Side : Direction.Plane.HORIZONTAL) {
                        int Nx = Dx + Side.getStepX();
                        int Nz = Dz + Side.getStepZ();
                        if (Nx >= 0 && Nx < Width && Nz >= 0 && Nz < Width) {
                            continue;
                        }
                        if (!World.getFluidState(Cursor.relative(Side)).isEmpty()) {
                            return null;
                        }
                    }
                    if (Y > Top || !blocking(World, Cursor, State)) {
                        continue;
                    }
                    if (guarded(World, Cursor.immutable(), State, Breacher)) {
                        return null;
                    }
                    if (++Cells > Cap) {
                        return null;
                    }
                }
            }
        }
        BlockPos Lip = lip(World, Corner, Top);
        if (Lip == null) {
            return null;
        }
        return new Plan(new BlockPos(Corner.getX(), Floor, Corner.getZ()), Floor, Top, Lip, Room, Cells);
    }

    public static List<BlockPos> sites(ServerLevel World, RaidSurvey Survey) {
        List<BlockPos> Found = new ArrayList<>();
        for (BlockPos Room : Survey.Rooms()) {
            if (RaidSurvey.covered(World, Room) && !Found.contains(Room)) {
                Found.add(Room);
            }
        }
        int Seen = 0;
        for (var Entry : Survey.Flood().steps().entrySet()) {
            if (Found.size() >= UndergroundRules.BreachRooms) {
                break;
            }
            if (Seen++ % 3 != 0) {
                continue;
            }
            BlockPos Cell = BlockPos.of(Entry.getKey());
            if (!Found.contains(Cell) && RaidSurvey.covered(World, Cell)) {
                Found.add(Cell);
            }
        }
        return Found;
    }

    @Nullable
    public static Plan plan(ServerLevel World, RaidSurvey Survey, @Nullable LivingEntity Breacher, int Cap) {
        Plan Best = null;
        double BestScore = Double.MAX_VALUE;
        BlockPos Anchor = Survey.Anchor();
        for (BlockPos Room : sites(World, Survey)) {
            for (int Ox = 1 - UndergroundRules.ShaftWidth; Ox <= 0; Ox++) {
                for (int Oz = 1 - UndergroundRules.ShaftWidth; Oz <= 0; Oz++) {
                    Plan Found = window(World, Room.offset(Ox, 0, Oz), Room, Breacher, Cap);
                    if (Found == null) {
                        continue;
                    }
                    double Score = Found.Cells() + 2.0 * Math.sqrt(Room.distSqr(Anchor));
                    if (Score < BestScore) {
                        BestScore = Score;
                        Best = Found;
                    }
                }
            }
        }
        return Best;
    }

    public static boolean has(Raid Current) {
        return Current.extra().contains(Key);
    }

    public static CompoundTag data(Raid Current) {
        return Current.extra().getCompound(Key);
    }

    public static byte mode(Raid Current) {
        return has(Current) ? data(Current).getByte("Mode") : ModeNone;
    }

    public static byte phase(Raid Current) {
        return data(Current).getByte("Phase");
    }

    public static void storeShaft(Raid Current, Plan Found, int Cap, @Nullable BlockPos Entrance) {
        CompoundTag Tag = new CompoundTag();
        Tag.putByte("Mode", ModeShaft);
        Tag.putByte("Phase", PhasePlanned);
        Tag.putLong("Corner", Found.Corner().asLong());
        Tag.putInt("Floor", Found.Floor());
        Tag.putInt("Top", Found.Top());
        Tag.putLong("Lip", Found.Lip().asLong());
        Tag.putLong("Room", Found.Room().asLong());
        Tag.putInt("Cells", Found.Cells());
        Tag.putInt("Cursor", Found.Top());
        Tag.putInt("Removed", 0);
        Tag.putInt("Cap", Cap);
        if (Entrance != null) {
            Tag.putLong("Entrance", Entrance.asLong());
        }
        Current.extra().put(Key, Tag);
        Current.changed();
    }

    public static void storeEntrance(Raid Current, BlockPos Entrance) {
        CompoundTag Tag = new CompoundTag();
        Tag.putByte("Mode", ModeEntrance);
        Tag.putLong("Entrance", Entrance.asLong());
        Current.extra().put(Key, Tag);
        Current.changed();
    }

    public static void storeNone(Raid Current) {
        CompoundTag Tag = new CompoundTag();
        Tag.putByte("Mode", ModeNone);
        Current.extra().put(Key, Tag);
        Current.changed();
    }

    public static BlockPos corner(CompoundTag Tag) {
        return BlockPos.of(Tag.getLong("Corner"));
    }

    public static Vec3 axis(CompoundTag Tag) {
        BlockPos Corner = corner(Tag);
        return new Vec3(Corner.getX() + UndergroundRules.ShaftWidth * 0.5, Tag.getInt("Floor"), Corner.getZ() + UndergroundRules.ShaftWidth * 0.5);
    }

    public static Vec3 charge(CompoundTag Tag) {
        Vec3 Axis = axis(Tag);
        return new Vec3(Axis.x, Tag.getInt("Top") + 1.05, Axis.z);
    }

    public static BlockPos lip(CompoundTag Tag) {
        return BlockPos.of(Tag.getLong("Lip"));
    }

    @Nullable
    public static BlockPos entrance(CompoundTag Tag) {
        return Tag.contains("Entrance") ? BlockPos.of(Tag.getLong("Entrance")) : null;
    }

    public static boolean inShaft(CompoundTag Tag, Vec3 Point) {
        BlockPos Corner = corner(Tag);
        int Width = UndergroundRules.ShaftWidth;
        return Point.x >= Corner.getX() - 0.3 && Point.x <= Corner.getX() + Width + 0.3 && Point.z >= Corner.getZ() - 0.3 && Point.z <= Corner.getZ() + Width + 0.3
                && Point.y < Tag.getInt("Top") + 0.5 && Point.y >= Tag.getInt("Floor") - 1.0;
    }

    public static boolean inside(ServerLevel World, Raid Current, Entity Body) {
        if (mode(Current) == ModeShaft && inShaft(data(Current), Body.position())) {
            return true;
        }
        return RaidSurvey.covered(World, Body.blockPosition());
    }

    public static boolean open(Raid Current) {
        return mode(Current) == ModeShaft && phase(Current) == PhaseOpen;
    }

    public static boolean held(Mob Body) {
        if (!(Body instanceof HostileRaider Raider) || !(Body.level() instanceof ServerLevel World)) {
            return false;
        }
        Raid Current = Raider.raid();
        if (Current == null || Current.stage() != RaidStage.Active) {
            return false;
        }
        byte Mode = mode(Current);
        return (Mode == ModeShaft || Mode == ModeEntrance) && !inside(World, Current, Body);
    }

    @Nullable
    public static UUID planter(Raid Current) {
        return Raid.uuid(data(Current).getString("Planter"));
    }

    public static boolean claim(ServerLevel World, Raid Current, Entity Body) {
        CompoundTag Tag = data(Current);
        UUID Held = planter(Current);
        if (Held != null && Held.equals(Body.getUUID())) {
            return true;
        }
        if (Held != null && World.getEntity(Held) instanceof LivingEntity Other && Other.isAlive() && !Other.isRemoved()) {
            return false;
        }
        Tag.putString("Planter", Body.getUUID().toString());
        Current.changed();
        return true;
    }

    @Nullable
    private static LivingEntity breacher(ServerLevel World, CompoundTag Tag) {
        UUID Id = Raid.uuid(Tag.getString("Planter"));
        return Id != null && World.getEntity(Id) instanceof LivingEntity Found && Found.isAlive() ? Found : null;
    }

    private static void notice(ServerLevel World, Raid Current, String Event) {
        RaidContext Context = RaidDirector.context(World.getServer(), Current, World.getServer().overworld().getGameTime());
        if (Context != null) {
            Context.notice(Event);
        }
    }

    public static void plant(ServerLevel World, Raid Current, LivingEntity Planter, long Now) {
        CompoundTag Tag = data(Current);
        byte Phase = Tag.getByte("Phase");
        Vec3 Charge = charge(Tag);
        if (Phase == PhasePlanned) {
            if (!allowed(World, Planter)) {
                fail(World, Current, "griefing is off");
                return;
            }
            Tag.putByte("Phase", PhasePlanting);
            Tag.putLong("Since", Now);
            Current.changed();
            World.playSound(null, Charge.x, Charge.y, Charge.z, HostileRegistry.BreachPlant.get(), SoundSource.HOSTILE, 1.2f, 1.0f);
            World.playSound(null, Charge.x, Charge.y, Charge.z, GrenadeRegistry.PinPull.get(), SoundSource.HOSTILE, 0.9f, 0.8f);
            notice(World, Current, "breach");
            return;
        }
        if (Phase != PhasePlanting) {
            return;
        }
        long Elapsed = Now - Tag.getLong("Since");
        if (Elapsed % 10 == 0) {
            World.sendParticles(ParticleTypes.SMOKE, Charge.x, Charge.y, Charge.z, 3, 0.25, 0.05, 0.25, 0.01);
        }
        if (Elapsed == UndergroundRules.PlantTicks / 2) {
            World.playSound(null, Charge.x, Charge.y, Charge.z, HostileRegistry.BreachPlant.get(), SoundSource.HOSTILE, 0.9f, 1.25f);
            World.playSound(null, Charge.x, Charge.y, Charge.z, GrenadeRegistry.PinClick.get(), SoundSource.HOSTILE, 0.8f, 0.9f);
        }
        if (Elapsed >= UndergroundRules.PlantTicks) {
            Tag.putByte("Phase", PhaseArmed);
            Tag.putLong("Since", Now);
            Current.changed();
            World.playSound(null, Charge.x, Charge.y, Charge.z, HostileRegistry.BreachBeep.get(), SoundSource.HOSTILE, 1.4f, 1.0f);
        }
    }

    public static void fail(ServerLevel World, Raid Current, String Why) {
        CompoundTag Tag = data(Current);
        BlockPos Entrance = entrance(Tag);
        Log.info("Raid {} breach failed ({}), removed {} blocks, falling back to {}", Current.id(), Why, Tag.getInt("Removed"), Entrance != null ? "the entrance" : "the surface");
        Tag.putByte("Phase", PhaseFailed);
        Tag.putByte("Mode", Entrance != null ? ModeEntrance : ModeNone);
        Current.changed();
    }

    public static void tick(ServerLevel World, Raid Current, long Now) {
        if (mode(Current) != ModeShaft) {
            return;
        }
        CompoundTag Tag = data(Current);
        byte Phase = Tag.getByte("Phase");
        if (Phase == PhaseArmed) {
            long Elapsed = Now - Tag.getLong("Since");
            int Every = Elapsed < 40 ? 20 : Elapsed < 64 ? 10 : 4;
            if (Elapsed % Every == 0) {
                Vec3 Charge = charge(Tag);
                float Pitch = 1.0f + Math.min(0.8f, Elapsed / (float) UndergroundRules.FuseTicks * 0.8f);
                World.playSound(null, Charge.x, Charge.y, Charge.z, HostileRegistry.BreachBeep.get(), SoundSource.HOSTILE, 1.2f, Pitch);
                World.sendParticles(Blink, Charge.x, Charge.y + 0.1, Charge.z, 2, 0.05, 0.05, 0.05, 0.0);
                if (Every <= 4) {
                    World.playSound(null, Charge.x, Charge.y, Charge.z, GrenadeRegistry.Fuse.get(), SoundSource.HOSTILE, 0.8f, 0.8f);
                }
            }
            if (Elapsed >= UndergroundRules.FuseTicks) {
                Tag.putByte("Phase", PhaseBlasting);
                Tag.putLong("Since", Now);
                Tag.putLong("Last", Now - UndergroundRules.BlastGap);
                Current.changed();
                Phase = PhaseBlasting;
            }
        }
        if (Phase == PhaseBlasting && Now - Tag.getLong("Last") >= UndergroundRules.BlastGap) {
            Tag.putLong("Last", Now);
            blast(World, Current, Tag);
        }
    }

    public static List<BlockPos> layer(ServerLevel World, CompoundTag Tag, int High, int Low) {
        List<BlockPos> Found = new ArrayList<>();
        BlockPos Corner = corner(Tag);
        int Width = UndergroundRules.ShaftWidth;
        for (int Y = High; Y >= Low; Y--) {
            for (int Dx = 0; Dx < Width; Dx++) {
                for (int Dz = 0; Dz < Width; Dz++) {
                    BlockPos Cell = new BlockPos(Corner.getX() + Dx, Y, Corner.getZ() + Dz);
                    if (blocking(World, Cell, World.getBlockState(Cell)) || !World.getFluidState(Cell).isEmpty()) {
                        Found.add(Cell);
                    }
                }
            }
        }
        return Found;
    }

    private static void blast(ServerLevel World, Raid Current, CompoundTag Tag) {
        int Floor = Tag.getInt("Floor");
        int Cursor = Tag.getInt("Cursor");
        int Low = Math.max(Floor, Cursor - UndergroundRules.LayersPerBlast + 1);
        boolean First = Cursor == Tag.getInt("Top");
        LivingEntity Breacher = breacher(World, Tag);
        if (!allowed(World, Breacher)) {
            fail(World, Current, "griefing is off");
            return;
        }
        List<BlockPos> Cells = layer(World, Tag, Cursor, Low);
        for (BlockPos Cell : Cells) {
            BlockState State = World.getBlockState(Cell);
            if (!State.getFluidState().isEmpty()) {
                fail(World, Current, "fluid in the shaft");
                return;
            }
            if (guarded(World, Cell, State, Breacher)) {
                fail(World, Current, "protected block");
                return;
            }
        }
        int Removed = Tag.getInt("Removed");
        if (Removed + Cells.size() > Tag.getInt("Cap")) {
            fail(World, Current, "block cap");
            return;
        }
        Vec3 Axis = axis(Tag);
        Vec3 Centre = new Vec3(Axis.x, (Cursor + Low) * 0.5 + 0.5, Axis.z);
        Explosion Boom = new Explosion(World, Breacher, Centre.x, Centre.y, Centre.z, BlastPower, false, Explosion.BlockInteraction.DESTROY, Cells);
        if (EventHooks.onExplosionStart(World, Boom)) {
            fail(World, Current, "explosion refused");
            return;
        }
        EventHooks.onExplosionDetonate(World, Boom, new ArrayList<>(), BlastPower * 2.0);
        if (Boom.getToBlow().size() < Cells.size()) {
            fail(World, Current, "protected area");
            return;
        }
        for (BlockPos Cell : Cells) {
            BlockState State = World.getBlockState(Cell);
            if (natural(State)) {
                World.removeBlock(Cell, false);
            } else {
                World.destroyBlock(Cell, true, Breacher);
            }
        }
        Tag.putInt("Removed", Removed + Cells.size());
        Tag.putInt("Cursor", Low - 1);
        Current.changed();
        effects(World, Centre, First);
        hurt(World, Centre, Breacher);
        if (Low <= Floor) {
            Tag.putByte("Phase", PhaseOpen);
            Current.changed();
            Log.info("Raid {} breach opened after removing {} blocks", Current.id(), Tag.getInt("Removed"));
            notice(World, Current, "breached");
        }
    }

    private static void effects(ServerLevel World, Vec3 Centre, boolean First) {
        if (First) {
            World.sendParticles(ParticleTypes.EXPLOSION_EMITTER, Centre.x, Centre.y + 1.0, Centre.z, 1, 0.0, 0.0, 0.0, 0.0);
            World.sendParticles(ParticleTypes.LARGE_SMOKE, Centre.x, Centre.y + 1.5, Centre.z, 30, 1.0, 1.2, 1.0, 0.05);
            World.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, Centre.x, Centre.y + 1.0, Centre.z, 8, 0.6, 0.4, 0.6, 0.02);
            World.playSound(null, Centre.x, Centre.y, Centre.z, GrenadeRegistry.FragBoom.get(), SoundSource.HOSTILE, 4.0f, 0.7f);
            World.playSound(null, Centre.x, Centre.y, Centre.z, GrenadeRegistry.FragCrack.get(), SoundSource.HOSTILE, 2.0f, 0.6f);
            World.playSound(null, Centre.x, Centre.y, Centre.z, GrenadeRegistry.FragDebris.get(), SoundSource.HOSTILE, 2.0f, 0.6f);
            return;
        }
        World.sendParticles(ParticleTypes.EXPLOSION, Centre.x, Centre.y, Centre.z, 3, 0.6, 0.6, 0.6, 0.0);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, Centre.x, Centre.y, Centre.z, 12, 0.7, 0.8, 0.7, 0.03);
        World.playSound(null, Centre.x, Centre.y, Centre.z, GrenadeRegistry.FragBoom.get(), SoundSource.HOSTILE, 2.5f, 0.8f + World.random.nextFloat() * 0.15f);
        World.playSound(null, Centre.x, Centre.y, Centre.z, GrenadeRegistry.FragDebris.get(), SoundSource.HOSTILE, 1.5f, 0.7f);
    }

    private static void hurt(ServerLevel World, Vec3 Centre, @Nullable LivingEntity Breacher) {
        double Reach = UndergroundRules.BlastHurt;
        DamageSource Source = World.damageSources().explosion(null, Breacher);
        for (LivingEntity Target : World.getEntitiesOfClass(LivingEntity.class, new AABB(Centre, Centre).inflate(Reach), Found -> Found.isAlive() && !(Found instanceof HostileRaider))) {
            Vec3 Aim = Target.getBoundingBox().getCenter();
            if (Aim.distanceToSqr(Centre) > Reach * Reach) {
                continue;
            }
            if (World.clip(new ClipContext(Centre, Aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Target)).getType() != HitResult.Type.MISS) {
                continue;
            }
            Target.hurt(Source, UndergroundRules.BlastDamage);
        }
    }
}
