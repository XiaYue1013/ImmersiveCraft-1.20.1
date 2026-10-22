package mcjty.immcraft.blocks.bundle;
import mcjty.immcraft.ImmersiveCraft;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
public final class BundleBlock extends BaseEntityBlock {
    public BundleBlock() { super(Properties.of().strength(0).noOcclusion().noCollission().sound(SoundType.WOOL)); }
    public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new BundleTE(pos,state); }
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t) { return l.isClientSide?null:createTickerHelper(t,ImmersiveCraft.BUNDLE_ENTITY.get(),(level,pos,state,be)->be.update()); }
    public void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos source,boolean moving) { if(!l.isClientSide && l.getBlockEntity(p) instanceof BundleTE be) be.checkConnections(); }
    public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving) {
        if(!s.is(next.getBlock()) && !l.isClientSide && l.getBlockEntity(p) instanceof BundleTE be) be.removeAllCables();
        super.onRemove(s,l,p,next,moving);
    }
    public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit) {
        if(!player.isShiftKeyDown() || !player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
        if(!l.isClientSide && l.mayInteract(player,p) && player.mayBuild() && l.getBlockEntity(p) instanceof BundleTE be) {
            var section=mcjty.immcraft.cables.CableSelection.findSelectedCable(player.getEyePosition(),player.getEyePosition().add(player.getLookAngle().scale(mcjty.immcraft.PortNetwork.reach(player))),be);
            if(section!=null) { be.removeCableFromNetwork(section);if(be.getCableSections().isEmpty()) l.removeBlock(p,false); }
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
}
