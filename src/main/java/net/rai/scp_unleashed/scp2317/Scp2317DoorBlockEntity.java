package net.rai.scp_unleashed.scp2317;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public class Scp2317DoorBlockEntity extends BlockEntity {
    public static final int RattleEvent = 1;
    public static final int CheckEvery = 20;
    public static final int SwingTicks = 16;
    public static final int RattleTicks = 14;
    public static final int HumEvery = 90;
    private static final Set<Scp2317DoorBlockEntity> Tracked = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private boolean Opened;
    private boolean Sealed;
    private boolean Unlocked;
    private float Progress;
    private float ProgressO;
    private int Rattle;

    public Scp2317DoorBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp2317Registry.DoorEntity.get(), Pos, State);
        if (far()) {
            Opened = true;
            Progress = 1.0f;
            ProgressO = 1.0f;
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            Tracked.add(this);
        }
    }

    @Override
    public void setRemoved() {
        Tracked.remove(this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        Tracked.remove(this);
        super.onChunkUnloaded();
    }

    public static void snapped(MinecraftServer Server, int Broken) {
        List<Scp2317DoorBlockEntity> Doors;
        synchronized (Tracked) {
            Doors = new ArrayList<>(Tracked);
        }
        for (Scp2317DoorBlockEntity Frame : Doors) {
            if (Frame.isRemoved() || !(Frame.level instanceof ServerLevel World) || World.getServer() != Server) {
                continue;
            }
            Scp2317Sounds.play(World, Frame.worldPosition.above(), Scp2317Registry.RattleLayers, 1.0f, 0.6f);
            World.blockEvent(Frame.worldPosition, Frame.getBlockState().getBlock(), RattleEvent, 0);
        }
    }

    public boolean far() {
        return getBlockState().getBlock() instanceof Scp2317DoorBlock Door && Door.far();
    }

    public boolean opened() {
        return Opened;
    }

    public boolean unlocked() {
        return Unlocked;
    }

    public boolean sealed() {
        if (level != null && !level.isClientSide && level.getServer() != null) {
            return !far() && Scp2317World.occupied(level.getServer());
        }
        return Sealed;
    }

    public Direction facing() {
        return getBlockState().getValue(Scp2317DoorBlock.Facing);
    }

    public float progress(float Partial) {
        return Mth.lerp(Partial, ProgressO, Progress);
    }

    public float rattle(float Partial) {
        return Math.max(0.0f, Rattle - Partial) / RattleTicks;
    }

    public static boolean read(MinecraftServer Server) {
        return ScarletState.get(Server).read();
    }

    public void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public void use(Player User) {
        if (!(level instanceof ServerLevel World) || far()) {
            return;
        }
        if (!read(World.getServer())) {
            Scp2317Sounds.play(World, worldPosition.above(), Scp2317Registry.RattleLayers, 0.8f, 0.9f + World.getRandom().nextFloat() * 0.15f);
            World.blockEvent(worldPosition, getBlockState().getBlock(), RattleEvent, 0);
            Scp2317DoorBlock.tell(User, "locked");
            return;
        }
        setOpened(!Opened);
    }

    public void setOpened(boolean Value) {
        if (Opened == Value || far()) {
            return;
        }
        Opened = Value;
        changed();
        if (level instanceof ServerLevel World) {
            Scp2317DoorBlock.setOpen(World, worldPosition, Value);
            Scp2317Sounds.play(World, worldPosition.above(), Value ? Scp2317Registry.OpenLayers : Scp2317Registry.ShutLayers, 0.9f, 0.9f);
        }
    }

    @Override
    public boolean triggerEvent(int Id, int Param) {
        if (Id == RattleEvent) {
            Rattle = RattleTicks;
            return true;
        }
        return super.triggerEvent(Id, Param);
    }

    public AABB field() {
        return Scp2317DoorBlock.field(worldPosition, facing());
    }

    public Direction sideOf(Vec3 Point) {
        Direction Front = facing();
        Vec3 Centre = Scp2317DoorBlock.world(worldPosition, Front, 1.0, 1.0, 0.5);
        double Along = (Point.x - Centre.x) * Front.getStepX() + (Point.z - Centre.z) * Front.getStepZ();
        return Along >= 0.0 ? Front : Front.getOpposite();
    }

    public Vec3 front(Direction Side, double Distance) {
        Vec3 Centre = Scp2317DoorBlock.world(worldPosition, facing(), 1.0, 0.0, 0.5);
        return Centre.add(Side.getStepX() * Distance, 0.0, Side.getStepZ() * Distance);
    }

    public AABB renderBounds() {
        return new AABB(worldPosition).inflate(2.5, 0.0, 2.5).expandTowards(0.0, 3.0, 0.0);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp2317DoorBlockEntity Frame) {
        if (!(World instanceof ServerLevel Server)) {
            return;
        }
        if (Server.getGameTime() % CheckEvery == 0) {
            boolean NowSealed = Frame.sealed();
            boolean NowUnlocked = Frame.far() || read(Server.getServer());
            if (NowSealed != Frame.Sealed || NowUnlocked != Frame.Unlocked) {
                Frame.Sealed = NowSealed;
                Frame.Unlocked = NowUnlocked;
                Frame.changed();
            }
            if (Frame.far() && !State.getValue(Scp2317DoorBlock.Open)) {
                Scp2317DoorBlock.setOpen(Server, Pos, true);
            }
        }
        if (!Frame.Opened) {
            return;
        }
        if (Server.getGameTime() % HumEvery == 0) {
            Vec3 Centre = Scp2317DoorBlock.world(Pos, Frame.facing(), 1.0, 1.2, 0.5);
            Server.playSound(null, Centre.x, Centre.y, Centre.z, Scp2317Registry.Hum.get(), SoundSource.BLOCKS, 0.35f, 1.0f);
        }
        List<ServerPlayer> Inside = new ArrayList<>();
        for (Player Found : Server.getEntitiesOfClass(Player.class, Frame.field())) {
            if (Found instanceof ServerPlayer User && User.isAlive() && !User.isSpectator()) {
                Inside.add(User);
            }
        }
        for (ServerPlayer User : Inside) {
            if (Frame.far()) {
                Scp2317Travel.leave(User);
            } else {
                Scp2317Travel.enter(User, Frame, new Vec3(User.xo, User.yo, User.zo));
            }
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, Scp2317DoorBlockEntity Frame) {
        Frame.ProgressO = Frame.Progress;
        float Goal = Frame.Opened ? 1.0f : 0.0f;
        Frame.Progress = Mth.clamp(Frame.Progress + Mth.clamp(Goal - Frame.Progress, -1.0f / SwingTicks, 1.0f / SwingTicks), 0.0f, 1.0f);
        if (Frame.Rattle > 0) {
            Frame.Rattle--;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Saved, HolderLookup.Provider Lookup) {
        super.saveAdditional(Saved, Lookup);
        Saved.putBoolean("Opened", Opened);
        Saved.putBoolean("Sealed", Sealed);
        Saved.putBoolean("Unlocked", Unlocked);
    }

    @Override
    protected void loadAdditional(CompoundTag Saved, HolderLookup.Provider Lookup) {
        super.loadAdditional(Saved, Lookup);
        Opened = Saved.getBoolean("Opened") || far();
        Sealed = Saved.getBoolean("Sealed");
        Unlocked = Saved.getBoolean("Unlocked");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Lookup) {
        CompoundTag Saved = new CompoundTag();
        saveAdditional(Saved, Lookup);
        return Saved;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
