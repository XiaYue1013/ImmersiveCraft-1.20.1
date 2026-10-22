package mcjty.immcraft.api.helpers;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
public final class IntersectionTools {
private static final float EPSILON=.001f;
    public static float calculateRayToLineDistance(Vec3 rayOrigin, Vec3 rayVec, Vec3 lineStart, Vec3 lineEnd) {
        Vec3 u = rayVec;
        Vec3 v = lineEnd.subtract(lineStart);
        Vec3 w = rayOrigin.subtract(lineStart);
        float a = (float) u.dot(u);     // always >= 0
        float b = (float) u.dot(v);
        float c = (float) v.dot(v);     // always >= 0
        float d = (float) u.dot(w);
        float e = (float) v.dot(w);
        float D = a * c - b * b;    // always >= 0
        float sc, sN, sD = D;    // sc = sN / sD, default sD = D >= 0
        float tc, tN, tD = D;    // tc = tN / tD, default tD = D >= 0

        // compute the line parameters of the two closest points
        if (D < EPSILON) {    // the lines are almost parallel
            sN = 0.0f;            // force using point P0 on segment S1
            sD = 1.0f;            // to prevent possible division by 0.0 later
            tN = e;
            tD = c;
        } else {                // get the closest points on the infinite lines
            sN = (b * e - c * d);
            tN = (a * e - b * d);
            if (sN < 0.0f) {    // sc < 0 => the s=0 edge is visible
                sN = 0.0f;
                tN = e;
                tD = c;
            }
        }

        if (tN < 0.0f) {        // tc < 0 => the t=0 edge is visible
            tN = 0.0f;
            // recompute sc for this edge
            if (-d < 0.0f) {
                sN = 0.0f;
            } else {
                sN = -d;
                sD = a;
            }
        } else if (tN > tD) {      // tc > 1 => the t=1 edge is visible
            tN = tD;
            // recompute sc for this edge
            if ((-d + b) < 0.0) {
                sN = 0;
            } else {
                sN = (-d + b);
                sD = a;
            }
        }
        // finally do the division to get sc and tc
        sc = (Math.abs(sN) < EPSILON ? 0.0f : sN / sD);
        tc = (Math.abs(tN) < EPSILON ? 0.0f : tN / tD);

        // get the difference of the two closest points

        Vec3 dP = w.add(u.scale(sc)).subtract(v.scale(tc));
//        Vector dP = w + (sc * u) - (tc * v);	// = S1(sc) - S2(tc)
//        info.iFracRay = sc;
//        info.iFracLine = tc;
        return (float) dP.length();
    }
    public static Vec3 intersectAtGrid(BlockPos c1, BlockPos c2, Vec3 v1, Vec3 v2) {
        if (c1.getX() == c2.getX() && c1.getZ() == c2.getZ()) {
            float yval;
            if (c1.getY() < c2.getY()) {
                yval = c2.getY();
            } else {
                yval = c1.getY();
            }
            float r = (float) ((yval - v1.y) / (v2.y - v1.y));
            return new Vec3(r * (v2.x - v1.x) + v1.x, yval, r * (v2.z - v1.z) + v1.z);
        } else if (c1.getX() == c2.getX() && c1.getY() == c2.getY()) {
            float zval;
            if (c1.getZ() < c2.getZ()) {
                zval = c2.getZ();
            } else {
                zval = c1.getZ();
            }
            float r = (float) ((zval - v1.z) / (v2.z - v1.z));
            return new Vec3(r * (v2.x - v1.x) + v1.x, r * (v2.y - v1.y) + v1.y, zval);
        } else if (c1.getY() == c2.getY() && c1.getZ() == c2.getZ()) {
            float xval;
            if (c1.getX() < c2.getX()) {
                xval = c2.getX();
            } else {
                xval = c1.getX();
            }
            float r = (float) ((xval - v1.x) / (v2.x - v1.x));
            return new Vec3(xval, r * (v2.y - v1.y) + v1.y, r * (v2.z - v1.z) + v1.z);
        } else {
            return new Vec3((v1.x + v2.x) / 2, (v1.y + v2.y) / 2, (v1.z + v2.z) / 2);
        }

    }
}