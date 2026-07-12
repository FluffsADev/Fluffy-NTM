package com.hbm.tileentity.machine;

import java.util.List;
import java.util.TreeSet;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.util.Compat;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class TileEntityCargoElevator extends TileEntityLoadedBase {

	public int height = 0;

	public double extension;
	public double prevExtension;
	public double syncExtension;
	private int sync;

	public double targetExtension = 0;

	public static final double speed = 2D / 20D; // 2 blocks per second
	public boolean renderPlatform = false;

	private final TreeSet<Integer> stops = new TreeSet<Integer>();

	public TileEntityCargoElevator() {
		stops.add(0);
	}

	@Override
	public void updateEntity() {

		this.prevExtension = this.extension;

		if(!worldObj.isRemote) {

			// legacy stack-merge logic updated for split blocks
			if(worldObj.getBlock(xCoord, yCoord - 1, zCoord) == ModBlocks.cargo_elevator_stop) {
				int[] pos = ((BlockDummyable) ModBlocks.cargo_elevator_stop).findCore(worldObj, xCoord, yCoord - 1, zCoord);
				if(pos != null && pos[0] == xCoord && pos[2] == zCoord) {
					TileEntityCargoElevator lower = (TileEntityCargoElevator) worldObj.getTileEntity(pos[0], pos[1], pos[2]);
					if(lower != null) {
						lower.height += this.height + 1;
						for(int x = xCoord - 1; x < xCoord + 2; x++) for(int z = zCoord - 1; z < zCoord + 2; z++) {
							for(int y = yCoord; y <= yCoord + this.height; y++) {
								worldObj.setBlock(x, y, z, ModBlocks.cargo_elevator_extension, 1, 3);
							}
						}
					}
					return;
				}
			}

			targetExtension = MathHelper.clamp_double(targetExtension, 0, this.height);

			if(this.extension < targetExtension) this.extension += speed;
			else if(this.extension > targetExtension) this.extension -= speed;

			if(Math.abs(this.extension - targetExtension) < speed) this.extension = targetExtension;

			this.extension = MathHelper.clamp_double(this.extension, 0, this.height);

			renderPlatform = true;
			this.networkPackNT(300);
		} else {

			if(this.sync > 0) {
				this.extension = this.extension + ((this.syncExtension - this.extension) / (float)this.sync);
				--this.sync;
			} else {
				this.extension = this.syncExtension;
			}
		}

		if(this.extension != this.prevExtension) {
			double liftUpper = this.yCoord + 1 + Math.max(this.extension, this.prevExtension);
			double liftLower = this.yCoord + 1 + Math.min(this.extension, this.prevExtension);
			List<Entity> toLift = worldObj.getEntitiesWithinAABB(Entity.class,
				AxisAlignedBB.getBoundingBox(xCoord - 0.99, liftLower, zCoord - 0.99, xCoord + 1.99, liftUpper, zCoord + 1.99));

			for(Entity e : toLift) {
				if(e instanceof EntityPlayer && !worldObj.isRemote) continue;
				if(e.boundingBox.minY >= liftLower && e.boundingBox.minY <= liftUpper) {
					double delta = e.boundingBox.minY - (this.yCoord + 1 + this.extension);
					e.moveEntity(0, -delta, 0);
					e.onGround = true;
					e.moveEntity(0, -0.125, 0);
				}
			}
		}
	}

	// stop add within built range
	public void addStop(int relY) {
		if(relY < 0 || relY > this.height) return;
		stops.add(relY);
		markDirty();
	}

	public void removeStop(int relY) {
		if(relY == 0) return; // base always
		stops.remove(relY);
		targetExtension = MathHelper.clamp_double(targetExtension, 0, this.height);
		extension = MathHelper.clamp_double(extension, 0, this.height);
		markDirty();
	}

	public boolean hasStop(int relY) {
		return stops.contains(relY);
	}

	// if stop placed on existing elevator above current height:
	// extend shaft as needed and register stop
	public void ensureHeightAndAddStop(World world, int coreX, int coreY, int coreZ, int relY) {
		if(relY < 0) return;

		while(this.height < relY) {
			int layerY = coreY + this.height + 1;
			for(int x = coreX - 1; x < coreX + 2; x++) {
				for(int z = coreZ - 1; z < coreZ + 2; z++) {
					if(world.getBlock(x, layerY, z).isReplaceable(world, x, layerY, z)) {
						world.setBlock(x, layerY, z, ModBlocks.cargo_elevator_extension, 1, 3);
					}
				}
			}
			this.height++;
		}

		stops.add(relY);
		markDirty();
	}

	public void goToNextUpStop() {
		int current = (int)Math.floor(this.extension + 1.0E-6D);
		Integer next = stops.higher(current);
		if(next != null) {
			targetExtension = next;
			markDirty();
		}
	}

	public void goToNextDownStop() {
		int current = (int)Math.ceil(this.extension - 1.0E-6D);
		Integer prev = stops.lower(current);
		if(prev != null) {
			targetExtension = prev;
			markDirty();
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeBoolean(renderPlatform);
		buf.writeShort((short)height);
		buf.writeDouble(extension);
		buf.writeDouble(targetExtension);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		this.renderPlatform = buf.readBoolean();
		this.height = buf.readShort();
		this.syncExtension = buf.readDouble();
		this.targetExtension = buf.readDouble();

		if(this.syncExtension > 0 && this.syncExtension < this.height) {
			this.sync = 3;
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		this.extension = nbt.getDouble("extension");
		this.targetExtension = nbt.getDouble("targetExtension");
		this.height = nbt.getInteger("height");

		stops.clear();
		stops.add(0);

		NBTTagList stopList = nbt.getTagList("stops", 3);
		for(int i = 0; i < stopList.tagCount(); i++) {
			NBTBase base = stopList.tagAt(i);
			if(base instanceof NBTTagInt) {
				int s = ((NBTTagInt)base).func_150287_d();
				if(s >= 0 && s <= this.height) stops.add(s);
			}
		}

		targetExtension = MathHelper.clamp_double(targetExtension, 0, this.height);
		extension = MathHelper.clamp_double(extension, 0, this.height);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);

		nbt.setDouble("extension", extension);
		nbt.setDouble("targetExtension", targetExtension);
		nbt.setInteger("height", height);

		NBTTagList stopList = new NBTTagList();
		for(Integer s : stops) {
			if(s != null && s >= 0 && s <= this.height) {
				stopList.appendTag(new NBTTagInt(s));
			}
		}
		nbt.setTag("stops", stopList);
	}

	AxisAlignedBB bb = null;

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		int h = Compat.isModLoaded(Compat.MOD_ANG) ? 256 - yCoord : 1 + this.height;

		if(bb == null || bb.maxY - bb.minY < h) {
			bb = AxisAlignedBB.getBoundingBox(
				xCoord - 1,
				yCoord,
				zCoord - 1,
				xCoord + 2,
				yCoord + h,
				zCoord + 2
			);
		}

		return bb;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}
}
