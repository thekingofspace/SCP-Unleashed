package net.rai.scp_unleashed.labware;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class LabTubeHolderBlockEntity extends BlockEntity {
    public static final int SlotCount = 4;
    public static final double MouthHeight = 0.6;

    private final NonNullList<ItemStack> Tubes = NonNullList.withSize(SlotCount, ItemStack.EMPTY);
    private boolean OpenCulture;

    public LabTubeHolderBlockEntity(BlockPos Pos, BlockState State) {
        super(LabwareRegistry.TubeHolderEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, LabTubeHolderBlockEntity Holder) {
        if (!Holder.OpenCulture || !(World instanceof ServerLevel Server) || !LabExposure.isPulse(World, Pos.asLong())) {
            return;
        }
        List<ResourceLocation> Samples = new ArrayList<>();
        for (ItemStack Tube : Holder.Tubes) {
            if (LabVials.isOpenCulture(Tube)) {
                Samples.add(LabVials.sampleOf(Tube));
            }
        }
        LabOrganisms.exposeAll(Server, Vec3.atBottomCenterOf(Pos).add(0.0, MouthHeight, 0.0), Pos::equals, Samples);
    }

    public ItemStack getTube(int Slot) {
        return Slot < 0 || Slot >= SlotCount ? ItemStack.EMPTY : Tubes.get(Slot);
    }

    public Direction getFacing() {
        BlockState State = getBlockState();
        return State.hasProperty(LabTubeHolderBlock.Facing) ? State.getValue(LabTubeHolderBlock.Facing) : Direction.NORTH;
    }

    public int getFilled() {
        int Count = 0;
        for (ItemStack Tube : Tubes) {
            if (!Tube.isEmpty()) {
                Count++;
            }
        }
        return Count;
    }

    public void insert(Player User, ItemStack Held, int Slot) {
        if (!isServer() || Slot < 0 || Slot >= SlotCount) {
            return;
        }
        if (!Tubes.get(Slot).isEmpty()) {
            refuse(User, "message.scp_unleashed.labware.slot_taken", Slot);
            return;
        }
        Tubes.set(Slot, Held.copyWithCount(1));
        Held.consume(1, User);
        changed();
        LabwareSounds.play(level, worldPosition, LabwareSounds.Racked, LabwareSounds.pitch(level));
    }

    public void take(Player User, int Slot) {
        if (!isServer() || Slot < 0 || Slot >= SlotCount) {
            return;
        }
        if (Tubes.get(Slot).isEmpty()) {
            refuse(User, "message.scp_unleashed.labware.slot_empty", Slot);
            return;
        }
        ItemStack Taken = Tubes.get(Slot);
        Tubes.set(Slot, ItemStack.EMPTY);
        changed();
        if (!User.getInventory().add(Taken)) {
            User.drop(Taken, false);
        }
        LabwareSounds.play(level, worldPosition, LabwareSounds.Racked, 1.2f + level.getRandom().nextFloat() * 0.08f);
    }

    public void toggleSeal(Player User, int Slot) {
        if (!isServer() || Slot < 0 || Slot >= SlotCount) {
            return;
        }
        ItemStack Vial = Tubes.get(Slot);
        if (Vial.isEmpty()) {
            refuse(User, "message.scp_unleashed.labware.slot_empty", Slot);
            return;
        }
        boolean Opening = !LabVials.isOpen(Vial);
        ItemStack Changed = Vial.copy();
        if (Opening) {
            LabVials.open(Changed);
        } else {
            LabVials.seal(Changed);
        }
        Tubes.set(Slot, Changed);
        changed();
        User.displayClientMessage(Component.translatable(Opening ? "message.scp_unleashed.labware.slot_opened" : "message.scp_unleashed.labware.slot_sealed", Slot + 1)
                .withStyle(Opening ? ChatFormatting.GOLD : ChatFormatting.AQUA), true);
        LabwareSounds.play(level, null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, LabwareSounds.Unsealed, SoundSource.BLOCKS,
                Opening ? 1.0f : 1.15f);
    }

    public void refuseSwab(Player User, int Slot) {
        if (isServer()) {
            refuse(User, "message.scp_unleashed.labware.vial_unswabbable", Slot);
        }
    }

    public void pour(Player User, InteractionHand Hand, int Slot) {
        if (!isServer() || Slot < 0 || Slot >= SlotCount) {
            return;
        }
        ItemStack Vial = Tubes.get(Slot);
        if (Vial.isEmpty()) {
            refuse(User, "message.scp_unleashed.labware.slot_empty", Slot);
            return;
        }
        if (!LabVials.isEmptyVial(Vial)) {
            refuse(User, "message.scp_unleashed.labware.slot_filled", Slot);
            return;
        }
        ItemStack Held = User.getItemInHand(Hand);
        LabSwabRecord Record = LabVials.recordOf(Held);
        if (Record == null) {
            return;
        }
        if (!Record.Labelled()) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.labware.swab_unread").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        ResourceLocation Sample = Record.cultureSample();
        if (Sample == null) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.labware.swab_nothing").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        ItemStack Filled = Vial.copy();
        LabVials.setSample(Filled, Sample);
        Tubes.set(Slot, Filled);
        Held.consume(1, User);
        changed();
        User.displayClientMessage(Component.translatable("message.scp_unleashed.labware.vial_filled", LabOrganisms.nameOf(Sample)).withStyle(ChatFormatting.GRAY), true);
        LabwareSounds.play(level, worldPosition, LabwareSounds.Streaked, LabwareSounds.pitch(level));
    }

    private boolean isServer() {
        return level != null && !level.isClientSide;
    }

    private void refuse(Player User, String Key, int Slot) {
        User.displayClientMessage(Component.translatable(Key, Slot + 1).withStyle(ChatFormatting.GRAY), true);
        LabwareSounds.play(level, null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, List.of(LabwareSounds.Refused), SoundSource.BLOCKS, 1.0f);
    }

    public void dropAll(Level World, BlockPos Pos) {
        Containers.dropContents(World, Pos, Tubes);
        Tubes.clear();
    }

    private void refreshCulture() {
        boolean Found = false;
        for (ItemStack Tube : Tubes) {
            Found |= LabVials.isOpenCulture(Tube);
        }
        OpenCulture = Found;
    }

    private void changed() {
        refreshCulture();
        setChanged();
        if (level != null) {
            BlockState State = getBlockState();
            level.sendBlockUpdated(worldPosition, State, State, Block.UPDATE_CLIENTS);
            level.updateNeighbourForOutputSignal(worldPosition, State.getBlock());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        ContainerHelper.saveAllItems(Tag, Tubes, true, Registries);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Tubes.clear();
        ContainerHelper.loadAllItems(Tag, Tubes, Registries);
        refreshCulture();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putInt("Filled", getFilled());
        ContainerHelper.saveAllItems(Tag, Tubes, true, Registries);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
