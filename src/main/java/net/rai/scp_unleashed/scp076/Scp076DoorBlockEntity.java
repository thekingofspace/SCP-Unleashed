package net.rai.scp_unleashed.scp076;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.control.ControlManager;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;

public class Scp076DoorBlockEntity extends BlockEntity {
    public static final byte Sealed = 0;
    public static final byte Unlocking = 1;
    public static final byte Opening = 2;
    public static final byte Open = 3;
    public static final byte Closing = 4;
    public static final byte Locking = 5;
    public static final int LockCount = 7;
    public static final int LockLead = 12;
    public static final int LockStep = 11;
    public static final int LockMotion = 6;
    public static final int UnlockTicks = LockLead + LockCount * LockStep + 8;
    public static final int OpenTicks = 64;
    public static final int LidLead = 30;
    public static final int CloseTicks = 54;
    public static final int LockingTicks = LockLead + LockCount * LockStep + 4;
    public static final int GrindEvery = 16;
    public static final int PendingCheck = 20;
    public static final float SoundRange = 1.6f;
    public static final int[] UnlockOrder = {0, 1, 2, 3, 4, 5, 6};
    public static final int[] LockOrder = {6, 5, 4, 3, 2, 1, 0};
    public static final int[][] Locks = {{24, 56, 2}, {8, 50, 0}, {40, 50, 1}, {8, 32, 0}, {40, 32, 1}, {8, 14, 0}, {40, 14, 1}};
    public static final DustParticleOptions Ash = new DustParticleOptions(new Vector3f(0.09f, 0.09f, 0.1f), 1.4f);
    public static final DustParticleOptions Ember = new DustParticleOptions(new Vector3f(0.62f, 0.06f, 0.05f), 0.8f);

    private byte Phase = Sealed;
    private long PhaseStart;
    private long SealedUntil;
    private boolean Released;
    @Nullable
    private UUID AbelId;
    private long NextSeal = Scp076Vault.KilledSeal;
    @Nullable
    private BlockPos CoffinPos;

