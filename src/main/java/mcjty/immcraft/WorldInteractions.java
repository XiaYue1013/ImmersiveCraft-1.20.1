package mcjty.immcraft;

import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=ImmersiveCraft.ID)
public final class WorldInteractions {
    public static boolean placeDisplay(Player p,BlockHitResult hit,boolean key) {
        Level l=p.level();ItemStack held=p.getMainHandItem();BlockPos support=hit.getBlockPos();
        if(l.isClientSide || held.isEmpty() || p.isSpectator() || !p.mayBuild()) return false;
        // The legacy P packet accepts any nonempty stack; automatic right click accepts ItemTool only.
        if(!key && (!(held.getItem() instanceof DiggerItem) || held.getItem() instanceof HoeItem)) return false;
        if(l.getBlockEntity(support) instanceof DeviceEntity existing && existing.kind.display()) {
            return existing.interact(p,InteractionHand.MAIN_HAND,ImmersiveBlock.selected(existing,p));
        }
        Direction face=hit.getDirection();
        if(face==Direction.DOWN) return false; // No underside implementation exists in 1.6.0.
        BlockPos target=support.relative(face);
        if(!l.hasChunkAt(target) || !l.isEmptyBlock(target) || !l.mayInteract(p,target) || !p.mayUseItemAt(target,face,held)) return false;
        if(!l.getBlockState(support).isFaceSturdy(l,support,face)) return false;
        Kind kind=face==Direction.UP?Kind.DISPLAY:Kind.WALL_DISPLAY;
        Direction facing=face==Direction.UP?p.getDirection().getOpposite():face;
        BlockState state=ImmersiveCraft.DEVICES.get(kind).get().defaultBlockState().setValue(ImmersiveBlock.FACING,facing);
        if(!l.setBlock(target,state,3)) return false;
        if(l.getBlockEntity(target) instanceof DeviceEntity be) {
            be.items.setStackInSlot(0,held.copy());p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);be.changed();p.inventoryMenu.broadcastChanges();return true;
        }
        l.removeBlock(target,false);return false;
    }
    @SubscribeEvent public static void rightClick(PlayerInteractEvent.RightClickBlock event) {
        Player p=event.getEntity();Level l=p.level();
        if(l.isClientSide || event.getHand()!=InteractionHand.MAIN_HAND || p.isSpectator() || !p.mayBuild() || !l.mayInteract(p,event.getPos())) return;
        ItemStack held=p.getMainHandItem();BlockPos pos=event.getPos();BlockState state=l.getBlockState(pos);
        boolean handled=false;
        if(held.is(Items.STICK)) {
            if(state.is(ImmersiveCraft.DEVICES.get(Kind.STICKS).get())) return;
            if(state.is(ImmersiveCraft.DEVICES.get(Kind.ROCK).get())) {
                l.playSound(null,pos,net.minecraft.sounds.SoundEvents.FLINTANDSTEEL_USE,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
                if(l.random.nextFloat()<PortConfig.FIRE_CHANCE.get()) {
                    Block.popResource(l,pos,new ItemStack(ImmersiveCraft.DEVICES.get(Kind.ROCK).get()));
                    l.setBlock(pos,ImmersiveCraft.DEVICES.get(Kind.STICKS).get().defaultBlockState(),3);
                    if(l.getBlockEntity(pos) instanceof DeviceEntity be) { addSticks(be,p);be.burnTime=600;be.changed(); }
                } else held.shrink(1);
                handled=true;
            } else if(l.isEmptyBlock(pos.above()) && state.isFaceSturdy(l,pos,Direction.UP) && l.mayInteract(p,pos.above()) && p.mayUseItemAt(pos.above(),Direction.UP,held)) {
                l.setBlock(pos.above(),ImmersiveCraft.DEVICES.get(Kind.STICKS).get().defaultBlockState().setValue(ImmersiveBlock.FACING,p.getDirection().getOpposite()),3);
                if(l.getBlockEntity(pos.above()) instanceof DeviceEntity be) addSticks(be,p);
                handled=true;
            }
        } else if(PortConfig.AXE.get() && held.is(ImmersiveCraft.DEVICES.get(Kind.ROCK).get().asItem()) && l.getBlockEntity(pos) instanceof DeviceEntity be && be.kind==Kind.STICKS && be.items.getStackInSlot(0).getCount()==1) {
            be.items.setStackInSlot(0,ItemStack.EMPTY);l.removeBlock(pos,false);held.shrink(1);Block.popResource(l,pos,new ItemStack(Items.STONE_AXE));handled=true;
        } else if(PortConfig.WORKBENCH.get() && held.is(Items.STONE_AXE) && state.is(BlockTags.LOGS)) {
            for(Direction dir:Direction.Plane.HORIZONTAL) {
                BlockPos next=pos.relative(dir);
                if(l.hasChunkAt(next) && l.getBlockState(next).is(BlockTags.LOGS) && l.mayInteract(p,next) && p.mayUseItemAt(next,dir,held)) {
                    Direction front=dir.getClockWise();
                    l.setBlock(pos,ImmersiveCraft.DEVICES.get(Kind.WORKBENCH).get().defaultBlockState().setValue(ImmersiveBlock.FACING,front),2);
                    l.setBlock(next,ImmersiveCraft.DEVICES.get(Kind.WORKBENCH_SEC).get().defaultBlockState().setValue(ImmersiveBlock.FACING,front),3);
                    l.updateNeighborsAt(pos,l.getBlockState(pos).getBlock());handled=true;break;
                }
            }
        } else if(PortConfig.RIGHT_CLICK.get() && !(state.is(Blocks.GRASS_BLOCK) && held.getItem() instanceof ShovelItem) && !(state.getBlock() instanceof ImmersiveBlock)) {
            handled=placeDisplay(p,event.getHitVec(),false);
        }
        if(handled) { event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS); }
    }
    private static void addSticks(DeviceEntity be,Player p) {
        ItemStack held=p.getMainHandItem();int n=Math.min(p.isShiftKeyDown()?held.getCount():1,64-be.items.getStackInSlot(0).getCount());
        ItemStack add=held.copy();add.setCount(n);ItemStack left=be.items.insertItem(0,add,false);held.shrink(n-left.getCount());
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST) public static void breakBlock(BlockEvent.BreakEvent e) {
        if(!e.getState().is(BlockTags.LEAVES) || e.getPlayer()==null || e.getPlayer().isCreative()) return;
        if(net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH,e.getPlayer().getMainHandItem())==0 && e.getLevel().getRandom().nextFloat()<PortConfig.LEAVES.get()) {
            if(e.getLevel() instanceof Level level) Block.popResource(level,e.getPos(),new ItemStack(Items.STICK));
        }
    }
}
