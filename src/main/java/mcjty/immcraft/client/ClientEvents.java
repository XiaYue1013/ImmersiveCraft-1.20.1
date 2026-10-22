package mcjty.immcraft.client;

import mcjty.immcraft.*;
import net.minecraft.client.*;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid=ImmersiveCraft.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ClientEvents {
    public static final KeyMapping PLACE=new KeyMapping("key.placeitem",GLFW.GLFW_KEY_P,"key.categories.immcraft");
    public static final KeyMapping NEXT=new KeyMapping("key.nextitem",GLFW.GLFW_KEY_LEFT_BRACKET,"key.categories.immcraft");
    public static final KeyMapping PREVIOUS=new KeyMapping("key.previtem",GLFW.GLFW_KEY_RIGHT_BRACKET,"key.categories.immcraft");
    public static final KeyMapping DEBUG=new KeyMapping("key.debughandles",GLFW.GLFW_KEY_UNKNOWN,"key.categories.immcraft");
    public static boolean debug;
    public static void networkInfo(PortNetwork.NetworkInfoResponse p) {
        Minecraft mc=Minecraft.getInstance();if(mc.level==null || !mc.level.dimension().location().equals(p.dimension())) return;
        var network=mcjty.immcraft.multiblock.MultiBlockData.getNetwork(p.name());if(network==null) return;
        var info=network.getFactory().createClientInfo();var buffer=io.netty.buffer.Unpooled.wrappedBuffer(p.data());
        try { info.readFromBuf(buffer);network.registerClientInfo(p.id(),info); } finally { buffer.release(); }
    }
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e) { e.register(PLACE);e.register(NEXT);e.register(PREVIOUS);e.register(DEBUG); }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(ImmersiveCraft.DEVICE.get(),DeviceRenderer::new);
        e.registerBlockEntityRenderer(ImmersiveCraft.BUNDLE_ENTITY.get(),BundleRenderer::new);
        e.registerEntityRenderer(ImmersiveCraft.ROCK_ENTITY.get(),ThrownItemRenderer::new);
    }
    @SubscribeEvent public static void models(ModelEvent.RegisterAdditional e) {
        e.register(new ResourceLocation("immcraft","block/chestlid"));e.register(new ResourceLocation("immcraft","block/cupboardlid"));
        for(String s:new String[]{"sticks1","sticks2","sticks3","sticks"}) e.register(new ResourceLocation("immcraft","block/"+s));
        for(String s:new String[]{"bookstand","bookstand_withbook","bookstand_withbook_closed"}) e.register(new ResourceLocation("immcraft","block/"+s));
    }
    @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent e) { e.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager -> { BookClient.clear();DeviceRenderer.clearBookCache(); }); }
    @Mod.EventBusSubscriber(modid=ImmersiveCraft.ID,value=Dist.CLIENT)
    public static final class Input {
        @SubscribeEvent public static void attack(InputEvent.InteractionKeyMappingTriggered event) {
            Minecraft mc=Minecraft.getInstance();
            if(event.isAttack() && mc.player!=null && mc.level!=null && mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit && mc.level.getBlockEntity(hit.getBlockPos()) instanceof DeviceEntity be && !mc.player.getMainHandItem().isEmpty() && ImmersiveBlock.selected(be,mc.player)>=0) {
                PortNetwork.send(3);event.setCanceled(true);event.setSwingHand(false);
            }
        }
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
            Minecraft mc=Minecraft.getInstance();
            // Consume before vanilla handleKeybinds: its social screen also defaults to P.
            if(e.phase!=TickEvent.Phase.START || mc.player==null || mc.screen!=null) return;
            while(PLACE.consumeClick()) {
                if(PLACE.getKey().equals(mc.options.keySocialInteractions.getKey())) while(mc.options.keySocialInteractions.consumeClick()) { }
                PortNetwork.send(0);
            }
            while(NEXT.consumeClick()) PortNetwork.send(1);
            while(PREVIOUS.consumeClick()) PortNetwork.send(2);
            while(DEBUG.consumeClick()) debug=!debug;
        }
    }
}
