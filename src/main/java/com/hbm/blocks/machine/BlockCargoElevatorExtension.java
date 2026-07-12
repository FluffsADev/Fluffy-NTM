package com.hbm.blocks.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.tileentity.machine.TileEntityCargoElevator;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockCargoElevatorExtension extends BlockDummyable {

	public BlockCargoElevatorExtension() {
		super(Material.iron);
		// give extension its own texture in your block registration/assets
		// setBlockTextureName("hbm:cargo_elevator_extension");
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return null; // no TE on extension block
	}

	@Override
	public int[] getDimensions() {
		return new int[] {0, 0, 1, 1, 1, 1};
	}

	@Override
	public int getOffset() {
		return 1;
	}

	// Any part clickable: extension forwards control to core
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if(world.isRemote) return true;

		int[] pos = ((BlockDummyable) ModBlocks.cargo_elevator_stop).findCore(world, x, y, z);
		if(pos == null) return true;

		TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);
		if(!(te instanceof TileEntityCargoElevator)) return true;
		TileEntityCargoElevator elevator = (TileEntityCargoElevator) te;

		if(player.isSneaking()) elevator.goToNextDownStop();
		else elevator.goToNextUpStop();

		return true;
	}
}
