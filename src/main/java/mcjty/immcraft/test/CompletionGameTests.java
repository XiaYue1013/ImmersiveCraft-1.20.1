package mcjty.immcraft.test;
import mcjty.immcraft.*;
import mcjty.immcraft.books.*;
import mcjty.immcraft.blocks.bundle.BundleTE;
import mcjty.immcraft.cables.*;
import mcjty.immcraft.multiblock.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import java.util.*;
import java.io.StringReader;
@GameTestHolder("immcraft") @PrefixGameTestTemplate(false)
public final class CompletionGameTests {
    private static final BlockPos POS=new BlockPos(3,2,3);
    private static DeviceEntity device(GameTestHelper h,Kind kind) { h.setBlock(POS.below(),Blocks.STONE);h.setBlock(POS.north(),Blocks.STONE);h.setBlock(POS,ImmersiveCraft.DEVICES.get(kind).get());return (DeviceEntity)h.getBlockEntity(POS); }
    private static BundleTE bundle(GameTestHelper h,BlockPos p) { h.setBlock(p,ImmersiveCraft.BUNDLE.get());return (BundleTE)h.getBlockEntity(p); }
    @GameTest(template="arena") public static void bookMarkupAndBuiltinManual(GameTestHelper h) {
        var doc=BookDocument.builtin(new ResourceLocation("immcraft","text/manual.json"));
        h.assertTrue(doc.pages().size()>5,"Original manual paginated");
        h.assertTrue(doc.navigate(0,"TOC")>0 && doc.navigate(0,"Introduction")>0,"Original chapter links resolve");
        h.assertTrue(doc.pages().stream().flatMap(p->p.elements().stream()).anyMatch(e->e.type()==BookDocument.Type.IMAGE),"Original image markup parsed");
        h.assertTrue(doc.pages().stream().flatMap(p->p.elements().stream()).anyMatch(e->e.type()==BookDocument.Type.ITEM),"Original item markup parsed");
        var example=BookDocument.parse(new StringReader("[{\"section\":\"start\",\"text\":[\"#:1,red,bold,center:Heading\",\"#\",\"#l:end\",\"#-\",\"#i:minecraft:stick\",\"#I:immcraft:textures/gui/title.png:0,0,16,16,64,64\"]},{\"page\":true},{\"section\":\"end\",\"text\":[\"End\"]}]"));
        h.assertTrue(example.navigate(0,"end")>0 && example.navigate(10,"^")==0,"Link and home navigation");
        h.succeed();
    }
    @GameTest(template="arena") public static void bookStandInventoryPagesAndSave(GameTestHelper h) {
        DeviceEntity be=device(h,Kind.BOOKSTAND);var p=h.makeMockPlayer();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ImmersiveCraft.MANUAL.get()));
        h.assertTrue(be.interactBook(p,InteractionHand.MAIN_HAND) && p.getMainHandItem().isEmpty(),"Book moved into stand");
        be.navigateBook(">");h.assertTrue(be.bookPage==1,"Stand opens to next page");be.navigateBook("Introduction");
        int page=be.bookPage;CompoundTag tag=be.saveWithoutMetadata();be.bookPage=0;be.load(tag);
        h.assertTrue(be.bookPage==page && be.items.getStackInSlot(0).is(ImmersiveCraft.MANUAL.get()),"Book and chapter persist");
        p.setShiftKeyDown(true);be.interactBook(p,InteractionHand.MAIN_HAND);var other=h.makeMockPlayer();other.setShiftKeyDown(true);be.interactBook(other,InteractionHand.MAIN_HAND);
        h.assertTrue(p.getInventory().countItem(ImmersiveCraft.MANUAL.get())==1 && be.empty(),"Book removed exactly once");h.succeed();
    }
    @GameTest(template="arena") public static void bookPageRayAllDirections(GameTestHelper h) {
        Vec3 at=BookPlane.ORIGIN.add(BookPlane.RIGHT.scale(.6)).add(BookPlane.DOWN.scale(.6)),n=BookPlane.RIGHT.cross(BookPlane.DOWN).normalize();
        for(Direction facing:Direction.Plane.HORIZONTAL) {
            Vec3 hit=BookPlane.hit(facing,SlotLayout.toWorld(at.add(n.scale(2)),facing),SlotLayout.toWorld(at.subtract(n.scale(2)),facing));
            h.assertTrue(hit!=null && Math.abs(hit.x-768*.6)<.001 && Math.abs(hit.y-900*.6)<.001,"Exact book plane "+facing);
        }
        h.succeed();
    }
    @GameTest(template="arena") public static void leftClickAndSingleItemTransactions(GameTestHelper h) {
        DeviceEntity be=device(h,Kind.CHEST);be.open=true;var p=h.makeMockPlayer();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND,64));
        h.assertTrue(be.insertClick(p,0,1),"Single place");h.assertTrue(be.items.getStackInSlot(0).getCount()==1 && p.getMainHandItem().getCount()==63,"Exactly one moved");
        be.insertClick(p,0,64);h.assertTrue(p.getMainHandItem().isEmpty() && be.items.getStackInSlot(0).getCount()==64,"Full left-click insertion");
        be.extractOne(p,0);h.assertTrue(be.items.getStackInSlot(0).getCount()==63 && p.getInventory().countItem(Items.DIAMOND)==1,"Single extract");h.succeed();
    }
    @GameTest(template="arena") public static void furnaceFuelContinuationAndRemainder(GameTestHelper h) {
        DeviceEntity be=device(h,Kind.FURNACE);be.items.setStackInSlot(0,new ItemStack(Items.LAVA_BUCKET));
        var p=h.makeMockPlayer();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.FLINT_AND_STEEL));be.ignite(p,InteractionHand.MAIN_HAND);
        h.assertTrue(be.items.getStackInSlot(0).is(Items.BUCKET),"Lava bucket remainder preserved");
        be.items.setStackInSlot(0,new ItemStack(Items.COAL));be.burnTime=1;DeviceEntity.tick(be.getLevel(),be.getBlockPos(),be.getBlockState(),be);
        h.assertTrue(be.burnTime==1601 && be.items.getStackInSlot(0).isEmpty(),"Continuously burning furnace automatically consumes next coal");h.succeed();
    }
    @GameTest(template="arena") public static void ignitionConsumeSourcesAndExperiencePersistence(GameTestHelper h) {
        DeviceEntity be=device(h,Kind.FURNACE);var player=h.makeMockPlayer();
        var ignition=PortConfig.IGNITION.get();var consume=PortConfig.CONSUME.get();
        try {
            PortConfig.IGNITION.set(List.of("minecraft:flint_and_steel"));PortConfig.CONSUME.set(List.of("minecraft:torch"));
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.TORCH,2));be.items.setStackInSlot(0,new ItemStack(Items.COAL));
            h.assertTrue(be.ignite(player,InteractionHand.MAIN_HAND) && player.getMainHandItem().getCount()==1 && be.burnTime==1600,"Consume-only ignition sources light furnace and consume exactly one");
            be.items.setStackInSlot(2,new ItemStack(Items.IRON_INGOT,2));CompoundTag saved=be.saveWithoutMetadata();saved.putFloat("experience",5);be.load(saved);
            be.open=true;player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);be.extractOne(player,2);
            h.assertTrue(be.saveWithoutMetadata().getFloat("experience")==2.5f,"Only extracted output share spends saved experience");
            be.interact(player,InteractionHand.MAIN_HAND,2);
            h.assertTrue(be.saveWithoutMetadata().getFloat("experience")==0 && be.items.getStackInSlot(2).isEmpty(),"Remaining output consumes XP once");
        } finally { PortConfig.IGNITION.set(ignition);PortConfig.CONSUME.set(consume); }
        h.succeed();
    }
    @GameTest(template="arena") public static void furnaceRainExtinguishesAndTerrainMetadata(GameTestHelper h) {
        boolean old=PortConfig.RAIN.get();var level=h.getLevel();boolean raining=level.isRaining(),thunder=level.isThundering();float rainLevel=level.getRainLevel(1);
        BlockPos sky=new BlockPos(h.absolutePos(POS).getX(),level.getMaxBuildHeight()-2,h.absolutePos(POS).getZ());
        try {
            level.setBlockAndUpdate(sky,ImmersiveCraft.DEVICES.get(Kind.FURNACE).get().defaultBlockState());
            var be=(DeviceEntity)level.getBlockEntity(sky);be.burnTime=100;PortConfig.RAIN.set(true);level.setWeatherParameters(0,1200,true,false);level.setRainLevel(1);
            h.assertTrue(level.canSeeSky(sky),"Rain fixture sees sky");DeviceEntity.tick(level,sky,be.getBlockState(),be);
            h.assertTrue(be.burnTime==0,"Configured exposed furnace extinguished by rain");
            h.assertTrue(PortConfig.ground(Blocks.PODZOL.defaultBlockState(),List.of("minecraft:dirt@2")),"Legacy dirt metadata maps to podzol");
            h.assertTrue(PortConfig.ground(Blocks.GRANITE.defaultBlockState(),List.of("minecraft:stone@1")),"Legacy stone metadata maps to granite");
            h.assertTrue(!PortConfig.ground(Blocks.STONE.defaultBlockState(),List.of("minecraft:stone@99")),"Unsupported metadata fails closed");
        } finally { level.removeBlock(sky,false);PortConfig.RAIN.set(old);level.setWeatherParameters(6000,6000,raining,thunder);level.setRainLevel(rainLevel); }
        h.succeed();
    }
    @GameTest(template="arena") public static void cablePathMergeSplitAndPersistence(GameTestHelper h) {
        TestCableType.register();var type=TestCableType.INSTANCE;BlockPos a=POS.west(),b=POS,c=POS.east();
        BundleTE first=bundle(h,a),last=bundle(h,c);
        first.addCableToNetwork(type,type,Vec3.atCenterOf(first.getBlockPos()));last.addCableToNetwork(type,type,Vec3.atCenterOf(last.getBlockPos()));
        h.assertTrue(first.getCableSections().get(0).getId()!=last.getCableSections().get(0).getId(),"Separated cable networks");
        BundleTE middle=bundle(h,b);middle.addCableToNetwork(type,type,Vec3.atCenterOf(middle.getBlockPos()));
        int id=middle.getCableSections().get(0).getId();var network=TestCableType.network(h.getLevel());
        h.assertTrue(network.getMultiblock(id).getBlockCount()==3 && first.getCableSections().get(0).getId()==id && last.getCableSections().get(0).getId()==id,"Merge updates all IDs and ordered path");
        CompoundTag saved=middle.saveWithoutMetadata();middle.load(saved);h.assertTrue(middle.getCableSections().get(0).getId()==id,"Bundle section NBT");
        middle.removeCableFromNetwork(middle.getCableSections().get(0));
        int idA=first.getCableSections().get(0).getId(),idC=last.getCableSections().get(0).getId();
        h.assertTrue(idA!=idC && network.getMultiblock(idA).getBlockCount()==1 && network.getMultiblock(idC).getBlockCount()==1,"Removing middle splits into two networks");
        h.assertTrue(network.resolveId(id,first.getBlockPos())==idA && network.resolveId(id,last.getBlockPos())==idC,"Persistable relocation for unloaded chunks");
        CompoundTag state=new CompoundTag();network.writeToNBT(state);network.readFromNBT(state);h.assertTrue(network.getMultiblock(idA).getBlockCount()==1,"SavedData network serialization");h.succeed();
    }
    @GameTest(template="arena") public static void cableCallbacksAndUnavailableAddonPreservation(GameTestHelper h) {
        TestCableType.register();BundleTE be=bundle(h,POS);var type=TestCableType.INSTANCE;be.addCableToNetwork(type,type,Vec3.atCenterOf(be.getBlockPos()));
        be.update();h.assertTrue(be.getPersistentData().getInt("test_callback_ticks")>0,"Addon handler tick invoked");
        CompoundTag unknown=new CompoundTag();unknown.putInt("size",1);CompoundTag section=new CompoundTag();section.putString("type","missing:addon");section.putString("subtype","missing:subtype");section.putString("custom_nbt","keep");unknown.put("c0",section);
        be.load(unknown);CompoundTag saved=be.saveWithoutMetadata();h.assertTrue(saved.getCompound("c0").getString("custom_nbt").equals("keep"),"Absent addon data preserved without crash or loss");h.succeed();
    }
    @GameTest(template="arena") public static void workbenchCreationAllDirections(GameTestHelper h) {
        boolean old=PortConfig.WORKBENCH.get();PortConfig.WORKBENCH.set(true);
        try {
            for(Direction d:Direction.Plane.HORIZONTAL) {
                for(Direction clear:Direction.Plane.HORIZONTAL) h.setBlock(POS.relative(clear),Blocks.AIR);
                h.setBlock(POS,Blocks.OAK_LOG);h.setBlock(POS.relative(d),Blocks.OAK_LOG);
                var player=h.makeMockPlayer();player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STONE_AXE));
                var event=new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player,InteractionHand.MAIN_HAND,h.absolutePos(POS),new BlockHitResult(Vec3.atCenterOf(h.absolutePos(POS)),Direction.UP,h.absolutePos(POS),false));
                WorldInteractions.rightClick(event);
                h.assertTrue(event.isCanceled() && h.getBlockState(POS).is(ImmersiveCraft.DEVICES.get(Kind.WORKBENCH).get()) && h.getBlockState(POS.relative(d)).is(ImmersiveCraft.DEVICES.get(Kind.WORKBENCH_SEC).get()),"Two-log formation "+d);
            }
        } finally { PortConfig.WORKBENCH.set(old); }
        h.succeed();
    }
    @GameTest(template="arena") public static void earlyRockFireAxeAndProjectile(GameTestHelper h) {
        var player=h.makeMockPlayer();var level=h.getLevel();var absolute=h.absolutePos(POS);
        boolean flint=PortConfig.FLINT.get(),axe=PortConfig.AXE.get();double chance=PortConfig.FIRE_CHANCE.get();
        try {
            PortConfig.FLINT.set(true);PortConfig.AXE.set(true);PortConfig.FIRE_CHANCE.set(1.0);
            device(h,Kind.ROCK);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.FLINT));
            var hit=new BlockHitResult(Vec3.atCenterOf(absolute),Direction.UP,absolute,false);
            level.getBlockState(absolute).use(level,player,InteractionHand.MAIN_HAND,hit);
            h.assertTrue(level.isEmptyBlock(absolute) && level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(absolute).inflate(2)).stream().mapToInt(e->e.getItem().is(Items.FLINT_AND_STEEL)?e.getItem().getCount():0).sum()==1,"Flint on rock drops exactly one flint and steel");
            device(h,Kind.ROCK);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
            WorldInteractions.rightClick(new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player,InteractionHand.MAIN_HAND,absolute,hit));
            var pile=(DeviceEntity)level.getBlockEntity(absolute);
            h.assertTrue(pile.kind==Kind.STICKS && pile.burnTime==600 && pile.items.getStackInSlot(0).getCount()==1,"Configured successful friction lights exactly one stick");
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ImmersiveCraft.DEVICES.get(Kind.ROCK).get()));
            WorldInteractions.rightClick(new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player,InteractionHand.MAIN_HAND,absolute,hit));
            h.assertTrue(level.isEmptyBlock(absolute) && level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(absolute).inflate(2)).stream().mapToInt(e->e.getItem().is(Items.STONE_AXE)?e.getItem().getCount():0).sum()==1,"Optional rock and single stick drops exactly one axe");
            player.getAbilities().instabuild=false;player.setPos(absolute.getX()+.5,absolute.getY()+2,absolute.getZ()+.5);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ImmersiveCraft.DEVICES.get(Kind.ROCK).get(),2));
            player.getMainHandItem().getItem().use(level,player,InteractionHand.MAIN_HAND);
            h.assertTrue(player.getMainHandItem().getCount()==1 && !level.getEntitiesOfClass(RockProjectile.class,new AABB(absolute).inflate(5)).isEmpty(),"Throwing creates native rock projectile and consumes one rock");
        } finally { PortConfig.FLINT.set(flint);PortConfig.AXE.set(axe);PortConfig.FIRE_CHANCE.set(chance); }
        h.succeed();
    }
}
