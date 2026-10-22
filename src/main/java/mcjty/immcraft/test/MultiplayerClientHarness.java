package mcjty.immcraft.test;
import mcjty.immcraft.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="immcraft",value=Dist.CLIENT)
public final class MultiplayerClientHarness {
    private static final boolean ENABLED=Boolean.getBoolean("immcraft.mpClient");
    private static boolean connecting,joined,took,placed,reported;private static int ticks,finished,aimTicks,placeTicks;
    @SubscribeEvent public static void screen(ScreenEvent.Init.Post e) {
        if(!ENABLED) return;var mc=Minecraft.getInstance();
        if(e.getScreen() instanceof AccessibilityOnboardingScreen) { mc.options.onboardAccessibility=false;mc.options.save();mc.setScreen(new TitleScreen());return; }
        if(e.getScreen() instanceof DisconnectedScreen && connecting) { System.out.println("IMMCRAFT_MP_CLIENT: disconnected after server test");mc.stop();return; }
        if(connecting || !(e.getScreen() instanceof TitleScreen)) return;
        connecting=true;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);
        ConnectScreen.startConnecting(e.getScreen(),mc,new ServerAddress("127.0.0.1",25565),new ServerData("Immersive Craft isolated test","127.0.0.1:25565",false),false);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if(!ENABLED || e.phase!=TickEvent.Phase.END) return;var mc=Minecraft.getInstance();if(mc.player==null || mc.level==null) return;
        joined=true;ticks++;
        if(mc.level.hasChunkAt(MultiplayerServerHarness.CHEST) && mc.level.getBlockEntity(MultiplayerServerHarness.CHEST) instanceof DeviceEntity chest) {
            String stage=chest.getPersistentData().getString("mp_stage");
            if(stage.equals("take") && !took && ticks>20) {
                var target=new net.minecraft.world.phys.Vec3(MultiplayerServerHarness.CHEST.getX()+.2,MultiplayerServerHarness.CHEST.getY()+.95,MultiplayerServerHarness.CHEST.getZ()+.25);
                var delta=target.subtract(mc.player.getEyePosition());
                mc.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                mc.player.setYRot((float)Math.toDegrees(Math.atan2(delta.z,delta.x))-90);mc.player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z))));
                if(++aimTicks>=10) { PortNetwork.send(5);took=true;System.out.println("IMMCRAFT_MP_CLIENT: "+mc.player.getName().getString()+" sent competing extraction request"); }
            }
            if(chest.items.getStackInSlot(1).getCount()==8 && !reported) { reported=true;System.out.println("IMMCRAFT_MP_CLIENT_PASS: live shared chest update received, diamond count=8"); }
            if(stage.equals("persisted") && (took || placed)) {
                if(finished++==40) { System.out.println("IMMCRAFT_MP_CLIENT_PASS: saved shared state received; disconnecting cleanly");mc.level.disconnect();mc.clearLevel(new TitleScreen());mc.stop(); }
            }
        }
        if(mc.player==null || mc.level==null) return;
        if(took && mc.player.getMainHandItem().getOrCreateTag().getString("mp_stage").equals("place") && !placed && Math.abs(mc.player.getX()-MultiplayerServerHarness.DISPLAY.getX()-.5)<.25 && Math.abs(mc.player.getZ()-MultiplayerServerHarness.DISPLAY.getZ()-.5)<.25) {
            mc.player.setYRot(0);mc.player.setXRot(90);
            if(++placeTicks==10) { KeyMapping.click(mcjty.immcraft.client.ClientEvents.PLACE.getKey());placed=true;System.out.println("IMMCRAFT_MP_CLIENT: real P mapping submitted remote placement"); }
        }
    }
}
