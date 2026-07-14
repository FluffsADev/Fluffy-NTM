package com.hbm.tileentity.machine;

import java.util.ArrayList;
import java.util.List;

import com.hbm.tileentity.TileEntityLoadedBase;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;


public class TileEntityCustomElevatorStop extends TileEntityLoadedBase {


	public int minX, minZ, maxX, maxZ;
	public int sizeX = 1, sizeZ = 1;

	public int systemId = -1;

	public List<int[]> links = new ArrayList<int[]>();

	public int platformType = 0;

	public int platformX = Integer.MIN_VALUE;
	public int platformY = Integer.MIN_VALUE;
	public int platformZ = Integer.MIN_VALUE;

	@Override
	public void updateEntity() {
		if(!worldObj.isRemote) {
			this.networkPackNT(250);
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeInt(minX);
		buf.writeInt(minZ);
		buf.writeInt(maxX);
		buf.writeInt(maxZ);
		buf.writeInt(sizeX);
		buf.writeInt(sizeZ);

		buf.writeInt(systemId);
		buf.writeInt(platformType);
		buf.writeInt(platformX);
		buf.writeInt(platformY);
		buf.writeInt(platformZ);

		buf.writeInt(links.size());
		for(int[] p : links) {
			buf.writeInt(p[0]);
			buf.writeInt(p[1]);
			buf.writeInt(p[2]);
		}
	}


	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		minX = buf.readInt();
		minZ = buf.readInt();
		maxX = buf.readInt();
		maxZ = buf.readInt();
		sizeX = buf.readInt();
		sizeZ = buf.readInt();

		systemId = buf.readInt();
		platformType = buf.readInt();
		platformX = buf.readInt();
		platformY = buf.readInt();
		platformZ = buf.readInt();

		links.clear();
		int c = buf.readInt();
		for(int i = 0; i < c; i++) {
			links.add(new int[] {buf.readInt(), buf.readInt(), buf.readInt()});
		}
	}


	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		minX = nbt.getInteger("minX");
		minZ = nbt.getInteger("minZ");
		maxX = nbt.getInteger("maxX");
		maxZ = nbt.getInteger("maxZ");
		sizeX = nbt.getInteger("sizeX");
		sizeZ = nbt.getInteger("sizeZ");

		systemId = nbt.getInteger("systemId");
		platformType = nbt.getInteger("platformType");
		platformX = nbt.hasKey("platformX") ? nbt.getInteger("platformX") : Integer.MIN_VALUE;
		platformY = nbt.hasKey("platformY") ? nbt.getInteger("platformY") : Integer.MIN_VALUE;
		platformZ = nbt.hasKey("platformZ") ? nbt.getInteger("platformZ") : Integer.MIN_VALUE;

		links.clear();
		int c = nbt.getInteger("linkCount");
		for(int i = 0; i < c; i++) {
			links.add(new int[] {
				nbt.getInteger("lx_" + i),
				nbt.getInteger("ly_" + i),
				nbt.getInteger("lz_" + i)
			});
		}
	}


	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("minX", minX);
		nbt.setInteger("minZ", minZ);
		nbt.setInteger("maxX", maxX);
		nbt.setInteger("maxZ", maxZ);
		nbt.setInteger("sizeX", sizeX);
		nbt.setInteger("sizeZ", sizeZ);

		nbt.setInteger("systemId", systemId);
		nbt.setInteger("platformType", platformType);
		nbt.setInteger("platformX", platformX);
		nbt.setInteger("platformY", platformY);
		nbt.setInteger("platformZ", platformZ);

		nbt.setInteger("linkCount", links.size());
		for(int i = 0; i < links.size(); i++) {
			int[] p = links.get(i);
			nbt.setInteger("lx_" + i, p[0]);
			nbt.setInteger("ly_" + i, p[1]);
			nbt.setInteger("lz_" + i, p[2]);
		}
	}


	public int getRepX() {
		return minX;
	}

	public int getRepY() {
		return yCoord;
	}


	public int getRepZ() {
		return minZ;
	}


	public int[] getRepPos() {
		return new int[] {getRepX(), getRepY(), getRepZ()};
	}


	public boolean isSameStop(TileEntityCustomElevatorStop other) {
		if(other == null) return false;
		if(other.worldObj != this.worldObj) return false;
		return this.minX == other.minX && this.maxX == other.maxX && this.minZ == other.minZ && this.maxZ == other.maxZ && this.yCoord == other.yCoord;
	}


	public boolean isLinkedTo(int x, int y, int z) {
		for(int[] p : links) {
			if(p[0] == x && p[1] == y && p[2] == z) return true;
		}
		return false;
	}

	public boolean addLink(int x, int y, int z) {
		if(isLinkedTo(x, y, z)) return false;
		links.add(new int[] {x, y, z});
		markDirty();
		return true;
	}

	public boolean removeLink(int x, int y, int z) {
		for(int i = 0; i < links.size(); i++) {
			int[] p = links.get(i);
			if(p[0] == x && p[1] == y && p[2] == z) {
				links.remove(i);
				markDirty();
				return true;
			}
		}
		return false;
	}


	public TileEntityCustomElevatorStop getLinkedStop(int[] link) {
		if(worldObj == null || link == null || link.length < 3) return null;
		TileEntity te = worldObj.getTileEntity(link[0], link[1], link[2]);
		if(te instanceof TileEntityCustomElevatorStop) return (TileEntityCustomElevatorStop) te;
		return null;
	}


	public void setPlatform(int type, int x, int y, int z) {
		this.platformType = type;
		this.platformX = x;
		this.platformY = y;
		this.platformZ = z;
		markDirty();
	}


	public void clearPlatform() {
		this.platformType = 0;
		this.platformX = Integer.MIN_VALUE;
		this.platformY = Integer.MIN_VALUE;
		this.platformZ = Integer.MIN_VALUE;
		markDirty();
	}


	public double[] getCornerPoint(int idx) {
		switch(idx & 3) {
			case 0: return new double[] {minX + 0.5D, yCoord + 1D, minZ + 0.5D};
			case 1: return new double[] {maxX + 0.5D, yCoord + 1D, minZ + 0.5D};
			case 2: return new double[] {maxX + 0.5D, yCoord + 1D, maxZ + 0.5D};
			default: return new double[] {minX + 0.5D, yCoord + 1D, maxZ + 0.5D};
		}
	}
}
