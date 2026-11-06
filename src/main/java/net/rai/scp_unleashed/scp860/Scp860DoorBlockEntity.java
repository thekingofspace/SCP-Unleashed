package net.rai.scp_unleashed.scp860;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class Scp860DoorBlockEntity extends BlockEntity {
    public static final int Entry = 0;
    public static final int Exit = 1;
    public static final int Arrival = 2;
    public static final int SealCheck = 20;
    public static final int SwingTicks = 12;
    public static final int HumEvery = 80;
    public static final double FieldDepth = 0.2;
    public static final double FieldInset = 2.5 / 16.0;
    public static final double ExitOpenReach = 4.5;
    public static final double ExitCloseReach = 6.0;
    private ItemStack Key = ItemStack.EMPTY;
    private boolean Opened;
    private boolean Sealed;
    private int Mode = Entry;
    private float Progress;
    private float ProgressO;
    private boolean Tidied;

    public Scp860DoorBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp860Registry.DoorEntity.get(), Pos, State);
        if (State.getBlock() instanceof Scp860DoorBlock Door && Door.far()) {
            Mode = Exit;
        }
    }

    public boolean keyed() {
        return !Key.isEmpty();
    }

    public ItemStack key() {
        return Key;
    }

    public boolean opened() {
        return Opened;
    }

    public int mode() {
        return Mode;
    }

    public boolean far() {
        return Mode != Entry;
    }

    public boolean sealed() {
        if (level != null && !level.isClientSide && level.getServer() != null) {
            return Mode == Entry && keyed() && Scp860Forest.occupied(level.getServer());
        }
        return Sealed;
    }

    public void setMode(int Value) {
        if (Mode != Value) {
            Mode = Value;
            changed();
        }
    }

    public Direction facing() {
        return getBlockState().getValue(Scp860DoorBlock.Facing);
    }

    public float progress(float Partial) {
        return Mth.lerp(Partial, ProgressO, Progress);
    }

    public void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public void insert(ItemStack Stack, Player User) {
        if (Mode != Entry || keyed() || Stack.isEmpty()) {
            return;
        }
        Key = Stack;
        changed();
        if (level instanceof ServerLevel World) {
            Scp860Sounds.play(World, worldPosition.above(), Scp860Registry.KeyInLayers, 0.8f, 1.0f);
        }
    }

    public void use(Player User) {
        if (!(level instanceof ServerLevel World)) {
            return;
        }
        if (Mode != Entry) {
            Scp860Sounds.play(World, worldPosition.above(), Scp860Registry.RattleLayers, 0.6f, 0.9f);
            return;
        }
        if (!keyed()) {
            Scp860Sounds.play(World, worldPosition.above(), Scp860Registry.RattleLayers, 0.8f, 1.0f);
            Scp860DoorBlock.tell(User, "locked");
            return;
        }
        if (User.isSecondaryUseActive() && User.getMainHandItem().isEmpty()) {
            takeKey(User);
            return;
        }
        setOpened(!Opened);
    }

    public boolean takeKey(Player User) {
        if (!(level instanceof ServerLevel World) || !keyed()) {
            return false;
        }
        if (sealed()) {
            Scp860Sounds.play(World, worldPosition.above(), Scp860Registry.RattleLayers, 0.5f, 1.3f);
            Scp860DoorBlock.tell(User, "key_stuck");
            return false;
        }
        ItemStack Taken = Key;
        Key = ItemStack.EMPTY;
        setOpened(false);
        changed();
        if (!User.getInventory().add(Taken)) {
            User.drop(Taken, false);
        }
        Scp860Sounds.play(World, worldPosition.above(), Scp860Registry.KeyOutLayers, 0.8f, 1.0f);
        return true;
    }

    public void setOpened(boolean Value) {
        if (Opened == Value) {
            return;
        }
        if (Value && Mode == Entry && !keyed()) {
            return;
        }
        Opened = Value;
        changed();
        if (level instanceof ServerLevel World) {
            Scp860DoorBlock.setOpen(World, worldPosition, Value);
            Scp860Sounds.play(World, worldPosition.above(), Value ? Scp860Registry.OpenLayers : Scp860Registry.ShutLayers, 0.9f, 1.0f);
            ServerLevel Forest = Scp860Forest.level(World.getServer());
            if (Value && Mode == Entry && Forest != null) {
                Scp860Builder.ensure(Forest);
            }
        }
    }

    public void dropKey() {
        if (level != null && !level.isClientSide && keyed()) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, Key);
            Key = ItemStack.EMPTY;
        }
    }

    public AABB field() {
        Direction Front = facing();
        double X = worldPosition.getX();
        double Y = worldPosition.getY();
        double Z = worldPosition.getZ();
        if (Front.getAxis() == Direction.Axis.Z) {
            return new AABB(X + FieldInset, Y, Z + 0.5 - FieldDepth, X + 1.0 - FieldInset, Y + 2.0, Z + 0.5 + FieldDepth);
        }
        return new AABB(X + 0.5 - FieldDepth, Y, Z + FieldInset, X + 0.5 + FieldDepth, Y + 2.0, Z + 1.0 - FieldInset);
    }

    public Direction sideOf(Vec3 Point) {
        Direction Front = facing();
        Vec3 Centre = Vec3.atCenterOf(worldPosition);
        double Along = (Point.x - Centre.x) * Front.getStepX() + (Point.z - Centre.z) * Front.getStepZ();
        return Along >= 0.0 ? Front : Front.getOpposite();
    }

    public AABB renderBounds() {
        return new AABB(worldPosition).inflate(1.0, 0.0, 1.0).expandTowards(0.0, 2.0, 0.0);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp860DoorBlockEntity Frame) {
        if (!(World instanceof ServerLevel Server)) {
            return;
        }
        if (!Frame.Tidied) {
            Frame.Tidied = Scp860DoorBlock.tidyLegacy(Server, Pos, State.getValue(Scp860DoorBlock.Facing));
        }
        if (Server.getGameTime() % SealCheck == 0) {
            boolean Now = Frame.sealed();
            if (Now != Frame.Sealed) {
                Frame.Sealed = Now;
                Frame.changed();
            }
        }
        if (Frame.Mode == Exit) {
            Frame.exitTick(Server);
        }
        if (!Frame.Opened || Frame.Mode == Arrival) {
            return;
        }
        if (Server.getGameTime() % HumEvery == 0) {
            Server.playSound(null, Pos.getX() + 0.5, Pos.getY() + 1.0, Pos.getZ() + 0.5, Scp860Registry.PortalHum.get(), SoundSource.BLOCKS, 0.4f, 1.0f);
        }
        List<ServerPlayer> Inside = new ArrayList<>();
        for (Player Found : Server.getEntitiesOfClass(Player.class, Frame.field())) {
            if (Found instanceof ServerPlayer User && User.isAlive() && !User.isSpectator()) {
                Inside.add(User);
            }
        }
        for (ServerPlayer User : Inside) {
            if (Frame.Mode == Entry) {
                Scp860Trip.enter(User, Frame, new Vec3(User.xo, User.yo, User.zo));
            } else if (Frame.Mode == Exit && Scp860Forest.participant(User)) {
                Scp860Trip.leave(User, Scp860Trip.Reason.Exit);
            }
        }
    }

    private void exitTick(ServerLevel World) {
        if (World.getGameTime() % 5 != 0) {
            return;
        }
        Vec3 Centre = Vec3.atCenterOf(worldPosition).add(0.0, 0.5, 0.0);
        boolean Near = false;
        boolean Close = false;
        for (ServerPlayer User : Scp860Forest.participants(World)) {
            double Gap = User.position().distanceTo(Centre);
            Near |= Gap <= ExitCloseReach;
            Close |= Gap <= ExitOpenReach;
        }
        if (!Opened && Close) {
            setOpened(true);
        } else if (Opened && !Near) {
            setOpened(false);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, Scp860DoorBlockEntity Frame) {
        Frame.ProgressO = Frame.Progress;
        float Goal = Frame.Opened ? 1.0f : 0.0f;
        Frame.Progress = Mth.clamp(Frame.Progress + Mth.clamp(Goal - Frame.Progress, -1.0f / SwingTicks, 1.0f / SwingTicks), 0.0f, 1.0f);
    }

    @Override
    protected void saveAdditional(CompoundTag Saved, HolderLookup.Provider Lookup) {
        super.saveAdditional(Saved, Lookup);
        if (!Key.isEmpty()) {
            Saved.put("Key", Key.save(Lookup));
        }
        Saved.putBoolean("Opened", Opened);
        Saved.putBoolean("Sealed", Sealed);
        Saved.putInt("Mode", Mode);
    }

    @Override
    protected void loadAdditional(CompoundTag Saved, HolderLookup.Provider Lookup) {
        super.loadAdditional(Saved, Lookup);
        Key = Saved.contains("Key") ? ItemStack.parse(Lookup, Saved.getCompound("Key")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        Opened = Saved.getBoolean("Opened");
        Sealed = Saved.getBoolean("Sealed");
        if (Saved.contains("Mode")) {
            Mode = Saved.getInt("Mode");
        }
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
