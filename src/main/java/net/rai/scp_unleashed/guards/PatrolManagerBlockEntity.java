package net.rai.scp_unleashed.guards;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.security.TurretCategory;
import net.rai.scp_unleashed.security.TurretRules;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PatrolManagerBlockEntity extends BlockEntity {
    public static final double LinkRange = 96.0;
    public static final double UseRange = 8.0;

    private UUID RouteId = UUID.randomUUID();
    private final List<PatrolPoint> Points = new ArrayList<>();
    private boolean PingPong;
    private final TurretRules Rules = new TurretRules();
    private boolean HoldFire;
    private boolean Melee = true;
    @Nullable
    private UUID Owner;
    private String OwnerName = "";
    private int Issued;

    public PatrolManagerBlockEntity(BlockPos Pos, BlockState State) {
        super(GuardsRegistry.PatrolManagerEntity.get(), Pos, State);
    }

    public enum LinkResult {
        Added, Removed, Full, TooFar
    }

    public UUID getRouteId() {
        return RouteId;
    }

    public List<PatrolPoint> getPoints() {
        return Collections.unmodifiableList(Points);
    }

    public boolean isPingPong() {
        return PingPong;
    }

    public TurretRules getRules() {
        return Rules;
    }

    public boolean isHoldFire() {
        return HoldFire;
    }

    public boolean isMelee() {
        return Melee;
    }

    @Nullable
    public UUID getOwner() {
        return Owner;
    }

    public String getOwnerName() {
        return OwnerName;
    }

    public int getIssued() {
        return Issued;
    }

    public void setOwner(Player User) {
        Owner = User.getUUID();
        OwnerName = User.getGameProfile().getName();
        changed();
    }

    public boolean isOwner(Player User) {
        return Owner != null && Owner.equals(User.getUUID());
    }

    public boolean canEdit(Player User) {
        return Owner == null || isOwner(User) || User.isCreative() && User.hasPermissions(2);
    }

    public boolean canIssue(Player User) {
        return canEdit(User) || Rules.allowsName(User.getGameProfile().getName());
    }

    public boolean isUsableBy(Player User) {
        return level != null && level.getBlockEntity(worldPosition) == this && User.distanceToSqr(Vec3.atCenterOf(worldPosition)) <= UseRange * UseRange;
    }

    public void denied(Player User) {
        User.displayClientMessage(Component.translatable("message.scp_unleashed.patrol_manager.locked", OwnerName).withStyle(ChatFormatting.RED), true);
    }

    public PatrolRoute route() {
        return new PatrolRoute(RouteId, level == null ? Level.OVERWORLD : level.dimension(), worldPosition, Points, PingPong,
                GuardRules.of(Rules, HoldFire, Melee), Optional.ofNullable(Owner), OwnerName);
    }

    public LinkResult link(BlockPos Pos, Direction Facing) {
        int Existing = indexOf(Pos);
        if (Existing >= 0) {
            Points.remove(Existing);
            changed();
            return LinkResult.Removed;
        }
        if (Points.size() >= PatrolRoute.MaxPoints) {
            return LinkResult.Full;
        }
        if (Vec3.atCenterOf(Pos).distanceToSqr(Vec3.atCenterOf(worldPosition)) > LinkRange * LinkRange) {
            return LinkResult.TooFar;
        }
        Points.add(new PatrolPoint(Pos, PatrolPoint.DefaultWait, Facing));
        changed();
        return LinkResult.Added;
    }

    public int indexOf(BlockPos Pos) {
        for (int Index = 0; Index < Points.size(); Index++) {
            if (Points.get(Index).Pos().equals(Pos)) {
                return Index;
            }
        }
        return -1;
    }

    public void noteIssued() {
        Issued++;
        changed();
    }

    public boolean applyConfig(int Action, int Index, int Value, String Text) {
        boolean Valid = Index >= 0 && Index < Points.size();
        switch (Action) {
            case PatrolConfigPayload.ActionToggle -> Rules.toggle(TurretCategory.byIndex(Index));
            case PatrolConfigPayload.ActionAllow -> {
                if (!Rules.add(Text, false)) {
                    return false;
                }
            }
            case PatrolConfigPayload.ActionDeny -> {
                if (!Rules.add(Text, true)) {
                    return false;
                }
            }
            case PatrolConfigPayload.ActionRemove -> {
                if (!Rules.remove(Index)) {
                    return false;
                }
            }
            case PatrolConfigPayload.ActionFlip -> {
                if (!Rules.flip(Index)) {
                    return false;
                }
            }
            case PatrolConfigPayload.ActionMode -> PingPong = !PingPong;
            case PatrolConfigPayload.ActionFire -> HoldFire = !HoldFire;
            case PatrolConfigPayload.ActionMelee -> Melee = !Melee;
            case PatrolConfigPayload.ActionWait -> {
                if (!Valid) {
                    return false;
                }
                PatrolPoint Old = Points.get(Index);
                Points.set(Index, Old.withWait(Mth.clamp(Old.Wait() + Value, 0, PatrolPoint.MaxWait)));
            }
            case PatrolConfigPayload.ActionMove -> {
                int Target = Index + Mth.sign(Value);
                if (!Valid || Target < 0 || Target >= Points.size()) {
                    return false;
                }
                Collections.swap(Points, Index, Target);
            }
            case PatrolConfigPayload.ActionUnlink -> {
                if (!Valid) {
                    return false;
                }
                Points.remove(Index);
            }
            default -> {
                return false;
            }
        }
        changed();
        return true;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void write(CompoundTag Data) {
        Data.putUUID("RouteId", RouteId);
        ListTag Saved = new ListTag();
        for (PatrolPoint Point : Points) {
            PatrolPoint.PointCodec.encodeStart(NbtOps.INSTANCE, Point).result().ifPresent(Saved::add);
        }
        Data.put("Points", Saved);
        Data.putBoolean("PingPong", PingPong);
        Data.put("Rules", Rules.save());
        Data.putBoolean("HoldFire", HoldFire);
        Data.putBoolean("Melee", Melee);
        Data.putInt("Issued", Issued);
        if (Owner != null) {
            Data.putUUID("Owner", Owner);
            Data.putString("OwnerName", OwnerName);
        }
    }

    private void read(CompoundTag Data) {
        if (Data.hasUUID("RouteId")) {
            RouteId = Data.getUUID("RouteId");
        }
        Points.clear();
        ListTag Saved = Data.getList("Points", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size() && Points.size() < PatrolRoute.MaxPoints; Index++) {
            PatrolPoint.PointCodec.parse(NbtOps.INSTANCE, Saved.get(Index)).result().ifPresent(Points::add);
        }
        PingPong = Data.getBoolean("PingPong");
        Rules.load(Data.getCompound("Rules"));
        HoldFire = Data.getBoolean("HoldFire");
        Melee = !Data.contains("Melee") || Data.getBoolean("Melee");
        Issued = Data.getInt("Issued");
        if (Data.hasUUID("Owner")) {
            Owner = Data.getUUID("Owner");
            OwnerName = Data.getString("OwnerName");
        } else {
            Owner = null;
            OwnerName = "";
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Data, HolderLookup.Provider Registries) {
        super.saveAdditional(Data, Registries);
        write(Data);
    }

    @Override
    protected void loadAdditional(CompoundTag Data, HolderLookup.Provider Registries) {
        super.loadAdditional(Data, Registries);
        read(Data);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Data = super.getUpdateTag(Registries);
        write(Data);
        return Data;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
