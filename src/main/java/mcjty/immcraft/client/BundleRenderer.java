package mcjty.immcraft.client;
import mcjty.immcraft.blocks.bundle.BundleTE;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
public final class BundleRenderer implements BlockEntityRenderer<BundleTE> {
    public BundleRenderer(BlockEntityRendererProvider.Context context) {}
    public void render(BundleTE be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        for(var s:be.getCableSections()) {
            Vec3 origin=Vec3.atLowerCornerOf(be.getBlockPos()),center=s.getVector().subtract(origin);
            ResourceLocation name=ResourceLocation.tryParse(s.getSubType().getTextureName());
            if(name==null) name=new ResourceLocation("immcraft","block/bundle");
            var sprite=Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(name);
            for(int i=0;i<2;i++) {
                Vec3 end=s.getVector(i);if(end==null) { if(i==1) continue;end=s.getVector().add(.1,.1,.1); }
                beam(pose,buffers,center,end.subtract(origin),sprite,light,overlay);
            }
        }
    }
    private void beam(PoseStack p,MultiBufferSource buffers,Vec3 a,Vec3 b,TextureAtlasSprite tex,int light,int overlay) {
        Vec3 axis=b.subtract(a).normalize();if(axis.lengthSqr()<.01) return;
        Vec3 u=axis.cross(Math.abs(axis.y)>.9?new Vec3(1,0,0):new Vec3(0,1,0)).normalize().scale(.1),v=axis.cross(u).normalize().scale(.1);
        VertexConsumer out=buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        Vec3[] ring={u.add(v),u.subtract(v),u.scale(-1).subtract(v),v.subtract(u)};
        for(int i=0;i<4;i++) {
            Vec3 d=ring[i],next=ring[(i+1)%4],normal=d.add(next).normalize();
            vertex(out,p,a.add(d),tex.getU0(),tex.getV0(),normal,light,overlay);vertex(out,p,b.add(d),tex.getU0(),tex.getV1(),normal,light,overlay);
            vertex(out,p,b.add(next),tex.getU1(),tex.getV1(),normal,light,overlay);vertex(out,p,a.add(next),tex.getU1(),tex.getV0(),normal,light,overlay);
        }
    }
    private void vertex(VertexConsumer out,PoseStack p,Vec3 at,float u,float v,Vec3 normal,int light,int overlay) {
        out.vertex(p.last().pose(),(float)at.x,(float)at.y,(float)at.z).color(255,255,255,255).uv(u,v).overlayCoords(overlay).uv2(light).normal(p.last().normal(),(float)normal.x,(float)normal.y,(float)normal.z).endVertex();
    }
    public int getViewDistance() { return 64; }
}
