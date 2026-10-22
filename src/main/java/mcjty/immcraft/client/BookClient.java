package mcjty.immcraft.client;

import mcjty.immcraft.*;
import mcjty.immcraft.api.book.IBook;
import mcjty.immcraft.books.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class BookClient {
    private static final Map<ResourceLocation,BookDocument> CACHE=new HashMap<>();
    public static void clear() { CACHE.clear(); }
    public static BookDocument document(IBook book) {
        return CACHE.computeIfAbsent(book.getJson(),id->{
            try(var in=Minecraft.getInstance().getResourceManager().open(id)) { return BookDocument.parse(new InputStreamReader(in,StandardCharsets.UTF_8)); }
            catch(IOException | RuntimeException e) { return BookDocument.parse(new StringReader("[{\"text\":[\"Cannot load book resource\"]}]")); }
        });
    }
    public static void open(ItemStack stack) { if(stack.getItem() instanceof IBook book) Minecraft.getInstance().setScreen(new ManualScreen(book)); }
    public static void sound() {
        if(PortConfig.PAGE_VOLUME.get()>0) Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(ImmersiveCraft.PAGE_TURN.get(),1,PortConfig.PAGE_VOLUME.get().floatValue()));
    }
    public static void renderStand(DeviceEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        Minecraft mc=Minecraft.getInstance();
        String model=be.items.getStackInSlot(0).isEmpty()?"bookstand":be.bookPage==0?"bookstand_withbook_closed":"bookstand_withbook";
        var baked=mc.getModelManager().getModel(new ResourceLocation("immcraft","block/"+model));
        pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(180));pose.translate(-.5,0,-.5);
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(),buffers.getBuffer(RenderType.cutout()),be.getBlockState(),baked,1,1,1,light,OverlayTexture.NO_OVERLAY);
        pose.popPose();
        if(!(be.items.getStackInSlot(0).getItem() instanceof IBook book)) return;
        BookDocument doc=document(book);
        Vec3 pointer=null;
        if(mc.player!=null) {
            Vec3 start=mc.player.getEyePosition(partial).subtract(Vec3.atLowerCornerOf(be.getBlockPos()));
            pointer=BookPlane.hit(be.facing(),start,start.add(mc.player.getLookAngle().scale(PortNetwork.reach(mc.player))));
        }
        pose.pushPose();
        Matrix4f matrix=new Matrix4f().m00(.68f/768).m11(-.68f/900).m12(.41f/900).m22(.001f).m30(.16f).m31(.811f).m32(.451f);
        pose.mulPoseMatrix(matrix);
        paint(doc,be.bookPage,pose,buffers,light,pointer==null?-1:pointer.x,pointer==null?-1:pointer.y);
        pose.popPose();
    }
    public static void paint(BookDocument doc,int page,PoseStack pose,MultiBufferSource buffers,int light,double mx,double my) {
        Minecraft mc=Minecraft.getInstance();
        for(BookDocument.Element e:doc.pages().get(doc.clamp(page)).elements()) {
            pose.pushPose();pose.translate(e.x(),e.y(),0);
            switch(e.type()) {
                case TEXT -> {
                    pose.scale(e.format().scale(),e.format().scale(),1);
                    int color=e.format().color() | 0xff000000;
                    if(e.link()!=null && e.contains(mx,my)) color=0xffcc3333;
                    mc.font.drawInBatch(e.format().component(e.value()).copy().withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(color & 0xffffff)),0,0,color,false,pose.last().pose(),buffers,Font.DisplayMode.POLYGON_OFFSET,0,light);
                }
                case ITEM -> {
                    ResourceLocation id=ResourceLocation.tryParse(e.value().split("@")[0]);
                    Item item=id==null?Items.AIR:net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id);
                    if(item!=null && item!=Items.AIR) {
                        pose.translate(e.width()/2f,e.height()/2f,2);pose.scale(e.width(),-e.height(),e.width());
                        mc.getItemRenderer().renderStatic(new ItemStack(item),ItemDisplayContext.GUI,light,OverlayTexture.NO_OVERLAY,pose,buffers,mc.level,0);
                    }
                }
                case IMAGE -> {
                    String[] crop=e.link().split(",");int w=Integer.parseInt(crop[0]),h=Integer.parseInt(crop[1]);
                    quad(pose,buffers,new ResourceLocation(e.value()),e.width(),e.height(),(float)e.u()/e.tw(),(float)e.v()/e.th(),(float)(e.u()+w)/e.tw(),(float)(e.v()+h)/e.th(),light);
                }
                case RULE -> quad(pose,buffers,new ResourceLocation("minecraft","textures/block/black_concrete.png"),e.width(),2,0,0,1,1,light);
            }
            pose.popPose();
        }
        if(page>0) {
            pose.pushPose();pose.translate(650,920,0);pose.scale(.4f,.4f,1);
            mc.font.drawInBatch(Component.literal(page+"/"+(doc.pages().size()-1)).withStyle(s->s.withFont(new ResourceLocation("immcraft","ubuntu"))),0,0,0xff0000cc,false,pose.last().pose(),buffers,Font.DisplayMode.NORMAL,0,light);pose.popPose();
        }
    }
    private static void quad(PoseStack p,MultiBufferSource buffers,ResourceLocation texture,float w,float h,float u0,float v0,float u1,float v1,int light) {
        VertexConsumer v=buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        vertex(v,p,0,0,u0,v0,light);vertex(v,p,0,h,u0,v1,light);vertex(v,p,w,h,u1,v1,light);vertex(v,p,w,0,u1,v0,light);
    }
    private static void vertex(VertexConsumer v,PoseStack p,float x,float y,float u,float vv,int light) {
        v.vertex(p.last().pose(),x,y,0).color(255,255,255,255).uv(u,vv).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.last().normal(),0,0,1).endVertex();
    }
    public static final class ManualScreen extends Screen {
        private final IBook book;private BookDocument doc;public int page;
        ManualScreen(IBook book) { super(Component.literal(book.getTitle()));this.book=book;doc=document(book); }
        @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
            renderBackground(g);int left=(width-200)/2,top=(height-220)/2;
            ResourceLocation texture=new ResourceLocation("immcraft","textures/gui/"+(page==0?"manual_front":"manual_paper")+".png");
            g.blit(texture,left,top,0,0,200,220,256,256);g.flush();
            PoseStack p=g.pose();p.pushPose();p.translate(left+20,top+25,10);p.scale(.2f,.2f,.2f);
            var buffers=Minecraft.getInstance().renderBuffers().bufferSource();
            paint(doc,page,p,buffers,LightTexture.FULL_BRIGHT,(mouseX-left-20)*5,(mouseY-top-25)*5);buffers.endBatch();p.popPose();
        }
        private void navigate(String action) { int next=doc.navigate(page,action);if(next!=page) { page=next;sound(); } }
        @Override public boolean mouseClicked(double x,double y,int button) {
            int left=(width-200)/2,top=(height-220)/2;
            if(button==0 && x>=left && x<=left+200 && y>=top && y<=top+220) {
                if(page==0) navigate(">");
                else navigate(BookPlane.action(doc,page,new Vec3((x-left-20)*5,(y-top-25)*5,0)));
                return true;
            }
            return super.mouseClicked(x,y,button);
        }
        @Override public boolean keyPressed(int key,int scan,int modifiers) {
            if(key==GLFW.GLFW_KEY_RIGHT || key==GLFW.GLFW_KEY_SPACE) { navigate(">");return true; }
            if(key==GLFW.GLFW_KEY_LEFT || key==GLFW.GLFW_KEY_BACKSPACE) { navigate("<");return true; }
            if(key==GLFW.GLFW_KEY_HOME) { navigate("^");return true; }
            return super.keyPressed(key,scan,modifiers);
        }
        @Override public boolean isPauseScreen() { return false; }
    }
}
