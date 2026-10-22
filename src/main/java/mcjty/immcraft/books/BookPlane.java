package mcjty.immcraft.books;

import mcjty.immcraft.SlotLayout;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** The exact four stand page corners from the original SOUTH orientation. */
public final class BookPlane {
    // Logical text X runs left-to-right. The source Plane constructor orders its corners
    // in the opposite direction and flips intersection X before dispatching page actions.
    public static final Vec3 ORIGIN=new Vec3(.16,.81,.45),RIGHT=new Vec3(.68,0,0),DOWN=new Vec3(0,-.68,.41);
    public static Vec3 hit(Direction facing,Vec3 from,Vec3 to) {
        Vec3 start=SlotLayout.toLocal(from,facing),delta=SlotLayout.toLocal(to,facing).subtract(start),normal=RIGHT.cross(DOWN);
        double denominator=normal.dot(delta);
        if(Math.abs(denominator)<1e-9) return null;
        double t=normal.dot(ORIGIN.subtract(start))/denominator;
        if(t<0 || t>1) return null;
        Vec3 p=start.add(delta.scale(t)).subtract(ORIGIN);
        double x=p.dot(RIGHT)/RIGHT.lengthSqr(),y=p.dot(DOWN)/DOWN.lengthSqr();
        return x<0 || x>1 || y<0 || y>1?null:new Vec3(x*BookDocument.WIDTH,y*BookDocument.HEIGHT,0);
    }
    public static String action(BookDocument doc,int page,Vec3 pixel) {
        if(pixel==null) return null;
        String link=doc.pages().get(doc.clamp(page)).linkAt(pixel.x,pixel.y);
        if(link!=null && !link.matches("\\d+,\\d+")) return link;
        if(pixel.x<BookDocument.WIDTH*.15) return "<";
        if(pixel.x>BookDocument.WIDTH*.9) return ">";
        return pixel.y<BookDocument.HEIGHT*.15?"^":null;
    }
}
