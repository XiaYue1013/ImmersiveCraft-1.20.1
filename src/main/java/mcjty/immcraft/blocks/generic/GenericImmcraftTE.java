package mcjty.immcraft.blocks.generic;
import mcjty.immcraft.ImmersiveCraft;
import mcjty.immcraft.api.helpers.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
/** Native lifecycle adapter retaining the source API's NBT helper hooks. */
public class GenericImmcraftTE extends BlockEntity {
    public GenericImmcraftTE(BlockPos pos,BlockState state) { this(ImmersiveCraft.BUNDLE_ENTITY.get(),pos,state); }
    public GenericImmcraftTE(BlockEntityType<?> type,BlockPos pos,BlockState state) { super(type,pos,state); }
    public Level getWorld() { return getLevel(); }
    public BlockPos getPos() { return getBlockPos(); }
    public void markDirtyClient() { setChanged();if(level!=null && !level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3); }
    public void markDirty() { setChanged(); }
    public void readFromNBT(CompoundTag nbt) { super.load(nbt); }
    protected void writeToNBT(NBTHelper helper) { super.saveAdditional(helper.get()); }
    @Override public final void load(CompoundTag nbt) { readFromNBT(nbt); }
    @Override protected final void saveAdditional(CompoundTag nbt) { writeToNBT(NBTHelper.create(nbt)); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
