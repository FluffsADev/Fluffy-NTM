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

public class ItemElevatorPlatformEnclosed extends Item {

	public ItemElevatorPlatformEnclosed() {
		this.setUnlocalizedName("elevator_platform_enclosed");
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

		String existing = ItemCustomElevatorLinker.findPlatformRepKey(stop);
		if(existing != null) {
			player.addChatMessage(new ChatComponentText("This system already has a platform."));
			return true;
		}

		List<TileEntityCustomElevatorStop> comp = ItemCustomElevatorLinker.collectComponentStops(stop);
		for(TileEntityCustomElevatorStop s : comp) {
			s.setPlatform(2, rep[0], rep[1], rep[2]);
		}
		ItemCustomElevatorLinker.normalizeFrom(stop);

		player.addChatMessage(new ChatComponentText("Enclosed elevator platform placed on ESF (" + stop.sizeX + "x" + stop.sizeZ + ")."));

		if(!player.capabilities.isCreativeMode) stack.stackSize--;
		return true;
	}
}
