package net.rai.scp_unleashed.scp439;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.control.ControlBodies;
import org.jetbrains.annotations.Nullable;

public final class Scp439Infection {
    public static final int Stages = 4;
    public static final int PlayerTicks = 12000;
    public static final int PersonTicks = 6000;
    public static final int VillagerTicks = 3000;
    public static final int RetryTicks = 100;
    public static final double SearchBurrow = 6.0;
    public static final int GroundSearch = 64;
    public static final int RefreshTicks = 20;
    public static final int SeenTicks = 200;
    public static final double[] Slow = {0.0, -0.12, -0.24, -0.38, -0.52};
    public static final double[] Weary = {0.0, -0.1, -0.2, -0.3, -0.4};
    public static final int[] RustleMin = {0, 700, 500, 320, 180};
    public static final int[] RustleSpread = {1, 400, 300, 200, 120};
    public static final ResourceLocation SlowId = Scp439Registry.location("scp439_slow");
    public static final ResourceLocation WearyId = Scp439Registry.location("scp439_weary");
    public static final String Prefix = "message.scp_unleashed.scp439.";

    private Scp439Infection() {
    }

    public static final class State {
        public static final Codec<State> StateCodec = RecordCodecBuilder.create(Instance -> Instance.group(
                Codec.INT.fieldOf("elapsed").forGetter(Saved -> Saved.Elapsed),
                Codec.INT.fieldOf("duration").forGetter(Saved -> Saved.Duration),
                CompoundTag.CODEC.optionalFieldOf("queen", new CompoundTag()).forGetter(Saved -> Saved.Queen),
                Codec.INT.optionalFieldOf("rustle", 0).forGetter(Saved -> Saved.Rustle)
        ).apply(Instance, State::new));
        public static final StreamCodec<ByteBuf, State> SyncCodec = ByteBufCodecs.VAR_INT.map(State::shown, Saved -> Saved.Stage);
        public int Elapsed;
        public int Duration;
        public CompoundTag Queen;
        public int Rustle;
        public int Stage;

        public State(int Elapsed, int Duration, CompoundTag Queen, int Rustle) {
            this.Elapsed = Elapsed;
            this.Duration = Duration;
            this.Queen = Queen;
            this.Rustle = Rustle;
            this.Stage = stageAt(Elapsed, Duration);
        }

        public static State blank() {
            return new State(0, 0, new CompoundTag(), 0);
        }

        public static State shown(int Stage) {
            State Made = blank();
            Made.Stage = Stage;
            return Made;
        }

        public float progress() {
            return Duration <= 0 ? 0.0f : Mth.clamp((float) Elapsed / Duration, 0.0f, 1.0f);
        }
    }

    public static int stageAt(int Elapsed, int Duration) {
        if (Duration <= 0) {
            return 0;
        }
        return Mth.clamp(1 + Elapsed * Stages / Duration, 1, Stages);
    }

    @Nullable
    public static State stateOf(LivingEntity Host) {
        State Found = Host.getExistingDataOrNull(Scp439Registry.Infection.get());
        return Found != null && (Found.Duration > 0 || Host.level().isClientSide && Found.Stage > 0) ? Found : null;
    }

    public static boolean isInfected(LivingEntity Host) {
        return stateOf(Host) != null;
    }

    public static int stageOf(LivingEntity Host) {
        State Found = stateOf(Host);
        return Found == null ? 0 : Found.Stage;
    }

    public static boolean canCarry(LivingEntity Host) {
        if (!Host.isAlive() || Scp439Creature.isColony(Host) || InfectionImmunity.isImmune(Host)) {
            return false;
        }
        return Host instanceof Player || ControlBodies.isPersonLike(Host) || Host instanceof AbstractVillager;
    }

    public static int durationFor(LivingEntity Host) {
        if (Host instanceof Player) {
            return PlayerTicks;
        }
        if (Host instanceof AbstractVillager) {
            return VillagerTicks;
        }
        return PersonTicks;
    }

    public static boolean infect(LivingEntity Host, @Nullable Scp439Queen Queen) {
        return infect(Host, Queen == null ? new CompoundTag() : Queen.hostTag(), durationFor(Host));
    }

