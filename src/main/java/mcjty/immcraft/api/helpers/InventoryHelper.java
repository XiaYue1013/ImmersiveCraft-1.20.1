package mcjty.immcraft.api.helpers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
public final class InventoryHelper {
    public static void spawnItemStack(Level level,BlockPos pos,ItemStack stack) { if(!level.isClientSide) net.minecraft.world.level.block.Block.popResource(level,pos,stack); }
    public static void giveItemToPlayer(Player player,ItemStack stack) { mcjty.immcraft.DeviceEntity.give(player,stack); }
}
