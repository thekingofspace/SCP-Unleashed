package net.rai.scp_unleashed.transferpod;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.stasis.StasisPods;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TransferPods {
    public static final String MessagePrefix = "message.scp_unleashed.transfer_pod.";
    private static final Logger Log = LogUtils.getLogger();
    private static final Map<ResourceLocation, TransferPodContent> Contents = Collections.synchronizedMap(new LinkedHashMap<>());

    private TransferPods() {
    }

    public static TransferPodContent register(TransferPodContent Content) {
        TransferPodContent Previous = Contents.putIfAbsent(Content.Id(), Content);
        if (Previous != null && Previous != Content) {
            throw new IllegalStateException("Transfer Pod content " + Content.Id() + " is already registered");
        }
        return Content;
    }

    public static TransferPodContent register(ResourceLocation Id, String LabelKey, int Colour, TransferPodContent.Release Releaser) {
        return register(new TransferPodContent(Id, LabelKey, Colour, Releaser));
    }

    public static TransferPodContent register(ResourceLocation Id, String LabelKey, int Colour, TransferPodContent.Release Releaser, boolean Listed) {
        return register(new TransferPodContent(Id, LabelKey, Colour, Releaser, Listed));
    }

    public static Optional<TransferPodContent> content(@Nullable ResourceLocation Id) {
        return Id == null ? Optional.empty() : Optional.ofNullable(Contents.get(Id));
    }

    public static List<TransferPodContent> all() {
        synchronized (Contents) {
            return List.copyOf(new ArrayList<>(Contents.values()));
        }
    }

    public static boolean isPod(ItemStack Stack) {
        return Stack.getItem() instanceof TransferPodItem;
    }

    @Nullable
    public static ResourceLocation contentId(ItemStack Stack) {
        return isPod(Stack) ? Stack.get(TransferPodRegistry.Contents.get()) : null;
    }

    @Nullable
    public static TransferPodContent contentOf(ItemStack Stack) {
        return content(contentId(Stack)).orElse(null);
    }

    public static boolean isLoaded(ItemStack Stack) {
        return contentId(Stack) != null;
    }

    public static ItemStack loaded(ResourceLocation Id) {
        ItemStack Stack = new ItemStack(TransferPodRegistry.Pod.get());
        Stack.set(TransferPodRegistry.Contents.get(), Id);
        return Stack;
    }

    public static ItemStack spent() {
        return new ItemStack(TransferPodRegistry.Pod.get());
    }

    public static Component labelOf(ItemStack Stack) {
        ResourceLocation Id = contentId(Stack);
        if (Id == null) {
            return Component.translatable(TransferPodItem.Key + "empty");
        }
        return content(Id).map(TransferPodContent::label).orElse(Component.translatable(TransferPodItem.Key + "unknown"));
    }

    public static int open(ServerLevel World, ItemStack Stack, Vec3 Where, float Yaw, @Nullable Player User) {
        ResourceLocation Id = contentId(Stack);
        if (Id == null) {
            tell(User, Component.translatable(MessagePrefix + "spent").withStyle(ChatFormatting.GRAY));
            TransferPodEffects.deny(World, Where);
            return 0;
        }
        TransferPodContent Held = Contents.get(Id);
        if (Held == null) {
            tell(User, Component.translatable(MessagePrefix + "unknown").withStyle(ChatFormatting.RED));
            TransferPodEffects.deny(World, Where);
            return 0;
        }
        int Freed;
        try {
            Freed = Held.Releaser().release(World, Where, Yaw, User);
        } catch (RuntimeException Problem) {
            Log.warn("Transfer Pod content {} failed to release", Id, Problem);
            Freed = 0;
        }
        if (Freed <= 0) {
            tell(User, Component.translatable(MessagePrefix + "failed", Held.label()).withStyle(ChatFormatting.RED));
            TransferPodEffects.deny(World, Where);
            return 0;
        }
        TransferPodEffects.release(World, Where, Held);
        World.gameEvent(User, GameEvent.ENTITY_PLACE, Where);
        tell(User, Component.translatable(MessagePrefix + "released", Held.label()).withStyle(Style.EMPTY.withColor(TransferPodItem.Colour)));
        return Freed;
    }

    public static boolean breakOut(ServerLevel World, ItemStack Stack, Vec3 Where) {
        TransferPodContent Held = contentOf(Stack);
        if (Held == null) {
            return false;
        }
        int Freed;
        try {
            Freed = Held.Releaser().release(World, Where, World.random.nextFloat() * 360.0f, null);
        } catch (RuntimeException Problem) {
            Log.warn("Transfer Pod content {} failed to break out", Held.Id(), Problem);
            Freed = 0;
        }
        TransferPodEffects.breakOut(World, Where, Held);
        StasisPods.notice(World, Where, Component.translatable(MessagePrefix + "broke_open", Held.label()));
        return Freed > 0;
    }

    private static void tell(@Nullable Player User, Component Text) {
        if (User != null) {
            User.displayClientMessage(Text, true);
        }
    }
}
