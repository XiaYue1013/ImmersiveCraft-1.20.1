package mcjty.immcraft.compat;

import mcjty.immcraft.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.ChatFormatting;

final class ProbeText {
    static Component text(DeviceEntity be,Player player) {
        if(be.kind==Kind.BOOKSTAND && !be.items.getStackInSlot(0).isEmpty()) return Component.literal("Sneak-right click to remove book").withStyle(ChatFormatting.BLUE);
        if(be.kind==Kind.STICKS) return Component.literal("Sticks: "+be.items.getStackInSlot(0).getCount()).withStyle(ChatFormatting.GREEN);
        int slot=ImmersiveBlock.selected(be,player);
        ItemStack stack=slot<0?ItemStack.EMPTY:be.kind==Kind.WORKBENCH_SEC && slot==1?WorkbenchRecipes.current(be).output():slot<be.items.getSlots()?be.items.getStackInSlot(slot):ItemStack.EMPTY;
        return stack.isEmpty()?Component.empty():stack.getHoverName().copy().append(" ("+stack.getCount()+")").withStyle(ChatFormatting.GREEN);
    }
    static Component bundle(mcjty.immcraft.blocks.bundle.BundleTE be,Player player) {
        var text=Component.empty();
        for(var section:be.getCableSections()) {
            var network=mcjty.immcraft.multiblock.MultiBlockData.getNetwork(section.getType().getCableHandler().getNetworkName(section.getSubType()));
            if(network==null || section.getId()<0) continue;
            int size;
            if(!be.getLevel().isClientSide) { network.bind(be.getLevel());var block=network.getMultiblock(section.getId());size=block==null?0:block.getBlockCount(); }
            else { var info=network.getClientInfo(section.getId());size=info instanceof mcjty.immcraft.cables.CableClientInfo c?c.getCount():0;network.refreshInfo(section.getId()); }
            if(!text.getString().isEmpty()) text.append("\n");
            text.append("Id: "+section.getId()+" (Size: "+size+")");
            if(player.isShiftKeyDown()) text.append("\n"+section.getType().getReadableName()+": "+section.getConnection(0)+" : "+be.getBlockPos()+" : "+section.getConnection(1));
        }
        return text.withStyle(ChatFormatting.GREEN);
    }
}
