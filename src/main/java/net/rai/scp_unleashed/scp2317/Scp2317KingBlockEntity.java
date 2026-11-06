package net.rai.scp_unleashed.scp2317;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;
import net.rai.scp_unleashed.scp2317.note.Scp2317RiteNote;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Scp2317KingBlockEntity extends BlockEntity implements GeoBlockEntity {
    public static final int CheckEvery = 20;
    public static final int StirBase = 1600;
    public static final int StirStep = 180;
    public static final double ReachX = 22.0;
    public static final double ReachY = 26.0;
    private static final RawAnimation Dormant = RawAnimation.begin().thenLoop("dormant");
    private static final RawAnimation Stir = RawAnimation.begin().thenPlay("stir");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int Broken;
    private int Cracked;

    public Scp2317KingBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp2317Registry.KingEntity.get(), Pos, State);
    }

    public int broken() {
        return Broken;
    }

    public int crackedMask() {
        return Cracked;
    }

    public boolean sync(int Value) {
        int Next = Math.max(Broken, Math.min(Scp2317Chains.count(), Value));
        if (Next == Broken) {
            return false;
        }
        Broken = Next;
        changed();
        return true;
    }

    public void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public AABB renderBounds() {
        return new AABB(worldPosition).inflate(ReachX, 0.0, ReachX).expandTowards(0.0, ReachY, 0.0);
    }

    public Vec3 anchorWorld(int Index) {
        return Vec3.atLowerCornerOf(worldPosition).add(Scp2317Chains.anchor(Index));
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp2317KingBlockEntity Self) {
        if (!(World instanceof ServerLevel Server) || Server.getGameTime() % CheckEvery != 0) {
            return;
        }
        Self.sync(ScarletState.get(Server.getServer()).broken());
        Scp2317RiteNote.tick(Server, Self);
        Self.crack(Server);
        if (Server.random.nextInt(Math.max(200, StirBase - Self.Broken * StirStep) / CheckEvery) == 0) {
            Self.triggerAnim("stir", "stir");
            Server.playSound(null, Pos.getX() + 0.5, Pos.getY() + 6.0, Pos.getZ() + 0.5, Scp2317Registry.Stir.get(), SoundSource.AMBIENT, 1.2f + Self.Broken * 0.2f, 0.6f);
        }
    }

    public void crack(ServerLevel World) {
        for (int Index = 0; Index < Broken; Index++) {
            int Bit = 1 << Index;
            if ((Cracked & Bit) != 0) {
                continue;
            }
            if (crackPillar(World, Index)) {
                Cracked |= Bit;
                setChanged();
            }
        }
    }

    public boolean crackPillar(ServerLevel World, int Index) {
        int[] Centre = Scp2317Plan.pillarCentre(Scp2317Chains.Pillar[Index], Scp2317Plan.CavePillarRadius);
        int BaseX = worldPosition.getX() + Centre[0];
        int BaseZ = worldPosition.getZ() + Centre[1];
        int Floor = worldPosition.getY() - 1;
        if (!World.isLoaded(new BlockPos(BaseX, Floor, BaseZ))) {
            return false;
        }
        Vec3 Anchor = anchorWorld(Index);
        for (int Dx = -1; Dx <= 1; Dx++) {
            for (int Dz = -1; Dz <= 1; Dz++) {
                for (int Y = Floor + 1; Y <= Floor + 30; Y++) {
                    BlockPos At = new BlockPos(BaseX + Dx, Y, BaseZ + Dz);
                    BlockState Here = World.getBlockState(At);
                    BlockState Next = Scp2317Plan.cracked(Here, At.getX(), At.getY(), At.getZ());
                    if (Next != Here) {
                        World.setBlock(At, Next, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        BlockPos Socket = BlockPos.containing(Anchor);
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE_BRICKS.defaultBlockState()), Anchor.x, Anchor.y, Anchor.z, 40, 0.4, 0.6, 0.4, 0.1);
        World.sendParticles(ParticleTypes.CRIT, Anchor.x, Anchor.y, Anchor.z, 16, 0.3, 0.3, 0.3, 0.2);
        Scp2317Sounds.play(World, Vec3.atCenterOf(Socket), Scp2317Registry.SnapLayers, SoundSource.BLOCKS, 2.0f, 0.7f);
        Vec3 Body = Vec3.atLowerCornerOf(worldPosition).add(Scp2317Chains.attach(Index));
        World.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.STONE.defaultBlockState()), Body.x, Body.y + 1.0, Body.z, 30, 1.2, 1.5, 1.2, 0.0);
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "dormant", 10, State -> State.setAndContinue(Dormant)));
        Controllers.add(new AnimationController<>(this, "stir", 4, State -> PlayState.STOP).triggerableAnim("stir", Stir));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    @Override
    protected void saveAdditional(CompoundTag Saved, HolderLookup.Provider Lookup) {
        super.saveAdditional(Saved, Lookup);
        Saved.putInt("Broken", Broken);
        Saved.putInt("Cracked", Cracked);
    }

    @Override
    protected void loadAdditional(CompoundTag Saved, HolderLookup.Provider Lookup) {
        super.loadAdditional(Saved, Lookup);
        Broken = Saved.getInt("Broken");
        Cracked = Saved.getInt("Cracked");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Lookup) {
        CompoundTag Saved = new CompoundTag();
        saveAdditional(Saved, Lookup);
        return Saved;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
