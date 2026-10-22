package mcjty.immcraft;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.*;
import java.util.*;

/** Canonical SOUTH-facing coordinates shared by rendering and server ray tests. */
public final class SlotLayout {
    public record Slot(int index, AABB bounds, Vec3 center, boolean flat, float scale) {
        Slot(int index, AABB b, boolean flat, float scale) { this(index,b,b.getCenter(),flat,scale); }
    }
    private static final Map<Kind,List<Slot>> SLOTS = new EnumMap<>(Kind.class);
    static {
        for (Kind k : Kind.values()) {
            List<Slot> s = new ArrayList<>();
            if (k == Kind.CHEST) {
                for (int y=0;y<3;y++) for(int x=0;x<4;x++) s.add(new Slot(y*4+x,new AABB(.1+.2*x,.90,.1+.3*y,.3+.2*x,1,.4+.3*y),true,.108f));
            } else if (k == Kind.CUPBOARD || k == Kind.SHELF || k == Kind.BOOKSHELF) {
                int w=k==Kind.BOOKSHELF?9:4,h=k==Kind.BOOKSHELF?3:4;
                for(int y=0;y<h;y++) for(int x=0;x<w;x++) {
                    AABB box=k==Kind.BOOKSHELF?new AABB(.1+.088*x,.095+.285*y,.2,.1+.088*(x+1),.045+.285*(y+1),.5):new AABB(.1+.2*x,.1+.2*y,.2,.3+.2*x,.3+.2*y,.5);
                    s.add(new Slot(y*w+x,box,false,k==Kind.BOOKSHELF?.2214f:.108f));
                }
            } else if(k==Kind.FURNACE) {
                s.add(new Slot(0,new AABB(0,.1,.3,1,.48,.8),false,.18f));
                s.add(new Slot(1,new AABB(0,.51,.3,.5,.99,.8),false,.18f));
                s.add(new Slot(2,new AABB(.51,.51,.3,1,.99,.8),false,.18f));
            } else if(k==Kind.DISPLAY || k==Kind.WORKBENCH) {
                for(int y=0;y<2;y++) for(int x=0;x<2;x++) {
                    double z=k==Kind.DISPLAY?.5-y*.5:y*.5;
                    s.add(new Slot(y*2+x,new AABB(x*.5,k==Kind.DISPLAY?0:.94,z,x*.5+.5,k==Kind.DISPLAY?.4:1.1,z+.5),true,.18f));
                }
            } else if(k==Kind.WALL_DISPLAY) {
                for(int y=0;y<2;y++) for(int x=0;x<2;x++) s.add(new Slot(y*2+x,new AABB(x*.5,.5-y*.5,0,x*.5+.5,1-y*.5,.2),false,.18f));
            } else if(k==Kind.WORKBENCH_SEC) {
                s.add(new Slot(0,new AABB(.6,.94,0,1,1.15,.4),true,.28f));
                s.add(new Slot(1,new AABB(0,.94,.25,.5,1.15,.75),true,.36f));
            }
            SLOTS.put(k,List.copyOf(s));
        }
    }
    public static List<Slot> slots(Kind k) { return SLOTS.get(k); }
    public static Vec3 toWorld(Vec3 p,Direction facing) {
        return switch(facing) {
            case NORTH -> new Vec3(1-p.x,p.y,1-p.z);
            case EAST -> new Vec3(p.z,p.y,1-p.x);
            case WEST -> new Vec3(1-p.z,p.y,p.x);
            default -> p;
        };
    }
    public static Vec3 toLocal(Vec3 p,Direction facing) {
        return toWorld(p, facing==Direction.EAST?Direction.WEST:facing==Direction.WEST?Direction.EAST:facing);
    }
    public static int select(Kind kind,Direction facing,Vec3 start,Vec3 end) {
        Vec3 a=toLocal(start,facing), b=toLocal(end,facing);
        double best=Double.MAX_VALUE; int index=-1;
        for(Slot s:slots(kind)) {
            Optional<Vec3> hit=s.bounds.clip(a,b);
            if(hit.isPresent()) {
                double d=hit.get().distanceToSqr(a);
                if(d<best) { best=d;index=s.index; }
            }
        }
        return index;
    }
}
