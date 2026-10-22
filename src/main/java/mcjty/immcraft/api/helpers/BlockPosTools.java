package mcjty.immcraft.api.helpers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
public final class BlockPosTools {
    public static boolean isAdjacent(BlockPos a,BlockPos b) { return a.distManhattan(b)==1; }
    public static void writeToNBT(CompoundTag n,String key,BlockPos p) { if(p!=null) { n.putInt(key+"x",p.getX());n.putInt(key+"y",p.getY());n.putInt(key+"z",p.getZ()); } }
    public static BlockPos readFromNBT(CompoundTag n,String key) { return n.contains(key+"x")?new BlockPos(n.getInt(key+"x"),n.getInt(key+"y"),n.getInt(key+"z")):null; }
}
