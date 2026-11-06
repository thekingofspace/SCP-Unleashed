package net.rai.scp_unleashed.dclass;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.control.ControlRoster;
import net.rai.scp_unleashed.dclass.network.OpenPersonnelFilePayload;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class PersonnelFileItem extends Item {
    private final boolean Blank;

    public PersonnelFileItem(boolean Blank, Properties Settings) {
        super(Settings);
        this.Blank = Blank;
    }

    public boolean isBlank() {
        return Blank;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        PersonnelRecord Record = Stack.get(DClassRegistry.RecordComponent.get());
        if (Blank || Record == null) {
            return InteractionResultHolder.pass(Stack);
        }
        if (User instanceof ServerPlayer Server) {
            ControlRoster.BodyRecord Found = ControlRoster.get(Server.getServer()).record(Record.Subject());
            ControlRoster.BodyStatus Status = Found == null ? ControlRoster.BodyStatus.Unknown : Found.Status();
            String Controller = Found == null || Found.ControllerName() == null ? "" : Found.ControllerName();
            PacketDistributor.sendToPlayer(Server, new OpenPersonnelFilePayload(Hand == InteractionHand.MAIN_HAND, (byte) Status.ordinal(), Controller));
            Server.level().playSound(null, Server.getX(), Server.getY(), Server.getZ(), DClassRegistry.FileOpen.get(), SoundSource.PLAYERS, 0.7f, 1.0f);
        }
        return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
    }

    @Override
    public Component getName(ItemStack Stack) {
        PersonnelRecord Record = Stack.get(DClassRegistry.RecordComponent.get());
        if (!Blank && Record != null) {
            return Component.translatable("item.scp_unleashed.dclass_personnel_file.named", Record.designation());
        }
        return super.getName(Stack);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        PersonnelRecord Record = Stack.get(DClassRegistry.RecordComponent.get());
        if (Blank || Record == null) {
            Lines.add(Component.translatable("item.scp_unleashed.blank_personnel_file.tooltip").withStyle(ChatFormatting.GRAY));
            return;
        }
        Lines.add(Component.translatable("item.scp_unleashed.dclass_personnel_file.id", Record.designation()).withStyle(ChatFormatting.GOLD));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.dclass_personnel_file.trait", Record.trait().getName()).withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("item.scp_unleashed.dclass_personnel_file.crime", Record.crime().getName()).withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("item.scp_unleashed.dclass_personnel_file.iq", Record.Iq()).withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("item.scp_unleashed.dclass_personnel_file.athletic", Record.Athletic(), DClassScores.band(Record.Athletic())).withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("item.scp_unleashed.dclass_personnel_file.read").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
