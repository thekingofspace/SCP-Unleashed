package net.rai.scp_unleashed.scp1730.puzzle;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp1730.Scp1730Breach;
import net.rai.scp_unleashed.scp1730.Scp1730Director;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.Scp1730State;
import net.rai.scp_unleashed.scp1730.Scp1730World;
import net.rai.scp_unleashed.scp1730.block.Scp1730ControlBlock;
import net.rai.scp_unleashed.scp1730.block.Scp1730SealBlock;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Blocks;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Fixture;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Plan;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Puzzles;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730CableJunctionBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730GeneratorBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730HatchBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730JunctionBoxBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730LockdownPanelBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730PickupBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730PylonBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730SluiceBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730SwitchBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730TerminalBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730ValveBlock;
import net.rai.scp_unleashed.scp1730.puzzle.block.Scp1730VentGrateBlock;

import java.util.List;

public final class Scp1730PuzzleDirector {
    public static final String Prefix = "message.scp_unleashed.scp1730.puzzle.";
    public static final int TickEvery = 10;
    public static final int HoldTicks = 600;
    public static final double HoldReach = 4.0;
    public static final int LockoutTicks = 100;
    public static final int RespawnDelay = 1200;
    public static final int RespawnEvery = 40;
    public static final int SampleSlow = 40;

    public record Context(ServerLevel World, int Run, int Zone, Scp1730Plan Plan, int Puzzle, Scp1730PuzzleState.Zone Data) {
    }

    private Scp1730PuzzleDirector() {
    }

    public static Context zoneContext(ServerLevel World, int Run, int Zone) {
        if (Zone < 0 || Zone >= Scp1730Puzzles.Zones || Run < 0) {
            return null;
        }
        Scp1730Plan Plan = Scp1730World.plan(Run);
        int Puzzle = Scp1730Puzzles.puzzleOf(Plan, Zone);
        if (Puzzle < 0) {
            return null;
        }
        return new Context(World, Run, Zone, Plan, Puzzle, Scp1730PuzzleState.get(World.getServer()).zone(Run, Zone));
    }

    public static Context context(ServerLevel World, BlockPos Pos) {
        if (!Scp1730World.inside(World)) {
            return null;
        }
        Scp1730State Main = Scp1730State.get(World.getServer());
        int Run = Main.run();
        if (!Main.active() || !Scp1730World.inPlan(Run, Pos.getX(), Pos.getZ())) {
            return null;
        }
        return zoneContext(World, Run, Scp1730World.zoneOf(Run, Pos));
    }

    public static Scp1730Fixture at(Context C, String Type, BlockPos Pos) {
        int X = Pos.getX() - Scp1730World.originX(C.Run());
        int Z = Pos.getZ() - Scp1730World.originZ();
        for (Scp1730Fixture Each : C.Plan().fixtures(Type, C.Zone())) {
            if (Each.X() == X && Each.Y() == Pos.getY() && Each.Z() == Z) {
                return Each;
            }
        }
        return null;
    }

    public static BlockPos where(Context C, Scp1730Fixture Fixture) {
        return Scp1730World.world(C.Run(), Fixture);
    }

    static ItemStack hand(ServerPlayer User, Item Wanted) {
        if (User.getMainHandItem().is(Wanted)) {
            return User.getMainHandItem();
        }
        return User.getOffhandItem().is(Wanted) ? User.getOffhandItem() : ItemStack.EMPTY;
    }

    static ItemStack offered(ServerPlayer User, ItemStack Held, Item Wanted) {
        if (Held.is(Wanted)) {
            return Held;
        }
        return Held.isEmpty() ? hand(User, Wanted) : ItemStack.EMPTY;
    }

    static void spend(ServerPlayer User, ItemStack Stack) {
        if (!User.getAbilities().instabuild) {
            Stack.shrink(1);
        }
    }

    static void bar(ServerPlayer User, String Key, ChatFormatting Style, Object... Args) {
        User.displayClientMessage(Component.translatable(Prefix + Key, Args).withStyle(Style), true);
    }

    static void sound(ServerLevel World, BlockPos Pos, SoundEvent Sound, float Volume, float Pitch) {
        World.playSound(null, Pos, Sound, SoundSource.BLOCKS, Volume, Pitch);
    }

    static void deny(ServerLevel World, BlockPos Pos, ServerPlayer User, String Key) {
        sound(World, Pos, Scp1730Registry.ReaderDeny.get(), 0.7f, 0.9f);
        bar(User, Key, ChatFormatting.RED);
    }

    static <T extends Comparable<T>> void change(ServerLevel World, BlockPos Pos, Property<T> Prop, T Value) {
        if (!World.isLoaded(Pos)) {
            return;
        }
        BlockState Current = World.getBlockState(Pos);
        if (Current.hasProperty(Prop) && !Current.getValue(Prop).equals(Value)) {
            World.setBlock(Pos, Current.setValue(Prop, Value), Block.UPDATE_ALL);
        }
    }

