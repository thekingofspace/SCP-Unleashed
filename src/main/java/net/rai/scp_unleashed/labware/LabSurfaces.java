package net.rai.scp_unleashed.labware;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LabSurfaces {
    private static final List<Surface> Kinds = new CopyOnWriteArrayList<>();

    private LabSurfaces() {
    }

    public interface Surface {
        void sample(ServerLevel World, BlockPos Pos, Direction Side, List<ResourceLocation> Found);

        int clean(ServerLevel World, BlockPos Centre, int Radius);
    }

    public static synchronized void register(Surface Kind) {
        if (!Kinds.contains(Kind)) {
            Kinds.add(Kind);
        }
    }

    public static List<ResourceLocation> sample(ServerLevel World, BlockPos Pos, Direction Side) {
        List<ResourceLocation> Found = new ArrayList<>();
        for (Surface Kind : Kinds) {
            List<ResourceLocation> Picked = new ArrayList<>();
            Kind.sample(World, Pos, Side, Picked);
            for (ResourceLocation Id : Picked) {
                if (!Found.contains(Id)) {
                    Found.add(Id);
                }
            }
        }
        return List.copyOf(Found);
    }

    public static int clean(ServerLevel World, BlockPos Centre, int Radius) {
        int Killed = 0;
        for (Surface Kind : Kinds) {
            Killed += Kind.clean(World, Centre, Radius);
        }
        return Killed;
    }

    public static InteractionResult swab(UseOnContext Context) {
        Player User = Context.getPlayer();
        Level World = Context.getLevel();
        ItemStack Held = Context.getItemInHand();
        if (User == null || !LabVials.isSterileSwab(Held)) {
            return InteractionResult.PASS;
        }
        if (World instanceof ServerLevel Server) {
            BlockPos Pos = Context.getClickedPos();
            Component Subject = World.getBlockState(Pos).getBlock().getName();
            LabSwabRecord Record = new LabSwabRecord(false, Subject, sample(Server, Pos, Context.getClickedFace()), World.getDayTime());
            Item Used = Held.getItem();
            User.setItemInHand(Context.getHand(), ItemUtils.createFilledResult(Held, User, LabVials.usedSwab(Record)));
            User.awardStat(Stats.ITEM_USED.get(Used));
            User.displayClientMessage(Component.translatable("message.scp_unleashed.labware.swabbed", Subject).withStyle(ChatFormatting.GRAY), true);
            LabwareSounds.play(World, Pos, LabwareSounds.Swabbed, LabwareSounds.pitch(World));
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }
}
