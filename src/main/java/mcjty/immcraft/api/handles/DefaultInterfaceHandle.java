package mcjty.immcraft.api.handles;
import mcjty.immcraft.api.input.KeyType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import java.util.function.BiConsumer;
public class DefaultInterfaceHandle<T extends DefaultInterfaceHandle<T>> implements IInterfaceHandle {
    protected final String selectorID;protected int slot;protected float scale=1;private BiConsumer<KeyType,Player> keys;
    public DefaultInterfaceHandle(String id) { selectorID=id; }
    @SuppressWarnings("unchecked") public T slot(int slot) { this.slot=slot;return (T)this; }
    @SuppressWarnings("unchecked") public T scale(float scale) { this.scale=scale;return (T)this; }
    @SuppressWarnings("unchecked") public T keys(BiConsumer<KeyType,Player> keys) { this.keys=keys;return (T)this; }
    public String getSelectorID() { return selectorID; }
    public float getScale() { return scale; }
    protected IItemHandler inventory(BlockEntity be) {
        if(be instanceof net.minecraft.world.Container container) return new net.minecraftforge.items.wrapper.InvWrapper(container);
        return be.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(()->new IllegalArgumentException("Handle requires inventory at "+be.getBlockPos()));
    }
    public ItemStack getCurrentStack(BlockEntity be) { IItemHandler inv=inventory(be);return slot>=0 && slot<inv.getSlots()?inv.getStackInSlot(slot):ItemStack.EMPTY; }
    public boolean acceptAsInput(ItemStack stack) { return false; }
    public int insertInput(BlockEntity be,ItemStack stack) { return inventory(be).insertItem(slot,stack,false).getCount(); }
    public ItemStack extractOutput(BlockEntity be,Player p,int amount) { return inventory(be).extractItem(slot,amount<0?64:amount,false); }
    public boolean isOutput() { return false; }
    public boolean isCrafting() { return false; }
    public void onKeyPress(BlockEntity be,KeyType key,Player player) { if(keys!=null) keys.accept(key,player); }
    public boolean isOutputWithItem() { return false; }
    public boolean isItemThatNeedsExtractionItem(ItemStack stack) { return false; }
    public boolean isSuitableExtractionItem(ItemStack stack) { return false; }
    public String getExtractionMessage() { return ""; }
}
