package mcjty.immcraft.apiimpl;
import mcjty.immcraft.*;
import mcjty.immcraft.api.IImmersiveCraft;
import mcjty.immcraft.api.cable.*;
import mcjty.immcraft.api.multiblock.*;
import mcjty.immcraft.cables.*;
import mcjty.immcraft.multiblock.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import java.util.Optional;
public final class ImmersiveCraftApi implements IImmersiveCraft {
    public void registerCableType(ICableType type) { CableRegistry.registerCableType(type); }
    public ICableItemBlockHelper createItemBlockHelper(ICableType type,ICableSubType subtype) { return new mcjty.immcraft.blocks.bundle.CableItemBlockHelper(type,subtype); }
    @SuppressWarnings("unchecked") public <T extends IMultiBlock> IMultiBlockNetwork<T> createMultiBlockNetwork(String name,IMultiBlockFactory<T> factory,Direction[] directions) {
        MultiBlockNetwork<?> existing=MultiBlockData.getNetwork(name);
        return existing==null?new MultiBlockNetwork<>(name,factory,directions):(IMultiBlockNetwork<T>)existing;
    }
    public IMultiBlockNetwork<Cable> createCableNetwork(String name,ICableType type,ICableSubType subType) {
        return createMultiBlockNetwork(name,new IMultiBlockFactory<Cable>() {
            public Cable create() { return new Cable(type,subType); }
            public boolean isSameType(IMultiBlock mb) { return mb instanceof Cable c && c.getType()==type && c.getSubType()==subType; }
            public IMultiBlockClientInfo createClientInfo() { return new CableClientInfo(0); }
        },Direction.values());
    }
    public Optional<IBundle> getBundle(Level level,BlockPos pos) { return level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof IBundle bundle?Optional.of(bundle):Optional.empty(); }
    public void requestIngredients(BlockPos pos) { PortNetwork.send(6); }
    public void registerBlockClick() { PortNetwork.send(3); }
    public double getMaxHandleRenderDistanceSquared() { return PortConfig.RENDER.get()*PortConfig.RENDER.get(); }
    public void openManual(Player player) { DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->mcjty.immcraft.client.BookClient.open(player.getMainHandItem())); }
}
