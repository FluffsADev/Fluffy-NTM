package com.hbm.items.machine;

import java.util.List;

import com.hbm.blocks.machine.BlockCustomElevatorStopFrame;
import com.hbm.items.tool.ItemCustomElevatorLinker;
import com.hbm.tileentity.machine.TileEntityCustomElevatorStop;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

public class ItemElevatorPlatformOpen extends Item {

	public ItemElevatorPlatformOpen() {
		this.setUnlocalizedName("elevator_platform_open");
		this.setMaxStackSize(1);
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hx, float hy, float hz) {
		if(world.isRemote) return true;

		Block b = world.getBlock(x, y, z);
		if(!(b instanceof BlockCustomElevatorStopFrame)) return false;

		TileEntity te = world.getTileEntity(x, y, z);
		if(!(te instanceof TileEntityCustomElevatorStop)) return false;

		TileEntityCustomElevatorStop stop = (TileEntityCustomElevatorStop) te;
		int[] rep = stop.getRepPos();

		for(int ix = stop.minX; ix <= stop.maxX; ix++) {
			for(int iz = stop.minZ; iz <= stop.maxZ; iz++) {
				TileEntity ste = world.getTileEntity(ix, stop.getRepY(), iz);
				if(ste instanceof TileEntityCustomElevatorStop) {
					TileEntityCustomElevatorStop s = (TileEntityCustomElevatorStop) ste;
					if(s.platformType > 0) {
						player.addChatMessage(new ChatComponentText("This stop already has a platform."));
						return true;
					}
				}
			}
		}

		String existing = ItemCustomElevatorLinker.findPlatformRepKey(stop);
		if(existing != null) {
			player.addChatMessage(new ChatComponentText("This system already has a platform."));
			return true;
		}

		List<TileEntityCustomElevatorStop> comp = ItemCustomElevatorLinker.collectComponentStops(stop);
		for(TileEntityCustomElevatorStop s : comp) {
			s.setPlatform(1, rep[0], rep[1], rep[2]);
			world.markBlockForUpdate(s.xCoord, s.yCoord, s.zCoord);
		}
		ItemCustomElevatorLinker.normalizeFrom(stop);

		player.addChatMessage(new ChatComponentText("Open elevator platform placed on ESF (" + stop.sizeX + "x" + stop.sizeZ + ")."));
		if(!player.capabilities.isCreativeMode) stack.stackSize--;
		return true;
	}
}
