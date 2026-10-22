package mcjty.immcraft.api.handles;
import mcjty.immcraft.DeviceEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.*;
public final class HandleSupport {
    private final List<IInterfaceHandle> handles=new ArrayList<>();
    public List<IInterfaceHandle> getInterfaceHandles() { return Collections.unmodifiableList(handles); }
    public void addInterfaceHandle(IInterfaceHandle h) { if(getHandleWithID(h.getSelectorID())!=null) throw new IllegalArgumentException("Duplicate handle "+h.getSelectorID());handles.add(h); }
    public IInterfaceHandle getHandleWithID(String id) { return handles.stream().filter(h->h.getSelectorID().equals(id)).findFirst().orElse(null); }
    public boolean addItemToHandle(BlockEntity be,Player player,ItemStack held,IInterfaceHandle h,int amount) {
        if(be.getLevel().isClientSide || held.isEmpty() || !h.acceptAsInput(held)) return false;
        int n=amount<0?held.getCount():Math.min(amount,held.getCount());ItemStack requested=held.copyWithCount(n);
        int remaining=h.insertInput(be,requested);if(remaining<0 || remaining>n) throw new IllegalStateException("Invalid handle remainder");
        held.shrink(n-remaining);be.setChanged();player.inventoryMenu.broadcastChanges();return remaining==0;
    }
    public boolean addItemAnywhere(BlockEntity be,Player player,ItemStack held,int amount) {
        for(var h:handles) if(h.acceptAsInput(held) && addItemToHandle(be,player,held,h,amount)) return true;
        return held.isEmpty();
    }
    public boolean handleClick(BlockEntity be,Player player,IInterfaceHandle h) {
        ItemStack held=player.getMainHandItem();if(held.isEmpty()) return false;
        int before=held.getCount();if(!addItemToHandle(be,player,held,h,-1)) addItemAnywhere(be,player,held,-1);return held.getCount()<before;
    }
    public boolean handleActivate(BlockEntity be,Player player,IInterfaceHandle h) {
        if(be.getLevel().isClientSide) return true;
        if(h.getAction()!=null && h.getAction().test(player)) return true;
        ItemStack held=player.getMainHandItem();boolean sneak=player.isShiftKeyDown();
        if(h.isOutputWithItem() && h.isItemThatNeedsExtractionItem(h.getCurrentStack(be))) {
            if(!h.isSuitableExtractionItem(held)) { player.displayClientMessage(net.minecraft.network.chat.Component.literal(h.getExtractionMessage()),true);return true; }
            int amount=h.getExtractAmount(sneak);boolean changed=false;
            for(int i=0;i<(amount<0?64:amount) && !held.isEmpty();i++) {
                ItemStack out=h.extractOutput(be,player,1);if(out.isEmpty()) break;held.shrink(1);DeviceEntity.give(player,out);changed=true;
            }
            return changed;
        }
        if(!held.isEmpty() && h.acceptAsInput(held) && addItemToHandle(be,player,held,h,h.getInsertAmount(sneak))) return true;
        ItemStack extracted=h.extractOutput(be,player,h.getExtractAmount(sneak));if(extracted.isEmpty()) return false;
        DeviceEntity.give(player,extracted);be.setChanged();return true;
    }
}
