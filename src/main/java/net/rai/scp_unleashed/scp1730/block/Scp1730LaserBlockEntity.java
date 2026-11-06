package net.rai.scp_unleashed.scp1730.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.rai.scp_unleashed.scp1730.Scp1730Power;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;

public class Scp1730LaserBlockEntity extends BlockEntity {
    public static final double Reach = 12.0;
    public static final int CheckEvery = 4;
    public static final int AlertTicks = 200;
    public static final int AlertRadius = 20;
    public static final int RearmTicks = 60;
    public static final double LensHeight = 9.0 / 16.0;
    public static final double LensBack = 2.5 / 16.0;
    public static final double LensToFace = 10.5 / 16.0;
    public static final double Thickness = 0.06;
    public static final float MaxHealth = 12.0f;
    private final Scp1730Power.Cache Power = new Scp1730Power.Cache();
    private boolean Tripped;
    private int Rearm;
    private float Health = MaxHealth;
    public double Beam = -1.0;

    public Scp1730LaserBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp1730Registry.LaserEntity.get(), Pos, State);
    }

    public Direction facing() {
        return getBlockState().getValue(Scp1730LaserBlock.Facing);
    }

    public boolean broken() {
        return getBlockState().getValue(Scp1730LaserBlock.Broken);
    }

    public float health() {
        return Health;
    }

    public static Vec3 lens(BlockPos Pos, Direction Side) {
        return new Vec3(Pos.getX() + 0.5 - Side.getStepX() * LensBack, Pos.getY() + LensHeight, Pos.getZ() + 0.5 - Side.getStepZ() * LensBack);
    }

    public Vec3 lens() {
        return lens(worldPosition, facing());
    }

    public static double measure(Level World, BlockPos Pos, Direction Side) {
        Vec3 From = lens(Pos, Side);
        Vec3 Start = From.add(Side.getStepX() * (LensToFace + 1.0e-4), 0.0, Side.getStepZ() * (LensToFace + 1.0e-4));
        Vec3 To = From.add(Side.getStepX() * Reach, 0.0, Side.getStepZ() * Reach);
        BlockHitResult Hit = World.clip(new ClipContext(Start, To, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        if (Hit.getType() == HitResult.Type.MISS) {
            return Reach;
        }
        return Math.min(Reach, Math.abs((Hit.getLocation().x - From.x) * Side.getStepX() + (Hit.getLocation().z - From.z) * Side.getStepZ()));
    }

    public double measure(Level World) {
        return measure(World, worldPosition, facing());
    }

    public Vec3 beamEnd(double Length) {
        Direction Side = facing();
        return lens().add(Side.getStepX() * Length, 0.0, Side.getStepZ() * Length);
    }

    public AABB beamBox(double Length) {
        return new AABB(lens(), beamEnd(Length)).inflate(Thickness);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp1730LaserBlockEntity Laser) {
        if (!(World instanceof ServerLevel Server) || (World.getGameTime() + Pos.asLong()) % CheckEvery != 0) {
            return;
        }
        if (State.getValue(Scp1730LaserBlock.Broken)) {
            Laser.Tripped = false;
            return;
        }
        boolean On = Laser.Power.powered(World, Pos);
        if (State.getValue(Scp1730LaserBlock.Powered) != On) {
            World.setBlock(Pos, State.setValue(Scp1730LaserBlock.Powered, On), Block.UPDATE_CLIENTS);
        }
        if (Laser.Rearm > 0) {
            Laser.Rearm -= CheckEvery;
        }
        if (!On) {
            Laser.Tripped = false;
            return;
        }
        double Length = Laser.measure(World);
        Laser.Beam = Length;
        boolean Crossed = !World.getEntitiesOfClass(Player.class, Laser.beamBox(Length), Each -> Each.isAlive() && !Each.isSpectator() && !Each.isCreative()).isEmpty();
        if (Crossed && !Laser.Tripped && Laser.Rearm <= 0) {
            Laser.trip(Server);
        }
        Laser.Tripped = Crossed;
    }

    public void damage(ServerLevel World, float Amount, Vec3 Point) {
        if (broken() || Amount <= 0.0f) {
            return;
        }
        Health -= Amount;
        setChanged();
        World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        World.sendParticles(ParticleTypes.ELECTRIC_SPARK, Point.x, Point.y, Point.z, 4, 0.05, 0.05, 0.05, 0.08);
        World.playSound(null, worldPosition, Scp1730Registry.TurretHit.get(), SoundSource.BLOCKS, 0.5f, 1.4f + World.random.nextFloat() * 0.2f);
        if (Health <= 0.0f) {
            disable(World);
        }
    }

    public void disable(ServerLevel World) {
        if (broken()) {
            return;
        }
        Health = 0.0f;
        Tripped = false;
        World.setBlock(worldPosition, getBlockState().setValue(Scp1730LaserBlock.Broken, true).setValue(Scp1730LaserBlock.Powered, false), Block.UPDATE_ALL);
        setChanged();
        Vec3 Lens = lens();
        World.sendParticles(ParticleTypes.ELECTRIC_SPARK, Lens.x, Lens.y, Lens.z, 14, 0.12, 0.12, 0.12, 0.2);
        World.sendParticles(ParticleTypes.SMOKE, Lens.x, Lens.y + 0.1, Lens.z, 6, 0.08, 0.05, 0.08, 0.02);
        World.playSound(null, worldPosition, Scp1730Registry.EmitterBreak.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
        World.playSound(null, worldPosition, Scp1730Registry.EmitterFizz.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
    }

    public void trip(ServerLevel World) {
        Rearm = RearmTicks;
        World.playSound(null, worldPosition, Scp1730Registry.LaserTrip.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        World.playSound(null, worldPosition, Scp1730Registry.Alarm.get(), SoundSource.BLOCKS, 1.4f, 1.0f);
        ChunkPos Centre = new ChunkPos(worldPosition);
        int Span = (AlertRadius >> 4) + 1;
        for (int Dx = -Span; Dx <= Span; Dx++) {
            for (int Dz = -Span; Dz <= Span; Dz++) {
                if (!World.hasChunk(Centre.x + Dx, Centre.z + Dz)) {
                    continue;
                }
                LevelChunk Chunk = World.getChunk(Centre.x + Dx, Centre.z + Dz);
                for (BlockEntity Each : Chunk.getBlockEntities().values()) {
                    if (Each instanceof Scp1730TurretBlockEntity Turret && Each.getBlockPos().distSqr(worldPosition) <= AlertRadius * AlertRadius) {
                        Turret.alert(AlertTicks);
                    }
                }
            }
        }
    }

    public boolean tripped() {
        return Tripped;
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, Scp1730LaserBlockEntity Laser) {
        if (Laser.Beam < 0.0 || World.getGameTime() % 5 == Math.floorMod(Pos.asLong(), 5)) {
            Laser.Beam = Laser.measure(World);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Lookup) {
        super.saveAdditional(Tag, Lookup);
        Tag.putFloat("Health", Health);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Lookup) {
        super.loadAdditional(Tag, Lookup);
        Health = Tag.contains("Health") ? Tag.getFloat("Health") : MaxHealth;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Lookup) {
        CompoundTag Tag = super.getUpdateTag(Lookup);
        Tag.putFloat("Health", Health);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
