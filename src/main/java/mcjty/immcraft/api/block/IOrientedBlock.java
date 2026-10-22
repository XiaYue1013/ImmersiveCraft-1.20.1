package mcjty.immcraft.api.block;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Implement this for blocks that need to connect to bundles and such. It defines
 * the orientation of the block.
 */
public interface IOrientedBlock {
    Direction getFrontDirection(BlockState state);

    Direction worldToBlockSpace(Level world, BlockPos pos, Direction side);
}
