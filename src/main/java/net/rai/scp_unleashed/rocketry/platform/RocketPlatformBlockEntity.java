package net.rai.scp_unleashed.rocketry.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.oil.OilLayout;
import net.rai.scp_unleashed.rocketry.RocketryRegistry;
import net.rai.scp_unleashed.rocketry.rocket.RocketDestination;
import net.rai.scp_unleashed.rocketry.rocket.RocketDestinations;
import net.rai.scp_unleashed.rocketry.rocket.RocketEntity;
import net.rai.scp_unleashed.rocketry.rocket.RocketItem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RocketPlatformBlockEntity extends LaunchPlatformBlockEntity {
    @Nullable
    private ResourceLocation Selected;

    public RocketPlatformBlockEntity(BlockPos Pos, BlockState State) {
        super(RocketryRegistry.RocketPlatformEntity.get(), Pos, State, LaunchPlatformKind.Rocket);
    }

    @Nullable
    public RocketEntity findRocket() {
        if (level == null) {
            return null;
        }
        Vec3 At = standAt();
        List<RocketEntity> Found = level.getEntitiesOfClass(RocketEntity.class, RocketItem.rocketBox(At).inflate(0.5), RocketEntity::isOnPad);
        return Found.isEmpty() ? null : Found.getFirst();
    }

    @Nullable
    public RocketDestination destination() {
        RocketDestination Found = RocketDestinations.byId(Selected);
        if (!(level instanceof ServerLevel World)) {
            return Found;
        }
        if (Found == null || Found.Dimension().equals(World.dimension())) {
            Found = RocketDestinations.defaultFrom(World.getServer(), World.dimension());
        }
        return Found;
    }

    public void select(@Nullable RocketDestination Wanted) {
        Selected = Wanted == null ? null : Wanted.Id();
        setChanged();
        if (level instanceof ServerLevel World) {
            refreshChecks(World);
        }
    }

    @Override
    public void pressCycle(ServerPlayer User, int Step) {
        if (!(level instanceof ServerLevel World) || isLaunching()) {
            return;
        }
        RocketDestination Next = RocketDestinations.step(World.getServer(), World.dimension(), destination(), Step);
        select(Next);
        Vec3 Centre = getCentre();
        World.playSound(null, Centre.x, Centre.y, Centre.z, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.4f);
    }

    @Override
    protected int destinationIndex() {
        return RocketDestinations.indexOf(destination());
    }

    @Override
    public int fuelCost() {
        RocketDestination Goal = destination();
        return Goal == null ? Kind.getFuelCost() : Goal.Fuel();
    }

    @Override
    public int powerCost() {
        RocketDestination Goal = destination();
        return Goal == null ? Kind.getPowerCost() : Goal.Power();
    }

    @Override
    public int powerCapacity() {
        return powerCost();
    }

    @Override
    protected List<BlockPos> buildSkyColumns() {
        List<BlockPos> Columns = new ArrayList<>();
        Direction Front = getFront();
        OilLayout Layout = RocketPlatformBlock.Layout;
        for (int Column = 1; Column <= 3; Column++) {
            for (int Layer = 1; Layer <= 3; Layer++) {
                Columns.add(Layout.cellPos(worldPosition, Front, Layout.part(Column, 0, Layer)));
            }
        }
        return Columns;
    }

    @Override
    protected int skyFloor() {
        return RocketPlatformBlock.Height;
    }

    @Override
    protected int computeChecks(ServerLevel World) {
        int Mask = 0;
        RocketEntity Rocket = findRocket();
        int Cost = fuelCost();
        if (Rocket != null) {
            Mask |= LaunchCheck.Payload.bit();
            if (Rocket.hasCrew()) {
                Mask |= LaunchCheck.Crew.bit();
            }
        }
        if (hasFuelFor(Cost) || Rocket != null && Rocket.getStoredFuel() >= Cost) {
            Mask |= LaunchCheck.Fuel.bit();
        }
        if (hasPowerFor(powerCost())) {
            Mask |= LaunchCheck.Power.bit();
        }
        if (skyClear(World)) {
            Mask |= LaunchCheck.Sky.bit();
        }
        RocketDestination Goal = destination();
        if (Goal != null && RocketDestinations.available(World.getServer(), Goal) && RocketDestinations.isLaunchSite(World.dimension())) {
            Mask |= LaunchCheck.Route.bit();
        }
        return Mask;
    }

    @Override
    protected boolean hasPayload() {
        return findRocket() != null;
    }

    @Override
    protected boolean isLaunching() {
        RocketEntity Rocket = findRocket();
        return Rocket != null && Rocket.isLaunching();
    }

    @Override
    protected boolean beginLaunch(ServerLevel World, @Nullable Player User) {
        RocketEntity Rocket = findRocket();
        RocketDestination Goal = destination();
        if (Rocket == null || Goal == null) {
            return false;
        }
        Rocket.ignite(this, Goal);
        return true;
    }

    @Override
    protected void saveExtra(CompoundTag Tag, HolderLookup.Provider Registries) {
        if (Selected != null) {
            Tag.putString("Destination", Selected.toString());
        }
    }

    @Override
    protected void loadExtra(CompoundTag Tag, HolderLookup.Provider Registries) {
        Selected = Tag.contains("Destination") ? ResourceLocation.tryParse(Tag.getString("Destination")) : null;
    }
}
