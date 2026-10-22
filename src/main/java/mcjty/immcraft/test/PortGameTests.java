package mcjty.immcraft.test;

import mcjty.immcraft.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.*;

@GameTestHolder("immcraft")
@PrefixGameTestTemplate(false)
public final class PortGameTests {
    private static final BlockPos POS=new BlockPos(3,2,3);
    private static DeviceEntity place(GameTestHelper h,Kind kind) {
        h.setBlock(POS.below(),Blocks.STONE);
        h.setBlock(POS.relative(Direction.NORTH),Blocks.STONE);
        h.setBlock(POS,ImmersiveCraft.DEVICES.get(kind).get());
        return (DeviceEntity)h.getBlockEntity(POS);
    }
    private static void ticks(DeviceEntity b,int n) {
        for(int i=0;i<n;i++) DeviceEntity.tick(b.getLevel(),b.getBlockPos(),b.getBlockState(),b);
    }
    @GameTest(template="arena") public static void capacitiesAndStackLimits(GameTestHelper h) {
        for(Kind kind:new Kind[]{Kind.CHEST,Kind.CUPBOARD,Kind.SHELF,Kind.BOOKSHELF}) {
            DeviceEntity b=place(h,kind);
            h.assertTrue(b.items.getSlots()==kind.slots,"Capacity "+kind);
            if(kind!=Kind.BOOKSHELF) {
                h.assertTrue(b.items.insertItem(0,new ItemStack(Items.ENDER_PEARL,20),false).getCount()==4,"Pearl limit");
                h.assertTrue(b.items.insertItem(1,new ItemStack(Items.DIAMOND_AXE,2),false).getCount()==1,"Tool limit");
            }
        }
        h.succeed();
    }
    @GameTest(template="arena") public static void allDirectionsEverySelector(GameTestHelper h) {
        for(Kind kind:Kind.values()) for(Direction facing:Direction.Plane.HORIZONTAL) for(SlotLayout.Slot slot:SlotLayout.slots(kind)) {
            Vec3 c=slot.center();
            Vec3 a=slot.flat()?new Vec3(c.x,3,c.z):new Vec3(c.x,c.y,3);
            Vec3 b=slot.flat()?new Vec3(c.x,-1,c.z):new Vec3(c.x,c.y,-1);
            Vec3 aw=SlotLayout.toWorld(a,facing),bw=SlotLayout.toWorld(b,facing);
            h.assertTrue(SlotLayout.toLocal(aw,facing).distanceToSqr(a)<1e-12,"Inverse transform "+facing);
            h.assertTrue(SlotLayout.select(kind,facing,aw,bw)==slot.index(),"Ray selects rendered center: "+kind+" "+facing+" "+slot.index());
        }
        h.succeed();
    }
    @GameTest(template="arena") public static void furnaceRequiresIgnitionAndMakesOneOutput(GameTestHelper h) {
        DeviceEntity b=place(h,Kind.FURNACE);
        b.items.setStackInSlot(0,new ItemStack(Items.COAL,2));b.items.setStackInSlot(1,new ItemStack(Items.RAW_IRON,2));
        ticks(b,250);
        h.assertTrue(b.burnTime==0 && b.items.getStackInSlot(2).isEmpty(),"Fuel alone must not ignite");
        Player p=h.makeMockPlayer();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.FLINT_AND_STEEL));
        h.assertTrue(b.ignite(p,InteractionHand.MAIN_HAND),"Flint ignition accepted");
        h.assertTrue(b.items.getStackInSlot(0).getCount()==1,"One coal consumed on ignition");
        h.assertTrue(p.getMainHandItem().getDamageValue()==1,"Igniter damaged");
        ticks(b,201);
        h.assertTrue(b.items.getStackInSlot(2).is(Items.IRON_INGOT) && b.items.getStackInSlot(2).getCount()==1,"Exactly one output");
        h.assertTrue(b.items.getStackInSlot(1).getCount()==1,"Exactly one input consumed");
        h.succeed();
    }
    @GameTest(template="arena") public static void furnaceBlockedOutputDoesNotLoseInput(GameTestHelper h) {
        DeviceEntity b=place(h,Kind.FURNACE);
        b.items.setStackInSlot(1,new ItemStack(Items.RAW_IRON,2));b.items.setStackInSlot(2,new ItemStack(Items.GOLD_INGOT,64));b.burnTime=500;
        ticks(b,250);
        h.assertTrue(b.items.getStackInSlot(1).getCount()==2,"Mismatched output retains inputs");
        h.assertTrue(b.items.getStackInSlot(2).getCount()==64,"Existing output unchanged");h.succeed();
    }
    @GameTest(template="arena") public static void furnaceSaveResumeAndSidedAutomation(GameTestHelper h) {
        DeviceEntity b=place(h,Kind.FURNACE);
        b.items.setStackInSlot(1,new ItemStack(Items.RAW_IRON,2));b.burnTime=500;ticks(b,100);
        CompoundTag tag=b.saveWithoutMetadata();DeviceEntity restored=new DeviceEntity(b.getBlockPos(),b.getBlockState());restored.setLevel(h.getLevel());restored.load(tag);
        h.assertTrue(restored.burnTime==b.burnTime && restored.cookTime==b.cookTime,"Progress restored");ticks(restored,101);
        h.assertTrue(restored.items.getStackInSlot(2).getCount()==1 && restored.items.getStackInSlot(1).getCount()==1,"Resume produces exactly once");
        var top=b.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.UP).orElseThrow(IllegalStateException::new);
        var bottom=b.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.DOWN).orElseThrow(IllegalStateException::new);
        var side=b.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.EAST).orElseThrow(IllegalStateException::new);
        h.assertTrue(top.getSlots()==1 && side.getSlots()==1 && bottom.getSlots()==1,"Sided slots");
        h.assertTrue(bottom.insertItem(0,new ItemStack(Items.COAL),true).isEmpty(),"Legacy automation permits inserting into an empty output");
        h.assertTrue(side.insertItem(0,new ItemStack(Items.COAL),true).isEmpty(),"Fuel from side");h.succeed();
    }
    @GameTest(template="arena") public static void displayNbtAndTwoPlayersCannotTakeTwice(GameTestHelper h) {
        DeviceEntity b=place(h,Kind.DISPLAY);
        ItemStack tool=new ItemStack(Items.DIAMOND_AXE);tool.setDamageValue(47);tool.getOrCreateTag().putString("foreign_mod_key","preserved");
        b.items.setStackInSlot(0,tool.copy());
        CompoundTag tag=b.saveWithoutMetadata();b.items.setStackInSlot(0,ItemStack.EMPTY);b.load(tag);
        h.assertTrue(ItemStack.matches(tool,b.items.getStackInSlot(0)),"Durability and foreign NBT survive");
        Player a=h.makeMockPlayer(),c=h.makeMockPlayer();b.interact(a,InteractionHand.MAIN_HAND,0);
        // A second request must use the current world BE rather than the detached old reference.
        if(h.getLevel().getBlockEntity(b.getBlockPos()) instanceof DeviceEntity current) current.interact(c,InteractionHand.MAIN_HAND,0);
        h.assertTrue(a.getInventory().countItem(Items.DIAMOND_AXE)+c.getInventory().countItem(Items.DIAMOND_AXE)==1,"Single ownership");
        h.assertTrue(h.getBlockState(POS).isAir(),"Empty display removed");h.succeed();
    }
    @GameTest(template="arena") public static void stickPileLimitBurnAndSave(GameTestHelper h) {
        DeviceEntity b=place(h,Kind.STICKS);
        h.assertTrue(b.items.insertItem(0,new ItemStack(Items.STICK,70),false).getCount()==6,"64 stick limit");
        b.burnTime=600;ticks(b,599);
        h.assertTrue(b.items.getStackInSlot(0).getCount()==64,"No early consumption");
        CompoundTag tag=b.saveWithoutMetadata();b.load(tag);ticks(b,1);
        h.assertTrue(b.items.getStackInSlot(0).getCount()==63 && b.burnTime==600,"One stick per 600 ticks after save");
        b.items.setStackInSlot(0,new ItemStack(Items.STICK));b.burnTime=1;ticks(b,1);
        h.assertTrue(h.getBlockState(POS).isAir(),"Last burning stick removes pile");h.succeed();
    }
    @GameTest(template="arena") public static void cupboardSupportLossDropsExactlyOnce(GameTestHelper h) {
        DeviceEntity b=place(h,Kind.CUPBOARD);b.items.setStackInSlot(0,new ItemStack(Items.DIAMOND,7));
        h.setBlock(POS.relative(Direction.NORTH),Blocks.AIR);
        h.runAfterDelay(2,()->{
            h.assertTrue(h.getBlockState(POS).isAir(),"Unsupported cupboard removed");
            int count=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(b.getBlockPos()).inflate(2)).stream().filter(e->e.getItem().is(Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum();
            h.assertTrue(count==7,"Exactly 7 content items dropped, got "+count);h.succeed();
        });
    }
    @GameTest(template="arena") public static void workbenchAtomicRecipesAndHotbar(GameTestHelper h) {
        DeviceEntity input=place(h,Kind.WORKBENCH);
        BlockPos sec=POS.relative(Direction.EAST);
        // SOUTH.getCounterClockWise() is EAST; secondary's clockwise direction returns to primary.
        h.setBlock(sec,ImmersiveCraft.DEVICES.get(Kind.WORKBENCH_SEC).get());
        DeviceEntity b=(DeviceEntity)h.getBlockEntity(sec);Player p=h.makeMockPlayer();
        input.items.setStackInSlot(0,new ItemStack(Items.STICK,3));
        p.getInventory().setItem(1,new ItemStack(ImmersiveCraft.DEVICES.get(Kind.ROCK).get(),1));
        WorkbenchRecipes.craft(b,p);
        h.assertTrue(input.items.getStackInSlot(0).getCount()==3,"Missing ingredient leaves all inputs untouched");
        p.getInventory().setItem(1,new ItemStack(ImmersiveCraft.DEVICES.get(Kind.ROCK).get(),2));
        WorkbenchRecipes.craft(b,p);
        h.assertTrue(p.getInventory().countItem(ImmersiveCraft.SAW.get())==1 && input.empty(),"Base saw recipe, hotbar and workbench consumed");
        h.assertTrue(WorkbenchRecipes.recipes(b).length==5,"5 base schemas");
        b.items.setStackInSlot(0,new ItemStack(ImmersiveCraft.SAW.get()));h.assertTrue(WorkbenchRecipes.recipes(b).length==2,"2 saw schemas");
        b.items.setStackInSlot(0,new ItemStack(ImmersiveCraft.CHISEL.get()));h.assertTrue(WorkbenchRecipes.recipes(b).length==3,"3 chisel schemas");h.succeed();
    }
    @GameTest(template="arena") public static void configTerrainSelectorsAndLoadedRecipes(GameTestHelper h) {
        h.assertTrue(PortConfig.ground(Blocks.GRASS_BLOCK.defaultBlockState(),java.util.List.of("minecraft:grass_block[snowy=false]")),"Blockstate predicate");
        h.assertTrue(!PortConfig.ground(Blocks.WATER.defaultBlockState(),PortConfig.ROCK_BLOCKS.get()),"Water excluded");
        h.assertTrue(PortConfig.ground(Blocks.OAK_LOG.defaultBlockState(),java.util.List.of("#minecraft:logs")),"Tag predicate");
        for(String id:new String[]{"chest","cupboard","furnace","saw","chisel","shelf","bookshelf","rock"}) h.assertTrue(h.getLevel().getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("immcraft",id)).isPresent(),"Loaded recipe "+id);
        h.succeed();
    }
    @GameTest(template="arena",timeoutTicks=200) public static void diskCheckpointAcrossServerRuns(GameTestHelper h) {
        java.nio.file.Path marker=java.nio.file.Path.of("immcraft-persistence-phase.txt");
        Kind[] kinds={Kind.CHEST,Kind.CUPBOARD,Kind.FURNACE,Kind.DISPLAY,Kind.WALL_DISPLAY,Kind.STICKS,Kind.WORKBENCH,Kind.WORKBENCH_SEC};
        boolean verify=java.nio.file.Files.exists(marker);
        try {
            for(int i=0;i<kinds.length;i++) {
                Kind kind=kinds[i];BlockPos pos=new BlockPos(128+i*3,150,128);
                // Explicitly load the test fixture chunk. Production interactions never do this.
                h.getLevel().getChunkAt(pos);
                if(!verify) {
                    h.getLevel().setBlock(pos.below(),Blocks.STONE.defaultBlockState(),2);
                    h.getLevel().setBlock(pos.north(),Blocks.STONE.defaultBlockState(),2);
                    h.getLevel().setBlock(pos,ImmersiveCraft.DEVICES.get(kind).get().defaultBlockState(),2);
                    DeviceEntity be=(DeviceEntity)h.getLevel().getBlockEntity(pos);
                    ItemStack stack=new ItemStack(kind==Kind.STICKS?Items.STICK:kind==Kind.FURNACE?Items.COAL:Items.DIAMOND_AXE,kind==Kind.STICKS?64:1);
                    stack.getOrCreateTag().putString("port_test","restart-"+kind.id);
                    be.items.setStackInSlot(0,stack);be.open=true;be.schema=2;be.cookTime=73;be.changed();
                } else {
                    h.assertTrue(h.getLevel().getBlockEntity(pos) instanceof DeviceEntity,"BlockEntity persisted: "+kind);
                    DeviceEntity be=(DeviceEntity)h.getLevel().getBlockEntity(pos);
                    h.assertTrue(be.kind==kind,"Device kind persisted "+kind);
                    h.assertTrue(be.items.getStackInSlot(0).getOrCreateTag().getString("port_test").equals("restart-"+kind.id),"Foreign NBT persisted "+kind);
                    h.assertTrue(be.cookTime==73 && be.open,"Progress/open state persisted "+kind);
                    if(kind==Kind.STICKS) h.assertTrue(be.items.getStackInSlot(0).getCount()==64,"64 sticks persisted");
                }
            }
            h.getLevel().getServer().saveEverything(false,true,true);
            if(verify) {
                java.nio.file.Files.delete(marker);
                System.out.println("IMMCRAFT_PERSISTENCE: VERIFIED after independent server restart");
            } else {
                java.nio.file.Files.writeString(marker,"written");
                System.out.println("IMMCRAFT_PERSISTENCE: WRITTEN; a second server process must verify");
            }
            h.succeed();
        } catch(java.io.IOException e) { throw new RuntimeException(e); }
    }
    @GameTest(template="arena") public static void everyOriginalWorkbenchSchema(GameTestHelper h) {
        DeviceEntity input=place(h,Kind.WORKBENCH);
        BlockPos second=POS.relative(input.facing().getCounterClockWise());
        h.setBlock(second,ImmersiveCraft.DEVICES.get(Kind.WORKBENCH_SEC).get());
        DeviceEntity tool=(DeviceEntity)h.getBlockEntity(second);Player p=h.makeMockPlayer();
        for(int group=0;group<3;group++) {
            tool.items.setStackInSlot(0,group==0?ItemStack.EMPTY:new ItemStack(group==1?ImmersiveCraft.SAW.get():ImmersiveCraft.CHISEL.get()));
            WorkbenchRecipes.Recipe[] recipes=WorkbenchRecipes.recipes(tool);
            for(int n=0;n<recipes.length;n++) {
                p.getInventory().clearContent();
                for(int i=0;i<input.items.getSlots();i++) input.items.setStackInSlot(i,ItemStack.EMPTY);
                var recipe=recipes[n];
                for(int i=0;i<recipe.inputs().length;i++) input.items.setStackInSlot(i,recipe.inputs()[i].copy());
                tool.schema=n;ItemStack before=tool.items.getStackInSlot(0).copy();
                WorkbenchRecipes.craft(tool,p);
                h.assertTrue(p.getInventory().countItem(recipe.output().getItem())==recipe.output().getCount(),"Schema "+group+"/"+n+" output");
                h.assertTrue(input.empty(),"Schema consumed its ingredients");
                h.assertTrue(ItemStack.matches(before,tool.items.getStackInSlot(0)),"Original crafting does not wear the installed tool");
            }
        }
        h.succeed();
    }
    @GameTest(template="arena",timeoutTicks=200) public static void worldgenRocksAndSticksUnderLeaves(GameTestHelper h) {
        BlockPos base=new BlockPos(256,200,256);var level=h.getLevel();
        for(int x=0;x<16;x++) for(int z=0;z<16;z++) {
            BlockPos ground=base.offset(x,0,z);
            level.setBlock(ground,Blocks.DIRT.defaultBlockState(),2);
            level.setBlock(ground.above(),Blocks.AIR.defaultBlockState(),2);
            level.setBlock(ground.above(5),Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT,true),2);
        }
        for(int seed=0;seed<32;seed++) new RubbleFeature().place(new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(java.util.Optional.empty(),level,level.getChunkSource().getGenerator(),net.minecraft.util.RandomSource.create(seed),base,net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE));
        int rocks=0,sticks=0;
        for(int x=0;x<16;x++) for(int z=0;z<16;z++) {
            var state=level.getBlockState(base.offset(x,1,z));
            if(state.is(ImmersiveCraft.DEVICES.get(Kind.ROCK).get())) rocks++;
            if(state.is(ImmersiveCraft.DEVICES.get(Kind.STICKS).get())) {
                sticks++;DeviceEntity be=(DeviceEntity)level.getBlockEntity(base.offset(x,1,z));
                h.assertTrue(be.items.getStackInSlot(0).getCount()>=1 && be.items.getStackInSlot(0).getCount()<=6,"Natural piles contain 1-6 sticks");
            }
        }
        h.assertTrue(rocks>0 && sticks>0,"Actual feature produces both resources, rocks="+rocks+" sticks="+sticks);
        h.succeed();
    }
}
