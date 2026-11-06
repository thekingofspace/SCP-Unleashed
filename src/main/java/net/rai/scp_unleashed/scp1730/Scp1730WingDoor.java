package net.rai.scp_unleashed.scp1730;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.scp1730.block.Scp1730SealBlock;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Fixture;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Plan;

import java.util.List;

public final class Scp1730WingDoor {
    public static final int OpenTicks = 200;
    public static final int Retry = 20;
    public static final int CheckEvery = 10;

    private Scp1730WingDoor() {
    }

    public static List<Scp1730Fixture> seals(Scp1730Plan Plan, int Zone) {
        return Plan.fixtures("bioseal", Zone);
    }

    public static boolean open(ServerLevel World, int Run, int Zone) {
        List<Scp1730Fixture> Seals = seals(Scp1730World.plan(Run), Zone);
        for (Scp1730Fixture Each : Seals) {
            if (World.getBlockState(Scp1730World.world(Run, Each)).getBlock() instanceof Scp1730SealBlock) {
                return false;
            }
        }
        return !Seals.isEmpty();
    }

    public static void use(ServerLevel World, BlockPos Pos, ServerPlayer User) {
        Scp1730State State = Scp1730State.get(World.getServer());
        int Run = State.run();
        int Zone = Scp1730World.zoneOf(Run, Pos);
        if (Zone < 0 || seals(Scp1730World.plan(Run), Zone).isEmpty()) {
            return;
        }
        if (open(World, Run, Zone)) {
            State.setWingClose(Zone, World.getGameTime());
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp1730.wing_closing").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        unseal(World, Run, Zone);
        State.setWingClose(Zone, World.getGameTime() + OpenTicks);
        User.displayClientMessage(Component.translatable("message.scp_unleashed.scp1730.wing_open").withStyle(ChatFormatting.YELLOW), true);
    }

    public static void unseal(ServerLevel World, int Run, int Zone) {
        double Sx = 0.0;
        double Sy = 0.0;
        double Sz = 0.0;
        List<Scp1730Fixture> Seals = seals(Scp1730World.plan(Run), Zone);
        for (Scp1730Fixture Each : Seals) {
            BlockPos At = Scp1730World.world(Run, Each);
            BlockState Current = World.getBlockState(At);
            if (Current.getBlock() instanceof Scp1730SealBlock) {
                World.setBlock(At, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                World.levelEvent(2001, At, Block.getId(Current));
            }
            Sx += At.getX() + 0.5;
            Sy += At.getY() + 0.5;
            Sz += At.getZ() + 0.5;
        }
        if (!Seals.isEmpty()) {
            World.playSound(null, Sx / Seals.size(), Sy / Seals.size(), Sz / Seals.size(), Scp1730Registry.SealOpen.get(), SoundSource.BLOCKS, 1.6f, 1.05f);
        }
    }

    public static AABB doorway(int Run, List<Scp1730Fixture> Seals) {
        AABB Box = null;
        for (Scp1730Fixture Each : Seals) {
            AABB Cell = new AABB(Scp1730World.world(Run, Each));
            Box = Box == null ? Cell : Box.minmax(Cell);
        }
        return Box;
    }

    public static boolean reseal(ServerLevel World, int Run, int Zone) {
        Scp1730Plan Plan = Scp1730World.plan(Run);
        List<Scp1730Fixture> Seals = seals(Plan, Zone);
        if (Seals.isEmpty()) {
            return true;
        }
        AABB Box = doorway(Run, Seals);
        if (!World.getEntitiesOfClass(LivingEntity.class, Box.inflate(0.05), LivingEntity::isAlive).isEmpty()) {
            return false;
        }
        double Sx = 0.0;
        double Sy = 0.0;
        double Sz = 0.0;
        for (Scp1730Fixture Each : Seals) {
            BlockPos At = Scp1730World.world(Run, Each);
            BlockState Current = World.getBlockState(At);
            if (!(Current.getBlock() instanceof Scp1730SealBlock)) {
                if (!Current.isAir()) {
                    World.destroyBlock(At, true);
                }
                World.setBlock(At, Scp1730World.state(Plan.stateAt(Each.X(), Each.Y(), Each.Z())), Block.UPDATE_ALL);
            }
            Sx += At.getX() + 0.5;
            Sy += At.getY() + 0.5;
            Sz += At.getZ() + 0.5;
        }
        World.playSound(null, Sx / Seals.size(), Sy / Seals.size(), Sz / Seals.size(), Scp1730Registry.SealOpen.get(), SoundSource.BLOCKS, 1.4f, 0.7f);
        return true;
    }

    public static void tick(ServerLevel World, Scp1730State State, int Run) {
        if (World.getGameTime() % CheckEvery != 0) {
            return;
        }
        closeDue(World, State, Run);
    }

    public static void closeDue(ServerLevel World, Scp1730State State, int Run) {
        long Now = World.getGameTime();
        for (int Zone = 0; Zone < Scp1730State.ZoneCount; Zone++) {
            long Close = State.wingClose(Zone);
            if (Close <= 0L || Now < Close) {
                continue;
            }
            if (reseal(World, Run, Zone)) {
                State.setWingClose(Zone, 0L);
            } else {
                State.setWingClose(Zone, Now + Retry);
            }
        }
    }
}
