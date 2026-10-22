package mcjty.immcraft.api.rendering;
import mcjty.immcraft.api.*;
import mcjty.immcraft.api.generic.*;
import mcjty.immcraft.api.handles.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
public final class BlockRenderHelper {
    public static IInterfaceHandle getFacingInterfaceHandle(GenericTE te,GenericBlock block) { var player=Minecraft.getInstance().player;return player==null?null:te.getHandle(player); }
    public static void rotateFacing(GenericTE te,PoseStack pose) {
        float degrees=switch(te.getBlock().getFrontDirection(te.getBlockState())) { case NORTH -> 180;case EAST -> 90;case WEST -> -90;default -> 0; };
        pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(degrees));pose.translate(-.5,0,-.5);
    }
    public static void renderInterfaceHandles(IImmersiveCraft api,GenericTE te,IInterfaceHandle selected,Vec3 textOffset,PoseStack pose,MultiBufferSource buffers,int light) {
        var mc=Minecraft.getInstance();if(mc.player==null || mc.player.distanceToSqr(Vec3.atCenterOf(te.getBlockPos()))>api.getMaxHandleRenderDistanceSquared()) return;
        for(var h:te.getInterfaceHandles()) {
            var selector=te.getBlock().getSelectors().get(h.getSelectorID());if(selector==null) continue;
            var stack=h.getCurrentStack(te);boolean ghost=stack.isEmpty() && h==selected && h.acceptAsInput(mc.player.getMainHandItem());
            if(ghost) stack=mc.player.getMainHandItem();stack=h.getRenderStack(te,stack);if(stack.isEmpty()) continue;
            Vec3 center=selector.getBox().getCenter().add(h.getRenderOffset());pose.pushPose();pose.translate(center.x,center.y,center.z);
            float scale=.18f*h.getScale()*(mc.getItemRenderer().getModel(stack,te.getLevel(),mc.player,0).isGui3d()?1:2);pose.scale(scale,scale,scale);
            MultiBufferSource target=ghost?type->new mcjty.immcraft.client.GhostVertexConsumer(buffers.getBuffer(RenderType.itemEntityTranslucentCull(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS))):buffers;
            mc.getItemRenderer().renderStatic(stack,ItemDisplayContext.NONE,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,pose,target,te.getLevel(),0);pose.popPose();
            if(h==selected) LevelRenderer.renderLineBox(pose,buffers.getBuffer(RenderType.lines()),selector.getBox(),.3f,.7f,1,1);
        }
    }
}
