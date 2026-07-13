package com.hbm.blocks.machine;

import java.util.List;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ICustomBlockHighlight;
import com.hbm.blocks.ModBlocks;
import com.hbm.tileentity.machine.TileEntityCargoElevator;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;

public class BlockCargoElevatorStop extends BlockDummyable {

	public BlockCargoElevatorStop() {
		super(Material.iron);
		// give stop its own texture in your block registration/assets
		// setBlockTextureName("hbm:cargo_elevator_stop");
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		if(meta >= 12) return new TileEntityCargoElevator(); // core only
		return null;
	}

	@Override
	public int[] getDimensions() {
		return new int[] {0, 0, 1, 1, 1, 1};
	}

	@Override
	public int getOffset() {
		return 1;
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if(world.isRemote) return true;

		int[] pos = ((BlockDummyable) ModBlocks.cargo_elevator_stop).findCore(world, x, y, z);
		if(pos == null) return true;

		TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);
		if(!(te instanceof TileEntityCargoElevator)) return true;
		TileEntityCargoElevator elevator = (TileEntityCargoElevator) te;

		ItemStack held = player.getHeldItem();

		// extension item: builds one plain 3x3 layer on top
		if(held != null && held.getItem() == Item.getItemFromBlock(ModBlocks.cargo_elevator_extension)) {

			int layerY = pos[1] + elevator.height + 1;
			boolean replacable = true;

			for(int i = pos[0] - 1; i < pos[0] + 2; i++) {
				for(int j = pos[2] - 1; j < pos[2] + 2; j++) {
					Block b = world.getBlock(i, layerY, j);
					if(!b.isReplaceable(world, i, layerY, j)) {
						replacable = false;
						break;
					}
				}
				if(!replacable) break;
			}

			if(replacable) {
				for(int i = pos[0] - 1; i < pos[0] + 2; i++) {
					for(int j = pos[2] - 1; j < pos[2] + 2; j++) {
						world.setBlock(i, layerY, j, ModBlocks.cargo_elevator_extension, 1, 3);
					}
				}
				elevator.height++;
				elevator.markDirty();

				if(!player.capabilities.isCreativeMode) {
					held.stackSize--;
					if(held.stackSize <= 0) player.setCurrentItemOrArmor(0, null);
				}
			}
			return true;
		}

		// stop item: builds a new STOP floor on top, same way the extension
		// item builds a plain floor - regardless of where on the elevator
		// you clicked to use it
		if(held != null && held.getItem() == Item.getItemFromBlock(ModBlocks.cargo_elevator_stop)) {

			boolean built = elevator.addTopStopFloor(world, pos[0], pos[1], pos[2]);

			if(built && !player.capabilities.isCreativeMode) {
				held.stackSize--;
				if(held.stackSize <= 0) player.setCurrentItemOrArmor(0, null);
			}
			return true;
		}

		// empty hand / unrelated item: clicked stop becomes a marker, then move
		int relY = y - pos[1];
		if(relY >= 0 && relY <= elevator.height) {
			elevator.addStop(relY);
		}

		if(player.isSneaking()) elevator.goToNextDownStop();
		else elevator.goToNextUpStop();

		return true;
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		super.onBlockAdded(world, x, y, z);
		if(world.isRemote) return;

		int[] pos = ((BlockDummyable) ModBlocks.cargo_elevator_stop).findCore(world, x, y, z);
		if(pos == null) return;

		TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);
		if(!(te instanceof TileEntityCargoElevator)) return;
		TileEntityCargoElevator elevator = (TileEntityCargoElevator) te;

		int relY = y - pos[1];
		if(relY < 0) return;

		if(relY > elevator.height) {
			// safety net for stop blocks placed by other means (commands,
			// other mods, etc) above the current shaft height
			elevator.ensureHeightAndAddStop(world, pos[0], pos[1], pos[2], relY);
		} else {
			elevator.addStop(relY);
		}
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		if(!world.isRemote) {
			int[] pos = ((BlockDummyable) ModBlocks.cargo_elevator_stop).findCore(world, x, y, z);
			if(pos != null) {
				TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);
				if(te instanceof TileEntityCargoElevator) {
					TileEntityCargoElevator elevator = (TileEntityCargoElevator) te;
					int relY = y - pos[1];
					elevator.removeStop(relY);
				}
			}
		}
		super.breakBlock(world, x, y, z, block, meta);
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.999F, 1.0F);
	}

	@Override
	public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB entityBounding, List list, Entity entity) {
		int[] pos = this.findCore(world, x, y, z);
		if(pos == null) return;

		TileEntityCargoElevator elevator = (TileEntityCargoElevator) world.getTileEntity(pos[0], pos[1], pos[2]);
		if(elevator == null) return;

		for(AxisAlignedBB aabb : getAABBs(elevator, pos[0], pos[1], pos[2])) {
			if(entityBounding.intersectsWith(aabb)) list.add(aabb);
		}
	}

	@Override
	public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 startVec, Vec3 endVec) {
		int[] pos = this.findCore(world, x, y, z);
		if(pos == null) return null;

		TileEntityCargoElevator elevator = (TileEntityCargoElevator) world.getTileEntity(pos[0], pos[1], pos[2]);
		if(elevator == null) return null;

		for(AxisAlignedBB aabb : getAABBs(elevator, pos[0], pos[1], pos[2])) {
			MovingObjectPosition intercept = aabb.calculateIntercept(startVec, endVec);
			if(intercept != null) {
				return new MovingObjectPosition(pos[0], pos[1], pos[2], intercept.sideHit, intercept.hitVec);			}
		}
		return null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void drawHighlight(DrawBlockHighlightEvent event, World world, int x, int y, int z) {
		int[] pos = this.findCore(world, x, y, z);
		if(pos == null) return;

		TileEntityCargoElevator elevator = (TileEntityCargoElevator) world.getTileEntity(pos[0], pos[1], pos[2]);
		if(elevator == null) return;

		EntityPlayer player = event.player;
		float interp = event.partialTicks;
		double dX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) interp;
		double dY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) interp;
		double dZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) interp;
		float exp = 0.002F;

		ICustomBlockHighlight.setup();
		for(AxisAlignedBB aabb : getAABBs(elevator, pos[0], pos[1], pos[2])) {
			RenderGlobal.drawOutlinedBoundingBox(aabb.expand(exp, exp, exp).getOffsetBoundingBox(-dX, -dY, -dZ), -1);
		}
		ICustomBlockHighlight.cleanup();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldDrawHighlight(World world, int x, int y, int z) {
		return true;
	}


	public AxisAlignedBB[] getAABBs(TileEntityCargoElevator elevator, int x, int y, int z) {
		int h = elevator.height + 1;
		return new AxisAlignedBB[] {
				AxisAlignedBB.getBoundingBox(x - 1, y, z - 1, x - 0.75, y + h, z - 0.75),
				AxisAlignedBB.getBoundingBox(x - 1, y, z + 1.75, x - 0.75, y + h, z + 2),
				AxisAlignedBB.getBoundingBox(x + 1.75, y, z - 1, x + 2, y + h, z - 0.75),
				AxisAlignedBB.getBoundingBox(x + 1.75, y, z + 1.75, x + 2, y + h, z + 2),
				AxisAlignedBB.getBoundingBox(x - 1, y + 0.75 + elevator.extension, z - 1, x + 2, y + 1 + elevator.extension, z + 2),
		};
	}
}
