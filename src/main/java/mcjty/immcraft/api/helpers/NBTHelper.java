package mcjty.immcraft.api.helpers;

import mcjty.immcraft.api.helpers.BlockPosTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class NBTHelper {
    private final CompoundTag tag;

    private NBTHelper(CompoundTag tag) {
        this.tag = tag;
    }

    public static NBTHelper create(CompoundTag tag) {
        return new NBTHelper(tag);
    }

    public static NBTHelper create() { return new NBTHelper(new CompoundTag()); }

    public NBTHelper set(String name, int value) {
        tag.putInt(name, value);
        return this;
    }

    public NBTHelper set(String name, float value) {
        tag.putFloat(name, value);
        return this;
    }

    public NBTHelper set(String name, double value) {
        tag.putDouble(name, value);
        return this;
    }

    public NBTHelper set(String name, boolean value) {
        tag.putBoolean(name, value);
        return this;
    }

    public NBTHelper set(String name, String value) {
        tag.putString(name, value);
        return this;
    }

    public NBTHelper set(String name, ListTag tc) {
        tag.put(name, tc);
        return this;
    }

    public NBTHelper set(String name, CompoundTag tc) {
        tag.put(name, tc);
        return this;
    }

    public NBTHelper set(String name, BlockPos c) {
        BlockPosTools.writeToNBT(tag, name, c);
        return this;
    }

    public NBTHelper set(String name, Vec3 c) {
        if (c != null) {
            tag.putFloat(name + "x", (float) c.x);
            tag.putFloat(name + "y", (float) c.y);
            tag.putFloat(name + "z", (float) c.z);
        }
        return this;
    }

    public CompoundTag get() {
        return tag;
    }
}
