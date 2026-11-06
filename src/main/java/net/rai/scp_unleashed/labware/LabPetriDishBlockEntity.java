package net.rai.scp_unleashed.labware;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class LabPetriDishBlockEntity extends BlockEntity {
    public static final double SurfaceHeight = 0.15;

    @Nullable
    private ResourceLocation Sample;

    public LabPetriDishBlockEntity(BlockPos Pos, BlockState State) {
        super(LabwareRegistry.PetriDishEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, LabPetriDishBlockEntity Dish) {
        if (World instanceof ServerLevel Server && Dish.Sample != null && LabExposure.isPulse(World, Pos.asLong())) {
            LabOrganisms.expose(Server, Vec3.atBottomCenterOf(Pos).add(0.0, SurfaceHeight, 0.0), Pos::equals, Dish.Sample);
        }
    }

    public boolean hasCulture() {
        return Sample != null;
    }

    @Nullable
    public ResourceLocation getSample() {
        return Sample;
    }

    public int getTint() {
        return LabOrganisms.cultureColourOf(Sample);
    }

    public void setSample(@Nullable ResourceLocation Wanted) {
        Sample = Wanted;
        setChanged();
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState State = getBlockState();
        if (State.hasProperty(LabPetriDishBlock.Cultured) && State.getValue(LabPetriDishBlock.Cultured) != (Sample != null)) {
            level.setBlock(worldPosition, State.setValue(LabPetriDishBlock.Cultured, Sample != null), Block.UPDATE_ALL);
        } else {
            level.sendBlockUpdated(worldPosition, State, State, Block.UPDATE_CLIENTS);
        }
    }

    public ResourceLocation culture(ResourceLocation Wanted) {
        ResourceLocation Grown = level instanceof ServerLevel Server
                ? LabOrganisms.find(Wanted).map(Organism -> Organism.cultureIn(Server, worldPosition, Server.getRandom())).orElse(Wanted) : Wanted;
        setSample(Grown);
        return Grown;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        if (Sample != null) {
            Tag.putString("Sample", Sample.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Sample = Tag.contains("Sample") ? ResourceLocation.tryParse(Tag.getString("Sample")) : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putBoolean("Cultured", Sample != null);
        if (Sample != null) {
            Tag.putString("Sample", Sample.toString());
        }
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection Link, ClientboundBlockEntityDataPacket Update, HolderLookup.Provider Registries) {
        ResourceLocation Before = Sample;
        super.onDataPacket(Link, Update, Registries);
        if (level != null && level.isClientSide && !Objects.equals(Before, Sample)) {
            BlockState State = getBlockState();
            level.sendBlockUpdated(worldPosition, State, State, Block.UPDATE_IMMEDIATE);
        }
    }
}
