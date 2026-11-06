package net.rai.scp_unleashed.dclass.duty;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;

import java.util.List;

public class DClassManagerItem extends Item {
    public DClassManagerItem(Properties Settings) {
        super(Settings);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack Stack, Player User, LivingEntity Target, InteractionHand Hand) {
        if (!(Target instanceof DClass Subject)) {
            return InteractionResult.PASS;
        }
        if (User instanceof ServerPlayer Server) {
            if (!DClassDuties.available(Subject)) {
                DClassDuties.warn(Server, "unavailable");
            } else {
                open(Server, Subject);
            }
        }
        return InteractionResult.sidedSuccess(User.level().isClientSide);
    }

    public static void open(ServerPlayer User, DClass Subject) {
        User.openMenu(new SimpleMenuProvider((Id, Inventory, Viewer) -> new DClassManagerMenu(Id, Inventory, Subject),
                Component.translatable("container.scp_unleashed.dclass_manager", Subject.getName())), Buffer -> Buffer.writeVarInt(Subject.getId()));
        User.level().playSound(null, User.getX(), User.getEyeY(), User.getZ(), DClassRegistry.FileOpen.get(), SoundSource.PLAYERS, 0.7f, 1.05f + User.getRandom().nextFloat() * 0.1f);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.dclass_manager.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
