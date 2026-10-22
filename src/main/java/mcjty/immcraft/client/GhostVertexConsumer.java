package mcjty.immcraft.client;
import com.mojang.blaze3d.vertex.VertexConsumer;
/** Preserve the item renderer's vertices and glint/NBT model while attenuating the preview alpha. */
public final class GhostVertexConsumer implements VertexConsumer {
    private final VertexConsumer out;
    public GhostVertexConsumer(VertexConsumer out) { this.out=out; }
    public VertexConsumer vertex(double x,double y,double z) { out.vertex(x,y,z);return this; }
    public VertexConsumer color(int r,int g,int b,int a) { out.color(r,g,b,Math.round(a*.35f));return this; }
    public VertexConsumer uv(float u,float v) { out.uv(u,v);return this; }
    public VertexConsumer overlayCoords(int u,int v) { out.overlayCoords(u,v);return this; }
    public VertexConsumer uv2(int u,int v) { out.uv2(u,v);return this; }
    public VertexConsumer normal(float x,float y,float z) { out.normal(x,y,z);return this; }
    public void endVertex() { out.endVertex(); }
    public void defaultColor(int r,int g,int b,int a) { out.defaultColor(r,g,b,Math.round(a*.35f)); }
    public void unsetDefaultColor() { out.unsetDefaultColor(); }
}
