package mcjty.immcraft.test;
import mcjty.immcraft.*;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
/** Two real external clients; all fixture mutations run on the dedicated server thread. */
@Mod.EventBusSubscriber(modid="immcraft")
public final class MultiplayerServerHarness {
    public static final BlockPos CHEST=new BlockPos(300,150,300),DISPLAY=new BlockPos(303,150,300);
    private static final boolean ENABLED=Boolean.getBoolean("immcraft.mpServer");
    private static final Path MARKER=Path.of("multiplayer-restart.txt");
    private static int tick,stage,stageTick;private static boolean restart;
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if(!ENABLED) return;
        tick=stage=stageTick=0;restart=Files.exists(MARKER);
        if(!restart) {
            var level=event.getServer().overworld();level.getChunkAt(CHEST);
            if(level.getBlockEntity(CHEST) instanceof DeviceEntity old) {
                old.getPersistentData().putString("mp_stage","waiting");old.changed();
            }
        }
        if(restart) {
            var level=event.getServer().overworld();level.getChunkAt(CHEST);level.getChunkAt(DISPLAY);
            verifySaved(event.getServer());System.out.println("IMMCRAFT_MP: restart disk state verified; waiting for Alice and Bob reconnect");
        }
    }
    private static void require(boolean value,String message) { if(!value) throw new IllegalStateException("IMMCRAFT_MP_FAIL: "+message);System.out.println("IMMCRAFT_MP_PASS: "+message); }
    private static void verifySaved(net.minecraft.server.MinecraftServer server) {
        var level=server.overworld();
        require(level.getBlockEntity(CHEST) instanceof DeviceEntity,"Chest exists after save/reload");
        var chest=(DeviceEntity)level.getBlockEntity(CHEST);
        require(chest.items.getStackInSlot(0).isEmpty() && chest.items.getStackInSlot(1).is(Items.DIAMOND) && chest.items.getStackInSlot(1).getCount()==8,"Chest exact contents after save/reload");
        require(level.getBlockEntity(DISPLAY) instanceof DeviceEntity,"Placed display exists after save/reload");
        var display=(DeviceEntity)level.getBlockEntity(DISPLAY);
        require(display.items.getStackInSlot(0).is(Items.IRON_PICKAXE) && display.items.getStackInSlot(0).getDamageValue()==37 && display.items.getStackInSlot(0).getOrCreateTag().getString("mp_nbt").equals("keep"),"Remote P tool durability and NBT after save/reload");
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if(!ENABLED || event.phase!=TickEvent.Phase.END) return;
        var server=event.getServer();var level=server.overworld();tick++;
        var alice=server.getPlayerList().getPlayerByName("Alice");var bob=server.getPlayerList().getPlayerByName("Bob");
        try {
            if(stage==0 && alice!=null && bob!=null) {
                if(restart) {
                    verifySaved(server);require(alice.getInventory().countItem(Items.DIAMOND_AXE)+bob.getInventory().countItem(Items.DIAMOND_AXE)==1,"Both player inventories survived disconnect and server restart without duplicate");
                    stage=10;stageTick=tick;return;
                }
                level.setBlockAndUpdate(CHEST.below(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(CHEST,ImmersiveCraft.DEVICES.get(Kind.CHEST).get().defaultBlockState());
                level.removeBlock(DISPLAY,false);
                var chest=(DeviceEntity)level.getBlockEntity(CHEST);chest.open=true;chest.items.setStackInSlot(0,new ItemStack(Items.DIAMOND_AXE));chest.items.setStackInSlot(1,new ItemStack(Items.DIAMOND,16));chest.getPersistentData().putString("mp_stage","take");chest.changed();
                for(var p:java.util.List.of(alice,bob)) {
                    p.getInventory().clearContent();p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();
                    double x=CHEST.getX()+(p==alice?-1:1.5),targetX=CHEST.getX()+.2;
                    float pitch=(float)Math.toDegrees(Math.atan2(2.67,Math.abs(targetX-x)));
                    p.getPersistentData().putBoolean("mp_take_requested",false);p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                    p.teleportTo(level,x,CHEST.getY()+2,CHEST.getZ()+.25,p==alice?-90:90,pitch);p.inventoryMenu.broadcastChanges();
                }
                System.out.println("IMMCRAFT_MP: both real clients ready, competing for one axe");stage=1;stageTick=tick;
            }
            if(stage==1 && tick-stageTick>=80 && alice!=null && bob!=null && alice.getPersistentData().getBoolean("mp_take_requested") && bob.getPersistentData().getBoolean("mp_take_requested")) {
                require(alice!=null && bob!=null,"Both clients connected during concurrent extraction");
                System.out.println("IMMCRAFT_MP_COUNTS: Alice="+alice.getInventory().countItem(Items.DIAMOND_AXE)+" Bob="+bob.getInventory().countItem(Items.DIAMOND_AXE)+" chest="+((DeviceEntity)level.getBlockEntity(CHEST)).items.getStackInSlot(0));
                require(alice.getInventory().countItem(Items.DIAMOND_AXE)+bob.getInventory().countItem(Items.DIAMOND_AXE)==1,"Two remote C2S requests transfer exactly one axe");
                var chest=(DeviceEntity)level.getBlockEntity(CHEST);require(chest.items.getStackInSlot(0).isEmpty(),"Shared chest slot empty after extraction");
                chest.items.setStackInSlot(1,new ItemStack(Items.DIAMOND,8));chest.getPersistentData().putString("mp_stage","place");chest.changed();
                level.setBlockAndUpdate(DISPLAY.below(),Blocks.STONE.defaultBlockState());
                ItemStack tool=new ItemStack(Items.IRON_PICKAXE);tool.setDamageValue(37);tool.getOrCreateTag().putString("mp_stage","place");tool.getOrCreateTag().putString("mp_nbt","keep");
                alice.getInventory().selected=8;alice.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,tool);alice.inventoryMenu.broadcastChanges();alice.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(8));
                alice.teleportTo(level,DISPLAY.getX()+.5,DISPLAY.getY()+2,DISPLAY.getZ()+.5,0,90);
                stage=2;stageTick=tick;
            }
            if(stage==2 && tick-stageTick>=100 && level.getBlockEntity(DISPLAY) instanceof DeviceEntity placed && placed.kind==Kind.DISPLAY) {
                verifySaved(server);var chest=(DeviceEntity)level.getBlockEntity(CHEST);chest.getPersistentData().putString("mp_stage","persisted");chest.changed();
                server.saveEverything(false,true,true);Files.writeString(MARKER,"written");System.out.println("IMMCRAFT_MP: saved; waiting for both clients to disconnect and chunk unload");stage=3;stageTick=tick;
            }
            if(stage==3 && server.getPlayerCount()==0 && !level.hasChunkAt(CHEST) && !level.hasChunkAt(DISPLAY)) {
                System.out.println("IMMCRAFT_MP_PASS: player tickets removed and fixture chunks genuinely unloaded");level.getChunkAt(CHEST);level.getChunkAt(DISPLAY);verifySaved(server);
                server.saveEverything(false,true,true);System.out.println("IMMCRAFT_MP: first process complete; graceful shutdown");server.halt(false);stage=99;
            }
            if(stage==10 && tick-stageTick>=80) {
                require(alice!=null && bob!=null,"Both real clients reconnected to restarted server");
                Files.writeString(MARKER,"verified");server.saveEverything(false,true,true);System.out.println("IMMCRAFT_MP: ALL REMOTE MULTIPLAYER AND RESTART CHECKS PASSED");server.halt(false);stage=99;
            }
            if(tick>3600 && stage!=99) { System.out.println("IMMCRAFT_MP_FAIL: timeout in stage "+stage);server.halt(false);stage=99; }
        } catch(Exception error) { error.printStackTrace();server.halt(false);stage=99; }
    }
}
