package mcjty.immcraft.test;

import mcjty.immcraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.*;

/** Development-only scene and resource reload smoke check; excluded from the binary JAR. */
@Mod.EventBusSubscriber(modid="immcraft",value=Dist.CLIENT)
public final class VisualHarness {
    private static final Logger LOG=LogManager.getLogger();
    private static final boolean ENABLED=Boolean.getBoolean("immcraft.clientSmokeTest");
    private static final boolean COMPLETION=Boolean.getBoolean("immcraft.completionSmokeTest");
    private static boolean started,placed,reloaded,finalShot;
    private static int ticks;
    private static volatile BlockPos keyTarget;
    private static volatile net.minecraft.world.phys.Vec3 bookLinkTarget;
    @SubscribeEvent public static void screen(ScreenEvent.Init.Post event) {
        if(!ENABLED || started || !(event.getScreen() instanceof TitleScreen)) return;
        started=true;Minecraft mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
        if(COMPLETION) TestCableType.register();
        mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        GameRules rules=new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);
        String world="immcraft_visual_"+System.currentTimeMillis();
        LevelSettings settings=new LevelSettings(world,GameType.CREATIVE,false,Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(world,settings,new WorldOptions(42L,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if(!ENABLED || e.phase!=TickEvent.Phase.END) return;
        Minecraft mc=Minecraft.getInstance();if(mc.level==null || mc.player==null || mc.getSingleplayerServer()==null) return;
        if(!placed) {
            placed=true;mc.setScreen(null);
            mc.getSingleplayerServer().execute(()->{
                var level=mc.getSingleplayerServer().overworld();
                int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,0,0);
                Kind[] kinds={Kind.CHEST,Kind.CUPBOARD,Kind.FURNACE,Kind.DISPLAY,Kind.WALL_DISPLAY,Kind.STICKS,Kind.SHELF,Kind.BOOKSHELF};
                for(int i=0;i<kinds.length;i++) {
                    Kind kind=kinds[i];BlockPos pos=new BlockPos((i%4)*2,y,-(i/4)*4);
                    level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(pos.north(),Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(pos,ImmersiveCraft.DEVICES.get(kind).get().defaultBlockState());
                    if(level.getBlockEntity(pos) instanceof DeviceEntity be) {
                        if(kind==Kind.STICKS) { be.items.setStackInSlot(0,new ItemStack(Items.STICK,64));be.burnTime=600; }
                        else {
                            if(be.items.getSlots()>0) be.items.setStackInSlot(0,new ItemStack(kind==Kind.FURNACE?Items.COAL:kind==Kind.BOOKSHELF?Items.ENCHANTED_BOOK:Items.DIAMOND_AXE));
                            if(be.items.getSlots()>1) be.items.setStackInSlot(1,new ItemStack(kind==Kind.FURNACE?Items.RAW_IRON:Items.DIAMOND,16));
                            if(kind==Kind.FURNACE) be.burnTime=800;
                        }
                        be.open=true;be.changed();
                    }
                }
                var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                player.getAbilities().flying=true;player.onUpdateAbilities();
                player.teleportTo(level,3.5,y+3,9,180,12);
                LOG.info("IMMCRAFT_VISUAL: scene placed at y={}",y);
            });
        }
        ticks++;
        if(ticks==40) { mc.player.setYRot(180);mc.player.setXRot(12);mc.options.hideGui=true; }
        if(ticks==120) {
            for(String name:new String[]{"chestlid","cupboardlid","sticks1","sticks2","sticks3","sticks"}) {
                var model=mc.getModelManager().getModel(new net.minecraft.resources.ResourceLocation("immcraft","block/"+name));
                LOG.info("IMMCRAFT_VISUAL: model {} missing={}",name,model==mc.getModelManager().getMissingModel());
            }
            Screenshot.grab(mc.gameDirectory,"immcraft-before-reload.png",mc.getMainRenderTarget(),c->LOG.info(c.getString()));
            mc.reloadResourcePacks().thenRun(()->{reloaded=true;LOG.info("IMMCRAFT_VISUAL: resource reload completed");});
        }
        if(ticks>220 && reloaded && !finalShot) {
            finalShot=true;
            Screenshot.grab(mc.gameDirectory,"immcraft-after-reload.png",mc.getMainRenderTarget(),c->LOG.info(c.getString()));
        }
        if(ticks==230) mc.getSingleplayerServer().execute(()->{
            var server=mc.getSingleplayerServer();var level=server.overworld();int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,12,4);
            level.setBlockAndUpdate(new BlockPos(12,y-1,4),Blocks.STONE.defaultBlockState());
            keyTarget=new BlockPos(12,y,4);
            var player=server.getPlayerList().getPlayers().get(0);
            player.getInventory().selected=0;
            ItemStack stack=new ItemStack(ImmersiveCraft.CHISEL.get());stack.getOrCreateTag().putString("key_test","preserved");
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);player.inventoryMenu.broadcastChanges();
            player.teleportTo(level,12.5,y+2,4.5,0,90);
        });
        if(ticks==250) { mc.player.setYRot(0);mc.player.setXRot(90); }
        if(ticks==255) net.minecraft.client.KeyMapping.click(mcjty.immcraft.client.ClientEvents.PLACE.getKey());
        if(ticks==280) mc.getSingleplayerServer().execute(()->{
            var level=mc.getSingleplayerServer().overworld();
            BlockPos pos=keyTarget;
            boolean ok=level.getBlockEntity(pos) instanceof DeviceEntity be && be.kind==Kind.DISPLAY && be.items.getStackInSlot(0).is(ImmersiveCraft.CHISEL.get()) && be.items.getStackInSlot(0).getOrCreateTag().getString("key_test").equals("preserved");
            LOG.info("IMMCRAFT_KEY_TEST: P binding -> C2S -> server ray -> stored tool = {}",ok);
        });
        if(ticks==300 && COMPLETION) mc.getSingleplayerServer().execute(()->{
            var level=mc.getSingleplayerServer().overworld();var pos=keyTarget;
            level.setBlockAndUpdate(pos,ImmersiveCraft.DEVICES.get(Kind.BOOKSTAND).get().defaultBlockState());
            var stand=(DeviceEntity)level.getBlockEntity(pos);stand.items.setStackInSlot(0,new ItemStack(ImmersiveCraft.MANUAL.get()));stand.bookPage=1;stand.changed();
            for(int i=0;i<3;i++) {
                var cablePos=pos.offset(2+i,0,0);level.setBlockAndUpdate(cablePos,ImmersiveCraft.BUNDLE.get().defaultBlockState());
                var be=(mcjty.immcraft.blocks.bundle.BundleTE)level.getBlockEntity(cablePos);be.addCableToNetwork(TestCableType.INSTANCE,TestCableType.INSTANCE,net.minecraft.world.phys.Vec3.atCenterOf(cablePos));
            }
            var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);player.teleportTo(level,pos.getX()+.5,pos.getY()+.2,pos.getZ()+2.5,180,30);
            LOG.info("IMMCRAFT_BOOK_UI: stand and three-segment cable scene ready");
        });
        if(ticks==325 && COMPLETION) { mc.player.setYRot(180);mc.player.setXRot(30); }
        if(ticks==350 && COMPLETION) {
            if(mc.level.getBlockEntity(keyTarget) instanceof DeviceEntity stand) LOG.info("IMMCRAFT_BOOK_UI: client stand page={} item={} elements={}",stand.bookPage,stand.items.getStackInSlot(0),mcjty.immcraft.client.BookClient.document((mcjty.immcraft.api.book.IBook)ImmersiveCraft.MANUAL.get()).pages().get(stand.bookPage).elements().size());
            for(int i=0;i<3;i++) if(mc.level.getBlockEntity(keyTarget.offset(2+i,0,0)) instanceof mcjty.immcraft.blocks.bundle.BundleTE cable) LOG.info("IMMCRAFT_BOOK_UI: client cable {} sections={} nbt={}",i,cable.getCableSections().size(),cable.saveWithoutMetadata());
            Screenshot.grab(mc.gameDirectory,"immcraft-bookstand-cables.png",mc.getMainRenderTarget(),c->LOG.info(c.getString()));
        }
        if(ticks==351 && COMPLETION) mc.getSingleplayerServer().execute(()->{
            var server=mc.getSingleplayerServer();var p=keyTarget;
            server.getPlayerList().getPlayers().get(0).teleportTo(server.overworld(),p.getX()+3.5,p.getY()+.2,p.getZ()+3.5,180,24);
        });
        if(ticks==355 && COMPLETION) { mc.player.setYRot(180);mc.player.setXRot(24); }
        if(ticks==359 && COMPLETION) Screenshot.grab(mc.gameDirectory,"immcraft-cable-network.png",mc.getMainRenderTarget(),c->LOG.info(c.getString()));
        if(ticks==360 && COMPLETION) mcjty.immcraft.client.BookClient.open(new ItemStack(ImmersiveCraft.MANUAL.get()));
        if(ticks==380 && COMPLETION) {
            Screenshot.grab(mc.gameDirectory,"immcraft-manual-cover.png",mc.getMainRenderTarget(),c->LOG.info(c.getString()));
            if(mc.screen instanceof mcjty.immcraft.client.BookClient.ManualScreen screen) screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT,0,0);
        }
        if(ticks==400 && COMPLETION) Screenshot.grab(mc.gameDirectory,"immcraft-manual-toc.png",mc.getMainRenderTarget(),c->LOG.info(c.getString()));
        if(ticks==405 && COMPLETION && mc.screen instanceof mcjty.immcraft.client.BookClient.ManualScreen screen) {
            var doc=mcjty.immcraft.books.BookDocument.builtin(new net.minecraft.resources.ResourceLocation("immcraft","text/manual.json"));
            for(var element:doc.pages().get(screen.page).elements()) if("Introduction".equals(element.link())) {
                screen.mouseClicked((screen.width-200)/2+20+(element.x()+element.width()/2)*.2,(screen.height-220)/2+25+(element.y()+element.height()/2)*.2,0);
                LOG.info("IMMCRAFT_BOOK_UI: actual GUI chapter click reached Introduction = {}",screen.page==doc.navigate(0,"Introduction"));break;
            }
        }
        if(ticks==425 && COMPLETION) Screenshot.grab(mc.gameDirectory,"immcraft-manual-chapter.png",mc.getMainRenderTarget(),c->LOG.info(c.getString()));
        if(ticks==440 && COMPLETION) {
            mc.setScreen(null);mc.getSingleplayerServer().execute(()->{
                var server=mc.getSingleplayerServer();var p=keyTarget;
                var doc=mcjty.immcraft.books.BookDocument.builtin(new net.minecraft.resources.ResourceLocation("immcraft","text/manual.json"));
                for(var element:doc.pages().get(1).elements()) if("Introduction".equals(element.link())) {
                    bookLinkTarget=net.minecraft.world.phys.Vec3.atLowerCornerOf(p).add(mcjty.immcraft.books.BookPlane.ORIGIN).add(mcjty.immcraft.books.BookPlane.RIGHT.scale((element.x()+element.width()/2.0)/768)).add(mcjty.immcraft.books.BookPlane.DOWN.scale((element.y()+element.height()/2.0)/900));break;
                }
                server.getPlayerList().getPlayers().get(0).teleportTo(server.overworld(),p.getX()+.5,p.getY()+.2,p.getZ()+2.5,180,30);
            });
        }
        if(ticks>=465 && ticks<=490 && COMPLETION && bookLinkTarget!=null) {
            var d=bookLinkTarget.subtract(mc.player.getEyePosition());
            mc.player.setYRot((float)Math.toDegrees(Math.atan2(d.z,d.x))-90);mc.player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))));
        }
        if(ticks==490 && COMPLETION && bookLinkTarget!=null) mc.gameMode.useItemOn(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(bookLinkTarget,net.minecraft.core.Direction.SOUTH,keyTarget,false));
        if(ticks==515 && COMPLETION) mc.getSingleplayerServer().execute(()->{
            var stand=(DeviceEntity)mc.getSingleplayerServer().overworld().getBlockEntity(keyTarget);
            var doc=mcjty.immcraft.books.BookDocument.builtin(new net.minecraft.resources.ResourceLocation("immcraft","text/manual.json"));
            LOG.info("IMMCRAFT_BOOK_WORLD: native right-click packet selected Introduction = {}",stand.bookPage==doc.navigate(0,"Introduction"));
        });
        if(ticks==520 && COMPLETION) net.minecraft.client.KeyMapping.click(mcjty.immcraft.client.ClientEvents.NEXT.getKey());
        if(ticks==545 && COMPLETION) mc.getSingleplayerServer().execute(()->{
            var stand=(DeviceEntity)mc.getSingleplayerServer().overworld().getBlockEntity(keyTarget);
            var doc=mcjty.immcraft.books.BookDocument.builtin(new net.minecraft.resources.ResourceLocation("immcraft","text/manual.json"));
            LOG.info("IMMCRAFT_BOOK_WORLD: bracket mapping advanced page = {}",stand.bookPage==doc.navigate(0,"Introduction")+1);
        });
        if((ticks==300 && !COMPLETION || ticks==560 && COMPLETION) && reloaded) {
            LOG.info("IMMCRAFT_VISUAL: client smoke completed");mc.stop();
        }
    }
}
