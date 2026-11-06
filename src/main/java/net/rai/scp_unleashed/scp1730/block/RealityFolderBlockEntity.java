package net.rai.scp_unleashed.scp1730.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp1730.Scp1730Anchors;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RealityFolderBlockEntity extends BlockEntity implements WireDevice, GeoBlockEntity {
    public static final int Demand = 18000;
    public static final int SpinTicks = 80;
    public static final int ShutdownTicks = 40;
    public static final int GraceTicks = 10;
    public static final int HumEvery = 60;
    public static final int StatusOff = 0;
    public static final int StatusSpinning = 1;
    public static final int StatusRunning = 2;
    public static final int StatusShutdown = 3;
    public static final double FieldLift = 3.0;
    public static final float CoilSpeed = 12.0f;
    public static final float RingSpeed = 3.0f;
    public static final String OffName = "off";
    public static final String SpinName = "spinup";
    public static final String RunningName = "running";
    public static final String ShutdownName = "shutdown";
    private static final RawAnimation Off = RawAnimation.begin().thenLoop(OffName);
    private static final RawAnimation Spin = RawAnimation.begin().thenPlayAndHold(SpinName);
    private static final RawAnimation Running = RawAnimation.begin().thenLoop(RunningName);
    private static final RawAnimation Stopping = RawAnimation.begin().thenPlayAndHold(ShutdownName);
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int Received;
    private int LastDelivered;
    private int Status = StatusOff;
    private int Progress;
    private int Starved;
    private boolean StalledShown;
    private boolean Primed;
    private float Charge;
    private float ChargeOld;
    private float Coil;
    private float CoilOld;
    private float Ring;
    private float RingOld;

    public RealityFolderBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp1730Registry.FolderEntity.get(), Pos, State);
    }

    public static Vec3 fieldCentre(BlockPos Master) {
        return new Vec3(Master.getX() + 0.5, Master.getY() + FieldLift, Master.getZ() + 0.5);
    }

    public static String animationFor(int Shown) {
        return switch (Shown) {
            case StatusSpinning -> SpinName;
            case StatusRunning -> RunningName;
            case StatusShutdown -> ShutdownName;
            default -> OffName;
        };
    }

    public static RawAnimation clipFor(int Shown) {
        return switch (animationFor(Shown)) {
            case SpinName -> Spin;
            case RunningName -> Running;
            case ShutdownName -> Stopping;
            default -> Off;
        };
    }

    public static boolean litFor(int Shown) {
        return Shown != StatusOff;
    }

    public boolean fieldUp() {
        return Status == StatusRunning;
    }

    public int status() {
        return Status;
    }

    public int progress() {
        return Progress;
    }

    public int lastDelivered() {
        return LastDelivered;
    }

    public boolean stalled() {
        return Status == StatusSpinning && Starved > 0;
    }

    public float charge() {
        return switch (Status) {
            case StatusSpinning -> Mth.clamp(Progress / (float) SpinTicks, 0.0f, 1.0f);
            case StatusRunning -> 1.0f;
            case StatusShutdown -> Mth.clamp(Progress / (float) ShutdownTicks, 0.0f, 1.0f);
            default -> 0.0f;
        };
    }

    public float spinShare() {
        return charge();
    }

    public float shownCharge(float Partial) {
        return Mth.lerp(Partial, ChargeOld, Charge);
    }

    public float coilAngle(float Partial) {
        return Mth.lerp(Partial, CoilOld, Coil);
    }

    public float ringAngle(float Partial) {
        return Mth.lerp(Partial, RingOld, Ring);
    }

    @Override
    public int getPowerDemand() {
        return Demand;
    }

    @Override
    public void insertPower(int Amount) {
        Received += Amount;
    }

    public void feed(int Amount) {
        Received += Amount;
    }

    public void raiseForTest() {
        Status = StatusRunning;
        Progress = SpinTicks;
        Starved = 0;
        if (level != null) {
            Scp1730Anchors.folderRunning(level, worldPosition, true);
        }
        setChanged();
    }

    public void advance(boolean Full) {
        switch (Status) {
            case StatusOff -> {
                if (Full) {
                    Status = StatusSpinning;
                    Progress = 0;
                    Starved = 0;
                }
            }
            case StatusSpinning -> {
                if (Full) {
                    Starved = 0;
                    Progress++;
                    if (Progress >= SpinTicks) {
                        Status = StatusRunning;
                        Progress = SpinTicks;
                    }
                } else if (++Starved > GraceTicks) {
                    windDown(Progress / (float) SpinTicks);
                }
            }
            case StatusRunning -> {
                if (Full) {
                    Starved = 0;
                } else if (++Starved > GraceTicks) {
                    windDown(1.0f);
                }
            }
            default -> {
                if (Full) {
                    Status = StatusSpinning;
                    Progress = Mth.clamp(Math.round(SpinTicks * Progress / (float) ShutdownTicks), 0, SpinTicks - 1);
                    Starved = 0;
                } else if (--Progress <= 0) {
                    Status = StatusOff;
                    Progress = 0;
                    Starved = 0;
                }
            }
        }
    }

    private void windDown(float Share) {
        Starved = 0;
        int Left = Mth.ceil(ShutdownTicks * Mth.clamp(Share, 0.0f, 1.0f));
        if (Left <= 0) {
            Status = StatusOff;
            Progress = 0;
            return;
        }
        Status = StatusShutdown;
        Progress = Left;
    }

    public void animate() {
        boolean Stalled = level != null && level.isClientSide ? StalledShown : stalled();
        if (!Primed) {
            Charge = charge();
            ChargeOld = Charge;
            Primed = true;
        }
        ChargeOld = Charge;
        CoilOld = Coil;
        RingOld = Ring;
        switch (Status) {
            case StatusSpinning -> {
                if (!Stalled) {
                    Charge = Math.min(1.0f, Charge + 1.0f / SpinTicks);
                }
            }
            case StatusRunning -> Charge = Math.min(1.0f, Charge + 1.0f / SpinTicks);
            default -> Charge = Math.max(0.0f, Charge - 1.0f / ShutdownTicks);
        }
        Coil += CoilSpeed * Charge;
        Ring += RingSpeed * Charge;
        if (Coil >= 360.0f) {
            Coil -= 360.0f;
            CoilOld -= 360.0f;
        }
        if (Ring >= 360.0f) {
            Ring -= 360.0f;
            RingOld -= 360.0f;
        }
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, RealityFolderBlockEntity Folder) {
        Folder.LastDelivered = Folder.Received;
        boolean Full = Folder.Received >= Demand;
        Folder.Received = 0;
        int Before = Folder.Status;
        boolean WasStalled = Folder.stalled();
        Folder.advance(Full);
        int After = Folder.Status;
        if (After == StatusRunning && Full && (World.getGameTime() + Pos.asLong()) % HumEvery == 0) {
            World.playSound(null, Pos, Scp1730Registry.FolderRunning.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        if (Before != After) {
            if (After == StatusSpinning) {
                World.playSound(null, Pos, Scp1730Registry.FolderSpinup.get(), SoundSource.BLOCKS, 1.4f, 1.0f);
            } else if (After == StatusRunning) {
                World.playSound(null, Pos, Scp1730Registry.AnchorBurst.get(), SoundSource.BLOCKS, 1.6f, 0.6f);
            } else if (Before == StatusRunning || Before == StatusSpinning) {
                World.playSound(null, Pos, Scp1730Registry.FolderShutdown.get(), SoundSource.BLOCKS, Before == StatusRunning ? 3.0f : 1.6f, 0.8f);
            }
            Scp1730Anchors.folderRunning(World, Pos, After == StatusRunning);
            boolean Lit = litFor(After);
            if (State.getValue(RealityFolderBlock.Powered) != Lit) {
                World.setBlock(Pos, State.setValue(RealityFolderBlock.Powered, Lit), Block.UPDATE_CLIENTS);
            }
        }
        if (Before != After || WasStalled != Folder.stalled()) {
            Folder.setChanged();
            World.sendBlockUpdated(Pos, World.getBlockState(Pos), World.getBlockState(Pos), 3);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, RealityFolderBlockEntity Folder) {
        Folder.animate();
        if (Folder.Status != StatusRunning) {
            return;
        }
        Vec3 Centre = fieldCentre(Pos);
        for (int Index = 0; Index < 3; Index++) {
            double Angle = World.random.nextDouble() * Math.PI * 2.0;
            double Rise = (World.random.nextDouble() - 0.35) * Scp1730Anchors.FolderRadius * 0.6;
            double Around = Math.sqrt(Math.max(0.0, Scp1730Anchors.FolderRadius * Scp1730Anchors.FolderRadius - Rise * Rise));
            World.addParticle(Scp1730Registry.AnchorParticle.get(), Centre.x + Math.cos(Angle) * Around, Centre.y + Rise, Centre.z + Math.sin(Angle) * Around, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && Status == StatusRunning) {
            Scp1730Anchors.folderRunning(level, worldPosition, true);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null) {
            Scp1730Anchors.folderRunning(level, worldPosition, false);
        }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Lookup) {
        super.saveAdditional(Tag, Lookup);
        Tag.putInt("Status", Status);
        Tag.putInt("Progress", Progress);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Lookup) {
        super.loadAdditional(Tag, Lookup);
        Status = Tag.getInt("Status");
        Progress = Tag.getInt("Progress");
        StalledShown = Tag.getBoolean("Stalled");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Lookup) {
        CompoundTag Tag = super.getUpdateTag(Lookup);
        Tag.putInt("Status", Status);
        Tag.putInt("Progress", Progress);
        Tag.putBoolean("Stalled", stalled());
        return Tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public AABB renderBox() {
        return new AABB(worldPosition).inflate(2.6, 0.0, 2.6).expandTowards(0.0, 6.2, 0.0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "main", 5, Animated -> Animated.setAndContinue(clipFor(Status))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
