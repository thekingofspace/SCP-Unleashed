package net.rai.scp_unleashed.block.entity;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.block.OutletBlock;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;
import net.rai.scp_unleashed.wire.ConnectorTier;
import net.rai.scp_unleashed.wire.WireConnector;
import net.rai.scp_unleashed.wire.WireLink;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import net.rai.scp_unleashed.wire.Wires;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OutletBlockEntity extends BlockEntity {
    private static final Codec<List<WireLink>> LinksCodec = WireLink.LinkCodec.listOf();

    private final List<WireLink> Links = new ArrayList<>();

    public OutletBlockEntity(BlockPos Pos, BlockState State) {
        this(ModBlockEntityTypes.Outlet.get(), Pos, State);
    }

    protected OutletBlockEntity(BlockEntityType<?> Type, BlockPos Pos, BlockState State) {
        super(Type, Pos, State);
    }

    public List<WireLink> getLinks() {
        return Collections.unmodifiableList(Links);
    }

    @Nullable
    public WireLink linkTo(BlockPos Target) {
        for (WireLink Link : Links) {
            if (Link.Target().equals(Target)) {
                return Link;
            }
        }
        return null;
    }

    public boolean isLinkedTo(BlockPos Target) {
        return linkTo(Target) != null;
    }

    public boolean hasFreeLink() {
        return Links.size() < Wires.MaxLinksPerOutlet;
    }

    public void addLink(WireLink Link) {
        Links.add(Link);
        if (level instanceof ServerLevel World) {
            WireNetworkManager.get(World).wireAdded(worldPosition, attachFor(Link), Link.Target(), Link.targetAttach(), Link.Tier());
        }
        linksChanged();
    }

    public boolean removeLink(BlockPos Target) {
        if (!Links.removeIf(Link -> Link.Target().equals(Target))) {
            return false;
        }
        if (level instanceof ServerLevel World) {
            WireNetworkManager.get(World).wireRemoved(worldPosition, Target);
        }
        linksChanged();
        return true;
    }

    public boolean setLinkHidden(BlockPos Target, boolean Hidden) {
        for (int Index = 0; Index < Links.size(); Index++) {
            WireLink Link = Links.get(Index);
            if (Link.Target().equals(Target)) {
                if (Link.Hidden() == Hidden) {
                    return false;
                }
                Links.set(Index, Link.withHidden(Hidden));
                linksChanged();
                return true;
            }
        }
        return false;
    }

    public boolean isFullBlock() {
        return getBlockState().getBlock() instanceof WireConnector Connector && Connector.isFullBlock();
    }

    public ConnectorTier getConnectorTier() {
        return getBlockState().getBlock() instanceof WireConnector Connector ? Connector.getConnectorTier() : ConnectorTier.Basic;
    }

    public Direction getFacing() {
        BlockState State = getBlockState();
        return State.hasProperty(OutletBlock.Facing) ? State.getValue(OutletBlock.Facing) : Direction.UP;
    }

    public Direction faceFor(WireLink Link) {
        return isFullBlock() && Link.LocalFace() != null ? Link.LocalFace() : getFacing();
    }

    public Vec3 attachFor(WireLink Link) {
        return Wires.attachPoint(worldPosition, faceFor(Link), isFullBlock());
    }

    public Vec3 attachAt(Direction Face) {
        boolean FullBlock = isFullBlock();
        return Wires.attachPoint(worldPosition, FullBlock ? Face : getFacing(), FullBlock);
    }

    private void linksChanged() {
        setChanged();
        if (level == null) {
            return;
        }
        BlockState Current = level.getBlockState(worldPosition);
        boolean HasWire = !Links.isEmpty();
        if (!isRemoved() && Current.hasProperty(OutletBlock.Connected) && Current.getValue(OutletBlock.Connected) != HasWire) {
            level.setBlock(worldPosition, Current.setValue(OutletBlock.Connected, HasWire), Block.UPDATE_CLIENTS);
        } else {
            level.sendBlockUpdated(worldPosition, Current, Current, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel World) {
            WireNetworkManager.get(World).outletLoaded(this);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level instanceof ServerLevel World) {
            WireNetworkManager.outletUnloaded(World, this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel World) {
            WireNetworkManager.outletUnloaded(World, this);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        LinksCodec.encodeStart(NbtOps.INSTANCE, Links).ifSuccess(Encoded -> Tag.put("Links", Encoded));
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Links.clear();
        if (Tag.contains("Links")) {
            LinksCodec.parse(NbtOps.INSTANCE, Tag.get("Links")).ifSuccess(Links::addAll);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        return saveCustomOnly(Registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