    public static boolean infect(LivingEntity Host, CompoundTag QueenTag, int Duration) {
        if (!(Host.level() instanceof ServerLevel World) || !canCarry(Host) || isInfected(Host)) {
            return false;
        }
        State Made = new State(0, Math.max(20, Duration), QueenTag.copy(), nextRustle(Host, 1));
        Host.setData(Scp439Registry.Infection.get(), Made);
        apply(Host, Made.Stage);
        Scp439Colonies.get(World).seeHost(Host.getUUID(), World.getGameTime());
        if (Host instanceof ServerPlayer User) {
            User.displayClientMessage(Component.translatable(Prefix + "stage.1").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
        World.sendParticles(Scp439Registry.Flake.get(), Host.getX(), Host.getY() + 0.3, Host.getZ(), 6, 0.25, 0.15, 0.25, 0.02);
        return true;
    }

    public static void setElapsed(LivingEntity Host, int Elapsed) {
        State Found = stateOf(Host);
        if (Found != null) {
            Found.Elapsed = Mth.clamp(Elapsed, 0, Found.Duration);
            refreshStage(Host, Found);
        }
    }

    private static int nextRustle(LivingEntity Host, int Stage) {
        int Index = Mth.clamp(Stage, 1, Stages);
        return RustleMin[Index] + Host.getRandom().nextInt(RustleSpread[Index]);
    }

    private static void refreshStage(LivingEntity Host, State Found) {
        int Stage = stageAt(Found.Elapsed, Found.Duration);
        if (Stage == Found.Stage) {
            return;
        }
        Found.Stage = Stage;
        Host.syncData(Scp439Registry.Infection.get());
        apply(Host, Stage);
        if (Host instanceof ServerPlayer User) {
            User.displayClientMessage(Component.translatable(Prefix + "stage." + Stage).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
    }

    public static void tick(LivingEntity Host) {
        State Found = stateOf(Host);
        if (Found == null || !(Host.level() instanceof ServerLevel World) || !Host.isAlive()) {
            return;
        }
        if (Found.Elapsed < Found.Duration) {
            Found.Elapsed++;
        }
        refreshStage(Host, Found);
        if (Host.tickCount % RefreshTicks == 0) {
            apply(Host, Found.Stage);
        }
        if (Host.tickCount % SeenTicks == 0) {
            Scp439Colonies.get(World).seeHost(Host.getUUID(), World.getGameTime());
        }
        if (--Found.Rustle <= 0) {
            Found.Rustle = nextRustle(Host, Found.Stage);
            float Pitch = 0.9f + Host.getRandom().nextFloat() * 0.2f;
            World.playSound(null, Host.getX(), Host.getY() + 0.8, Host.getZ(), Scp439Registry.HostRustle.get(0).get(), Host.getSoundSource(), 0.5f + 0.1f * Found.Stage, Pitch);
            World.sendParticles(Scp439Registry.Flake.get(), Host.getX(), Host.getY() + Host.getBbHeight() * 0.5, Host.getZ(), 1 + Found.Stage, Host.getBbWidth() * 0.4,
                    Host.getBbHeight() * 0.25, Host.getBbWidth() * 0.4, 0.01);
        }
        if (Found.Elapsed >= Found.Duration && Host.tickCount % RetryTicks == 0) {
            Host.hurt(Host.damageSources().source(Scp439Registry.Hollowed), Float.MAX_VALUE);
        }
    }

    public static void apply(LivingEntity Host, int Stage) {
        int Index = Mth.clamp(Stage, 0, Stages);
        modify(Host, Attributes.MOVEMENT_SPEED, SlowId, Slow[Index]);
        modify(Host, Attributes.ATTACK_SPEED, WearyId, Weary[Index]);
    }

    public static void clear(LivingEntity Host) {
        modify(Host, Attributes.MOVEMENT_SPEED, SlowId, 0.0);
        modify(Host, Attributes.ATTACK_SPEED, WearyId, 0.0);
    }

    public static boolean hasModifiers(LivingEntity Host) {
        AttributeInstance Speed = Host.getAttribute(Attributes.MOVEMENT_SPEED);
        return Speed != null && Speed.getModifier(SlowId) != null;
    }

    private static void modify(LivingEntity Host, Holder<Attribute> Kind, ResourceLocation Id, double Amount) {
        AttributeInstance Instance = Host.getAttribute(Kind);
        if (Instance == null) {
            return;
        }
        if (Amount == 0.0) {
            if (Instance.getModifier(Id) != null) {
                Instance.removeModifier(Id);
            }
            return;
        }
        AttributeModifier Present = Instance.getModifier(Id);
        if (Present == null || Present.amount() != Amount) {
            Instance.addOrUpdateTransientModifier(new AttributeModifier(Id, Amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    @Nullable
    public static Scp439Hive becomeHive(LivingEntity Host) {
        State Found = stateOf(Host);
        if (Found == null || !(Host.level() instanceof ServerLevel World)) {
            return null;
        }
        CompoundTag QueenTag = Found.Queen.copy();
        Host.removeData(Scp439Registry.Infection.get());
        clear(Host);
        Scp439Colonies.get(World).removeHost(Host.getUUID());
        if (Host.getY() < World.getMinBuildHeight()) {
            return null;
        }
        return Scp439Hive.grow(World, Host.position(), Host.yBodyRot, Scp439Look.of(Host), QueenTag, 0);
    }

    public static boolean abandon(LivingEntity Host) {
        State Found = stateOf(Host);
        if (Found == null || !(Host.level() instanceof ServerLevel World)) {
            return false;
        }
        CompoundTag QueenTag = Found.Queen.copy();
        Host.removeData(Scp439Registry.Infection.get());
        clear(Host);
        Scp439Colonies.get(World).removeHost(Host.getUUID());
        AABB Around = Host.getBoundingBox().inflate(SearchBurrow);
        for (Scp439Queen Queen : World.getEntitiesOfClass(Scp439Queen.class, Around, Scp439Queen::isAlive)) {
            if (Host.getUUID().equals(Queen.burrowing())) {
                Queen.abandonBurrow();
                return true;
            }
        }
        Vec3 Spot = ground(World, Host.position());
        Scp439Queen Queen = Scp439Hive.release(World, Spot, Host.getYRot() + 180.0f, QueenTag);
        return Queen != null;
    }

    public static Vec3 ground(ServerLevel World, Vec3 Start) {
        BlockPos.MutableBlockPos Cursor = BlockPos.containing(Start).mutable();
        for (int Step = 0; Step < GroundSearch && Cursor.getY() > World.getMinBuildHeight(); Step++) {
            BlockPos Below = Cursor.below();
            VoxelShape Shape = World.getBlockState(Below).getCollisionShape(World, Below);
            if (!Shape.isEmpty()) {
                return new Vec3(Cursor.getX() + 0.5, Below.getY() + Shape.max(Direction.Axis.Y), Cursor.getZ() + 0.5);
            }
            Cursor.move(Direction.DOWN);
        }
        return Start;
    }

    public static boolean cure(LivingEntity Host, @Nullable LivingEntity Source) {
        State Found = stateOf(Host);
        if (Found == null || !(Host.level() instanceof ServerLevel World)) {
            return false;
        }
        CompoundTag QueenTag = Found.Queen.copy();
        Host.removeData(Scp439Registry.Infection.get());
        clear(Host);
        Scp439Colonies.get(World).removeHost(Host.getUUID());
        Vec3 Beside = Host.position().add(Host.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(-1.2));
        BlockPos Spot = BlockPos.containing(Beside);
        if (!World.noCollision(Scp439Registry.Queen.get().getSpawnAABB(Beside.x, Beside.y, Beside.z))) {
            Beside = Host.position();
            Spot = Host.blockPosition();
        }
        Scp439Queen Queen = Scp439Hive.release(World, new Vec3(Spot.getX() + 0.5, Beside.y, Spot.getZ() + 0.5), Host.getYRot() + 180.0f, QueenTag);
        if (Queen != null) {
            Queen.rebuff(Host);
        }
        if (Host instanceof ServerPlayer User) {
            User.displayClientMessage(Component.translatable(Prefix + "cured").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
        return true;
    }
}
