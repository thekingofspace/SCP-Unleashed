package net.rai.scp_unleashed.speaker.cassette;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.archive.UniqueGifts;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.speaker.SoundCatalog;
import net.rai.scp_unleashed.speaker.SpeakerRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class CassetteItem extends Item {
    public CassetteItem(Properties Properties) {
        super(Properties);
    }

    public static ItemStack create(ResourceLocation SoundId, Component Title, @Nullable Integer Colour) {
        ItemStack Stack = new ItemStack(SpeakerRegistry.CassetteTape.get());
        Stack.set(SpeakerRegistry.CassetteComponent.get(), new CassetteRecording(SoundId, Title, Optional.ofNullable(Colour)));
        return Stack;
    }

    public static ItemStack create(SoundCatalog.Entry Entry) {
        return create(Entry.Id(), Component.translatable(Entry.getTranslationKey()), null);
    }

    @Nullable
    public static CassetteRecording recordingOf(ItemStack Stack) {
        return Stack.get(SpeakerRegistry.CassetteComponent.get());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        return insert(World, User, Hand);
    }

    public boolean outlastsInsert(@Nullable Player User) {
        return false;
    }

    public static boolean canInsert(ItemStack Stack) {
        return Stack.getItem() instanceof CassetteItem && recordingOf(Stack) != null;
    }

    public static ItemInteractionResult insertInto(Level World, Player User, InteractionHand Hand) {
        InteractionResult Result = insert(World, User, Hand).getResult();
        if (Result == InteractionResult.FAIL) {
            return ItemInteractionResult.FAIL;
        }
        return Result.shouldSwing() ? ItemInteractionResult.SUCCESS : ItemInteractionResult.CONSUME;
    }

    public static InteractionResultHolder<ItemStack> insert(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        CassetteRecording Recording = recordingOf(Stack);
        if (Recording == null) {
            if (!World.isClientSide) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.cassette.blank"), true);
            }
            return InteractionResultHolder.fail(Stack);
        }
        if (!(User instanceof ServerPlayer Listener)) {
            return AnomalousLibrary.clientOwns(Recording.Sound()) ? InteractionResultHolder.consume(Stack) : InteractionResultHolder.success(Stack);
        }
        AnomalousLibrary.Outcome Result = AnomalousLibrary.add(Listener, Recording);
        if (Result == AnomalousLibrary.Outcome.Duplicate) {
            Listener.displayClientMessage(Component.translatable("message.scp_unleashed.cassette.duplicate", Recording.Title()), true);
            return InteractionResultHolder.fail(Stack);
        }
        if (Result == AnomalousLibrary.Outcome.Full) {
            Listener.displayClientMessage(Component.translatable("message.scp_unleashed.cassette.full", CassetteLibrary.MaxRecordings), true);
            return InteractionResultHolder.fail(Stack);
        }
        World.playSound(null, Listener.getX(), Listener.getY(), Listener.getZ(), SpeakerRegistry.CassetteInsert.get(), SoundSource.PLAYERS, 0.9f,
                0.95f + World.getRandom().nextFloat() * 0.1f);
        Listener.displayClientMessage(Component.translatable("message.scp_unleashed.cassette.added", Recording.Title()), true);
        Listener.awardStat(Stats.ITEM_USED.get(Stack.getItem()));
        if (!(Stack.getItem() instanceof CassetteItem Tape) || !Tape.outlastsInsert(Listener)) {
            Stack.consume(1, Listener);
        }
        return InteractionResultHolder.success(Stack);
    }

    @Override
    public Component getName(ItemStack Stack) {
        CassetteRecording Recording = recordingOf(Stack);
        return Recording == null ? super.getName(Stack) : Component.translatable("item.scp_unleashed.cassette_tape.named", Recording.Title());
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        CassetteRecording Recording = recordingOf(Stack);
        if (Recording == null) {
            Lines.add(Component.translatable("item.scp_unleashed.cassette_tape.blank").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        Lines.add(Component.translatable("item.scp_unleashed.cassette_tape.anomalous").withStyle(Look -> Look.withColor(0xE38176)));
        Lines.add(Component.translatable("item.scp_unleashed.cassette_tape.hint").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.cassette_tape.sound", Recording.Sound().toString()).withStyle(ChatFormatting.GOLD));
            if (outlastsInsert(null)) {
                Lines.add(Component.translatable("item.scp_unleashed.cassette_tape.library_kept").withStyle(ChatFormatting.GOLD));
                Lines.add(UniqueGifts.keptLine());
            } else {
                Lines.add(Component.translatable("item.scp_unleashed.cassette_tape.library").withStyle(ChatFormatting.GOLD));
            }
        }
    }
}
