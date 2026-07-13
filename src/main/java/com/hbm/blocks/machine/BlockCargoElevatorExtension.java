package com.hbm.blocks.machine;

import java.util.List;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ICustomBlockHighlight;
import com.hbm.blocks.ModBlocks;
import com.hbm.tileentity.machine.TileEntityCargoElevator;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
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

public class BlockCargoElevatorExtension extends BlockDummyable {

	public BlockCargoElevatorExtension() {
		super(Material.iron);
		// setBlockTextureName("hbm:cargo_elevator_extension");
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
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

	/**
	 * Extension (corner/edge) blocks never contain the real TileEntity -
	 * only the CENTER column does, as a uniform vertical stack of
	 * cargo_elevator_stop blocks with the real TE at the bottom. That's
	 * exactly the case BlockDummyable's own findCore is built for and
	 * already reliably handles (proven by TileEntityCargoElevator's own
	 * merge logic, which calls it directly). So instead of walking the
	 * corner column itself (which can never find the TE, at any height -
	 * this was the bug in the previous version), we hop to each of the 8
	 * possible CENTER columns around (x,z) and delegate to the stop
	 * block's findCore there.
	 *
	 * One edge case: the very top of the shaft has an extra guide-rail
	 * collision layer one block above the last real floor (see
	 * getAABBs: h = elevator.height + 1), but no dummy block is ever
	 * physically placed at that extra height. A same-y lookup fails
	 * there since there's nothing at the center position at that y - so
	 * if the first attempt fails, we retry one block below, which will
	 * land on the real topmost floor's center dummy.
	 */
	public static int[] findElevatorCore(World world, int x, int y, int z) {
		BlockDummyable stopBlock = (BlockDummyable) ModBlocks.cargo_elevator_stop;

		// try current y first
		for(int dx = -1; dx <= 1; dx++) {
			for(int dz = -1; dz <= 1; dz++) {
				if(dx == 0 && dz == 0) continue;

				int cx = x + dx;
				int cz = z + dz;

				int[] pos = stopBlock.findCore(world, cx, y, cz);
				if(pos != null) {
					TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);
					if(te instanceof TileEntityCargoElevator) {
						TileEntityCargoElevator elevator = (TileEntityCargoElevator) te;
						// allow any y within shaft bounds + 1 for top guide layer
						if(y >= pos[1] && y <= pos[1] + elevator.height + 1) {
							return pos;
						}
					}
				}
			}
		}

		// fallback: try one block below (handles top guide-rail layer with no
		// physical center dummy)
		for(int dx = -1; dx <= 1; dx++) {
			for(int dz = -1; dz <= 1; dz++) {
				if(dx == 0 && dz == 0) continue;

				int cx = x + dx;
				int cz = z + dz;

				int[] pos = stopBlock.findCore(world, cx, y - 1, cz);
				if(pos != null) {
					TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);
					if(te instanceof TileEntityCargoElevator) {
						TileEntityCargoElevator elevator = (TileEntityCargoElevator) te;
						if(y >= pos[1] && y <= pos[1] + elevator.height + 1) {
							return pos;
						}
					}
				}
			}
		}

		return null;
	}

	private static int[] tryFindCoreAt(World world, BlockDummyable stopBlock, int cx, int queryY, int cz, int callerY) {
		// try the same y-level first, then one below to cover the extra
		// top guide-rail layer that has no physical center dummy
		for(int cy : new int[] {queryY, queryY - 1}) {
			int[] pos = stopBlock.findCore(world, cx, cy, cz);
			if(pos == null) continue;

			TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);
			if(te instanceof TileEntityCargoElevator) {
				TileEntityCargoElevator elevator = (TileEntityCargoElevator) te;
				if(callerY >= pos[1] && callerY <= pos[1] + elevator.height + 1) {
					return pos;
				}
			}
		}
		return null;
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if(world.isRemote) return true;

		int[] pos = findElevatorCore(world, x, y, z);
		if(pos == null) return true;

		TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);
		if(!(te instanceof TileEntityCargoElevator)) return true;
		TileEntityCargoElevator elevator = (TileEntityCargoElevator) te;

		ItemStack held = player.getHeldItem();

		// stop item: builds a new stop floor on top, same as clicking the
		// core/stop column with the stop item in hand
		if(held != null && held.getItem() == Item.getItemFromBlock(ModBlocks.cargo_elevator_stop)) {
			boolean built = elevator.addTopStopFloor(world, pos[0], pos[1], pos[2]);

			if(built && !player.capabilities.isCreativeMode) {
				held.stackSize--;
				if(held.stackSize <= 0) player.setCurrentItemOrArmor(0, null);
			}
			return true;
		}

		if(player.isSneaking()) elevator.goToNextDownStop();
		else elevator.goToNextUpStop();

		return true;
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.999F, 1.0F);
	}

	@Override
	public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB entityBounding, List list, Entity entity) {
		// only the center column adds collision
		int[] pos = findElevatorCore(world, x, y, z);
		if(pos == null) return;

		// only add AABB if we're at the CENTER
		if(x != pos[0] || z != pos[2]) return;

		TileEntityCargoElevator elevator = (TileEntityCargoElevator) world.getTileEntity(pos[0], pos[1], pos[2]);
		if(elevator == null) return;

		for(AxisAlignedBB aabb : getAABBs(elevator, pos[0], pos[1], pos[2])) {
			if(entityBounding.intersectsWith(aabb)) list.add(aabb);
		}
	}

	@Override
	public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 startVec, Vec3 endVec) {
		// only raycast at center
		int[] pos = findElevatorCore(world, x, y, z);
		if(pos == null) return null;

		if(x != pos[0] || z != pos[2]) return null;

		TileEntityCargoElevator elevator = (TileEntityCargoElevator) world.getTileEntity(pos[0], pos[1], pos[2]);
		if(elevator == null) return null;

		for(AxisAlignedBB aabb : getAABBs(elevator, pos[0], pos[1], pos[2])) {
			MovingObjectPosition intercept = aabb.calculateIntercept(startVec, endVec);
			if(intercept != null) {
				return new MovingObjectPosition(pos[0], pos[1], pos[2], intercept.sideHit, intercept.hitVec);
			}
		}
		return null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void drawHighlight(DrawBlockHighlightEvent event, World world, int x, int y, int z) {
		int[] pos = findElevatorCore(world, x, y, z);
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
				// NW corner
				AxisAlignedBB.getBoundingBox(x - 1, y, z - 1, x - 0.5, y + h, z - 0.5),
				// NE corner
				AxisAlignedBB.getBoundingBox(x + 0.5, y, z - 1, x + 1, y + h, z - 0.5),
				// SW corner
				AxisAlignedBB.getBoundingBox(x - 1, y, z + 0.5, x - 0.5, y + h, z + 1),
				// SE corner
				AxisAlignedBB.getBoundingBox(x + 0.5, y, z + 0.5, x + 1, y + h, z + 1),
				// platform (movable)
				AxisAlignedBB.getBoundingBox(x - 1, y + 0.75 + elevator.extension, z - 1, x + 2, y + 1 + elevator.extension, z + 2),
		};
	}
}
