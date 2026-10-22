package mcjty.immcraft.api.rendering;
import mcjty.immcraft.api.IImmersiveCraft;
import mcjty.immcraft.api.generic.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
/** Source name retained; implementation is a native BlockEntityRenderer, with no legacy GL calls. */
public abstract class HandleTESR<T extends GenericTE> implements BlockEntityRenderer<T> {
    protected final GenericBlock block;protected Vec3 textOffset=Vec3.ZERO;
    public HandleTESR(GenericBlock block) { this.block=block; }
    protected abstract IImmersiveCraft getApi();
    protected abstract void renderExtra(T te,float partial,PoseStack pose,MultiBufferSource buffers,int light);
    public final void render(T te,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(!(te.getBlockState().getBlock() instanceof GenericBlock)) return;
        pose.pushPose();BlockRenderHelper.rotateFacing(te,pose);renderExtra(te,partial,pose,buffers,light);
        BlockRenderHelper.renderInterfaceHandles(getApi(),te,BlockRenderHelper.getFacingInterfaceHandle(te,block),textOffset,pose,buffers,light);pose.popPose();
    }
}
