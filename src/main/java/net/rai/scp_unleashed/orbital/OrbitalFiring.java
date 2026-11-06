package net.rai.scp_unleashed.orbital;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public final class OrbitalFiring {
    public static final int ClickGuardTicks = 10;

    public enum Outcome {
        Fired,
        Linked,
        NoArray,
        Foreign,
        Incomplete,
        WrongDimension,
        Recharging,
        NoTarget,
        Buried
    }

    private OrbitalFiring() {
    }

    public static Outcome use(ServerPlayer User, ItemStack Stack) {
        HitResult Hit = User.pick(OrbitalConfig.range(), 1.0F, false);
        BlockPos Marked = Hit instanceof BlockHitResult Block && Hit.getType() == HitResult.Type.BLOCK ? Block.getBlockPos() : null;
        return use(User, Stack, Marked);
    }

    public static Outcome use(ServerPlayer User, ItemStack Stack, @Nullable BlockPos Marked) {
        MinecraftServer Server = User.server;
        OrbitalArray Array = OrbitalArray.get(Server);
        UUID Id = User.getUUID();
        long Now = Server.overworld().getGameTime();
        OrbitalLink Link = Stack.get(OrbitalRegistry.Link.get());
        if (Link == null) {
            if (!Array.complete(Id)) {
                deny(User, Component.translatable("message.scp_unleashed.orbital.no_array", Array.count(Id), OrbitalPart.Count));
                return Outcome.NoArray;
            }
            Stack.set(OrbitalRegistry.Link.get(), new OrbitalLink(Id, User.getGameProfile().getName()));
            stamp(Stack, Array.readyAt(Id), Now);
            tell(User, Component.translatable("message.scp_unleashed.orbital.linked").withStyle(ChatFormatting.GREEN));
            User.playNotifySound(OrbitalRegistry.DesignatorLink.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
            OrbitalLaunch.sync(User);
            return Outcome.Linked;
        }
        if (!Link.ownedBy(Id)) {
            deny(User, Component.translatable("message.scp_unleashed.orbital.foreign"));
            return Outcome.Foreign;
        }
        if (!Array.complete(Id)) {
            deny(User, Component.translatable("message.scp_unleashed.orbital.incomplete", Array.count(Id), OrbitalPart.Count));
            return Outcome.Incomplete;
        }
        if (User.level().dimension() != Level.OVERWORLD) {
            deny(User, Component.translatable("message.scp_unleashed.orbital.no_coverage"));
            return Outcome.WrongDimension;
        }
        long ReadyAt = Array.readyAt(Id);
        if (Now < ReadyAt) {
            stamp(Stack, ReadyAt, Now);
            deny(User, Component.translatable("message.scp_unleashed.orbital.recharging", (ReadyAt - Now + 19L) / 20L));
            return Outcome.Recharging;
        }
        if (Marked == null) {
            tell(User, Component.translatable("message.scp_unleashed.orbital.ready").withStyle(ChatFormatting.AQUA));
            return Outcome.NoTarget;
        }
        ServerLevel World = User.serverLevel();
        Optional<BlockPos> Impact = impact(World, Marked);
        if (Impact.isEmpty()) {
            deny(User, Component.translatable("message.scp_unleashed.orbital.buried"));
            return Outcome.Buried;
        }
        call(World, Impact.get(), User);
        int Cooldown = OrbitalConfig.cooldownTicks();
        Array.fired(Id, Now + Cooldown);
        stamp(Stack, Now + Cooldown, Now);
        User.getCooldowns().addCooldown(Stack.getItem(), ClickGuardTicks);
        tell(User, Component.translatable("message.scp_unleashed.orbital.inbound").withStyle(ChatFormatting.GOLD));
        User.playNotifySound(OrbitalRegistry.DesignatorConfirm.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        OrbitalLaunch.sync(User);
        return Outcome.Fired;
    }

    public static Optional<BlockPos> impact(ServerLevel World, BlockPos Marked) {
        int Top = World.getHeight(Heightmap.Types.MOTION_BLOCKING, Marked.getX(), Marked.getZ());
        int Floor = Marked.getY() + 1;
        if (Top - Floor > OrbitalConfig.maxCover()) {
            return Optional.empty();
        }
        return Optional.of(new BlockPos(Marked.getX(), Math.max(Top, Floor), Marked.getZ()));
    }

    public static OrbitalStrikeEntity call(ServerLevel World, BlockPos Impact, @Nullable ServerPlayer Owner) {
        OrbitalStrikeEntity Strike = OrbitalStrikeEntity.create(World, Vec3.atBottomCenterOf(Impact), Owner == null ? null : Owner.getUUID(), OrbitalStrike.Settings.current(),
                OrbitalConfig.chargeTicks());
        World.addFreshEntity(Strike);
        return Strike;
    }

    public static void stamp(ItemStack Stack, long ReadyAt, long Now) {
        int Span = OrbitalConfig.cooldownTicks();
        if (ReadyAt <= Now || Span <= 0) {
            Stack.remove(OrbitalRegistry.Recharge.get());
            return;
        }
        Stack.set(OrbitalRegistry.Recharge.get(), new OrbitalRecharge(ReadyAt, (int) Math.max(Span, ReadyAt - Now)));
    }

    private static void tell(ServerPlayer User, Component Text) {
        User.displayClientMessage(Text, true);
    }

    private static void deny(ServerPlayer User, MutableComponent Text) {
        User.displayClientMessage(Text.withStyle(ChatFormatting.RED), true);
        User.playNotifySound(OrbitalRegistry.DesignatorDeny.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }
}
