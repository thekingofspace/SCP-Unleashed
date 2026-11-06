package net.rai.scp_unleashed.scp1762;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;
import java.util.Locale;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp1762Chat {
    public static final double Range = 16.0;
    public static final double ReviveRange = Scp1762BoxBlockEntity.ArmReach;
    private static final String Words = "they are no longer here";
    private static final String Revival = "here be dragons";

    private Scp1762Chat() {
    }

    public static String normalise(String Raw) {
        String Text = Raw == null ? "" : Raw.strip().toLowerCase(Locale.ROOT);
        Text = Text.replaceAll("[\\p{Punct}\\s\\u2026\\u2019\\u201D]+$", "");
        return Text.replaceAll("\\s+", " ");
    }

    public static boolean matches(String Raw) {
        return Words.equals(normalise(Raw));
    }

    public static boolean reviving(String Raw) {
        return Revival.equals(normalise(Raw));
    }

    public static boolean holdsPaper(Player Speaker) {
        return Speaker.getMainHandItem().is(Items.PAPER) || Speaker.getOffhandItem().is(Items.PAPER);
    }

    public static boolean called(ServerPlayer Speaker, String Raw) {
        if (Speaker.isSpectator() || !reviving(Raw) || !holdsPaper(Speaker) || !(Speaker.level() instanceof ServerLevel World)) {
            return false;
        }
        boolean Armed = false;
        BlockPos Centre = Speaker.blockPosition();
        int Reach = SectionPos.blockToSectionCoord((int) Math.ceil(ReviveRange)) + 1;
        ChunkPos Middle = new ChunkPos(Centre);
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                if (!World.hasChunk(Middle.x + Dx, Middle.z + Dz)) {
                    continue;
                }
                for (BlockEntity Found : List.copyOf(World.getChunk(Middle.x + Dx, Middle.z + Dz).getBlockEntities().values())) {
                    if (Found instanceof Scp1762BoxBlockEntity Box && !Box.isRemoved() && Box.dormant() && Box.centre().distanceToSqr(Speaker.position()) <= ReviveRange * ReviveRange) {
                        Box.arm(World, Speaker);
                        Armed = true;
                    }
                }
            }
        }
        return Armed;
    }

    public static boolean heard(ServerPlayer Speaker, String Raw) {
        if (Speaker.isSpectator() || !matches(Raw) || !(Speaker.level() instanceof ServerLevel World)) {
            return false;
        }
        boolean Answered = false;
        for (Scp1762BoxBlockEntity Box : Scp1762BoxBlockEntity.active()) {
            if (Box.getLevel() != World || Box.isRemoved() || !Box.listening()) {
                continue;
            }
            if (Box.centre().distanceToSqr(Speaker.position()) <= Range * Range) {
                Box.recall(World);
                Answered = true;
            }
        }
        return Answered;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void chat(ServerChatEvent Event) {
        ServerPlayer Speaker = Event.getPlayer();
        String Raw = Event.getRawText();
        Speaker.server.execute(() -> {
            if (!heard(Speaker, Raw)) {
                called(Speaker, Raw);
            }
        });
    }
}
