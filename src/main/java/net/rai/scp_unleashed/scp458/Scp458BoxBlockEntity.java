package net.rai.scp_unleashed.scp458;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class Scp458BoxBlockEntity extends BlockEntity {
    public static final String ContentsKey = "Contents";

    private Scp458Contents Held = Scp458Contents.Sealed;
    private boolean Primed;
    private boolean Showing;
    private float From;
    private float Ticks;
    private float LastTicks;

    public Scp458BoxBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp458Registry.BoxEntity.get(), Pos, State);
    }

    public Scp458Contents contents() {
        return Held;
    }

    public void store(Scp458Contents Next) {
        Held = Next;
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState State = getBlockState();
            BlockState Wanted = State.hasProperty(Scp458BoxBlock.Open) ? State.setValue(Scp458BoxBlock.Open, Next.Open()) : State;
            if (Wanted != State) {
                level.setBlock(worldPosition, Wanted, Block.UPDATE_ALL);
            } else {
                level.sendBlockUpdated(worldPosition, State, State, Block.UPDATE_CLIENTS);
            }
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, Scp458BoxBlockEntity Self) {
        boolean Target = Scp458BoxBlock.isOpen(State);
        if (!Self.Primed) {
            Self.Primed = true;
            Self.Showing = Target;
            Self.From = Scp458Lid.settled(Target);
            Self.Ticks = Scp458Lid.duration(Target);
            Self.LastTicks = Self.Ticks;
            return;
        }
        if (Target != Self.Showing) {
            Self.From = Scp458Lid.angle(Self.Showing, Self.From, Self.Ticks);
            Self.Showing = Target;
            Self.Ticks = 0.0f;
            Self.LastTicks = 0.0f;
            return;
        }
        Self.LastTicks = Self.Ticks;
        Self.Ticks = Math.min(Self.Ticks + 1.0f, Scp458Lid.duration(Target));
    }

    public float lidAngle(float Partial) {
        if (!Primed) {
            return Scp458Lid.settled(Scp458BoxBlock.isOpen(getBlockState()));
        }
        return Scp458Lid.angle(Showing, From, Mth.lerp(Partial, LastTicks, Ticks));
    }

    @Override
    protected void saveAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.saveAdditional(Saved, Registries);
        Scp458Contents.ContentsCodec.encodeStart(NbtOps.INSTANCE, Held).result().ifPresent(Encoded -> Saved.put(ContentsKey, Encoded));
    }

    @Override
    protected void loadAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.loadAdditional(Saved, Registries);
        Held = Saved.contains(ContentsKey)
                ? Scp458Contents.ContentsCodec.parse(NbtOps.INSTANCE, Saved.get(ContentsKey)).result().orElse(Scp458Contents.Sealed)
                : Scp458Contents.Sealed;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        return saveCustomOnly(Registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder Components) {
        super.collectImplicitComponents(Components);
        if (!Held.equals(Scp458Contents.Sealed)) {
            Components.set(Scp458Registry.Contents.get(), Held);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput Input) {
        super.applyImplicitComponents(Input);
        Scp458Contents Carried = Input.get(Scp458Registry.Contents.get());
        if (Carried != null) {
            Held = Carried;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag Saved) {
        super.removeComponentsFromTag(Saved);
        Saved.remove(ContentsKey);
    }
}
