package mcjty.immcraft.cables;
import mcjty.immcraft.api.helpers.IntersectionTools;
import mcjty.immcraft.blocks.bundle.BundleTE;
import net.minecraft.world.phys.Vec3;
public final class CableSelection {
    public static CableSection findSelectedCable(Vec3 origin,Vec3 end,BundleTE bundle) {
        CableSection closest=null;double distance=.15;
        for(CableSection s:bundle.getCableSections()) for(int i=0;i<2;i++) {
            Vec3 to=s.getVector(i);if(to==null) { if(i==1) continue;to=s.getVector().add(.2,.2,.2); }
            double d=IntersectionTools.calculateRayToLineDistance(origin,end.subtract(origin),s.getVector(),to);
            if(d<distance) { distance=d;closest=s; }
        }
        return closest;
    }
}
