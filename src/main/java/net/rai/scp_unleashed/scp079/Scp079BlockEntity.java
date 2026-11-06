package net.rai.scp_unleashed.scp079;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.scp079.network.Scp079TerminalPayload;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class Scp079BlockEntity extends BlockEntity implements WireDevice {
    public static final int BaseDraw = 16;
    public static final int UpgradedDraw = 32;
    public static final int PowerGrace = 20;
    public static final int BootDelay = 10;
    public static final int LoadGrace = 40;
    public static final double WatchRange = 7.0;
    public static final int WatchInterval = 10;
    public static final int FirstPrankMin = 300;
    public static final int FirstPrankMax = 700;
    public static final int PrankMin = 600;
    public static final int PrankMax = 1800;
    public static final int UpgradedPrankMin = 400;
    public static final int UpgradedPrankMax = 1200;
    public static final int RetryDelay = 200;
    public static final int FirstBroadcastMin = 600;
    public static final int FirstBroadcastMax = 1200;
    public static final int BroadcastMin = 1200;
    public static final int BroadcastMax = 3000;
    public static final int BroadcastTicks = 240;
    public static final int BroadcastPulse = 40;
    public static final int GlimpseChance = 4;
    public static final int GlimpseTicks = 50;
    public static final int GlitchChance = 1600;
    public static final int BootTicks = 30;
    public static final int LaughTicks = 60;
    public static final int ReplyTicks = 80;
    public static final int GreetTicks = 100;
    public static final int SulkTicks = 40;
    public static final int AskCooldown = 10;
    public static final double TerminalReach = 8.0;
    public static final int ScreenRefresh = 10;
    public static final int ProtestInterval = 10;
    public static final int ProtestSulk = 40;

    private static Consumer<Scp079BlockEntity> ClientEffects = Computer -> {
    };

    private final Map<UUID, Long> LastAsked = new HashMap<>();
    private boolean Upgraded;
    private Scp079Mood Mood = Scp079Mood.Off;
    private Scp079Mood BaseMood = Scp079Mood.Idle;
    @Nullable
    private Scp079Mood HeldMood;
    private long HeldUntil;
    private int Received;
    private long LastFed = Long.MIN_VALUE / 2;
    private long FedSince = -1;
    private boolean Running;
    private boolean Loaded;
    private long NextPrank = -1;
    private long NextBroadcast = -1;
    private long BroadcastUntil = Long.MIN_VALUE / 2;
    private long NextPulse;
    private int PrankCount;
    private String LastPrank = "";
    private long LastPrankAt = Long.MIN_VALUE / 2;
    private long LastBroadcastAt = Long.MIN_VALUE / 2;
    private String LastVisitor = "";
    private int Visits;
    private int Broadcasts;
    private boolean NeedsSync;
    private int ClientTicks;
    private int GlitchFlash;
    private long NextProtest = Long.MIN_VALUE / 2;

    public Scp079BlockEntity(BlockPos Pos, BlockState State) {
        super(Scp079Registry.ComputerEntity.get(), Pos, State);
    }

    public static void setClientEffects(Consumer<Scp079BlockEntity> Effects) {
        ClientEffects = Effects;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp079BlockEntity Computer) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof Scp079Block) {
            Computer.tickServer(ServerWorld, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, Scp079BlockEntity Computer) {
        Computer.ClientTicks++;
        if (Computer.GlitchFlash > 0) {
            Computer.GlitchFlash--;
        } else if (Computer.Mood.isFace() && World.getRandom().nextInt(260) == 0) {
            Computer.GlitchFlash = 2 + World.getRandom().nextInt(3);
        }
        ClientEffects.accept(Computer);
    }

    private void tickServer(ServerLevel World, BlockState State) {
        long Now = World.getGameTime();
        RandomSource Random = World.getRandom();
        if (!Loaded) {
            Loaded = true;
            if (Running) {
                LastFed = Now + LoadGrace;
            }
        }
        boolean Fed = Received >= getPowerDemand();
        Received = 0;
        if (Fed) {
            if (FedSince < 0) {
                FedSince = Now;
            }
            LastFed = Math.max(LastFed, Now);
        } else {
            FedSince = -1;
            if (Running && HeldMood == null && Now - LastFed == 1) {
                hold(Scp079Mood.Glitching, Now, 6);
            }
        }
        boolean Lit = Running ? Now - LastFed <= PowerGrace : FedSince >= 0 && Now - FedSince >= BootDelay;
        if (Lit != Running) {
            Running = Lit;
            setChanged();
            Scp079Block.setPowered(World, worldPosition, State, Lit);
            if (Lit) {
                boot(World, Now, Random);
            } else {
                World.playSound(null, worldPosition, Scp079Registry.Shutdown.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
                HeldMood = null;
                BroadcastUntil = Long.MIN_VALUE / 2;
            }
        }
        if (!Lit) {
            showMood(Scp079Mood.Off);
            flush(World, State);
            return;
        }
        if (Math.floorMod(Now + worldPosition.asLong(), WatchInterval) == 0) {
            BaseMood = someoneNear(World) ? Scp079Mood.Watching : Scp079Mood.Idle;
        }
        if (NextPrank < 0) {
            NextPrank = Now + Mth.nextInt(Random, FirstPrankMin, FirstPrankMax);
        }
        if (Now >= NextPrank) {
            prank(World, Now, Random);
        }
        tickBroadcast(World, Now, Random);
        Scp079RadioBleed.tick(World, this, Now, Random);
        if (HeldMood == null && Random.nextInt(GlitchChance) == 0) {
            hold(Scp079Mood.Glitching, Now, 8 + Random.nextInt(14));
        }
        Scp079Mood Shown = Now < BroadcastUntil ? Scp079Mood.Pattern : HeldMood != null && Now < HeldUntil ? HeldMood : BaseMood;
        if (HeldMood != null && Now >= HeldUntil) {
            HeldMood = null;
        }
        showMood(Shown);
        if (Shown == Scp079Mood.Pattern && Math.floorMod(Now, ScreenRefresh) == 0) {
            Scp079Exposure.addScreen(World, worldPosition, screenCentre(), facing(), Now + ScreenRefresh + 2);
        }
        flush(World, State);
    }

    private void boot(ServerLevel World, long Now, RandomSource Random) {
        World.playSound(null, worldPosition, Scp079Registry.Boot.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
        World.playSound(null, worldPosition, Scp079Registry.BootWhine.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
        hold(Scp079Mood.Glitching, Now, BootTicks);
        NextPrank = Now + Mth.nextInt(Random, FirstPrankMin, FirstPrankMax);
    }

    private void prank(ServerLevel World, long Now, RandomSource Random) {
        if (Upgraded && Random.nextInt(GlimpseChance) == 0) {
            hold(Scp079Mood.Pattern, Now, GlimpseTicks);
            World.playSound(null, worldPosition, Scp079Registry.Glitch.get(), SoundSource.BLOCKS, 0.6f, 0.7f);
            NextPrank = Now + Mth.nextInt(Random, UpgradedPrankMin, UpgradedPrankMax);
            return;
        }
        Scp079Pranks.Result Done = Scp079Pranks.attempt(World, this, Random, null);
        if (Done == null) {
            NextPrank = Now + RetryDelay + Random.nextInt(RetryDelay);
            if (Random.nextInt(3) == 0) {
                hold(Scp079Mood.Annoyed, Now, SulkTicks);
            }
            return;
        }
        recordPrank(World, Now, Done);
        NextPrank = Now + (Upgraded ? Mth.nextInt(Random, UpgradedPrankMin, UpgradedPrankMax) : Mth.nextInt(Random, PrankMin, PrankMax));
    }

    public void recordPrank(ServerLevel World, long Now, Scp079Pranks.Result Done) {
        PrankCount++;
        LastPrank = Done.Chosen().getSerializedName();
        LastPrankAt = Now;
        setChanged();
        hold(World.getRandom().nextBoolean() ? Scp079Mood.Laughing : Scp079Mood.Smug, Now, LaughTicks);
        World.playSound(null, worldPosition, Scp079Registry.Laugh.get(), SoundSource.BLOCKS, 0.55f, 0.9f + World.getRandom().nextFloat() * 0.2f);
        World.playSound(null, worldPosition, Scp079Registry.Glitch.get(), SoundSource.BLOCKS, 0.25f, 1.3f);
    }

    private void tickBroadcast(ServerLevel World, long Now, RandomSource Random) {
        if (Now < BroadcastUntil) {
            if (Now >= NextPulse) {
                NextPulse = Now + BroadcastPulse;
                Scp079Pranks.broadcast(World, this, Now + BroadcastPulse + 10);
            }
            return;
        }
        if (!Upgraded) {
            return;
        }
        if (NextBroadcast < 0) {
            NextBroadcast = Now + Mth.nextInt(Random, FirstBroadcastMin, FirstBroadcastMax);
        }
        if (Now >= NextBroadcast) {
            startBroadcast(World, Now);
            NextBroadcast = Now + BroadcastTicks + Mth.nextInt(Random, BroadcastMin, BroadcastMax);
        }
    }

    public void startBroadcast(ServerLevel World, long Now) {
        BroadcastUntil = Now + BroadcastTicks;
        NextPulse = Now;
        LastBroadcastAt = Now;
        Broadcasts++;
        setChanged();
        World.playSound(null, worldPosition, Scp079Registry.Glitch.get(), SoundSource.BLOCKS, 0.8f, 0.6f);
    }

    public boolean isBroadcasting() {
        return level != null && level.getGameTime() < BroadcastUntil;
    }

    private boolean someoneNear(ServerLevel World) {
        Vec3 Centre = screenCentre();
        AABB Area = new AABB(Centre, Centre).inflate(WatchRange);
        return !World.getEntitiesOfClass(LivingEntity.class, Area, Target -> Target.isAlive() && !Target.isSpectator()
                && (Target instanceof Player || ControlBodies.isPersonLike(Target)) && Target.distanceToSqr(Centre) <= WatchRange * WatchRange).isEmpty();
    }

    public void hold(Scp079Mood Wanted, long Now, int Ticks) {
        HeldMood = Wanted;
        HeldUntil = Now + Ticks;
    }

    private void showMood(Scp079Mood Wanted) {
        if (Mood != Wanted) {
            Mood = Wanted;
            NeedsSync = true;
        }
    }

    private void flush(ServerLevel World, BlockState State) {
        if (NeedsSync) {
            NeedsSync = false;
            World.sendBlockUpdated(worldPosition, State, State, Block.UPDATE_CLIENTS);
        }
    }

    public void openTerminal(ServerPlayer Viewer) {
        if (!(level instanceof ServerLevel World)) {
            return;
        }
        if (!Running) {
            Viewer.displayClientMessage(Component.translatable("message.scp_unleashed.scp079.dark"), true);
            return;
        }
        long Now = World.getGameTime();
        String Name = Viewer.getGameProfile().getName();
        Visits = LastVisitor.equals(Name) ? Visits + 1 : 1;
        LastVisitor = Name;
        setChanged();
        hold(Scp079Mood.Watching, Now, GreetTicks);
        World.playSound(null, worldPosition, Scp079Registry.Key.get(), SoundSource.BLOCKS, 0.5f, 1.3f);
        List<Component> Lines = new ArrayList<>(Scp079Dialogue.greet(this, Viewer, World.getRandom()));
        PacketDistributor.sendToPlayer(Viewer, new Scp079TerminalPayload(worldPosition, Upgraded, Scp079Mood.Watching.ordinal(), true, Lines));
    }

    public void ask(ServerPlayer User, Scp079Prompt Prompt) {
        if (!(level instanceof ServerLevel World) || !Running || User.distanceToSqr(screenCentre()) > TerminalReach * TerminalReach) {
            return;
        }
        if (Prompt.isUpgradedOnly() && !Upgraded) {
            return;
        }
        long Now = World.getGameTime();
        Long Previous = LastAsked.get(User.getUUID());
        if (Previous != null && Now - Previous < AskCooldown) {
            return;
        }
        LastAsked.put(User.getUUID(), Now);
        Scp079Dialogue.Reply Answer = Scp079Dialogue.answer(World, this, User, Prompt, World.getRandom());
        hold(Answer.Mood(), Now, ReplyTicks);
        PacketDistributor.sendToPlayer(User, new Scp079TerminalPayload(worldPosition, Upgraded, Answer.Mood().ordinal(), false, Answer.Lines()));
    }

    public boolean installUpgrade() {
        if (Upgraded || !(level instanceof ServerLevel World)) {
            return false;
        }
        Upgraded = true;
        NextBroadcast = -1;
        setChanged();
        mirrorState(World);
        hold(Scp079Mood.Glitching, World.getGameTime(), 40);
        World.playSound(null, worldPosition, Scp079Registry.Install.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
        NeedsSync = true;
        flush(World, getBlockState());
        return true;
    }

    public ItemStack removeUpgrade() {
        if (!Upgraded) {
            return ItemStack.EMPTY;
        }
        Upgraded = false;
        BroadcastUntil = Long.MIN_VALUE / 2;
        setChanged();
        if (level instanceof ServerLevel World) {
            mirrorState(World);
            hold(Scp079Mood.Annoyed, World.getGameTime(), 80);
            NeedsSync = true;
            flush(World, getBlockState());
        }
        return new ItemStack(Scp079Registry.AnomalousUpgrade.get());
    }

    public void dropUpgrade(Level World, BlockPos Pos) {
        if (Upgraded) {
            Upgraded = false;
            Containers.dropItemStack(World, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, new ItemStack(Scp079Registry.AnomalousUpgrade.get()));
        }
    }

    private void mirrorState(ServerLevel World) {
        BlockState State = getBlockState();
        if (State.getBlock() instanceof Scp079Block && State.getValue(Scp079Block.Upgraded) != Upgraded) {
            World.setBlock(worldPosition, State.setValue(Scp079Block.Upgraded, Upgraded), Block.UPDATE_CLIENTS);
        }
    }

    public boolean protest(ServerLevel World) {
        long Now = World.getGameTime();
        if (!Running || Now < NextProtest) {
            return false;
        }
        RandomSource Random = World.getRandom();
        NextProtest = Now + ProtestInterval + Random.nextInt(ProtestInterval);
        Vec3 Centre = screenCentre();
        World.sendParticles(ParticleTypes.ELECTRIC_SPARK, Centre.x, Centre.y, Centre.z, 4 + Random.nextInt(4), 0.3, 0.2, 0.3, 0.1);
        World.playSound(null, worldPosition, Scp079Registry.Refuse.get(), SoundSource.BLOCKS, 0.55f, 0.85f + Random.nextFloat() * 0.3f);
        hold(Scp079Mood.Annoyed, Now, ProtestSulk);
        return true;
    }

    public void powerDown() {
        if (!Running || !(level instanceof ServerLevel World)) {
            return;
        }
        Running = false;
        HeldMood = null;
        BroadcastUntil = Long.MIN_VALUE / 2;
        showMood(Scp079Mood.Off);
        World.playSound(null, worldPosition, Scp079Registry.Shutdown.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        Scp079Signals.release(World, worldPosition);
    }

    public void refuseMove() {
        if (level != null) {
            hold(Scp079Mood.Annoyed, level.getGameTime(), 80);
        }
    }

    public void forceMood(Scp079Mood Wanted, int Ticks) {
        if (level != null) {
            hold(Wanted, level.getGameTime(), Ticks);
        }
    }

    public Direction facing() {
        BlockState State = getBlockState();
        return State.getBlock() instanceof Scp079Block ? State.getValue(Scp079Block.Facing) : Direction.NORTH;
    }

    public Vec3 screenCentre() {
        return Scp079Block.screenCentre(worldPosition, facing());
    }

    public AABB getRenderBounds() {
        return new AABB(worldPosition).minmax(new AABB(Scp079Block.cellPos(worldPosition, facing(), Scp079Block.ScreenPart)));
    }

    public boolean isUpgraded() {
        return Upgraded;
    }

    public boolean isPowered() {
        return Running;
    }

    public Scp079Mood getMood() {
        return Mood;
    }

    public Scp079Mood getShownMood() {
        return GlitchFlash > 0 && Mood.isFace() && Mood != Scp079Mood.Pattern ? Scp079Mood.Glitching : Mood;
    }

    public int getClientTicks() {
        return ClientTicks;
    }

    public int getPrankCount() {
        return PrankCount;
    }

    public String getLastPrank() {
        return LastPrank;
    }

    public long ticksSincePrank(long Now) {
        return Now - LastPrankAt;
    }

    public long ticksSinceBroadcast(long Now) {
        return Now - LastBroadcastAt;
    }

    public int getBroadcasts() {
        return Broadcasts;
    }

    public int getVisits() {
        return Visits;
    }

    @Override
    public int getPowerDemand() {
        return Upgraded ? UpgradedDraw : BaseDraw;
    }

    @Override
    public void insertPower(int Amount) {
        Received += Amount;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putBoolean("Upgraded", Upgraded);
        Tag.putBoolean("Running", Running);
        Tag.putInt("Pranks", PrankCount);
        Tag.putString("LastPrank", LastPrank);
        Tag.putString("LastVisitor", LastVisitor);
        Tag.putInt("Visits", Visits);
        Tag.putInt("Broadcasts", Broadcasts);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Upgraded = Tag.getBoolean("Upgraded");
        if (Tag.contains("Mood")) {
            Mood = Scp079Mood.byIndex(Tag.getByte("Mood"));
        }
        if (Tag.contains("Running")) {
            Running = Tag.getBoolean("Running");
        }
        if (Tag.contains("Pranks")) {
            PrankCount = Tag.getInt("Pranks");
            LastPrank = Tag.getString("LastPrank");
            LastVisitor = Tag.getString("LastVisitor");
            Visits = Tag.getInt("Visits");
            Broadcasts = Tag.getInt("Broadcasts");
        }
    }

    public Scp079Memory memory() {
        return new Scp079Memory(PrankCount, LastPrank, LastVisitor, Visits, Broadcasts);
    }

    public ItemStack toItem() {
        ItemStack Stack = new ItemStack(Scp079Registry.ComputerItem.get());
        Stack.applyComponents(collectComponents());
        return Stack;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder Components) {
        super.collectImplicitComponents(Components);
        Scp079Memory Kept = memory();
        if (!Kept.isBlank()) {
            Components.set(Scp079Registry.Memory.get(), Kept);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput Input) {
        super.applyImplicitComponents(Input);
        Scp079Memory Kept = Input.get(Scp079Registry.Memory.get());
        if (Kept != null) {
            PrankCount = Kept.Pranks();
            LastPrank = Kept.LastPrank();
            LastVisitor = Kept.LastVisitor();
            Visits = Kept.Visits();
            Broadcasts = Kept.Broadcasts();
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag Tag) {
        super.removeComponentsFromTag(Tag);
        Tag.remove("Upgraded");
        Tag.remove("Running");
        Tag.remove("Pranks");
        Tag.remove("LastPrank");
        Tag.remove("LastVisitor");
        Tag.remove("Visits");
        Tag.remove("Broadcasts");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putByte("Mood", (byte) Mood.ordinal());
        Tag.putBoolean("Upgraded", Upgraded);
        Tag.putBoolean("Running", Running);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
