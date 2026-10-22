package mcjty.immcraft.api.handles;
import net.minecraft.world.item.ItemStack;
import java.util.*;
public class InputInterfaceHandle extends DefaultInterfaceHandle<InputInterfaceHandle> {
    private final List<ItemStack> accepted=new ArrayList<>();
    public InputInterfaceHandle(String id) { super(id); }
    public InputInterfaceHandle input(ItemStack item) { accepted.add(item.copy());return this; }
    public boolean acceptAsInput(ItemStack stack) { return !stack.isEmpty() && (accepted.isEmpty() || accepted.stream().anyMatch(s->s.is(stack.getItem()))); }
}
