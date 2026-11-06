package net.rai.scp_unleashed.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.reactor.core.ReactorTuning;

public class TowerSegmentBlockEntity extends BlockEntity {
    private ItemStack Rod = ItemStack.EMPTY;
    private float BurnDone;
    private float Temperature = ReactorTuning.Ambient;
    private float Integrity = ReactorTuning.FullIntegrity;
    private float Water;
    private float Steam;
    private float Gas;
    private float Insertion = 100.0f;
    private float Target = 100.0f;

    public TowerSegmentBlockEntity(BlockPos Pos, BlockState State) {
        super(ReactorRegistry.SegmentEntity.get(), Pos, State);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel World) {
            ReactorGridManager.get(World).seed(worldPosition);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level instanceof ServerLevel World) {
            ReactorGridManager.unloaded(World, worldPosition);
        }
    }

    public ItemStack getRod() {
        return Rod;
    }

    public void setRod(ItemStack Stack, float Done) {
        Rod = Stack.isEmpty() ? ItemStack.EMPTY : Stack.copyWithCount(1);
        BurnDone = Math.max(0.0f, Done);
        setChanged();
    }

    public boolean hasFreshRod() {
        return TowerFuel.isFuel(Rod) && BurnDone < TowerFuel.burnTicks(Rod);
    }

    public float getBurnDone() {
        return BurnDone;
    }

    public float burnLeft() {
        return TowerFuel.isFuel(Rod) ? Math.max(0.0f, TowerFuel.burnTicks(Rod) - BurnDone) : 0.0f;
    }

    public void setBurnLeft(float Left) {
        if (TowerFuel.isFuel(Rod)) {
            BurnDone = Mth.clamp(TowerFuel.burnTicks(Rod) - Left, 0.0f, TowerFuel.burnTicks(Rod));
        }
    }

    public float getTemperature() {
        return Temperature;
    }

    public float getIntegrity() {
        return Integrity;
    }

    public float getWater() {
        return Water;
    }

    public float getSteam() {
        return Steam;
    }

    public float getGas() {
        return Gas;
    }

    public float getInsertion() {
        return Insertion;
    }

    public float getTarget() {
        return Target;
    }

    public void setWater(float Amount) {
        Water = Math.max(0.0f, Amount);
        setChanged();
    }

    public void setTarget(float Value) {
        Target = Mth.clamp(Value, 0.0f, 100.0f);
        setChanged();
    }

    public void store(float NewTemperature, float NewIntegrity, float NewWater, float NewSteam, float NewGas, float NewInsertion, float NewTarget) {
        Temperature = Math.max(ReactorTuning.Ambient, NewTemperature);
        Integrity = Mth.clamp(NewIntegrity, 0.0f, ReactorTuning.FullIntegrity);
        Water = Math.max(0.0f, NewWater);
        Steam = Math.max(0.0f, NewSteam);
        Gas = Math.max(0.0f, NewGas);
        Insertion = Mth.clamp(NewInsertion, 0.0f, 100.0f);
        Target = Mth.clamp(NewTarget, 0.0f, 100.0f);
        setChanged();
    }

    public void dropContents(Level World, BlockPos Pos) {
        if (Rod.isEmpty()) {
            return;
        }
        ItemStack Dropped = BurnDone > 0.0f && TowerFuel.isFuel(Rod) ? TowerFuel.depleted() : Rod.copy();
        Containers.dropItemStack(World, Pos.getX(), Pos.getY(), Pos.getZ(), Dropped);
        Rod = ItemStack.EMPTY;
        BurnDone = 0.0f;
    }

    public void clearContents() {
        Rod = ItemStack.EMPTY;
        BurnDone = 0.0f;
        Water = 0.0f;
        Steam = 0.0f;
        Gas = 0.0f;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        if (!Rod.isEmpty()) {
            Tag.put("Rod", Rod.save(Registries));
            Tag.putFloat("BurnDone", BurnDone);
        }
        Tag.putFloat("Temperature", Temperature);
        Tag.putFloat("Integrity", Integrity);
        Tag.putFloat("Water", Water);
        Tag.putFloat("Steam", Steam);
        Tag.putFloat("Gas", Gas);
        Tag.putFloat("Insertion", Insertion);
        Tag.putFloat("Target", Target);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Sync")) {
            return;
        }
        Rod = Tag.contains("Rod") ? ItemStack.parse(Registries, Tag.getCompound("Rod")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        BurnDone = Math.max(0.0f, Tag.getFloat("BurnDone"));
        Temperature = Tag.contains("Temperature") ? Math.max(ReactorTuning.Ambient, Tag.getFloat("Temperature")) : ReactorTuning.Ambient;
        Integrity = Tag.contains("Integrity") ? Mth.clamp(Tag.getFloat("Integrity"), 0.0f, ReactorTuning.FullIntegrity) : ReactorTuning.FullIntegrity;
        Water = Math.max(0.0f, Tag.getFloat("Water"));
        Steam = Math.max(0.0f, Tag.getFloat("Steam"));
        Gas = Math.max(0.0f, Tag.getFloat("Gas"));
        Insertion = Tag.contains("Insertion") ? Mth.clamp(Tag.getFloat("Insertion"), 0.0f, 100.0f) : 100.0f;
        Target = Tag.contains("Target") ? Mth.clamp(Tag.getFloat("Target"), 0.0f, 100.0f) : 100.0f;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putBoolean("Sync", true);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
