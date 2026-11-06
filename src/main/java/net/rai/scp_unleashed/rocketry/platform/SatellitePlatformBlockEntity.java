package net.rai.scp_unleashed.rocketry.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.rai.scp_unleashed.oil.OilLayout;
import net.rai.scp_unleashed.orbital.OrbitalLaunch;
import net.rai.scp_unleashed.rocketry.RocketryRegistry;
import net.rai.scp_unleashed.rocketry.satellite.SatelliteBoosterEntity;
import net.rai.scp_unleashed.rocketry.satellite.SatelliteItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class SatellitePlatformBlockEntity extends LaunchPlatformBlockEntity {
    public static final int LaunchHold = 80;

    private final ItemStackHandler Payload = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int Slot, ItemStack Stack) {
            return SatelliteItem.isPayload(Stack);
        }

        @Override
        public int getSlotLimit(int Slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int Slot) {
            setChanged();
            markForSync();
        }
    };
    private long LaunchingUntil = Long.MIN_VALUE / 2;
    private ItemStack Shown = ItemStack.EMPTY;

    public SatellitePlatformBlockEntity(BlockPos Pos, BlockState State) {
        super(RocketryRegistry.SatellitePlatformEntity.get(), Pos, State, LaunchPlatformKind.Satellite);
    }

    @Override
    public IItemHandler getItemHandler() {
        return Payload;
    }

    public ItemStack getPayload() {
        return Payload.getStackInSlot(0);
    }

    public ItemStack getShownPayload() {
        return Shown;
    }

    public boolean loadPayload(ItemStack Stack) {
        if (!Payload.getStackInSlot(0).isEmpty() || !SatelliteItem.isPayload(Stack)) {
            return false;
        }
        Payload.setStackInSlot(0, Stack.split(1));
        return true;
    }

    @Override
    protected List<BlockPos> buildSkyColumns() {
        OilLayout Layout = SatellitePlatformBlock.Layout;
        return List.of(Layout.cellPos(worldPosition, getFront(), Layout.part(1, 0, 1)));
    }

    @Override
    protected int computeChecks(ServerLevel World) {
        int Mask = 0;
        if (hasPayload()) {
            Mask |= LaunchCheck.Payload.bit();
        }
        if (hasFuelFor(fuelCost())) {
            Mask |= LaunchCheck.Fuel.bit();
        }
        if (hasPowerFor(powerCost())) {
            Mask |= LaunchCheck.Power.bit();
        }
        if (skyClear(World)) {
            Mask |= LaunchCheck.Sky.bit();
        }
        return Mask;
    }

    @Override
    protected int skyFloor() {
        return SatellitePlatformBlock.Height;
    }

    @Override
    protected boolean hasPayload() {
        return !Payload.getStackInSlot(0).isEmpty();
    }

    @Override
    protected boolean isLaunching() {
        return level != null && level.getGameTime() < LaunchingUntil;
    }

    @Override
    protected boolean beginLaunch(ServerLevel World, @Nullable Player User) {
        ItemStack Satellite = Payload.getStackInSlot(0);
        if (Satellite.isEmpty()) {
            return false;
        }
        if (!OrbitalLaunch.mayLaunch(World, User, Satellite, getCentre())) {
            return false;
        }
        Component Named = Satellite.get(DataComponents.CUSTOM_NAME);
        UUID Owner = User != null ? User.getUUID() : null;
        Vec3 At = standAt();
        SatelliteBoosterEntity Booster = SatelliteBoosterEntity.launching(World, At, getFront().toYRot(), Owner, Named == null ? null : Named.getString())
                .withPayload(OrbitalLaunch.payloadId(Satellite));
        if (!World.addFreshEntity(Booster)) {
            return false;
        }
        burnFuel(fuelCost());
        drainPower(powerCost());
        Payload.setStackInSlot(0, ItemStack.EMPTY);
        LaunchingUntil = World.getGameTime() + LaunchHold;
        return true;
    }

    @Override
    public void dropContents(Level World, BlockPos Pos) {
        ItemStack Held = Payload.getStackInSlot(0);
        if (!Held.isEmpty()) {
            Containers.dropItemStack(World, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Held.copy());
            Payload.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveExtra(CompoundTag Tag, HolderLookup.Provider Registries) {
        Tag.put("Payload", Payload.serializeNBT(Registries));
    }

    @Override
    protected void loadExtra(CompoundTag Tag, HolderLookup.Provider Registries) {
        if (Tag.contains("Payload")) {
            Payload.deserializeNBT(Registries, Tag.getCompound("Payload"));
        }
    }

    @Override
    protected void writeSync(CompoundTag Tag, HolderLookup.Provider Registries) {
        ItemStack Held = Payload.getStackInSlot(0);
        Tag.putBoolean("Loaded", !Held.isEmpty());
        if (!Held.isEmpty()) {
            Tag.put("Shown", Held.save(Registries));
        }
    }

    @Override
    protected void readSync(CompoundTag Tag, HolderLookup.Provider Registries) {
        Shown = Tag.getBoolean("Loaded") && Tag.contains("Shown") ? ItemStack.parseOptional(Registries, Tag.getCompound("Shown")) : ItemStack.EMPTY;
    }
}
