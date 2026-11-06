package net.rai.scp_unleashed.orbital;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.notice.Notice;
import net.rai.scp_unleashed.notice.NoticeIcon;
import net.rai.scp_unleashed.notice.Notices;
import net.rai.scp_unleashed.orbital.network.OrbitalStatusPayload;
import net.rai.scp_unleashed.rocketry.RocketryRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public final class OrbitalLaunch {
    public static final NoticeIcon Icon = NoticeIcon.generic("satellite");

    private OrbitalLaunch() {
    }

    @Nullable
    public static String payloadId(ItemStack Stack) {
        return Stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(Stack.getItem()).toString();
    }

    public static Optional<OrbitalPart> partOf(@Nullable String PayloadId) {
        if (PayloadId == null) {
            return Optional.empty();
        }
        ResourceLocation Id = ResourceLocation.tryParse(PayloadId);
        if (Id == null || !BuiltInRegistries.ITEM.containsKey(Id)) {
            return Optional.empty();
        }
        Item Found = BuiltInRegistries.ITEM.get(Id);
        return Found instanceof OrbitalPartItem Part ? Optional.of(Part.getPart()) : Optional.empty();
    }

    public static boolean mayLaunch(ServerLevel World, @Nullable Player User, ItemStack Payload, Vec3 Centre) {
        Optional<OrbitalPart> Part = OrbitalPart.of(Payload);
        if (Part.isEmpty()) {
            return true;
        }
        MutableComponent Problem = null;
        if (User == null) {
            Problem = Component.translatable("message.scp_unleashed.orbital.launch_unclaimed");
        } else if (OrbitalArray.get(World.getServer()).has(User.getUUID(), Part.get())) {
            Problem = Component.translatable("message.scp_unleashed.orbital.launch_duplicate", Component.translatable(Part.get().getTranslationKey()));
        }
        if (Problem == null) {
            return true;
        }
        if (User != null) {
            User.displayClientMessage(Problem.withStyle(ChatFormatting.RED), true);
        }
        World.playSound(null, Centre.x, Centre.y, Centre.z, RocketryRegistry.Refused.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        return false;
    }

    public static boolean deployed(ServerLevel World, @Nullable UUID Owner, @Nullable String PayloadId) {
        Optional<OrbitalPart> Part = partOf(PayloadId);
        if (Part.isEmpty()) {
            return false;
        }
        if (Owner == null || Util.NIL_UUID.equals(Owner)) {
            return true;
        }
        MinecraftServer Server = World.getServer();
        OrbitalArray Array = OrbitalArray.get(Server);
        Array.record(Owner, Part.get(), Server.overworld().getGameTime());
        ServerPlayer Launcher = Server.getPlayerList().getPlayer(Owner);
        if (Launcher != null) {
            int Count = Array.count(Owner);
            Component Detail = Array.complete(Owner) ? Component.translatable("notice.scp_unleashed.orbital.array_complete")
                    : Component.translatable("notice.scp_unleashed.orbital.array_progress", Count, OrbitalPart.Count);
            Notices.send(Launcher, Notice.info(Component.translatable("notice.scp_unleashed.orbital.part_deployed", Component.translatable(Part.get().getTranslationKey())))
                    .withDetail(Detail)
                    .withIcon(Icon));
            Launcher.playNotifySound(OrbitalRegistry.PartDeployed.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
            sync(Launcher);
        }
        return true;
    }

    public static void sync(ServerPlayer Reader) {
        if (Reader instanceof FakePlayer || Reader.connection == null || !Reader.connection.hasChannel(OrbitalStatusPayload.PayloadType)) {
            return;
        }
        OrbitalArray Array = OrbitalArray.get(Reader.server);
        PacketDistributor.sendToPlayer(Reader, new OrbitalStatusPayload(Array.mask(Reader.getUUID()), Array.readyAt(Reader.getUUID())));
    }
}
