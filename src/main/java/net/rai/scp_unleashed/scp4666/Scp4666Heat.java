package net.rai.scp_unleashed.scp4666;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.rai.scp_unleashed.scp4666.block.Scp4666FireplaceBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666HeaterBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666TallBlock;

public final class Scp4666Heat {
    public static final int LogTicks = 1500;
    public static final int FireMax = 12000;
    public static final int FireStep = 3000;
    public static final int FireLevels = 4;
    public static final int ScuttleTicks = 4800;
    public static final int CoalMax = 14400;
    public static final int CoalStep = 4800;
    public static final int CoalLevels = 3;
    public static final int FireNight = 6000;
    public static final int CoalNight = 4800;
    public static final int[] FireWarmth = {0, 15, 30, 40, 50};
    public static final int[] BoilerWarmth = {0, 20, 35, 50};
    public static final int DraughtWarmth = 10;
    public static final int RefreshEvery = 20;
    public static final int SignalLevel = 3;
    public static final String Prefix = Scp4666Director.Prefix;

    private Scp4666Heat() {
    }

    public static int fireLevel(int Fuel) {
        return Fuel <= 0 ? 0 : Math.min(FireLevels, (Fuel + FireStep - 1) / FireStep);
    }

    public static int boilerLevel(int Fuel) {
        return Fuel <= 0 ? 0 : Math.min(CoalLevels, (Fuel + CoalStep - 1) / CoalStep);
    }

    public static int fireLevel(Scp4666State State) {
        return fireLevel(State.fire());
    }

    public static int boilerLevel(Scp4666State State) {
        return boilerLevel(State.coal());
    }

    public static int warmth(Scp4666State State) {
        int Value = FireWarmth[fireLevel(State)] + BoilerWarmth[boilerLevel(State)] - DraughtWarmth * State.cracked().size();
        return Math.max(0, Math.min(100, Value));
    }

    public static boolean dead(Scp4666State State) {
        return fireLevel(State) == 0 && boilerLevel(State) == 0;
    }

    public static void tick(ServerLevel World, Scp4666Map Layout, Scp4666State State, int Burned) {
        if (Burned > 0) {
            burn(World, Layout, State, Burned, Burned);
        }
        if (World.getServer().getTickCount() % RefreshEvery == 0) {
            refresh(World, Layout, State);
        }
    }

    public static void overnight(ServerLevel World, Scp4666Map Layout, Scp4666State State) {
        burn(World, Layout, State, FireNight, CoalNight);
    }

    public static void burn(ServerLevel World, Scp4666Map Layout, Scp4666State State, int FireTicks, int CoalTicks) {
        int FireBefore = fireLevel(State);
        int CoalBefore = boilerLevel(State);
        State.setFire(State.fire() - FireTicks);
        State.setCoal(State.coal() - CoalTicks);
        int FireAfter = fireLevel(State);
        int CoalAfter = boilerLevel(State);
        if (FireAfter == FireBefore && CoalAfter == CoalBefore) {
            return;
        }
        refresh(World, Layout, State);
        if (FireAfter < FireBefore && FireAfter <= 1) {
            cue(World, Layout, FireAfter == 0 ? "fire_out" : "fire_embers");
        }
        if (CoalAfter < CoalBefore && CoalAfter == 0) {
            cue(World, Layout, "boiler_cold");
            World.playSound(null, Layout.Heater, Scp4666Registry.BoilerTick.get(), SoundSource.BLOCKS, 0.8f, 0.7f);
        }
    }

    private static void cue(ServerLevel World, Scp4666Map Layout, String Key) {
        for (ServerPlayer User : Scp4666World.living(World)) {
            if (Layout.insideCabin(User.position())) {
                User.displayClientMessage(Component.translatable(Prefix + Key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            }
        }
    }

    public static boolean fireFull(Scp4666State State) {
        return State.fire() > FireMax - LogTicks;
    }

    public static boolean boilerFull(Scp4666State State) {
        return State.coal() > CoalMax - ScuttleTicks;
    }

    public static void feedFire(ServerLevel World, Scp4666Map Layout, Scp4666State State) {
        State.setFire(Math.min(FireMax, State.fire() + LogTicks));
        refresh(World, Layout, State);
    }

    public static void feedBoiler(ServerLevel World, Scp4666Map Layout, Scp4666State State) {
        State.setCoal(Math.min(CoalMax, State.coal() + ScuttleTicks));
        refresh(World, Layout, State);
    }

    public static void refresh(ServerLevel World, Scp4666Map Layout, Scp4666State State) {
        int Fire = fireLevel(State);
        setTall(World, Layout.Fireplace, Scp4666FireplaceBlock.class, Fire);
        setTall(World, Layout.Heater, Scp4666HeaterBlock.class, boilerLevel(State));
        BlockState Chimney = World.getBlockState(Layout.Chimney);
        if (Chimney.getBlock() instanceof CampfireBlock) {
            BlockState Next = Chimney.setValue(CampfireBlock.LIT, Fire > 0).setValue(CampfireBlock.SIGNAL_FIRE, Fire >= SignalLevel);
            if (Next != Chimney) {
                World.setBlock(Layout.Chimney, Next, Block.UPDATE_ALL);
            }
        }
    }

    private static void setTall(ServerLevel World, BlockPos Lower, Class<? extends Block> Kind, int Value) {
        for (BlockPos Pos : new BlockPos[]{Lower, Lower.above()}) {
            BlockState Found = World.getBlockState(Pos);
            if (!Kind.isInstance(Found.getBlock())) {
                continue;
            }
            BlockState Next = Found;
            if (Found.getBlock() instanceof Scp4666FireplaceBlock) {
                Next = Found.setValue(Scp4666FireplaceBlock.Fire, Math.min(FireLevels, Value));
            } else if (Found.getBlock() instanceof Scp4666HeaterBlock) {
                Next = Found.setValue(Scp4666HeaterBlock.Heat, Math.min(CoalLevels, Value));
            }
            if (Next != Found) {
                World.setBlock(Pos, Next, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    public static boolean lowerHalf(BlockState State) {
        return State.getValue(Scp4666TallBlock.Half) == DoubleBlockHalf.LOWER;
    }
}
