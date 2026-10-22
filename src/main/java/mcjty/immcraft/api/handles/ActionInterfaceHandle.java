package mcjty.immcraft.api.handles;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.function.Predicate;
public class ActionInterfaceHandle extends DefaultInterfaceHandle<ActionInterfaceHandle> {
    private Predicate<Player> action;
    public ActionInterfaceHandle(String id) { super(id); }
    public ActionInterfaceHandle action(Predicate<Player> action) { this.action=action;return this; }
    public Predicate<Player> getAction() { return action; }
    public ItemStack getCurrentStack(BlockEntity be) { return ItemStack.EMPTY; }
    public ItemStack getRenderStack(BlockEntity be,ItemStack stack) { return ItemStack.EMPTY; }
}
