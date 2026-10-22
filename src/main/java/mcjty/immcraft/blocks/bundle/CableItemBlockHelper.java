package mcjty.immcraft.blocks.bundle;
import mcjty.immcraft.*;
import mcjty.immcraft.api.cable.*;
import mcjty.immcraft.cables.CableSection;
import net.minecraft.core.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.*;
/** Native server-validated equivalent of the source helper; caller retains Item/stack consumption. */
public final class CableItemBlockHelper implements ICableItemBlockHelper {
    private final ICableType type;private final ICableSubType subType;
    public CableItemBlockHelper(ICableType type,ICableSubType subType) { this.type=type;this.subType=subType; }
    public boolean onItemUse(Player player,Level world,BlockPos pos) {
        if(world.isClientSide) return true;
        if(!allowed(player,world,pos)) return false;
        if(world.getBlockEntity(pos) instanceof BundleTE bundle && bundle.countCableEndPoints(type,subType)==0) {
            BlockHitResult hit=PortNetwork.trace(player);
            if(hit.getType()==HitResult.Type.BLOCK && hit.getBlockPos().equals(pos)) {
                Vec3 p=hit.getLocation().subtract(Vec3.atLowerCornerOf(pos));
                addCable(world,pos,hit.getDirection(),(float)p.x,(float)p.y,(float)p.z);
                // Old helper returns false to skip normal block placement; consume exactly one new segment here.
                if(!player.getAbilities().instabuild) player.getMainHandItem().shrink(1);
                player.inventoryMenu.broadcastChanges();return false;
            }
        }
        return true;
    }
    private boolean allowed(Player p,Level l,BlockPos pos) {
        return !p.isSpectator() && p.mayBuild() && l.hasChunkAt(pos) && l.mayInteract(p,pos) && p.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos))<=Math.pow(PortNetwork.reach(p)+1,2);
    }
    public boolean placeBlockAt(ItemStack stack,Player player,Level world,BlockPos pos,Direction side,float x,float y,float z) {
        if(world.isClientSide) return true;
        if(!allowed(player,world,pos) || !Float.isFinite(x+y+z) || x<0 || x>1 || y<0 || y>1 || z<0 || z>1) return false;
        if(!(world.getBlockEntity(pos) instanceof BundleTE)) {
            if(!world.getBlockState(pos).canBeReplaced() || !world.setBlock(pos,ImmersiveCraft.BUNDLE.get().defaultBlockState(),3)) return false;
        }
        addCable(world,pos,side,x,y,z);return true;
    }
    private void addCable(Level world,BlockPos pos,Direction hit,float x,float y,float z) {
        BlockPos adj=pos.relative(hit.getOpposite());Vec3 vector=Vec3.atCenterOf(pos);
        if(world.hasChunkAt(adj) && world.getBlockState(adj).isFaceSturdy(world,adj,hit)) vector=new Vec3(adj.getX()+x+hit.getStepX()*.1,adj.getY()+y+hit.getStepY()*.1,adj.getZ()+z+hit.getStepZ()*.1);
        else if(world.hasChunkAt(adj) && world.getBlockEntity(adj) instanceof BundleTE be) {
            CableSection s=be.findConnectableSection(type,subType,Collections.emptySet());
            if(s!=null) vector=s.getVector().add(hit.getStepX(),hit.getStepY(),hit.getStepZ());
        }
        if(world.getBlockEntity(pos) instanceof BundleTE be) be.addCableToNetwork(type,subType,vector);
    }
}
