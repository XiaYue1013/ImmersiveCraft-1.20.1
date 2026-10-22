package mcjty.immcraft.api.generic;
import mcjty.immcraft.*;
import mcjty.immcraft.api.block.IOrientedBlock;
import mcjty.immcraft.api.handles.HandleSelector;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.*;
import java.util.*;
public abstract class GenericBlock extends BaseEntityBlock implements IOrientedBlock {
    private final Map<String,HandleSelector> selectors=new LinkedHashMap<>();
    protected GenericBlock(Properties properties) { super(properties); }
    protected mcjty.immcraft.api.IImmersiveCraft getApi() { return ImmersiveCraft.api; }
    public void addSelector(HandleSelector selector) { selectors.put(selector.getId(),selector); }
    public Map<String,HandleSelector> getSelectors() { return Collections.unmodifiableMap(selectors); }
    public Direction getFrontDirection(BlockState s) { return mcjty.immcraft.api.helpers.BlockOrientation.DEFAULT.getFrontDirection(s); }
    public Direction worldToBlockSpace(Level world,BlockPos pos,Direction side) { return mcjty.immcraft.api.helpers.BlockOrientation.DEFAULT.worldToBlockSpace(world,pos,side); }
    public Vec3 blockToWorldSpace(Level world,BlockPos pos,Vec3 v) { return SlotLayout.toWorld(v,getFrontDirection(world.getBlockState(pos))); }
    public Vec3 worldToBlockSpace(Level world,BlockPos pos,Vec3 v) { return SlotLayout.toLocal(v,getFrontDirection(world.getBlockState(pos))); }
    public HandleSelector select(Level world,BlockPos pos,Player player) {
        Vec3 start=worldToBlockSpace(world,pos,player.getEyePosition().subtract(Vec3.atLowerCornerOf(pos))),end=worldToBlockSpace(world,pos,player.getEyePosition().add(player.getLookAngle().scale(PortNetwork.reach(player))).subtract(Vec3.atLowerCornerOf(pos)));
        double best=Double.MAX_VALUE;HandleSelector result=null;
        for(var selector:selectors.values()) { var hit=selector.getBox().clip(start,end);if(hit.isPresent() && hit.get().distanceToSqr(start)<best) { best=hit.get().distanceToSqr(start);result=selector; } }
        return result;
    }
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    public InteractionResult use(BlockState state,Level world,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(!world.mayInteract(player,pos) || player.isSpectator() || !player.mayBuild()) return InteractionResult.FAIL;
        if(world.getBlockEntity(pos) instanceof GenericTE te) return world.isClientSide || te.onActivate(player)?InteractionResult.sidedSuccess(world.isClientSide):InteractionResult.PASS;
        return InteractionResult.PASS;
    }
    public boolean onClick(Level world,BlockPos pos,Player player) { return world.getBlockEntity(pos) instanceof GenericTE te && te.onClick(player); }
    public void onRemove(BlockState state,Level world,BlockPos pos,BlockState next,boolean moving) {
        if(!state.is(next.getBlock()) && !world.isClientSide && world.getBlockEntity(pos) instanceof net.minecraft.world.Container container) net.minecraft.world.Containers.dropContents(world,pos,container);
        super.onRemove(state,world,pos,next,moving);
    }
}
