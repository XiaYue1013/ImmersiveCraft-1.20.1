package mcjty.immcraft;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import org.jetbrains.annotations.Nullable;

public final class DeviceEntity extends BlockEntity {
    public final Kind kind;
    private boolean loading;
    private boolean automation;
    public final ItemStackHandler items;
    private LazyOptional<IItemHandler> all;
    private final java.util.Map<Direction,LazyOptional<IItemHandler>> sides = new java.util.EnumMap<>(Direction.class);
    public boolean open;
    public int burnTime, cookTime, schema;
    public int bookPage;
    public float lid, previousLid;
    private float experience;

    public DeviceEntity(BlockPos pos,BlockState state) {
        super(ImmersiveCraft.DEVICE.get(),pos,state);
        kind=((ImmersiveBlock)state.getBlock()).kind;
        items=new ItemStackHandler(kind.slots) {
            @Override protected void onContentsChanged(int slot) { if(!loading) changed(); }
            @Override public boolean isItemValid(int slot, ItemStack stack) { return automation || valid(slot,stack); }
            @Override public int getSlotLimit(int slot) { return kind==Kind.BOOKSTAND?1:64; }
        };
        createCapabilities();
    }
    public Direction facing() { return getBlockState().getValue(ImmersiveBlock.FACING); }
    private void createCapabilities() {
        all=LazyOptional.of(() -> new RangedHandler(java.util.stream.IntStream.range(0,kind.slots).toArray()));
        sides.clear();
        for(Direction d:Direction.values()) {
            int[] slots=kind==Kind.FURNACE ? new int[]{d==Direction.UP?1:d==Direction.DOWN?2:0}
                : kind==Kind.WORKBENCH && d!=Direction.UP ? new int[0] : java.util.stream.IntStream.range(0,kind.slots).toArray();
            sides.put(d,LazyOptional.of(() -> new RangedHandler(slots)));
        }
    }
    private final class RangedHandler implements IItemHandler {
        private final int[] slots;
        RangedHandler(int[] slots) { this.slots=slots; }
        public int getSlots() { return slots.length; }
        public ItemStack getStackInSlot(int i) { return items.getStackInSlot(slots[i]); }
        public ItemStack insertItem(int i,ItemStack s,boolean simulate) { automation=true;try { return items.insertItem(slots[i],s,simulate); } finally { automation=false; } }
        public ItemStack extractItem(int i,int n,boolean simulate) { return items.extractItem(slots[i],n,simulate); }
        public int getSlotLimit(int i) { return items.getSlotLimit(slots[i]); }
        public boolean isItemValid(int i,ItemStack s) { return true; }
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,@Nullable Direction side) {
        if(!isRemoved() && kind!=Kind.BOOKSTAND && kind!=Kind.STICKS && kind!=Kind.ROCK && cap==ForgeCapabilities.ITEM_HANDLER) return (side==null?all:sides.get(side)).cast();
        return super.getCapability(cap,side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps();all.invalidate();sides.values().forEach(LazyOptional::invalidate); }
    @Override public void reviveCaps() { super.reviveCaps();createCapabilities(); }

    public boolean valid(int slot,ItemStack stack) {
        if(kind==Kind.BOOKSTAND) return stack.getItem() instanceof mcjty.immcraft.api.book.IBook;
        if(kind==Kind.FURNACE) return slot==0 ? ForgeHooks.getBurnTime(stack,RecipeType.SMELTING)>0 : slot==1 && smelt(stack)!=null;
        if(kind==Kind.WORKBENCH_SEC) return stack.is(ImmersiveCraft.SAW.get()) || stack.is(ImmersiveCraft.CHISEL.get());
        if(kind==Kind.BOOKSHELF) return PortConfig.book(stack);
        if(kind==Kind.STICKS) return stack.is(Items.STICK);
        return true;
    }
    public SmeltingRecipe smelt(ItemStack stack) {
        if(level==null || stack.isEmpty()) return null;
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SimpleContainer(stack),level).orElse(null);
    }
    public void changed() {
        setChanged();
        if(level!=null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
            boolean lit=burnTime>0;
            if(getBlockState().getValue(ImmersiveBlock.LIT)!=lit) level.setBlock(worldPosition,getBlockState().setValue(ImmersiveBlock.LIT,lit),3);
        }
    }
    public void toggle() {
        open=!open;
        level.playSound(null,worldPosition,open?SoundEvents.CHEST_OPEN:SoundEvents.CHEST_CLOSE,SoundSource.BLOCKS,.5f,.9f+level.random.nextFloat()*.1f);
        changed();
    }
    public static void give(Player p,ItemStack stack) {
        if(!stack.isEmpty() && !p.getInventory().add(stack)) p.drop(stack,false);
        p.inventoryMenu.broadcastChanges();
    }
    public boolean ignite(Player p,InteractionHand hand) {
        ItemStack held=p.getItemInHand(hand);
        if((kind!=Kind.FURNACE && kind!=Kind.STICKS) || !(PortConfig.itemIn(held,PortConfig.IGNITION.get()) || PortConfig.itemIn(held,PortConfig.CONSUME.get()))) return false;
        if(kind==Kind.STICKS) burnTime=600;
        else if(burnTime==0) {
            burnTime=ForgeHooks.getBurnTime(items.getStackInSlot(0),RecipeType.SMELTING);
            if(burnTime>0) consumeFuel();
        }
        // Original consumes/damages the ignition source even if there is no fuel.
        if(PortConfig.itemIn(held,PortConfig.CONSUME.get())) held.shrink(1);
        else held.hurtAndBreak(1,p,q -> q.broadcastBreakEvent(hand));
        level.playSound(null,worldPosition,SoundEvents.FLINTANDSTEEL_USE,SoundSource.BLOCKS,1,.9f+level.random.nextFloat()*.2f);
        changed(); return true;
    }
    public boolean interact(Player p,InteractionHand hand,int slot) {
        if(kind==Kind.BOOKSTAND) return interactBook(p,hand);
        if(ignite(p,hand)) return true;
        if(kind.lid() && !open) { toggle();return true; }
        if(kind==Kind.WORKBENCH_SEC && slot==1) return WorkbenchRecipes.craft(this,p);
        if(slot<0 || slot>=items.getSlots()) {
            if(kind.lid()) { toggle();return true; }
            return false;
        }
        ItemStack held=p.getItemInHand(hand);
        if(kind==Kind.STICKS && held.is(Items.STICK)) {
            int n=Math.min(p.isShiftKeyDown()?held.getCount():1,64-items.getStackInSlot(0).getCount());
            if(n>0) {
                ItemStack add=held.copy();add.setCount(n);
                ItemStack rest=items.insertItem(0,add,false);held.shrink(n-rest.getCount());
            }
            return true;
        }
        int n=p.isShiftKeyDown()?1:64;
        if(!held.isEmpty() && items.isItemValid(slot,held)) {
            ItemStack insert=held.copy();insert.setCount(Math.min(n,held.getCount()));
            ItemStack rest=items.insertItem(slot,insert,false);
            int accepted=insert.getCount()-rest.getCount();
            if(accepted>0) { held.shrink(accepted); if(kind==Kind.WORKBENCH_SEC) schema=0;return true; }
        }
        ItemStack extracted=items.extractItem(slot,n,false);
        if(extracted.isEmpty()) return false;
        awardOutputExperience(p,slot,extracted);
        give(p,extracted);
        changed();
        if(kind.display() && empty() || kind==Kind.STICKS && empty()) level.removeBlock(worldPosition,false);
        return true;
    }
    private void awardOutputExperience(Player p,int slot,ItemStack extracted) {
        if(kind==Kind.FURNACE && slot==2 && level instanceof ServerLevel sl) {
            int count=extracted.getCount(), remaining=items.getStackInSlot(2).getCount();
            float part=experience*count/(count+remaining);
            experience-=part;
            int xp=(int)part;
            if(sl.random.nextFloat()<part-xp) xp++;
            ExperienceOrb.award(sl,p.position(),xp);
            net.minecraftforge.event.ForgeEventFactory.firePlayerSmeltedEvent(p,extracted);
        }
    }
    public boolean insertClick(Player player,int selected,int amount) {
        ItemStack held=player.getMainHandItem();if(held.isEmpty() || kind==Kind.BOOKSTAND || selected<0 || selected>=items.getSlots()) return false;
        if(kind.lid() && !open) return false;
        int remaining=Math.min(amount,held.getCount()),original=remaining;
        for(int n=0;n<items.getSlots() && remaining>0;n++) {
            int slot=n==0?selected:n<=selected?n-1:n;
            if(!valid(slot,held)) continue;
            ItemStack requested=held.copyWithCount(remaining),rest=items.insertItem(slot,requested,false);
            int moved=remaining-rest.getCount();held.shrink(moved);remaining-=moved;
        }
        player.inventoryMenu.broadcastChanges();return remaining<original;
    }
    public boolean extractOne(Player player,int slot) {
        if(slot<0 || slot>=items.getSlots() || kind.lid() && !open) return false;
        ItemStack extracted=items.extractItem(slot,1,false);if(extracted.isEmpty()) return false;
        awardOutputExperience(player,slot,extracted);changed();
        give(player,extracted);if(kind.display() && empty() || kind==Kind.STICKS && empty()) level.removeBlock(worldPosition,false);return true;
    }
    public boolean empty() {
        for(int i=0;i<items.getSlots();i++) if(!items.getStackInSlot(i).isEmpty()) return false;
        return true;
    }
    public boolean interactBook(Player p,InteractionHand hand) {
        ItemStack current=items.getStackInSlot(0),held=p.getItemInHand(hand);
        if(!current.isEmpty()) {
            if(p.isShiftKeyDown()) { give(p,items.extractItem(0,1,false));bookPage=0;changed();return true; }
            if(current.getItem() instanceof mcjty.immcraft.api.book.IBook book) {
                var doc=mcjty.immcraft.books.BookDocument.builtin(book.getJson());
                var start=p.getEyePosition().subtract(net.minecraft.world.phys.Vec3.atLowerCornerOf(worldPosition));
                var pixel=mcjty.immcraft.books.BookPlane.hit(facing(),start,start.add(p.getLookAngle().scale(PortNetwork.reach(p))));
                String action=bookPage==0?">":mcjty.immcraft.books.BookPlane.action(doc,bookPage,pixel);
                navigateBook(action);return true;
            }
        } else if(held.getItem() instanceof mcjty.immcraft.api.book.IBook) {
            items.setStackInSlot(0,held.split(1));bookPage=0;changed();p.inventoryMenu.broadcastChanges();return true;
        }
        return false;
    }
    public void navigateBook(String action) {
        if(items.getStackInSlot(0).getItem() instanceof mcjty.immcraft.api.book.IBook book) {
            var doc=mcjty.immcraft.books.BookDocument.builtin(book.getJson());int next=doc.navigate(bookPage,action);
            if(next!=bookPage) { bookPage=next;changed();level.playSound(null,worldPosition,ImmersiveCraft.PAGE_TURN.get(),SoundSource.BLOCKS,PortConfig.PAGE_VOLUME.get().floatValue(),1); }
        }
    }
    private void consumeFuel() {
        ItemStack fuel=items.extractItem(0,1,false);
        if(items.getStackInSlot(0).isEmpty() && fuel.hasCraftingRemainingItem()) items.setStackInSlot(0,fuel.getCraftingRemainingItem());
    }
    public static void tick(Level level,BlockPos pos,BlockState state,DeviceEntity be) {
        if(level.isClientSide) {
            be.previousLid=be.lid;
            be.lid=net.minecraft.util.Mth.clamp(be.lid+(be.open?.08f:-.08f),0,1);
            return;
        }
        // Empty displays can also be emptied by automation.
        if(be.kind.display() && be.empty()) { level.removeBlock(pos,false);return; }
        if(be.burnTime<=0) return;
        boolean wasBurning=be.burnTime>0;
        be.burnTime--; be.setChanged();
        if(be.kind==Kind.STICKS) {
            if(be.burnTime==0) {
                be.items.extractItem(0,1,false);
                if(be.empty()) { level.removeBlock(pos,false);return; }
                be.burnTime=600;be.changed();
            }
        } else if(be.kind==Kind.FURNACE) {
            if(be.burnTime==0) {
                int next=ForgeHooks.getBurnTime(be.items.getStackInSlot(0),RecipeType.SMELTING);
                if(next>0) { be.burnTime=next+1;be.consumeFuel(); }
            }
            if(be.cookTime<=0) {
                if(!be.items.getStackInSlot(1).isEmpty()) be.cookTime=200;
            } else if(--be.cookTime==0) {
                SmeltingRecipe recipe=be.smelt(be.items.getStackInSlot(1));
                if(recipe!=null) {
                    ItemStack out=recipe.assemble(new SimpleContainer(be.items.getStackInSlot(1)),level.registryAccess());
                    ItemStack current=be.items.getStackInSlot(2);
                    if(!out.isEmpty() && (current.isEmpty() || ItemStack.isSameItemSameTags(current,out)) && current.getCount()+out.getCount()<=Math.min(64,out.getMaxStackSize())) {
                        // Check the entire output before consuming input. Fixes legacy mismatched-output loss.
                        be.items.extractItem(1,1,false);
                        out.grow(current.getCount());be.items.setStackInSlot(2,out);
                        be.experience+=recipe.getExperience();
                        be.changed();
                    } else be.cookTime=40;
                } else be.cookTime=40;
            }
            if(PortConfig.RAIN.get() && level.isRaining() && level.canSeeSky(pos)) be.burnTime=0;
        }
        if(wasBurning!=(be.burnTime>0)) be.changed();
        else if(level.getGameTime()%20==0) {
            // Small vanilla block events synchronize progress without resending the inventory.
            level.blockEvent(pos,state.getBlock(),1,be.burnTime);
            level.blockEvent(pos,state.getBlock(),2,be.cookTime);
        }
    }
    public void dropContents() {
        if(level==null || level.isClientSide) return;
        for(int i=0;i<items.getSlots();i++) {
            ItemStack stack=items.extractItem(i,64,false);
            if(!stack.isEmpty()) net.minecraft.world.level.block.Block.popResource(level,worldPosition,stack);
        }
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory",items.serializeNBT());tag.putBoolean("open",open);
        tag.putInt("burnTime",burnTime);tag.putInt("cookTime",cookTime);tag.putInt("schema",schema);tag.putFloat("experience",experience);
        tag.putInt("bookPage",bookPage);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);loading=true;
        if(tag.contains("inventory")) {
            // Keep the registry-defined slot count, even if a malformed save supplies Size.
            CompoundTag inventory=tag.getCompound("inventory").copy();inventory.putInt("Size",kind.slots);items.deserializeNBT(inventory);
        }
        open=tag.getBoolean("open");burnTime=Math.max(0,tag.getInt("burnTime"));cookTime=Math.max(0,tag.getInt("cookTime"));
        schema=Math.max(0,tag.getInt("schema"));experience=Math.max(0,tag.getFloat("experience"));loading=false;
        bookPage=Math.max(0,tag.getInt("bookPage"));
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public boolean triggerEvent(int id,int value) {
        if(level!=null && level.isClientSide) {
            if(id==1) { burnTime=Math.max(0,value);return true; }
            if(id==2) { cookTime=Math.max(0,value);return true; }
        }
        return id==1 || id==2 || super.triggerEvent(id,value);
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(1); }
}
