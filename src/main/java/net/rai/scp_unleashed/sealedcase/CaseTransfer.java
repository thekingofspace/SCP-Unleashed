package net.rai.scp_unleashed.sealedcase;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import org.jetbrains.annotations.Nullable;

public final class CaseTransfer {
    public static final String Prefix = "message.scp_unleashed.sealed_case.";

    private CaseTransfer() {
    }

    public static InteractionResult use(ItemStack Case, UseOnContext Context) {
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        if (!StorageContainerBlock.isContainer(World.getBlockState(Pos))) {
            return InteractionResult.PASS;
        }
        StorageContainerBlockEntity Locker = StorageContainerBlock.containerAt(World, Pos);
        if (Locker == null) {
            return InteractionResult.PASS;
        }
        Player User = Context.getPlayer();
        SealedContents Inside = SealedCaseItem.contents(Case);
        boolean Sneaking = User != null && User.isSecondaryUseActive();
        if (Inside == null && !Sneaking) {
            return InteractionResult.PASS;
        }
        if (User != null && User.getCooldowns().isOnCooldown(Case.getItem())) {
            return InteractionResult.CONSUME;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        boolean Done = Inside == null ? retrieve(World, User, Case, Locker) : deliver(World, User, Case, Inside, Locker);
        if (!Done) {
            return InteractionResult.FAIL;
        }
        World.gameEvent(User, GameEvent.BLOCK_CHANGE, Locker.getStoragePos());
        if (User != null) {
            User.getCooldowns().addCooldown(Case.getItem(), SealedCaseItem.UseCooldown);
            User.awardStat(Stats.ITEM_USED.get(Case.getItem()));
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean deliver(Level World, @Nullable Player User, ItemStack Case, SealedContents Inside, StorageContainerBlockEntity Locker) {
        ItemStack Held = Inside.stack();
        ItemStack One = Held.copyWithCount(1);
        if (Locker.hasContents()) {
            refuse(World, User, Locker, Component.translatable(Prefix + "occupied", SealedCaseItem.describe(Locker.peekContents())));
            return false;
        }
        Component Refusal = Locker.refusalFor(One);
        if (Refusal != null) {
            refuse(World, User, Locker, Refusal);
            return false;
        }
        Locker.receive(One);
        ItemStack Rest = Held.copyWithCount(Held.getCount() - 1);
        if (Rest.isEmpty()) {
            Case.remove(SealedCaseRegistry.Contents.get());
        } else {
            Case.set(SealedCaseRegistry.Contents.get(), new SealedContents(Rest));
        }
        play(World, User, Locker, SealedCaseRegistry.Unlatch.get(), 1.0f);
        play(World, User, Locker, SealedCaseRegistry.Release.get(), 0.7f);
        if (User != null) {
            Component Message = Rest.isEmpty() ? Component.translatable(Prefix + "transferred", SealedCaseItem.describe(One))
                    : Component.translatable(Prefix + "transferred_rest", SealedCaseItem.describe(One), SealedCaseItem.describe(Rest));
            User.displayClientMessage(Message.copy().withStyle(ChatFormatting.GOLD), true);
        }
        return true;
    }

    private static boolean retrieve(Level World, @Nullable Player User, ItemStack Case, StorageContainerBlockEntity Locker) {
        if (!Locker.hasContents()) {
            if (User != null) {
                User.displayClientMessage(Component.translatable(Prefix + "container_empty").withStyle(ChatFormatting.GRAY), true);
            }
            return false;
        }
        ItemStack Stored = Locker.peekContents();
        if (!SealedCaseItem.canSeal(Stored)) {
            refuse(World, User, Locker, Component.translatable(Prefix + "refused", Stored.getHoverName()));
            return false;
        }
        ItemStack Taken = Locker.release();
        Case.set(SealedCaseRegistry.Contents.get(), new SealedContents(Taken));
        play(World, User, Locker, SealedCaseRegistry.Seal.get(), 1.0f);
        play(World, User, Locker, SealedCaseRegistry.Clamp.get(), 0.9f);
        if (User != null) {
            User.displayClientMessage(Component.translatable(Prefix + "sealed", SealedCaseItem.describe(Taken)).withStyle(ChatFormatting.GOLD), true);
        }
        return true;
    }

    private static void refuse(Level World, @Nullable Player User, StorageContainerBlockEntity Locker, Component Reason) {
        if (User != null) {
            User.displayClientMessage(Reason.copy().withStyle(ChatFormatting.RED), true);
        }
        play(World, User, Locker, SealedCaseRegistry.Deny.get(), 0.8f);
        Locker.refuse();
    }

    private static void play(Level World, @Nullable Player User, StorageContainerBlockEntity Locker, SoundEvent Sound, float Volume) {
        Vec3 Where = User != null ? new Vec3(User.getX(), User.getY() + 1.0, User.getZ()) : Locker.soundCentre();
        World.playSound(null, Where.x, Where.y, Where.z, Sound, SoundSource.PLAYERS, Volume, 0.95f + World.random.nextFloat() * 0.08f);
    }
}