    public static void pa(Context C, String Key) {
        MutableComponent Line = Component.translatable(Prefix + "pa", Component.translatable(Prefix + Key)).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        for (ServerPlayer User : Scp1730Director.inside(C.World())) {
            User.sendSystemMessage(Line);
            C.World().playSound(null, User.getX(), User.getY() + 3.0, User.getZ(), Scp1730PuzzleRegistry.Pa.get(), SoundSource.AMBIENT, 0.8f, 1.0f);
            C.World().playSound(null, User.getX(), User.getY() + 3.0, User.getZ(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.AMBIENT, 0.35f, 0.7f);
        }
    }

    public static int lamps(Context C) {
        return Math.min(Scp1730Puzzles.steps(C.Puzzle()), Integer.bitCount(C.Data().Progress));
    }

    public static void refresh(Context C, boolean Blink) {
        Scp1730Fixture Panel = Scp1730Puzzles.panel(C.Plan(), C.Zone());
        if (Panel == null) {
            return;
        }
        int Lit = lamps(C);
        if (Blink && !C.Data().Solved && C.World().getGameTime() / TickEvery % 2 == 0) {
            Lit = Math.min(Scp1730Puzzles.steps(C.Puzzle()), Lit + 1);
        }
        change(C.World(), where(C, Panel), Scp1730LockdownPanelBlock.Lit, Lit);
    }

    public static void step(Context C, BlockPos Pos, int Bit, String Key) {
        Scp1730PuzzleState.Zone Data = C.Data();
        if (Data.has(Bit) || Data.Solved) {
            return;
        }
        Data.Progress |= 1 << Bit;
        Data.changed();
        sound(C.World(), Pos, Scp1730PuzzleRegistry.Step.get(), 1.0f, 1.0f);
        if (Integer.bitCount(Data.Progress) >= Scp1730Puzzles.steps(C.Puzzle())) {
            solve(C);
            return;
        }
        pa(C, Key);
        refresh(C, false);
    }

    public static void unstep(Context C, int Bit) {
        C.Data().Progress &= ~(1 << Bit);
        C.Data().changed();
        refresh(C, false);
    }

    public static void solve(Context C) {
        Scp1730PuzzleState.Zone Data = C.Data();
        if (Data.Solved) {
            return;
        }
        Data.Solved = true;
        Data.Progress = (1 << Scp1730Puzzles.steps(C.Puzzle())) - 1;
        Data.Timer = 0L;
        Data.changed();
        refresh(C, false);
        if (C.Zone() < Scp1730Puzzles.DeepZone) {
            Scp1730Director.openSeal(C.World(), C.Run(), C.Zone());
        } else {
            openGate(C, true);
        }
        pa(C, "lifted." + C.Zone());
    }

    public static boolean use(ServerLevel World, BlockPos Pos, BlockState State, ServerPlayer User, ItemStack Held) {
        Context C = context(World, Pos);
        if (C == null) {
            return idle(World, Pos, State, User, Held);
        }
        if (State.getBlock() instanceof Scp1730LockdownPanelBlock) {
            return panel(C, Pos, User, Held);
        }
        Scp1730Fixture Device = at(C, "pdevice", Pos);
        if (Device == null) {
            return idle(World, Pos, State, User, Held);
        }
        int Role = Scp1730Puzzles.role(Device.Data());
        int Extra = Scp1730Puzzles.extra(Device.Data());
        return switch (Role) {
            case Scp1730Puzzles.RoleJunction -> junction(C, Pos, State, User, Held);
            case Scp1730Puzzles.RoleTerminalA, Scp1730Puzzles.RoleTerminalB -> terminal(C, Pos, State, User, Held, Role == Scp1730Puzzles.RoleTerminalA ? 0 : 1, Extra);
            case Scp1730Puzzles.RoleKeyA, Scp1730Puzzles.RoleKeyB -> key(C, Pos, State, User, Held, Role == Scp1730Puzzles.RoleKeyA ? 0 : 1);
            case Scp1730Puzzles.RoleInterlock -> lever(C, Pos, State, User, Held, 0, "vent.0", Scp1730Registry.Breaker.get());
            case Scp1730Puzzles.RoleVent, Scp1730Puzzles.RoleVentInner -> vent(C, Pos, State, User, Held, Role == Scp1730Puzzles.RoleVent);
            case Scp1730Puzzles.RoleFan -> fan(C, Pos, State, User, Held);
            case Scp1730Puzzles.RoleCableA, Scp1730Puzzles.RoleCableB -> cable(C, Pos, State, User, Held, Role == Scp1730Puzzles.RoleCableA ? 0 : 1);
            case Scp1730Puzzles.RoleRelayRed, Scp1730Puzzles.RoleRelayYellow, Scp1730Puzzles.RoleRelayBlue -> relay(C, Pos, State, User, Held, Role - Scp1730Puzzles.RoleRelayRed,
                    Extra);
            case Scp1730Puzzles.RoleHatch -> hatch(C, Pos, State, User, Held);
            case Scp1730Puzzles.RoleLockA, Scp1730Puzzles.RoleLockB, Scp1730Puzzles.RoleLockC -> lever(C, Pos, State, User, Held, Role - Scp1730Puzzles.RoleLockA,
                    "cells." + (Role - Scp1730Puzzles.RoleLockA), Scp1730Registry.Release.get());
            case Scp1730Puzzles.RoleGenerator -> generator(C, Pos, State, User, Held);
            case Scp1730Puzzles.RoleInflow, Scp1730Puzzles.RoleBypass, Scp1730Puzzles.RoleDrain -> valve(C, Pos, State, User, Held, Role - Scp1730Puzzles.RoleInflow);
            case Scp1730Puzzles.RoleController -> controller(C, Pos, State, User, Held);
            case Scp1730Puzzles.RolePylonA, Scp1730Puzzles.RolePylonB, Scp1730Puzzles.RolePylonC -> pylon(C, Pos, State, User, Held, Role - Scp1730Puzzles.RolePylonA, Extra);
            case Scp1730Puzzles.RoleSluiceA, Scp1730Puzzles.RoleSluiceB -> sluice(C, Pos, State, User, Held, Role - Scp1730Puzzles.RoleSluiceA);
            default -> idle(World, Pos, State, User, Held);
        };
    }

    static boolean idle(ServerLevel World, BlockPos Pos, BlockState State, ServerPlayer User, ItemStack Held) {
        if (!Held.isEmpty()) {
            return false;
        }
        Block Kind = State.getBlock();
        if (Kind instanceof Scp1730SwitchBlock) {
            World.setBlock(Pos, State.cycle(Scp1730SwitchBlock.On), Block.UPDATE_ALL);
            sound(World, Pos, Scp1730Registry.Breaker.get(), 0.8f, 1.1f);
        } else if (Kind instanceof Scp1730ValveBlock) {
            World.setBlock(Pos, State.cycle(Scp1730ValveBlock.Open), Block.UPDATE_ALL);
            sound(World, Pos, Scp1730PuzzleRegistry.ValveTurn.get(), 0.8f, 1.0f);
        } else if (Kind instanceof Scp1730VentGrateBlock) {
            World.setBlock(Pos, State.cycle(Scp1730VentGrateBlock.Open), Block.UPDATE_ALL);
            sound(World, Pos, Scp1730PuzzleRegistry.VentOpen.get(), 0.8f, 1.0f);
        } else if (Kind instanceof Scp1730TerminalBlock) {
            World.setBlock(Pos, State.cycle(Scp1730TerminalBlock.Glyph), Block.UPDATE_ALL);
            sound(World, Pos, Scp1730PuzzleRegistry.Beep.get(), 0.6f, 1.2f);
        } else if (Kind instanceof Scp1730PylonBlock) {
            World.setBlock(Pos, State.cycle(Scp1730PylonBlock.Glyph), Block.UPDATE_ALL);
            sound(World, Pos, Scp1730PuzzleRegistry.Crank.get(), 0.8f, 1.4f);
        } else if (Kind instanceof Scp1730SluiceBlock) {
            World.setBlock(Pos, State.cycle(Scp1730SluiceBlock.Turn), Block.UPDATE_ALL);
            sound(World, Pos, Scp1730PuzzleRegistry.Crank.get(), 0.8f, 1.0f);
        } else if (Kind instanceof Scp1730GeneratorBlock) {
            World.setBlock(Pos, State.cycle(Scp1730GeneratorBlock.Running), Block.UPDATE_ALL);
            sound(World, Pos, Scp1730PuzzleRegistry.GeneratorStart.get(), 0.8f, 1.0f);
        } else {
            return false;
        }
        return true;
    }

    static boolean panel(Context C, BlockPos Pos, ServerPlayer User, ItemStack Held) {
        if (C.Data().Solved) {
            if (!Held.isEmpty()) {
                return false;
            }
            sound(C.World(), Pos, Scp1730Registry.ReaderAccept.get(), 0.7f, 1.2f);
            bar(User, "panel.lifted", ChatFormatting.GREEN);
            return true;
        }
        if (C.Puzzle() == Scp1730Puzzles.Keycard) {
            ItemStack Card = offered(User, Held, Scp1730PuzzleRegistry.OverrideCard.get());
            if (!Card.isEmpty()) {
                sound(C.World(), Pos, Scp1730Registry.ReaderAccept.get(), 1.0f, 0.9f);
                solve(C);
                return true;
            }
        }
        if (!Held.isEmpty()) {
            return false;
        }
        if (C.Puzzle() == Scp1730Puzzles.Generator && C.Data().has(1)) {
            bar(User, "panel.hold", ChatFormatting.YELLOW, Math.min(100, C.Data().Hold * 100 / HoldTicks));
            return true;
        }
        deny(C.World(), Pos, User, "panel." + Scp1730Puzzles.Ids[C.Puzzle()]);
        return true;
    }

    static boolean lever(Context C, BlockPos Pos, BlockState State, ServerPlayer User, ItemStack Held, int Bit, String Key, SoundEvent Sound) {
        if (!Held.isEmpty()) {
            return false;
        }
        if (State.getValue(Scp1730SwitchBlock.On)) {
            bar(User, "switch.set", ChatFormatting.GRAY);
            return true;
        }
        C.World().setBlock(Pos, State.setValue(Scp1730SwitchBlock.On, true), Block.UPDATE_ALL);
        sound(C.World(), Pos, Sound, 1.0f, 0.9f);
        step(C, Pos, Bit, Key);
        return true;
    }

    static boolean junction(Context C, BlockPos Pos, BlockState State, ServerPlayer User, ItemStack Held) {
        ItemStack Fuse = offered(User, Held, Scp1730PuzzleRegistry.Fuse.get());
        int Seated = C.Data().Counts[0];
        if (Fuse.isEmpty() || Seated >= 3) {
            if (!Held.isEmpty()) {
                return false;
            }
            sound(C.World(), Pos, SoundEvents.UI_BUTTON_CLICK.value(), 0.4f, 0.8f);
            bar(User, Seated >= 3 ? "junction.full" : "junction.empty", ChatFormatting.GRAY);
            return true;
        }
        spend(User, Fuse);
        Seated++;
        C.Data().Counts[0] = Seated;
        C.Data().changed();
        C.World().setBlock(Pos, State.setValue(Scp1730JunctionBoxBlock.Fuses, Seated), Block.UPDATE_ALL);
        sound(C.World(), Pos, Scp1730PuzzleRegistry.FuseSeat.get(), 1.0f, 1.0f);
        sound(C.World(), Pos, Scp1730PuzzleRegistry.Spark.get(), 0.5f, 1.4f);
        step(C, Pos, Seated - 1, "fuses." + (Seated - 1));
        return true;
    }

    static boolean terminal(Context C, BlockPos Pos, BlockState State, ServerPlayer User, ItemStack Held, int Bit, int Code) {
        if (!Held.isEmpty()) {
            return false;
        }
        long Now = C.World().getGameTime();
        if (State.getValue(Scp1730TerminalBlock.Accepted)) {
            bar(User, "terminal.accepted", ChatFormatting.GREEN);
            return true;
        }
        if (C.Data().Lockout > Now) {
            deny(C.World(), Pos, User, "terminal.locked");
            return true;
        }
        if (User.isShiftKeyDown()) {
            if (State.getValue(Scp1730TerminalBlock.Glyph) == Code) {
                C.World().setBlock(Pos, State.setValue(Scp1730TerminalBlock.Accepted, true), Block.UPDATE_ALL);
                sound(C.World(), Pos, Scp1730Registry.ReaderAccept.get(), 1.0f, 1.0f);
                step(C, Pos, Bit, "override." + Bit);
            } else {
                C.Data().Lockout = Now + LockoutTicks;
                C.Data().changed();
                sound(C.World(), Pos, Scp1730PuzzleRegistry.Wrong.get(), 1.0f, 0.8f);
                sound(C.World(), Pos, Scp1730Registry.Alarm.get(), 0.7f, 1.2f);
                bar(User, "terminal.rejected", ChatFormatting.RED);
            }
            return true;
        }
        C.World().setBlock(Pos, State.cycle(Scp1730TerminalBlock.Glyph), Block.UPDATE_ALL);
        sound(C.World(), Pos, Scp1730PuzzleRegistry.Beep.get(), 0.6f, 1.1f + State.getValue(Scp1730TerminalBlock.Glyph) * 0.05f);
        bar(User, "terminal.select", ChatFormatting.GRAY);
        return true;
    }

    static int window(Context C) {
        Scp1730Fixture Second = Scp1730Puzzles.device(C.Plan(), C.Zone(), Scp1730Puzzles.RoleKeyB);
        return Second == null ? 30 : Math.max(15, Scp1730Puzzles.extra(Second.Data()));
    }

    static boolean key(Context C, BlockPos Pos, BlockState State, ServerPlayer User, ItemStack Held, int Bit) {
        if (!Held.isEmpty()) {
            return false;
        }
        long Now = C.World().getGameTime();
        Scp1730PuzzleState.Zone Data = C.Data();
        lapse(C, Now);
        syncKeys(C);
        if (Data.has(Bit)) {
            bar(User, "key.turned", ChatFormatting.GRAY);
            return true;
        }
        if (hand(User, Scp1730PuzzleRegistry.DutyKey.get()).isEmpty()) {
            deny(C.World(), Pos, User, "key.needed");
            return true;
        }
        C.World().setBlock(Pos, C.World().getBlockState(Pos).setValue(Scp1730SwitchBlock.On, true), Block.UPDATE_ALL);
        sound(C.World(), Pos, Scp1730Registry.Release.get(), 0.9f, 1.3f);
        if (Data.has(1 - Bit) && Data.Timer > Now) {
            step(C, Pos, Bit, "dualkey." + Bit);
            return true;
        }
        Data.Progress = 1 << Bit;
        Data.Timer = Now + window(C) * 20L;
        Data.changed();
        sound(C.World(), Pos, Scp1730PuzzleRegistry.Step.get(), 1.0f, 1.0f);
        pa(C, "dualkey.armed");
        refresh(C, false);
        return true;
    }

    static void climb(ServerPlayer User, BlockPos Pos, BlockState State) {
        float Yaw = Scp1730Plan.yawOf(Scp1730Blocks.opposite(State.getValue(Scp1730VentGrateBlock.FACING).getSerializedName()));
        User.stopRiding();
        User.setPose(Pose.SWIMMING);
        User.teleportTo(User.serverLevel(), Pos.getX() + 0.5, Pos.getY(), Pos.getZ() + 0.5, Yaw, User.getXRot());
        User.setPose(Pose.SWIMMING);
        sound(User.serverLevel(), Pos, Scp1730PuzzleRegistry.VentOpen.get(), 0.6f, 0.7f);
    }

    static boolean vent(Context C, BlockPos Pos, BlockState State, ServerPlayer User, ItemStack Held, boolean Outer) {
        if (!Held.isEmpty()) {
            return false;
        }
        if (State.getValue(Scp1730VentGrateBlock.Open)) {
            climb(User, Pos, State);
            return true;
        }
        if (Outer && !C.Data().has(0)) {
            sound(C.World(), Pos, Scp1730PuzzleRegistry.Refuse.get(), 0.8f, 1.2f);
            bar(User, "vent.latched", ChatFormatting.GRAY);
            return true;
        }
        C.World().setBlock(Pos, State.setValue(Scp1730VentGrateBlock.Open, true), Block.UPDATE_ALL);
        sound(C.World(), Pos, Scp1730PuzzleRegistry.VentOpen.get(), 1.0f, 1.0f);
        if (Outer) {
            step(C, Pos, 1, "vent.1");
        }
        return true;
    }

    static boolean fan(Context C, BlockPos Pos, BlockState State, ServerPlayer User, ItemStack Held) {
        if (!Held.isEmpty()) {
            return false;
        }
        if (!State.getValue(Scp1730SwitchBlock.On)) {
            bar(User, "fan.stopped", ChatFormatting.GRAY);
            return true;
        }
        C.World().setBlock(Pos, State.setValue(Scp1730SwitchBlock.On, false), Block.UPDATE_ALL);
        sound(C.World(), Pos, Scp1730Registry.Breaker.get(), 1.0f, 0.8f);
        sound(C.World(), Pos, Scp1730PuzzleRegistry.FanStop.get(), 1.2f, 0.8f);
        step(C, Pos, 2, "vent.2");
        return true;
    }

    static void sparks(ServerLevel World, BlockPos Pos) {
        World.sendParticles(ParticleTypes.ELECTRIC_SPARK, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, 12, 0.25, 0.25, 0.25, 0.05);
        sound(World, Pos, Scp1730PuzzleRegistry.Spark.get(), 0.9f, 1.0f);
    }

    static boolean cable(Context C, BlockPos Pos, BlockState State, ServerPlayer User, ItemStack Held, int Bit) {
        ItemStack Kit = offered(User, Held, Scp1730PuzzleRegistry.SpliceKit.get());
        boolean Fixed = State.getValue(Scp1730CableJunctionBlock.Fixed);
        if (Kit.isEmpty() || Fixed) {
            if (!Held.isEmpty()) {
                return false;
            }
            if (Fixed) {
                bar(User, "cable.fixed", ChatFormatting.GRAY);
            } else {
                sparks(C.World(), Pos);
                bar(User, "cable.cut", ChatFormatting.GRAY);
            }
            return true;
        }
        spend(User, Kit);
        C.World().setBlock(Pos, State.setValue(Scp1730CableJunctionBlock.Fixed, true), Block.UPDATE_ALL);
        sparks(C.World(), Pos);
        sound(C.World(), Pos, Scp1730PuzzleRegistry.FuseSeat.get(), 0.8f, 0.8f);
        step(C, Pos, Bit, "splice." + Bit);
        return true;
    }

    static boolean relay(Context C, BlockPos Pos, BlockState State, ServerPlayer User, ItemStack Held, int Colour, int Order) {
        if (!Held.isEmpty()) {
            return false;
        }
        int[] Sequence = Scp1730Puzzles.Orders[Math.floorMod(Order, Scp1730Puzzles.Orders.length)];
        Scp1730PuzzleState.Zone Data = C.Data();
        int Next = Data.Counts[0];
        syncRelays(C);
        boolean Thrown = false;
        for (int Index = 0; Index < Next && Index < 3; Index++) {
            Thrown |= Sequence[Index] == Colour;
        }
        if (Thrown) {
            bar(User, "switch.set", ChatFormatting.GRAY);
            return true;
        }
        C.World().setBlock(Pos, C.World().getBlockState(Pos).setValue(Scp1730SwitchBlock.On, true), Block.UPDATE_ALL);
        sound(C.World(), Pos, Scp1730Registry.Breaker.get(), 1.0f, 1.0f);
        if (Next < 3 && Sequence[Next] == Colour) {
            Data.Counts[0] = Next + 1;
            Data.changed();
            step(C, Pos, Next, "relays." + Next);
            return true;
        }
        Data.Counts[0] = 0;
        Data.Progress = 0;
        Data.changed();
        sparks(C.World(), Pos);
        sound(C.World(), Pos, Scp1730PuzzleRegistry.Wrong.get(), 1.0f, 0.7f);
        syncRelays(C);
        refresh(C, false);
        pa(C, "relays.fault");
        return true;
    }

    static void syncRelays(Context C) {
        int[] Sequence = null;
        for (int Role = Scp1730Puzzles.RoleRelayRed; Role <= Scp1730Puzzles.RoleRelayBlue; Role++) {
            Scp1730Fixture Each = Scp1730Puzzles.device(C.Plan(), C.Zone(), Role);
            if (Each == null) {
                continue;
            }
            if (Sequence == null) {
                Sequence = Scp1730Puzzles.Orders[Math.floorMod(Scp1730Puzzles.extra(Each.Data()), Scp1730Puzzles.Orders.length)];
            }
            int Colour = Role - Scp1730Puzzles.RoleRelayRed;
            boolean Wanted = false;
            for (int Index = 0; Index < C.Data().Counts[0] && Index < 3; Index++) {
                Wanted |= Sequence[Index] == Colour;
            }
            change(C.World(), where(C, Each), Scp1730SwitchBlock.On, Wanted || C.Data().Solved);
        }
    }

    static boolean hatch(Context C, BlockPos Pos, BlockState State, ServerPlayer User, ItemStack Held) {
        ItemStack Sample = offered(User, Held, Scp1730PuzzleRegistry.SampleCanister.get());
        boolean Loaded = State.getValue(Scp1730HatchBlock.Loaded);
        if (Sample.isEmpty() || Loaded) {
            if (!Held.isEmpty()) {
                return false;
            }
            bar(User, Loaded ? "hatch.loaded" : "hatch.empty", ChatFormatting.GRAY);
            return true;
        }
        spend(User, Sample);
        C.World().setBlock(Pos, State.setValue(Scp1730HatchBlock.Loaded, true), Block.UPDATE_ALL);
        sound(C.World(), Pos, SoundEvents.IRON_TRAPDOOR_CLOSE, 1.0f, 0.6f);
        sound(C.World(), Pos, Scp1730Registry.Release.get(), 0.6f, 1.3f);
        step(C, Pos, 0, "sample.0");
        return true;
    }

    static boolean generator(Context C, BlockPos Pos, BlockState State, ServerPlayer User, ItemStack Held) {
        ItemStack Can = offered(User, Held, Scp1730PuzzleRegistry.FuelCanister.get());
        int Fuel = State.getValue(Scp1730GeneratorBlock.Fuel);
        if (!Can.isEmpty() && Fuel < 2) {
            spend(User, Can);
            Fuel++;
            C.Data().Counts[0] = Fuel;
            C.Data().changed();
            C.World().setBlock(Pos, State.setValue(Scp1730GeneratorBlock.Fuel, Fuel), Block.UPDATE_ALL);
            sound(C.World(), Pos, SoundEvents.BUCKET_EMPTY, 1.0f, 0.7f);
            if (Fuel >= 2) {
                step(C, Pos, 0, "generator.0");
            } else {
                bar(User, "generator.fuel", ChatFormatting.GRAY);
            }
            return true;
        }
        if (!Held.isEmpty()) {
            return false;
        }
        if (State.getValue(Scp1730GeneratorBlock.Running)) {
            bar(User, "generator.running", ChatFormatting.GRAY);
            return true;
        }
        if (Fuel < 2) {
            sound(C.World(), Pos, Scp1730PuzzleRegistry.Refuse.get(), 0.8f, 1.4f);
            bar(User, "generator.dry", ChatFormatting.GRAY);
            return true;
        }
        C.World().setBlock(Pos, State.setValue(Scp1730GeneratorBlock.Running, true), Block.UPDATE_ALL);
        sound(C.World(), Pos, Scp1730PuzzleRegistry.GeneratorStart.get(), 1.2f, 0.8f);
        sound(C.World(), Pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.6f, 0.6f);
        step(C, Pos, 1, "generator.1");
        return true;
    }

    static boolean valve(Context C, BlockPos Pos, BlockState State, ServerPlayer User, ItemStack Held, int Index) {
        if (!Held.isEmpty()) {
            return false;
        }
        Scp1730PuzzleState.Zone Data = C.Data();
        if (Data.has(Index)) {
            bar(User, "valve.set", ChatFormatting.GRAY);
            return true;
        }
        if (Index > 0 && !Data.has(Index - 1)) {
            sound(C.World(), Pos, Scp1730PuzzleRegistry.Refuse.get(), 1.0f, 0.6f);
            sound(C.World(), Pos, Scp1730Registry.AmbienceGroan.get(), 0.6f, 1.2f);
            bar(User, "valve.stuck", ChatFormatting.GRAY);
            return true;
        }
        C.World().setBlock(Pos, State.setValue(Scp1730ValveBlock.Open, Index != 0).setValue(Scp1730ValveBlock.High, false), Block.UPDATE_ALL);
        sound(C.World(), Pos, Scp1730PuzzleRegistry.ValveTurn.get(), 1.0f, 0.9f);
        step(C, Pos, Index, "coolant." + Index);
        syncValves(C);
        return true;
    }

    static void syncValves(Context C) {
        int[] Roles = {Scp1730Puzzles.RoleInflow, Scp1730Puzzles.RoleBypass, Scp1730Puzzles.RoleDrain};
        for (int Index = 0; Index < 3; Index++) {
            Scp1730Fixture Each = Scp1730Puzzles.device(C.Plan(), C.Zone(), Roles[Index]);
            if (Each == null) {
                continue;
            }
            boolean Done = C.Data().has(Index) || C.Data().Solved;
            boolean Pressure = !Done && (Index == 0 || !C.Data().has(Index - 1));
            BlockPos At = where(C, Each);
            change(C.World(), At, Scp1730ValveBlock.High, Pressure);
            change(C.World(), At, Scp1730ValveBlock.Open, Index == 0 ? !Done : Done);
        }
    }

    static boolean controller(Context C, BlockPos Pos, BlockState State, ServerPlayer User, ItemStack Held) {
        if (!Held.isEmpty()) {
            return false;
        }
        if (State.getValue(Scp1730SwitchBlock.On)) {
            bar(User, "switch.set", ChatFormatting.GRAY);
            return true;
        }
        if (Scp1730State.get(C.World().getServer()).powered(C.Zone())) {
            sparks(C.World(), Pos);
            bar(User, "controller.live", ChatFormatting.GOLD);
            return true;
        }
        C.World().setBlock(Pos, State.setValue(Scp1730SwitchBlock.On, true), Block.UPDATE_ALL);
        sound(C.World(), Pos, Scp1730Registry.Breaker.get(), 1.0f, 1.2f);
        step(C, Pos, 1, "powercycle.1");
        return true;
    }

    static boolean pylon(Context C, BlockPos Pos, BlockState State, ServerPlayer User, ItemStack Held, int Bit, int Target) {
        if (!Held.isEmpty()) {
            return false;
        }
        BlockState Turned = State.cycle(Scp1730PylonBlock.Glyph);
        boolean Aligned = Turned.getValue(Scp1730PylonBlock.Glyph) == Target;
        C.World().setBlock(Pos, Turned.setValue(Scp1730PylonBlock.Lit, Aligned), Block.UPDATE_ALL);
        sound(C.World(), Pos, Scp1730PuzzleRegistry.Crank.get(), 0.9f, 1.4f);
        if (Aligned) {
            sound(C.World(), Pos, Scp1730Registry.AnchorRing.get(), 0.6f, 1.3f);
            step(C, Pos, Bit, "pylons." + Bit);
        } else if (C.Data().has(Bit)) {
            unstep(C, Bit);
        }
        return true;
    }

    static boolean sluice(Context C, BlockPos Pos, BlockState State, ServerPlayer User, ItemStack Held, int Index) {
        if (!Held.isEmpty()) {
            return false;
        }
        if (State.getValue(Scp1730SluiceBlock.Open)) {
            bar(User, "sluice.open", ChatFormatting.GRAY);
            return true;
        }
        Scp1730PuzzleState.Zone Data = C.Data();
        Data.Counts[Index] = Data.Counts[Index] + 1;
        Data.changed();
        boolean Done = Data.Counts[Index] >= Scp1730Puzzles.Cranks;
        C.World().setBlock(Pos, State.cycle(Scp1730SluiceBlock.Turn).setValue(Scp1730SluiceBlock.Open, Done), Block.UPDATE_ALL);
        sound(C.World(), Pos, Scp1730PuzzleRegistry.Crank.get(), 1.0f, 0.7f + Data.Counts[Index] * 0.04f);
        if (Done) {
            sound(C.World(), Pos, SoundEvents.BUCKET_EMPTY, 1.0f, 0.5f);
            step(C, Pos, Index, "sluices." + Index);
        }
        return true;
    }

    public static void take(ServerLevel World, BlockPos Pos, BlockState State, ServerPlayer User) {
        ItemStack Made = new ItemStack(Scp1730PuzzleRegistry.itemFor(State.getValue(Scp1730PickupBlock.Item)));
        if (!User.getInventory().add(Made)) {
            User.drop(Made, false);
        }
        World.removeBlock(Pos, false);
        sound(World, Pos, SoundEvents.ITEM_PICKUP, 0.8f, 0.9f);
        Context C = context(World, Pos);
        if (C == null) {
            return;
        }
        List<Scp1730Fixture> Spots = C.Plan().fixtures("pickup", C.Zone());
        for (int Index = 0; Index < Spots.size() && Index < 31; Index++) {
            if (where(C, Spots.get(Index)).equals(Pos)) {
                C.Data().Taken |= 1 << Index;
                C.Data().TakenAt = World.getGameTime();
                C.Data().changed();
            }
        }
    }

    public static void objective(ServerLevel World, int Run, int Zone, BlockPos Pos, Scp1730ControlBlock.Kind Kind, boolean Value, ServerPlayer User) {
        Context C = zoneContext(World, Run, Zone);
        if (C == null) {
            if (Kind != Scp1730ControlBlock.Kind.Breaker || Value && Zone == Scp1730State.HeavyZone) {
                Scp1730Director.openSeal(World, Run, Zone);
            }
            return;
        }
        Scp1730Fixture Device = at(C, "pdevice", Pos);
        int Role = Device == null ? -1 : Scp1730Puzzles.role(Device.Data());
        if (Kind == Scp1730ControlBlock.Kind.Reader && Role == Scp1730Puzzles.RoleLockerReader) {
            unlock(C);
            return;
        }
        if (Kind == Scp1730ControlBlock.Kind.Release && Role == Scp1730Puzzles.RoleFlush) {
            if (C.Data().has(0)) {
                sound(World, Pos, Scp1730PuzzleRegistry.FanStop.get(), 1.0f, 1.4f);
                step(C, Pos, 1, "sample.1");
            } else {
                change(World, Pos, Scp1730ControlBlock.Active, false);
                sound(World, Pos, Scp1730PuzzleRegistry.Refuse.get(), 0.8f, 1.2f);
                bar(User, "flush.empty", ChatFormatting.GRAY);
            }
            return;
        }
        if (Kind == Scp1730ControlBlock.Kind.Breaker) {
            if (Role == Scp1730Puzzles.RoleBreaker && !C.Data().Solved) {
                if (Value) {
                    step(C, Pos, 0, "powercycle.0");
                } else if (C.Data().has(1)) {
                    step(C, Pos, 2, "powercycle.2");
                } else if (C.Data().has(0)) {
                    unstep(C, 0);
                    pa(C, "powercycle.early");
                }
            }
            return;
        }
        change(World, Pos, Scp1730ControlBlock.Active, false);
        sound(World, Pos, Scp1730PuzzleRegistry.Refuse.get(), 0.6f, 1.3f);
        bar(User, "control.dead", ChatFormatting.GRAY);
    }

    static void unlock(Context C) {
        double Sx = 0.0;
        double Sy = 0.0;
        double Sz = 0.0;
        List<Scp1730Fixture> Seals = C.Plan().fixtures("pseal", C.Zone());
        for (Scp1730Fixture Each : Seals) {
            BlockPos At = where(C, Each);
            BlockState Current = C.World().getBlockState(At);
            if (Current.getBlock() instanceof Scp1730SealBlock) {
                C.World().setBlock(At, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                C.World().levelEvent(2001, At, Block.getId(Current));
            }
            Sx += At.getX() + 0.5;
            Sy += At.getY() + 0.5;
            Sz += At.getZ() + 0.5;
        }
        if (!Seals.isEmpty()) {
            C.World().playSound(null, Sx / Seals.size(), Sy / Seals.size(), Sz / Seals.size(), Scp1730Registry.SealOpen.get(), SoundSource.BLOCKS, 1.6f, 1.1f);
        }
        if (!Seals.isEmpty()) {
            step(C, where(C, Seals.get(4 % Seals.size())), 0, "keycard.0");
        }
    }

    static BlockState gateState(Scp1730Fixture Each) {
        String Facing = Each.Facing();
        boolean AlongX = Facing.equals("north") || Facing.equals("south");
        return Scp1730World.state(Scp1730Blocks.Seal + "[axis=" + (AlongX ? "x" : "z") + "]");
    }

    static void shutGate(Context C) {
        for (Scp1730Fixture Each : C.Plan().fixtures("dseal", C.Zone())) {
            BlockPos At = where(C, Each);
            if (!C.World().isLoaded(At) || !C.World().getBlockState(At).isAir()) {
                continue;
            }
            if (!C.World().getEntitiesOfClass(LivingEntity.class, new AABB(At), LivingEntity::isAlive).isEmpty()) {
                continue;
            }
            C.World().setBlock(At, gateState(Each), Block.UPDATE_ALL);
        }
    }

    static void openGate(Context C, boolean Loud) {
        double Sx = 0.0;
        double Sy = 0.0;
        double Sz = 0.0;
        int Count = 0;
        for (Scp1730Fixture Each : C.Plan().fixtures("dseal", C.Zone())) {
            BlockPos At = where(C, Each);
            if (!C.World().isLoaded(At)) {
                continue;
            }
            BlockState Current = C.World().getBlockState(At);
            if (Current.getBlock() instanceof Scp1730SealBlock) {
                C.World().setBlock(At, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                if (Loud) {
                    C.World().levelEvent(2001, At, Block.getId(Current));
                }
            }
            Sx += At.getX() + 0.5;
            Sy += At.getY() + 0.5;
            Sz += At.getZ() + 0.5;
            Count++;
        }
        if (Loud && Count > 0) {
            C.World().playSound(null, Sx / Count, Sy / Count, Sz / Count, Scp1730Registry.SealOpen.get(), SoundSource.BLOCKS, 2.0f, 0.7f);
        }
    }

    public static void tick(MinecraftServer Server) {
        ServerLevel World = Scp1730World.level(Server);
        if (World == null) {
            return;
        }
        Scp1730State Main = Scp1730State.get(Server);
        if (!Main.active()) {
            return;
        }
        List<ServerPlayer> Inside = Scp1730Director.inside(World);
        if (Inside.isEmpty()) {
            return;
        }
        int Run = Main.run();
        long Now = World.getGameTime();
        for (int Zone = 0; Zone < Scp1730Puzzles.Zones; Zone++) {
            Context C = zoneContext(World, Run, Zone);
            if (C == null) {
                continue;
            }
            Scp1730PuzzleState.Zone Data = C.Data();
            if (Zone < Scp1730Puzzles.DeepZone && Main.open(Zone) && !Data.Solved) {
                Data.Solved = true;
                Data.Progress = (1 << Scp1730Puzzles.steps(C.Puzzle())) - 1;
                Data.changed();
            }
            if (Data.Solved) {
                if (Zone == Scp1730Puzzles.DeepZone) {
                    openGate(C, false);
                }
                refresh(C, false);
                continue;
            }
            if (Zone == Scp1730Puzzles.DeepZone) {
                shutGate(C);
            }
            boolean Blink = false;
            switch (C.Puzzle()) {
                case Scp1730Puzzles.DualKey -> Blink = dualTick(C, Now);
                case Scp1730Puzzles.Generator -> Blink = holdTick(C, Inside, Now);
                case Scp1730Puzzles.Coolant -> syncValves(C);
                case Scp1730Puzzles.Relays -> syncRelays(C);
                default -> {
                }
            }
            if (Now % RespawnEvery < TickEvery) {
                respawn(C, Inside, Now);
            }
            refresh(C, Blink);
        }
        for (ServerPlayer User : Inside) {
            if (User.getInventory().contains(new ItemStack(Scp1730PuzzleRegistry.SampleCanister.get()))) {
                User.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SampleSlow, 0, true, false, true));
            }
        }
    }

    static Scp1730Fixture[] keys(Context C) {
        return new Scp1730Fixture[]{Scp1730Puzzles.device(C.Plan(), C.Zone(), Scp1730Puzzles.RoleKeyA), Scp1730Puzzles.device(C.Plan(), C.Zone(), Scp1730Puzzles.RoleKeyB)};
    }

    static void lapse(Context C, long Now) {
        Scp1730PuzzleState.Zone Data = C.Data();
        if (Data.Timer <= 0L || Now < Data.Timer || Data.Solved) {
            return;
        }
        Data.Timer = 0L;
        Data.Progress = 0;
        Data.changed();
        for (Scp1730Fixture Each : keys(C)) {
            if (Each != null) {
                sound(C.World(), where(C, Each), Scp1730PuzzleRegistry.Wrong.get(), 1.0f, 0.7f);
            }
        }
        pa(C, "dualkey.lapsed");
        refresh(C, false);
    }

    static void syncKeys(Context C) {
        Scp1730Fixture[] Keys = keys(C);
        for (int Index = 0; Index < 2; Index++) {
            if (Keys[Index] != null) {
                change(C.World(), where(C, Keys[Index]), Scp1730SwitchBlock.On, C.Data().has(Index) || C.Data().Solved);
            }
        }
    }

    static boolean dualTick(Context C, long Now) {
        Scp1730PuzzleState.Zone Data = C.Data();
        Scp1730Fixture[] Keys = keys(C);
        boolean Armed = Data.Timer > Now && Integer.bitCount(Data.Progress) == 1;
        lapse(C, Now);
        syncKeys(C);
        for (int Index = 0; Index < 2; Index++) {
            if (Keys[Index] == null) {
                continue;
            }
            BlockPos At = where(C, Keys[Index]);
            if (Armed && Now % 20 < TickEvery) {
                float Left = (Data.Timer - Now) / (float) (window(C) * 20);
                C.World().playSound(null, At, Scp1730PuzzleRegistry.Beep.get(), SoundSource.BLOCKS, 1.2f, 1.9f - Left);
            }
        }
        if (Armed && Data.Timer - Now < 100L) {
            Scp1730Fixture Panel = Scp1730Puzzles.panel(C.Plan(), C.Zone());
            if (Panel != null) {
                C.World().playSound(null, where(C, Panel), Scp1730PuzzleRegistry.Beep.get(), SoundSource.BLOCKS, 0.8f, 2.0f);
            }
        }
        return Armed;
    }

    static boolean holdTick(Context C, List<ServerPlayer> Inside, long Now) {
        Scp1730PuzzleState.Zone Data = C.Data();
        if (!Data.has(1) || Data.has(2)) {
            return false;
        }
        Scp1730Fixture Panel = Scp1730Puzzles.panel(C.Plan(), C.Zone());
        if (Panel == null) {
            return false;
        }
        BlockPos At = where(C, Panel);
        boolean Near = false;
        for (ServerPlayer User : Inside) {
            Near |= User.position().distanceTo(Vec3.atCenterOf(At)) <= HoldReach + 0.5;
        }
        if (!Near) {
            return Data.Hold > 0;
        }
        if (Data.Hold == 0 || !Data.Waved && Data.Hold >= HoldTicks / 2) {
            if (Data.Hold > 0) {
                Data.Waved = true;
            }
            wave(C, Inside);
            pa(C, Data.Hold == 0 ? "generator.cycle" : "generator.wave");
        }
        Data.Hold += TickEvery;
        Data.changed();
        if (Now % 40 < TickEvery) {
            C.World().playSound(null, At, Scp1730Registry.Alarm.get(), SoundSource.BLOCKS, 1.2f, 1.1f);
        }
        if (Data.Hold >= HoldTicks) {
            step(C, At, 2, "generator.2");
        }
        return true;
    }

    static void wave(Context C, List<ServerPlayer> Inside) {
        int[] ZoneOf = new int[Inside.size()];
        for (int Index = 0; Index < Inside.size(); Index++) {
            ZoneOf[Index] = Scp1730World.zoneOf(C.Run(), Inside.get(Index).blockPosition());
        }
        Scp1730Breach.breach(C.World(), C.Run(), C.Plan(), C.Zone(), Inside, ZoneOf);
    }

    static int needed(Context C) {
        Scp1730PuzzleState.Zone Data = C.Data();
        return switch (C.Puzzle()) {
            case Scp1730Puzzles.Keycard, Scp1730Puzzles.DualKey -> 1;
            case Scp1730Puzzles.Fuses -> 3 - Data.Counts[0];
            case Scp1730Puzzles.Splice -> 2 - Integer.bitCount(Data.Progress & 3);
            case Scp1730Puzzles.Sample -> Data.has(0) ? 0 : 1;
            case Scp1730Puzzles.Generator -> 2 - Data.Counts[0];
            default -> 0;
        };
    }

    public static void respawn(Context C, List<ServerPlayer> Inside, long Now) {
        int[] Need = Scp1730Puzzles.Needs[C.Puzzle()];
        if (Need.length < 2 || Now - C.Data().TakenAt < RespawnDelay) {
            return;
        }
        Item Wanted = Scp1730PuzzleRegistry.itemFor(Scp1730PickupBlock.Kind.byIndex(Need[0]));
        int Held = 0;
        for (ServerPlayer User : Inside) {
            for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
                ItemStack Stack = User.getInventory().getItem(Slot);
                if (Stack.is(Wanted)) {
                    Held += Stack.getCount();
                }
            }
        }
        List<Scp1730Fixture> Spots = C.Plan().fixtures("pickup", C.Zone());
        int Present = 0;
        for (int Index = 0; Index < Spots.size() && Index < 31; Index++) {
            if ((C.Data().Taken & 1 << Index) == 0) {
                Present++;
            }
        }
        if (Held + Present >= needed(C)) {
            return;
        }
        for (int Index = 0; Index < Spots.size() && Index < 31; Index++) {
            if ((C.Data().Taken & 1 << Index) == 0) {
                continue;
            }
            Scp1730Fixture Each = Spots.get(Index);
            BlockPos At = where(C, Each);
            if (!C.World().isLoaded(At) || !C.World().getBlockState(At).isAir()) {
                continue;
            }
            C.World().setBlock(At, Scp1730World.state(C.Plan().stateAt(Each.X(), Each.Y(), Each.Z())), Block.UPDATE_ALL);
            C.Data().Taken &= ~(1 << Index);
            C.Data().changed();
            return;
        }
    }

    public static List<String> status(ServerLevel World) {
        Scp1730State Main = Scp1730State.get(World.getServer());
        List<String> Lines = new java.util.ArrayList<>();
        if (Main.run() < 0) {
            Lines.add("no SCP-1730 run yet");
            return Lines;
        }
        for (int Zone = 0; Zone < Scp1730Puzzles.Zones; Zone++) {
            Context C = zoneContext(World, Main.run(), Zone);
            if (C == null) {
                Lines.add("zone " + Zone + ": no puzzle");
                continue;
            }
            Lines.add("zone " + Zone + ": " + Scp1730Puzzles.Ids[C.Puzzle()] + " " + lamps(C) + "/" + Scp1730Puzzles.steps(C.Puzzle()) + (C.Data().Solved ? " solved" : ""));
        }
        return Lines;
    }

    public static boolean forceSolve(ServerLevel World, int Zone) {
        Scp1730State Main = Scp1730State.get(World.getServer());
        Context C = zoneContext(World, Main.run(), Zone);
        if (C == null) {
            return false;
        }
        solve(C);
        return true;
    }
}
