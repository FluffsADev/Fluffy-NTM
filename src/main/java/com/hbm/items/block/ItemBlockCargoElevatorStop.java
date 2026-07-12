package com.hbm.items.block;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.tileentity.machine.TileEntityCargoElevator;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Best-guess custom item placement:
 * - If placing on existing elevator shaft, place stop there and register stop.
 * - Otherwise fallback to default placement (new elevator creation path).
 */
public class ItemBlockCargoElevatorStop extends ItemBlock {

	public ItemBlockCargoElevatorStop(Block block) {
		super(block);
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {

		// place offset like normal
		int px = x;
		int py = y;
		int pz = z;

		Block target = world.getBlock(x, y, z);
		if(!target.isReplaceable(world, x, y, z)) {
			switch(side) {
				case 0: py--; break;
				case 1: py++; break;
				case 2: pz--; break;
				case 3: pz++; break;
				case 4: px--; break;
				case 5: px++; break;
			}
		}

		if(world.isRemote) {
			return true;
		}

		// Try attach-to-existing-elevator flow
		int[] pos = ((BlockDummyable) ModBlocks.cargo_elevator_stop).findCore(world, x, y, z);
		if(pos == null) {
			pos = ((BlockDummyable) ModBlocks.cargo_elevator_stop).findCore(world, px, py, pz);
		}

		if(pos != null) {
			TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);
			if(te instanceof TileEntityCargoElevator) {
				TileEntityCargoElevator elevator = (TileEntityCargoElevator) te;
				int relY = py - pos[1];
				if(relY >= 0) {
					if(world.getBlock(px, py, pz).isReplaceable(world, px, py, pz) || world.getBlock(px, py, pz) == ModBlocks.cargo_elevator_extension) {
						world.setBlock(px, py, pz, ModBlocks.cargo_elevator_stop, 1, 3);
						elevator.ensureHeightAndAddStop(world, pos[0], pos[1], pos[2], relY);

						if(!player.capabilities.isCreativeMode) {
							stack.stackSize--;
						}
						return true;
					}
				}
			}
		}

		// fallback normal placement (new machine)
		return super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
	}
}
