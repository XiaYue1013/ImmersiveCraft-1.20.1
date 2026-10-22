package mcjty.immcraft.api.cable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * A helper for itemblocks for cables
 */
public interface ICableItemBlockHelper {

    /**
     * Call this from your ItemBlock onItemUse implementation. If this returns
     * true you need to call super.onItemUse() otherwise just return false
     * @param player
     * @param world
     * @param pos
     * @return
     */
    boolean onItemUse(Player player, Level world, BlockPos pos);

    /**
     * Call this from your ItemBlock placeBlockAt implementation.
     * @param stack
     * @param player
     * @param world
     * @param pos
     * @param side
     * @param hitX
     * @param hitY
     * @param hitZ
     * @return
     */
    boolean placeBlockAt(ItemStack stack, Player player, Level world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ);

}
