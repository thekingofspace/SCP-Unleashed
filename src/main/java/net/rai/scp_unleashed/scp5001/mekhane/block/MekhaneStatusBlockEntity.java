package net.rai.scp_unleashed.scp5001.mekhane.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.rai.scp_unleashed.scp5001.core.Scp5001State;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneDirector;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneProtocol;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneValues;

public abstract class MekhaneStatusBlockEntity extends BlockEntity {
    public static final int RefreshEvery = 10;
    private int Stage;
    private int Phase;
    private long Iron;
    private long Electronics;
    private long IronNeed = MekhaneValues.ironNeed();
    private long ElectronicsNeed = MekhaneValues.electronicsNeed();

    protected MekhaneStatusBlockEntity(BlockEntityType<?> Type, BlockPos Pos, BlockState State) {
        super(Type, Pos, State);
    }

    public static boolean lit(MekhaneProtocol.Stage Stage, Scp5001State.Phase Phase) {
        return MekhaneDirector.awake(Phase) || Stage != MekhaneProtocol.Stage.Idle;
    }

    protected void refresh(BooleanProperty Awake) {
        if (level == null || level.isClientSide || level.getGameTime() % RefreshEvery != 0) {
            return;
        }
        MinecraftServer Server = level.getServer();
        if (Server == null) {
            return;
        }
        MekhaneProtocol Protocol = MekhaneProtocol.get(Server);
        Scp5001State State = Scp5001State.get(Server);
        claim(Protocol);
        int NextStage = Protocol.stage().ordinal();
        int NextPhase = State.phase().ordinal();
        long NextIron = Protocol.iron();
        long NextElectronics = Protocol.electronics();
        long NextIronNeed = MekhaneValues.ironNeed();
        long NextElectronicsNeed = MekhaneValues.electronicsNeed();
        if (NextStage != Stage || NextPhase != Phase || NextIron != Iron || NextElectronics != Electronics || NextIronNeed != IronNeed || NextElectronicsNeed != ElectronicsNeed) {
            Stage = NextStage;
            Phase = NextPhase;
            Iron = NextIron;
            Electronics = NextElectronics;
            IronNeed = NextIronNeed;
            ElectronicsNeed = NextElectronicsNeed;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        boolean Wanted = lit(Protocol.stage(), State.phase());
        BlockState Current = getBlockState();
        if (Current.hasProperty(Awake) && Current.getValue(Awake) != Wanted) {
            level.setBlock(worldPosition, Current.setValue(Awake, Wanted), Block.UPDATE_ALL);
        }
    }

    protected void claim(MekhaneProtocol Protocol) {
    }

    public MekhaneProtocol.Stage stage() {
        MekhaneProtocol.Stage[] All = MekhaneProtocol.Stage.values();
        return All[Math.max(0, Math.min(All.length - 1, Stage))];
    }

    public Scp5001State.Phase phase() {
        Scp5001State.Phase[] All = Scp5001State.Phase.values();
        return All[Math.max(0, Math.min(All.length - 1, Phase))];
    }

    public long iron() {
        return Iron;
    }

    public long electronics() {
        return Electronics;
    }

    public long ironNeed() {
        return Math.max(1, IronNeed);
    }

    public long electronicsNeed() {
        return Math.max(1, ElectronicsNeed);
    }

    private void write(CompoundTag Tag) {
        Tag.putInt("Stage", Stage);
        Tag.putInt("Phase", Phase);
        Tag.putLong("Iron", Iron);
        Tag.putLong("Electronics", Electronics);
        Tag.putLong("IronNeed", IronNeed);
        Tag.putLong("ElectronicsNeed", ElectronicsNeed);
    }

    private void read(CompoundTag Tag) {
        Stage = Tag.getInt("Stage");
        Phase = Tag.getInt("Phase");
        Iron = Tag.getLong("Iron");
        Electronics = Tag.getLong("Electronics");
        IronNeed = Tag.contains("IronNeed") ? Tag.getLong("IronNeed") : MekhaneValues.ironNeed();
        ElectronicsNeed = Tag.contains("ElectronicsNeed") ? Tag.getLong("ElectronicsNeed") : MekhaneValues.electronicsNeed();
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        write(Tag);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        read(Tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        write(Tag);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
