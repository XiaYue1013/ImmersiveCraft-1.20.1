package mcjty.immcraft.varia;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
public final class BlockTools {
    public static <T extends BlockEntity> Optional<T> getTE(Class<T> type,Level level,BlockPos pos) {
        if(level==null || !level.hasChunkAt(pos)) return Optional.empty();
        BlockEntity be=level.getBlockEntity(pos);
        return type.isInstance(be)?Optional.of(type.cast(be)):Optional.empty();
    }
}
