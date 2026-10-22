package mcjty.immcraft.cables;

import mcjty.immcraft.api.cable.*;
import mcjty.immcraft.api.helpers.NBTHelper;
import mcjty.immcraft.api.helpers.BlockPosTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

public class CableSection implements ICableSection {
    private final ICableType type;
    private final ICableSubType subType;

    // The multiblock network id for this cable.
    private int id;

    private ConnectionInfo[] info = new ConnectionInfo[] { new ConnectionInfo(), new ConnectionInfo() };

    private final Vec3 vector;

    public CableSection(CompoundTag tagCompound) {
        type = CableRegistry.getTypeByID(tagCompound.getString("type"));
        subType = type.getSubTypeByID(tagCompound.getString("subtype"));
        id = tagCompound.getInt("id");
        vector = new Vec3(tagCompound.getFloat("vx"), tagCompound.getFloat("vy"), tagCompound.getFloat("vz"));
        info[0].readFromNBT(tagCompound, "0");
        info[1].readFromNBT(tagCompound, "1");
    }

    public CableSection(ICableType type, ICableSubType subType, int id, Vec3 vector) {
        this.id = id;
        this.type = type;
        this.subType = subType;
        this.vector = vector;
    }

    public CableSectionRender getRenderer(BlockPos pos) {
        Vec3 v1 = getVector(0);
        Vec3 v2 = getVector(1);

        return new CableSectionRender(subType, vector.subtract(pos.getX(), pos.getY(), pos.getZ()), v1 == null ? null : v1.subtract(pos.getX(), pos.getY(), pos.getZ()), v2 == null ? null : v2.subtract(pos.getX(), pos.getY(), pos.getZ()));
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public ICableType getType() {
        return type;
    }

    @Override
    public ICableSubType getSubType() {
        return subType;
    }

    public void setConnection(int directionId, BlockPos connection, Vec3 vector, int connectorId) {
        info[directionId].setConnection(connection);
        info[directionId].setVector(vector);
        info[directionId].setConnectorId(connectorId);
    }

    public BlockPos getConnection(int directionId) {
        return info[directionId].getConnection();
    }

    public Vec3 getVector() {
        return vector;
    }

    public Vec3 getVector(int directionId) {
        return info[directionId].getVector();
    }

    public ICable getCable(Level world) {
        return type.getCableHandler().getCable(world, subType, id);
    }

    public int getConnectorID(int idx) {
        return info[idx].getConnectorId();
    }

    public void releaseConnector(Level world, int directionId) {
        info[directionId].releaseConnector(world);
    }

    @Override
    public ICableConnector getConnector(Level worldObj, int directionId) {
        return info[directionId].getConnector(worldObj);
    }

    public NBTHelper writeToNBT(NBTHelper helper) {
        helper
                .set("type", type.getTypeID())
                .set("subtype", subType.getTypeID())
                .set("id", id)
                .set("v", vector);
        info[0].writeToNBT(helper, "0");
        info[1].writeToNBT(helper, "1");
        return helper;
    }

    private static class ConnectionInfo {
        private Vec3 vector;
        private BlockPos connection;
        private int connectorId;

        public Vec3 getVector() {
            return vector;
        }

        public void setVector(Vec3 vector) {
            this.vector = vector;
        }

        public BlockPos getConnection() {
            return connection;
        }

        public void setConnection(BlockPos connection) {
            this.connection = connection;
        }

        public int getConnectorId() {
            return connectorId;
        }

        public void setConnectorId(int connectorId) {
            this.connectorId = connectorId;
        }

        public void releaseConnector(Level world) {
            ICableConnector c = getConnector(world);
            if (c != null && connectorId != -1) {
                c.disconnect(connectorId);
            }
        }

        public ICableConnector getConnector(Level worldObj) {
            if (connection == null) {
                return null;
            }
            BlockEntity te = worldObj.hasChunkAt(connection)?worldObj.getBlockEntity(connection):null;
            if (te instanceof ICableConnector) {
                return (ICableConnector) te;
            } else {
                return null;
            }
        }

        public void readFromNBT(CompoundTag tagCompound, String suffix) {
            String vk = "v"+suffix;
            if (tagCompound.contains(vk+"x")) {
                vector = new Vec3(tagCompound.getFloat(vk + "x"), tagCompound.getFloat(vk + "y"), tagCompound.getFloat(vk + "z"));
            } else {
                vector = null;
            }
            connection = BlockPosTools.readFromNBT(tagCompound, "c" + suffix);
            connectorId = tagCompound.getInt("i"+suffix);
        }

        public NBTHelper writeToNBT(NBTHelper helper, String suffix) {
            return helper
                    .set("i"+suffix, connectorId)
                    .set("c" + suffix, connection)
                    .set("v"+suffix, vector);
        }
    }
}
