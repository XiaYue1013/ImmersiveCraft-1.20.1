package mcjty.immcraft.api.generic;
import mcjty.immcraft.api.handles.*;
import mcjty.immcraft.api.input.KeyType;
import net.minecraft.core.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.*;
public class GenericTE extends mcjty.immcraft.blocks.generic.GenericImmcraftTE {
    protected final HandleSupport handleSupport=new HandleSupport();
    private List<String> ingredients=List.of(),missing=List.of();
    public GenericTE(BlockEntityType<?> type,BlockPos pos,BlockState state) { super(type,pos,state); }
    public GenericBlock getBlock() { return (GenericBlock)getBlockState().getBlock(); }
    public List<String> getIngredients() { return ingredients; }
    public List<String> getMissingIngredients() { return missing; }
    public void setIngredients(List<String> ingredients,List<String> missing) { this.ingredients=List.copyOf(ingredients);this.missing=List.copyOf(missing); }
    public void calculateIngredients(List<String> present,List<String> absent,Player player) { present.addAll(ingredients);absent.addAll(missing); }
    public void addInterfaceHandle(IInterfaceHandle handle) { handleSupport.addInterfaceHandle(handle); }
    public List<IInterfaceHandle> getInterfaceHandles() { return handleSupport.getInterfaceHandles(); }
    public IInterfaceHandle getHandle(Player player) { var selector=getBlock().select(getLevel(),getBlockPos(),player);return selector==null?null:handleSupport.getHandleWithID(selector.getId()); }
    public boolean onClick(Player player) { var h=getHandle(player);return h!=null && handleSupport.handleClick(this,player,h); }
    public boolean onActivate(Player player) { var h=getHandle(player);return h!=null && handleSupport.handleActivate(this,player,h); }
    public void onKeyPress(KeyType key,Player player,Direction worldSide,Direction side,Vec3 hit) { var h=getHandle(player);if(h!=null) h.onKeyPress(this,key,player); }
}
