package com.hbm.blocks.machine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hbm.main.MainRegistry;
import org.lwjgl.opengl.GL11;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.items.ModItems;
import com.hbm.lib.RefStrings;
import com.hbm.render.block.ISBRHUniversal;
import com.hbm.render.util.RenderBlocksNT;
import com.hbm.tileentity.machine.TileEntityCustomElevatorStop;
import com.hbm.util.i18n.I18nUtil;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;


public class BlockCustomElevatorStopFrame extends BlockDummyable implements ISBRHUniversal, ILookOverlay {

	public static final int META_FRAME_STOP  = 1;

	@SideOnly(Side.CLIENT) private IIcon iconPlatformMiddle;
	@SideOnly(Side.CLIENT) private IIcon iconPlatformEdge;
	@SideOnly(Side.CLIENT) private IIcon iconPlatformCorner;
	@SideOnly(Side.CLIENT) private IIcon iconBeam;

	public BlockCustomElevatorStopFrame() {
		super(Material.iron);
		this.setBlockName("elevator_stop_frame");
		this.setHardness(3.0F);
		this.setResistance(10.0F);
	}

	@Override
	public int getRenderType() {
		return ISBRHUniversal.renderID;
	}


	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		this.blockIcon = reg.registerIcon(RefStrings.MODID + ":elevator_frame_block");
		this.iconPlatformMiddle = reg.registerIcon(RefStrings.MODID + ":elevator_platform_middle");
		this.iconPlatformEdge = reg.registerIcon(RefStrings.MODID + ":elevator_platform_edge");
		this.iconPlatformCorner = reg.registerIcon(RefStrings.MODID + ":elevator_platform_corner");
		this.iconBeam = reg.registerIcon(RefStrings.MODID + ":elevator_frame_beam");
	}


	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		if(meta == META_FRAME_STOP) {
			return iconPlatformMiddle != null ? iconPlatformMiddle : this.blockIcon;
		}
		return this.blockIcon;
	}


	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		if(meta == META_FRAME_STOP) return new TileEntityCustomElevatorStop();
		return null;
	}


	@Override
	public int[] getDimensions() {
		return new int[] {0, 0, 0, 0, 0, 0};
	}


	@Override
	public int getOffset() {
		return 0;
	}


	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		if(isFinalizedFrame(world, x, y, z)) return null;
		return AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1);
	}


	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
		if(isFinalizedFrame(world, x, y, z)) {
			return AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1);
		}
		return super.getSelectedBoundingBoxFromPool(world, x, y, z);
	}


	@Override
	public void renderInventoryBlock(Block block, int meta, int modelId, Object renderBlocks) {
		RenderBlocks renderer = (RenderBlocks) renderBlocks;

		GL11.glPushMatrix();
		GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
		GL11.glTranslatef(-0.5F, -0.5F, -0.5F);

		renderInvBox(renderer, iconPlatformMiddle != null ? iconPlatformMiddle : blockIcon, 0D, 0.8125D, 0D, 1D, 1D, 1D, block, meta);
		renderInvBox(renderer, iconBeam != null ? iconBeam : blockIcon, 0D, 0D, 0D, 0.125D, 1D, 0.125D, block, meta);
		renderInvBox(renderer, iconBeam != null ? iconBeam : blockIcon, 0.875D, 0D, 0D, 1D, 1D, 0.125D, block, meta);
		renderInvBox(renderer, iconBeam != null ? iconBeam : blockIcon, 0D, 0D, 0.875D, 0.125D, 1D, 1D, block, meta);
		renderInvBox(renderer, iconBeam != null ? iconBeam : blockIcon, 0.875D, 0D, 0.875D, 1D, 1D, 1D, block, meta);

		GL11.glPopMatrix();
	}


	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, Object renderBlocks) {
		RenderBlocksNT renderer = RenderBlocksNT.INSTANCE.setWorld(world);
		Tessellator tessellator = Tessellator.instance;
		tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
		tessellator.setColorOpaque_F(1F, 1F, 1F);

		if(!isFinalizedFrame(world, x, y, z)) {
			renderBox(renderer, blockIcon, block, x, y, z, 0D, 0D, 0D, 1D, 1D, 1D);
			return true;
		}

		TileEntity te = world.getTileEntity(x, y, z);
		if(!(te instanceof TileEntityCustomElevatorStop)) {
			renderBox(renderer, blockIcon, block, x, y, z, 0D, 0D, 0D, 1D, 1D, 1D);
			return true;
		}

		TileEntityCustomElevatorStop stop = (TileEntityCustomElevatorStop) te;
		boolean minX = x == stop.minX;
		boolean maxX = x == stop.maxX;
		boolean minZ = z == stop.minZ;
		boolean maxZ = z == stop.maxZ;
		boolean corner = (minX || maxX) && (minZ || maxZ);
		boolean edge = !corner && (minX || maxX || minZ || maxZ);

		IIcon platformIcon = corner ? iconPlatformCorner : edge ? iconPlatformEdge : iconPlatformMiddle;
		renderBox(renderer, platformIcon != null ? platformIcon : blockIcon, block, x, y, z, 0D, 0.8125D, 0D, 1D, 1D, 1D);

		IIcon beamIcon = iconBeam != null ? iconBeam : blockIcon;
		if(minX && !isFinalizedFrame(world, x - 1, y, z)) renderBox(renderer, beamIcon, block, x, y, z, 0D, 0D, 0D, 0.125D, 1D, 1D);
		if(maxX && !isFinalizedFrame(world, x + 1, y, z)) renderBox(renderer, beamIcon, block, x, y, z, 0.875D, 0D, 0D, 1D, 1D, 1D);
		if(minZ && !isFinalizedFrame(world, x, y, z - 1)) renderBox(renderer, beamIcon, block, x, y, z, 0D, 0D, 0D, 1D, 1D, 0.125D);
		if(maxZ && !isFinalizedFrame(world, x, y, z + 1)) renderBox(renderer, beamIcon, block, x, y, z, 0D, 0D, 0.875D, 1D, 1D, 1D);

		if(corner) {
			renderBox(renderer, beamIcon, block, x, y, z, 0D, 0D, 0D, 0.125D, 1D, 0.125D);
			renderBox(renderer, beamIcon, block, x, y, z, 0.875D, 0D, 0D, 1D, 1D, 0.125D);
			renderBox(renderer, beamIcon, block, x, y, z, 0D, 0D, 0.875D, 0.125D, 1D, 1D);
			renderBox(renderer, beamIcon, block, x, y, z, 0.875D, 0D, 0.875D, 1D, 1D, 1D);
		}

		return true;
	}


	@SideOnly(Side.CLIENT)
	private void renderInvBox(RenderBlocks renderer, IIcon icon, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Block block, int meta) {
		renderer.setOverrideBlockTexture(icon);
		renderer.setRenderBounds(minX, minY, minZ, maxX, maxY, maxZ);
		RenderBlocksNT.renderStandardInventoryBlock(block, meta, renderer);
		renderer.clearOverrideBlockTexture();
	}


	@SideOnly(Side.CLIENT)
	private void renderBox(RenderBlocksNT renderer, IIcon icon, Block block, int x, int y, int z, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		renderer.setOverrideBlockTexture(icon);
		renderer.setRenderBounds(minX, minY, minZ, maxX, maxY, maxZ);
		renderer.renderStandardBlock(block, x, y, z);
		renderer.clearOverrideBlockTexture();
	}


	private boolean isFinalizedFrame(IBlockAccess world, int x, int y, int z) {
		if(world.getBlock(x, y, z) != this) return false;
		TileEntity te = world.getTileEntity(x, y, z);
		return te instanceof TileEntityCustomElevatorStop;
	}

	private boolean isFrameBlock(IBlockAccess world, int x, int y, int z) {
		if(world.getBlock(x, y, z) != this) return false;
		TileEntity te = world.getTileEntity(x, y, z);
		return !(te instanceof TileEntityCustomElevatorStop);
	}


	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		ItemStack held = player.getHeldItem();
		if(held == null || held.getItem() != ModItems.custom_ele_linker) return false;

		TileEntity te = world.getTileEntity(x, y, z);


		if (!(te instanceof TileEntityCustomElevatorStop)) {
			if(world.isRemote) return true;
			finalizeFrameCluster(world, x, y, z, player);
			return true;
		}

		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void printHook(Pre event, World world, int x, int y, int z) {
		if(!isFinalizedFrame(world, x, y, z)) return;

		TileEntity te = world.getTileEntity(x, y, z);
		if(!(te instanceof TileEntityCustomElevatorStop)) return;

		TileEntityCustomElevatorStop stop = (TileEntityCustomElevatorStop) te;
		List<TileEntityCustomElevatorStop> all = collectStops(stop);
		Collections.sort(all, new Comparator<TileEntityCustomElevatorStop>() {
			@Override
			public int compare(TileEntityCustomElevatorStop a, TileEntityCustomElevatorStop b) {
				if(a.getRepY() != b.getRepY()) return a.getRepY() - b.getRepY();
				if(a.getRepX() != b.getRepX()) return a.getRepX() - b.getRepX();
				return a.getRepZ() - b.getRepZ();
			}
		});

		Map<String, Integer> indexMap = new HashMap<String, Integer>();
		for(int i = 0; i < all.size(); i++) {
			TileEntityCustomElevatorStop s = all.get(i);
			indexMap.put(k(s.getRepX(), s.getRepY(), s.getRepZ()), i + 1);
		}

		int myNum = 0;
		Integer idx = indexMap.get(k(stop.getRepX(), stop.getRepY(), stop.getRepZ()));
		if(idx != null) myNum = idx.intValue();

		List<String> text = new ArrayList<String>();
		text.add("Stop #" + myNum + " / " + all.size());
		text.add("System: " + stop.systemId);
		text.add("Size: " + stop.sizeX + "x" + stop.sizeZ);

		if(stop.links.isEmpty()) {
			text.add("Linked stops: none");
		} else {
			text.add("Linked stops:");
			for(int[] l : stop.links) {
				Integer li = indexMap.get(k(l[0], l[1], l[2]));
				if(li != null) {
					text.add(" - #" + li.intValue() + " (" + l[0] + ", " + l[1] + ", " + l[2] + ")");
				} else {
					text.add(" - ? (" + l[0] + ", " + l[1] + ", " + l[2] + ")");
				}
			}
		}

		ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getUnlocalizedName() + ".name"), 0xffff00, 0x404000, text);
	}


	private List<TileEntityCustomElevatorStop> collectStops(TileEntityCustomElevatorStop start) {
		List<TileEntityCustomElevatorStop> out = new ArrayList<TileEntityCustomElevatorStop>();
		Set<String> seen = new HashSet<String>();
		ArrayDeque<TileEntityCustomElevatorStop> q = new ArrayDeque<TileEntityCustomElevatorStop>();
		q.add(start);

		while(!q.isEmpty()) {
			TileEntityCustomElevatorStop cur = q.poll();
			if(cur == null || cur.getWorldObj() == null) continue;

			String key = k(cur.getRepX(), cur.getRepY(), cur.getRepZ());
			if(seen.contains(key)) continue;
			seen.add(key);
			out.add(cur);

			for(int[] l : cur.links) {
				TileEntity n = cur.getWorldObj().getTileEntity(l[0], l[1], l[2]);
				if(n instanceof TileEntityCustomElevatorStop) q.add((TileEntityCustomElevatorStop) n);
			}
		}

		return out;
	}


	private String k(int x, int y, int z) {
		return x + "," + y + "," + z;
	}


	@SuppressWarnings("unchecked")
	public void finalizeFrameCluster(World world, int sx, int sy, int sz, EntityPlayer player) {
		ArrayList<int[]> cluster = collectCluster(world, sx, sy, sz);
		if(cluster.isEmpty()) {
			player.addChatMessage(new ChatComponentText("ESF finalize error: no valid frame blocks found at cursor."));
			return;
		}

		for(int[] p : cluster) {
			if(p[1] != sy) {
				player.addChatMessage(new ChatComponentText("Invalid frame: must be flat on one Y level."));
				return;
			}
		}

		int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

		for(int[] p : cluster) {
			if(p[0] < minX) minX = p[0];
			if(p[0] > maxX) maxX = p[0];
			if(p[2] < minZ) minZ = p[2];
			if(p[2] > maxZ) maxZ = p[2];
		}

		int sizeX = maxX - minX + 1;
		int sizeZ = maxZ - minZ + 1;

		if(sizeX < 1 || sizeZ < 1 || sizeX > 7 || sizeZ > 7) {
			player.addChatMessage(new ChatComponentText("Invalid frame size: must be between 1x1 and 7x7."));
			return;
		}

		for(int x = minX; x <= maxX; x++) {
			for(int z = minZ; z <= maxZ; z++) {
				if(!isFrameBlock(world, x, sy, z)) {
					player.addChatMessage(new ChatComponentText("Invalid frame: must be a full rectangle."));
					return;
				}
			}
		}

		for(int x = minX; x <= maxX; x++) {
			for(int z = minZ; z <= maxZ; z++) {
				TileEntity te = world.getTileEntity(x, sy, z);
				if(!(te instanceof TileEntityCustomElevatorStop)) {
					world.removeTileEntity(x, sy, z);
					world.setTileEntity(x, sy, z, new TileEntityCustomElevatorStop());
					te = world.getTileEntity(x, sy, z);
				}

				if(!(te instanceof TileEntityCustomElevatorStop)) {
					player.addChatMessage(new ChatComponentText("ESF finalize error: failed to create stop tile entity."));
					return;
				}

				TileEntityCustomElevatorStop stop = (TileEntityCustomElevatorStop) te;
				stop.minX = minX;
				stop.minZ = minZ;
				stop.maxX = maxX;
				stop.maxZ = maxZ;
				stop.sizeX = sizeX;
				stop.sizeZ = sizeZ;
				stop.markDirty();
			}
		}

		player.addChatMessage(new ChatComponentText("Elevator stop finalized: " + sizeX + "x" + sizeZ));
	}

	/**
	 * Helper: collect all connected ESFB blocks via breadth-first search.
	 * Only considers horizontal neighbors (X and Z axes at same Y level).
	 */
	@SuppressWarnings("unchecked")
	private ArrayList<int[]> collectCluster(World world, int sx, int sy, int sz) {
		ArrayList<int[]> out = new ArrayList<int[]>();
		Set<String> visited = new HashSet<String>();
		ArrayDeque<int[]> q = new ArrayDeque<int[]>();

		q.add(new int[] {sx, sy, sz});
		visited.add(sx + "," + sy + "," + sz);

		while(!q.isEmpty()) {
			int[] p = q.poll();
			int x = p[0], y = p[1], z = p[2];

			if(!isFrameBlock(world, x, y, z)) continue;

			out.add(new int[] {x, y, z});

			int[][] dirs = new int[][] {
				{ 1, 0, 0}, {-1, 0, 0},
				{ 0, 0, 1}, { 0, 0,-1}
			};

			for(int[] d : dirs) {
				int nx = x + d[0];
				int ny = y + d[1];
				int nz = z + d[2];
				String k = nx + "," + ny + "," + nz;
				if(visited.contains(k)) continue;
				visited.add(k);
				q.add(new int[] {nx, ny, nz});
			}
		}

		return out;
	}
}
