package mcjty.immcraft.api.util;

import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

public class Vector {
    // Given two vectors, calculate a vector that is perpendicular to this while keeping on an axis
    public static Pair<Vec3,Vec3> calculatePerpendicularVector(Vec3 v1, Vec3 v2) {
        Vec3 d = v2.subtract(v1);
        if (d.x >= d.y && d.x >= d.z) {
            // X is the biggest distance.
            Vec3 vectorZ = d.cross(new Vec3(0, 1, 0));
            Vec3 vectorY = d.cross(vectorZ);
            return Pair.of(vectorY, vectorZ);
        } else if (d.z >= d.y) {
            // Z is the biggest distance.
            Vec3 vectorX = d.cross(new Vec3(0, 1, 0));
            Vec3 vectorY = d.cross(vectorX);
            return Pair.of(vectorY, vectorX);
        } else {
            // Y is the biggest distance.
            Vec3 vectorX = d.cross(new Vec3(0, 0, 1));
            Vec3 vectorZ = d.cross(vectorX);
            return Pair.of(vectorX, vectorZ);
        }
    }
}
