package net.rai.scp_unleashed.scp063;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Scp063RestingBlockEntity extends BlockEntity {
    private ItemStack Kept = ItemStack.EMPTY;
    @Nullable
    private UUID Owner;
    private String OwnerName = "";
    private int Work;
    private int Fall;
    private boolean Active;
    private boolean Falling;
    private boolean Released;
    private int RefusedAge;
    private final Set<BlockPos> Refused = new HashSet<>();

    public Scp063RestingBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp063Registry.RestingEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp063RestingBlockEntity Rest) {
        if (World instanceof ServerLevel Server) {
            Rest.tick(Server);
        }
    }

    private void tick(ServerLevel World) {
        BlockPos Below = worldPosition.below();
        if (Scp063Erosion.canFallInto(World, Below)) {
            Falling = true;
            setActive(false);
            if (++Fall >= Scp063Erosion.FallTicks) {
                Fall = 0;
                Scp063Erosion.settle(World, this);
            }
            return;
        }
        Fall = 0;
        if (Falling) {
            Falling = false;
            Scp063Erosion.landed(World, worldPosition);
        }
        if (!Refused.isEmpty() && ++RefusedAge >= Scp063Erosion.RetryTicks) {
            Refused.clear();
            RefusedAge = 0;
        }
        BlockPos Target = Scp063Erosion.enabled(World) ? Scp063Erosion.nextTarget(World, worldPosition, Refused) : null;
        if (Target == null) {
            setActive(false);
            return;
        }
        setActive(true);
        Work++;
        BlockState State = World.getBlockState(Target);
        if (Work % Scp063Erosion.GritInterval == 0) {
            Scp063Erosion.grit(World, Target, State);
        }
        if (Work < Scp063Erosion.ErodeTicks) {
            return;
        }
        Work = 0;
        FakePlayer Agent = Scp063Erosion.agent(World, Owner, OwnerName, worldPosition);
        if (Scp063Erosion.permitted(World, Agent, Target, State)) {
            Scp063Erosion.wipe(World, Target, State, Agent);
        } else {
            Refused.add(Target.immutable());
        }
        setChanged();
    }

    public void rest(ItemStack Stack, @Nullable Player Placer) {
        Kept = Stack.copyWithCount(1);
        if (Placer != null && !(Placer instanceof FakePlayer)) {
            Owner = Placer.getUUID();
            OwnerName = Placer.getGameProfile().getName();
        }
        setChanged();
        sync();
    }

    public ItemStack kept() {
        if (Released) {
            return ItemStack.EMPTY;
        }
        return Kept.isEmpty() ? new ItemStack(Scp063Registry.Toothbrush.get()) : Kept;
    }

    public boolean isActive() {
        return Active;
    }

    public int work() {
        return Work;
    }

    @Nullable
    public UUID owner() {
        return Owner;
    }

    public void inherit(Scp063RestingBlockEntity Old) {
        Falling = Old.Falling;
        Refused.clear();
        RefusedAge = 0;
        Active = false;
        sync();
    }

    public void release() {
        Kept = ItemStack.EMPTY;
        Released = true;
    }

    private void setActive(boolean Now) {
        if (Active != Now) {
            Active = Now;
            setChanged();
            sync();
        }
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        if (!Kept.isEmpty()) {
            Tag.put("Brush", Kept.save(Registries));
        }
        if (Owner != null) {
            Tag.putUUID("Owner", Owner);
            Tag.putString("OwnerName", OwnerName);
        }
        Tag.putInt("Work", Work);
        Tag.putBoolean("Falling", Falling);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Kept = Tag.contains("Brush") ? ItemStack.parseOptional(Registries, Tag.getCompound("Brush")) : ItemStack.EMPTY;
        Owner = Tag.hasUUID("Owner") ? Tag.getUUID("Owner") : null;
        OwnerName = Tag.getString("OwnerName");
        Work = Tag.getInt("Work");
        Falling = Tag.getBoolean("Falling");
        if (Tag.contains("Active")) {
            Active = Tag.getBoolean("Active");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putBoolean("Active", Active);
        if (!Kept.isEmpty()) {
            Tag.put("Brush", Kept.save(Registries));
        }
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
