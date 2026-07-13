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
 * Placing the stop item on (or near) an already-existing elevator does NOT
 * place a block at the clicked position - it adds a new stop floor on top
 * of the shaft, the same way the extension item does. This path is mostly
 * a safety net: normally Block#onBlockActivated on the clicked elevator
 * block fires first and handles this (see BlockCargoElevatorStop /
 * BlockCargoElevatorExtension), so onItemUse here only matters if that
 * ever returns false. If no elevator is found at all, this falls back to
 * ordinary placement to create a brand new machine.
 */
public class ItemBlockCargoElevatorStop extends ItemBlock {

	public ItemBlockCargoElevatorStop(Block block) {
		super(block);
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {

		if(world.isRemote) {
			return true;
		}

		int[] pos = ((BlockDummyable) ModBlocks.cargo_elevator_stop).findCore(world, x, y, z);
		if(pos == null) {
			pos = com.hbm.blocks.machine.BlockCargoElevatorExtension.findElevatorCore(world, x, y, z);
		}

		if(pos != null) {
			TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);
			if(te instanceof TileEntityCargoElevator) {
				TileEntityCargoElevator elevator = (TileEntityCargoElevator) te;

				boolean built = elevator.addTopStopFloor(world, pos[0], pos[1], pos[2]);
				if(built) {
					if(!player.capabilities.isCreativeMode) {
						stack.stackSize--;
					}
					return true;
				}
				// found an elevator but couldn't build (blocked) - don't fall
				// through to placing a stray stop block somewhere odd
				return true;
			}
		}

		// no elevator nearby at all - fallback normal placement (new machine)
		return super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
	}
}