    public Scp076DoorBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp076Registry.DoorEntity.get(), Pos, State);
    }

    public byte phase() {
        return Phase;
    }

    public boolean released() {
        return Released;
    }

    @Nullable
    public UUID abelId() {
        return AbelId;
    }

    public boolean holds() {
        return level != null && Phase == Sealed && level.getGameTime() < SealedUntil;
    }

    public long sealLeft() {
        return level == null || Phase != Sealed ? 0L : Math.max(0L, SealedUntil - level.getGameTime());
    }

    public Direction front() {
        BlockState State = getBlockState();
        return State.hasProperty(Scp076DoorBlock.Facing) ? State.getValue(Scp076DoorBlock.Facing) : Direction.SOUTH;
    }

    public BlockPos centre() {
        return Scp076Vault.centreFromDoor(worldPosition, front());
    }

    public void bindCoffin(BlockPos Pos) {
        CoffinPos = Pos.immutable();
        setChanged();
    }

    public double elapsed(float Partial) {
        return level == null ? 0.0 : level.getGameTime() - PhaseStart + Partial;
    }

    private static float ease(double Value) {
        float Clamped = (float) Mth.clamp(Value, 0.0, 1.0);
        return Clamped * Clamped * (3.0f - 2.0f * Clamped);
    }

    private static int orderIndex(int[] Order, int Lock) {
        for (int Index = 0; Index < Order.length; Index++) {
            if (Order[Index] == Lock) {
                return Index;
            }
        }
        return 0;
    }

    public float engaged(int Lock, float Partial) {
        double Time = elapsed(Partial);
        return switch (Phase) {
            case Unlocking -> 1.0f - ease((Time - LockLead - orderIndex(UnlockOrder, Lock) * LockStep) / LockMotion);
            case Opening, Open, Closing -> 0.0f;
            case Locking -> ease((Time - LockLead - orderIndex(LockOrder, Lock) * LockStep) / LockMotion);
            default -> 1.0f;
        };
    }

    public float glow(float Partial) {
        float Total = 0.0f;
        for (int Lock = 0; Lock < LockCount; Lock++) {
            Total += engaged(Lock, Partial);
        }
        return Total / LockCount;
    }

    public float drop(float Partial) {
        double Time = elapsed(Partial);
        return switch (Phase) {
            case Opening -> ease(Time / OpenTicks);
            case Open -> 1.0f;
            case Closing -> 1.0f - ease(Time / CloseTicks);
            default -> 0.0f;
        };
    }

    public AABB renderBounds() {
        return new AABB(worldPosition).inflate(4.5, 5.5, 4.5);
    }

    private void setPhase(byte Value) {
        Phase = Value;
        PhaseStart = level == null ? 0L : level.getGameTime();
        setChanged();
        sync();
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public void use(ServerPlayer Visitor) {
        if (!(level instanceof ServerLevel World)) {
            return;
        }
        switch (Phase) {
            case Sealed -> {
                if (World.getGameTime() < SealedUntil) {
                    Visitor.displayClientMessage(Component.translatable("block.scp_unleashed.scp076_vault_door.sealed").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                    cue(World, Scp076Registry.LockRattle, 0.7f, 0.8f + World.random.nextFloat() * 0.15f);
                    return;
                }
                Released = false;
                AbelId = null;
                NextSeal = Scp076Vault.KilledSeal;
                setPhase(Unlocking);
                Visitor.displayClientMessage(Component.translatable("block.scp_unleashed.scp076_vault_door.yields").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
                cue(World, Scp076Registry.LockRattle, 1.0f, 0.7f);
                cue(World, Scp076Registry.DoorRumble, 0.7f, 0.6f);
            }
            case Unlocking, Opening -> Visitor.displayClientMessage(Component.translatable("block.scp_unleashed.scp076_vault_door.turning")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            default -> Visitor.displayClientMessage(Component.translatable("block.scp_unleashed.scp076_vault_door.closing")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
    }

    public void reseal(long Ticks) {
        if (level == null) {
            return;
        }
        switch (Phase) {
            case Open -> {
                NextSeal = Ticks;
                AbelId = null;
                setPhase(Closing);
                if (level instanceof ServerLevel World) {
                    cue(World, Scp076Registry.Seal, 1.4f, 0.8f);
                    cue(World, Scp076Registry.SealLow, 1.2f, 0.7f);
                    Scp076CoffinBlockEntity Coffin = coffin(World);
                    if (Coffin != null) {
                        Coffin.closeLid();
                    }
                }
            }
            case Closing, Locking -> {
                NextSeal = Math.max(NextSeal, Ticks);
                setChanged();
            }
            case Sealed -> {
                SealedUntil = Math.max(SealedUntil, level.getGameTime() + Ticks);
                setChanged();
            }
            default -> {
            }
        }
    }

    public void unseal() {
        SealedUntil = 0L;
        setChanged();
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel World)) {
            return;
        }
        long Now = World.getGameTime();
        long Time = Now - PhaseStart;
        if ((Now + worldPosition.asLong()) % PendingCheck == 0) {
            OptionalLong Waiting = Scp076Vaults.get(World.getServer()).takeReseal(World.dimension(), worldPosition);
            if (Waiting.isPresent()) {
                reseal(Waiting.getAsLong());
                return;
            }
        }
        switch (Phase) {
            case Unlocking -> {
                for (int Index = 0; Index < LockCount; Index++) {
                    if (Time == LockLead + (long) Index * LockStep) {
                        lockCue(World, UnlockOrder[Index], false);
                    }
                }
                if (Time >= UnlockTicks) {
                    setPhase(Opening);
                    grind(World);
                }
            }
            case Opening -> {
                if (Time % GrindEvery == 0) {
                    grind(World);
                }
                if (Time % 3 == 0) {
                    dust(World);
                }
                if (Time == OpenTicks - LidLead) {
                    Scp076CoffinBlockEntity Coffin = coffin(World);
                    if (Coffin != null) {
                        Coffin.openLid();
                    }
                }
                if (Time >= OpenTicks) {
                    Scp076DoorBlock.setOpen(World, worldPosition, front(), true);
                    setPhase(Open);
                    cue(World, Scp076Registry.DoorBoom, 1.2f, 0.65f);
                    release(World);
                }
            }
            case Open -> {
                if (!Released && Time % 20 == 0) {
                    release(World);
                }
            }
            case Closing -> {
                if (Time % GrindEvery == 0) {
                    grind(World);
                }
                if (Time % 3 == 0) {
                    dust(World);
                }
                pushDoorway(World);
                if (Time >= CloseTicks) {
                    evictDoorway(World);
                    Scp076DoorBlock.setOpen(World, worldPosition, front(), false);
                    cue(World, Scp076Registry.DoorShut, 1.4f, 0.6f);
                    cue(World, Scp076Registry.DoorBoom, 1.2f, 0.55f);
                    sealKill(World);
                    setPhase(Locking);
                }
            }
            case Locking -> {
                for (int Index = 0; Index < LockCount; Index++) {
                    if (Time == LockLead + (long) Index * LockStep) {
                        lockCue(World, LockOrder[Index], true);
                    }
                }
                if (Time >= LockingTicks) {
                    SealedUntil = Now + NextSeal;
                    setPhase(Sealed);
                }
            }
            default -> {
            }
        }
    }

    public Vec3 faceCentre() {
        Direction Front = front();
        return lockPoint(Front, 24.0, 32.0, 0.1);
    }

    public Vec3 lockPoint(Direction Front, double U, double V, double Out) {
        Direction Across = Scp076DoorBlock.across(Front);
        Vec3 Base = new Vec3(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5);
        return Base.add(Front.getStepX() * (0.5 + Out), V / 16.0, Front.getStepZ() * (0.5 + Out)).add(Across.getStepX() * (U / 16.0 - 0.5), 0.0, Across.getStepZ() * (U / 16.0 - 0.5));
    }

    private void cue(ServerLevel World, DeferredHolder<SoundEvent, SoundEvent> Sound, float Volume, float Pitch) {
        Vec3 Where = faceCentre();
        World.playSound(null, Where.x, Where.y, Where.z, Sound.get(), SoundSource.BLOCKS, Volume * SoundRange, Pitch);
    }

    private void lockCue(ServerLevel World, int Lock, boolean Engaging) {
        int[] Spot = Locks[Lock];
        Vec3 Where = lockPoint(front(), Spot[0], Spot[1], 0.15);
        float Pitch = (Engaging ? 0.62f : 0.72f) + Lock * 0.025f;
        World.playSound(null, Where.x, Where.y, Where.z, Scp076Registry.LockTurn.get(), SoundSource.BLOCKS, 1.3f, Pitch);
        World.playSound(null, Where.x, Where.y, Where.z, Scp076Registry.LockClunk.get(), SoundSource.BLOCKS, 1.1f, Pitch * 0.85f);
        World.sendParticles(Ash, Where.x, Where.y, Where.z, 6, 0.12, 0.12, 0.05, 0.01);
        if (!Engaging) {
            World.sendParticles(Ember, Where.x, Where.y, Where.z, 4, 0.08, 0.08, 0.04, 0.0);
        }
    }

    private void grind(ServerLevel World) {
        cue(World, Scp076Registry.DoorGrind, 1.1f, 0.55f + World.random.nextFloat() * 0.08f);
        cue(World, Scp076Registry.DoorRumble, 0.9f, 0.5f);
    }

    private void dust(ServerLevel World) {
        Direction Front = front();
        BlockState Stone = Scp076Registry.VaultStone.get().defaultBlockState();
        for (int Step = 0; Step < 3; Step++) {
            double U = 4.0 + World.random.nextDouble() * 40.0;
            Vec3 Low = lockPoint(Front, U, 0.5, 0.2);
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Stone), Low.x, Low.y, Low.z, 2, 0.05, 0.05, 0.05, 0.05);
            World.sendParticles(Ash, Low.x, Low.y + 0.2, Low.z, 1, 0.1, 0.1, 0.1, 0.01);
        }
    }

    @Nullable
    public Scp076CoffinBlockEntity coffin(ServerLevel World) {
        if (CoffinPos != null && World.isLoaded(CoffinPos) && World.getBlockEntity(CoffinPos) instanceof Scp076CoffinBlockEntity Found) {
            return Found;
        }
        AABB Box = Scp076Vault.interior(centre());
        for (BlockPos Cell : BlockPos.betweenClosed(BlockPos.containing(Box.minX, Box.minY, Box.minZ), BlockPos.containing(Box.maxX - 1, Box.maxY - 1, Box.maxZ - 1))) {
            if (World.isLoaded(Cell) && World.getBlockEntity(Cell) instanceof Scp076CoffinBlockEntity Found) {
                CoffinPos = Cell.immutable();
                setChanged();
                return Found;
            }
        }
        return null;
    }

    private void release(ServerLevel World) {
        if (Released) {
            return;
        }
        Direction Front = front();
        Scp076CoffinBlockEntity Coffin = coffin(World);
        Vec3 Spot = Coffin != null ? Coffin.restingPoint() : Vec3.atBottomCenterOf(centre().above());
        Scp076Abel Abel = Scp076Registry.Abel.get().create(World);
        if (Abel == null) {
            return;
        }
        float Yaw = Front.toYRot();
        Abel.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Abel.setYHeadRot(Yaw);
        Abel.setYBodyRot(Yaw);
        Abel.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.TRIGGERED, null);
        Abel.setPersistenceRequired();
        Abel.setHome(World.dimension(), worldPosition, Coffin != null ? Coffin.getBlockPos() : null, Front);
        Abel.beginRise();
        if (World.addFreshEntity(Abel)) {
            Released = true;
            AbelId = Abel.getUUID();
            setChanged();
            sync();
            World.playSound(null, Spot.x, Spot.y, Spot.z, Scp076Registry.Rise.get(), SoundSource.HOSTILE, 1.6f, 0.8f);
            World.playSound(null, Spot.x, Spot.y, Spot.z, Scp076Registry.RiseLow.get(), SoundSource.HOSTILE, 1.4f, 0.6f);
        }
    }

    private void pushDoorway(ServerLevel World) {
        Direction Front = front();
        for (Entity Found : World.getEntitiesOfClass(Entity.class, Scp076Vault.doorway(worldPosition, Front), Entity::isAlive)) {
            Found.push(Front.getStepX() * 0.08, 0.0, Front.getStepZ() * 0.08);
            Found.hurtMarked = true;
        }
    }

    private void evictDoorway(ServerLevel World) {
        Direction Front = front();
        for (Entity Found : World.getEntitiesOfClass(Entity.class, Scp076Vault.doorway(worldPosition, Front), Entity::isAlive)) {
            Vec3 At = Found.position();
            double Depth = (At.x - (worldPosition.getX() + 0.5)) * Front.getStepX() + (At.z - (worldPosition.getZ() + 0.5)) * Front.getStepZ();
            double Shift = 0.5 + Found.getBbWidth() / 2.0 + 0.05 - Depth;
            if (Shift > 0.0) {
                Found.teleportTo(At.x + Front.getStepX() * Shift, At.y, At.z + Front.getStepZ() * Shift);
            }
        }
    }

    private void sealKill(ServerLevel World) {
        AABB Box = Scp076Vault.interior(centre());
        List<LivingEntity> Inside = World.getEntitiesOfClass(LivingEntity.class, Box, Body -> Body.isAlive() && !(Body instanceof Scp076Abel));
        DamageSource Source = World.damageSources().source(Scp076Registry.SealedDamage);
        for (LivingEntity Body : Inside) {
            if (Body instanceof Player User && (User.isCreative() || User.isSpectator())) {
                continue;
            }
            Vec3 At = Body.position();
            Body.invulnerableTime = 0;
            Body.hurt(Source, Float.MAX_VALUE);
            if (Body instanceof ServerPlayer Controller && ControlManager.hasPendingDeath(Controller)) {
                ControlManager.processDeath(Controller);
            }
            World.sendParticles(Ash, At.x, At.y + 1.0, At.z, 24, 0.3, 0.6, 0.3, 0.02);
            World.sendParticles(ParticleTypes.LARGE_SMOKE, At.x, At.y + 0.8, At.z, 6, 0.25, 0.4, 0.25, 0.01);
        }
        Vec3 Middle = Box.getCenter();
        World.playSound(null, Middle.x, Middle.y, Middle.z, Scp076Registry.SealLow.get(), SoundSource.BLOCKS, 1.5f, 0.55f);
    }

    @Override
    protected void saveAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.saveAdditional(Saved, Registries);
        Saved.putByte("Phase", Phase);
        Saved.putLong("PhaseStart", PhaseStart);
        Saved.putLong("SealedUntil", SealedUntil);
        Saved.putBoolean("Released", Released);
        Saved.putLong("NextSeal", NextSeal);
        if (AbelId != null) {
            Saved.putUUID("Abel", AbelId);
        }
        if (CoffinPos != null) {
            Saved.put("Coffin", NbtUtils.writeBlockPos(CoffinPos));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.loadAdditional(Saved, Registries);
        Phase = Saved.getByte("Phase");
        PhaseStart = Saved.getLong("PhaseStart");
        SealedUntil = Saved.getLong("SealedUntil");
        Released = Saved.getBoolean("Released");
        NextSeal = Saved.contains("NextSeal") ? Saved.getLong("NextSeal") : Scp076Vault.KilledSeal;
        AbelId = Saved.hasUUID("Abel") ? Saved.getUUID("Abel") : null;
        CoffinPos = NbtUtils.readBlockPos(Saved, "Coffin").orElse(null);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Saved = new CompoundTag();
        Saved.putByte("Phase", Phase);
        Saved.putLong("PhaseStart", PhaseStart);
        Saved.putBoolean("Released", Released);
        return Saved;
    }

    @Override
    public void handleUpdateTag(CompoundTag Saved, HolderLookup.Provider Registries) {
        Phase = Saved.getByte("Phase");
        PhaseStart = Saved.getLong("PhaseStart");
        Released = Saved.getBoolean("Released");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
