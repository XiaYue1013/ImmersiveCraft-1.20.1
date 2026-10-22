package mcjty.immcraft.api.helpers;
import mcjty.immcraft.api.block.IOrientedBlock;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
public final class BlockOrientation implements IOrientedBlock {
    public static final BlockOrientation DEFAULT=new BlockOrientation();
    public Direction getFrontDirection(BlockState s) { return s.hasProperty(BlockStateProperties.HORIZONTAL_FACING)?s.getValue(BlockStateProperties.HORIZONTAL_FACING):Direction.SOUTH; }
    public Direction worldToBlockSpace(Level level,BlockPos pos,Direction side) {
        if(side.getAxis().isVertical()) return side;
        Direction facing=getFrontDirection(level.getBlockState(pos));
        return switch(facing) { case NORTH -> side.getOpposite();case EAST -> side.getCounterClockWise();case WEST -> side.getClockWise();default -> side; };
    }
}
