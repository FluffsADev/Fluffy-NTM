package com.hbm.items.tool;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hbm.blocks.machine.BlockCustomElevatorStopFrame;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.machine.TileEntityCustomElevatorStop;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;


public class ItemCustomElevatorLinker extends Item {

	private static int nextSystemId = 1;

	private static final String SEL_X = "esf_sel_x";
	private static final String SEL_Y = "esf_sel_y";
	private static final String SEL_Z = "esf_sel_z";

	private static final String BRK_TIME = "esf_break_time";
	private static final String BRK_X = "esf_break_x";
	private static final String BRK_Y = "esf_break_y";
	private static final String BRK_Z = "esf_break_z";

	public ItemCustomElevatorLinker() {
		this.setMaxStackSize(1);
	}


	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hx, float hy, float hz) {
		Block b = world.getBlock(x, y, z);
		if(!(b instanceof BlockCustomElevatorStopFrame)) return false;

		TileEntity baseTe = world.getTileEntity(x, y, z);

		if(player.isSneaking()) {
			if(!(baseTe instanceof TileEntityCustomElevatorStop)) return false;
			if(world.isRemote) return true;

			if(stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();

			long now = world.getTotalWorldTime();
			long last = stack.stackTagCompound.getLong(BRK_TIME);
			int lx = stack.stackTagCompound.getInteger(BRK_X);
			int ly = stack.stackTagCompound.getInteger(BRK_Y);
			int lz = stack.stackTagCompound.getInteger(BRK_Z);

			if(lx == x && ly == y && lz == z && (now - last) <= 30) {
				breakStop(world, x, y, z, player);
				stack.stackTagCompound.removeTag(BRK_TIME);
			} else {
				stack.stackTagCompound.setLong(BRK_TIME, now);
				stack.stackTagCompound.setInteger(BRK_X, x);
				stack.stackTagCompound.setInteger(BRK_Y, y);
				stack.stackTagCompound.setInteger(BRK_Z, z);
				player.addChatMessage(new ChatComponentText("Shift-right click again to dismantle ESF."));
			}

			return true;
		}

		if(!(baseTe instanceof TileEntityCustomElevatorStop)) return false;
		if(world.isRemote) return true;

		TileEntityCustomElevatorStop clicked = getStopAt(world, x, y, z);
		if(clicked == null) return false;

		if(stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();

		int rx = clicked.getRepX();
		int ry = clicked.getRepY();
		int rz = clicked.getRepZ();

		if(!hasSelection(stack.stackTagCompound)) {
			setSelection(stack.stackTagCompound, rx, ry, rz);
			player.addChatMessage(new ChatComponentText("ESF selected at " + rx + ", " + ry + ", " + rz + ". Right-click another ESF to connect/disconnect."));
			return true;
		}

		int sx = stack.stackTagCompound.getInteger(SEL_X);
		int sy = stack.stackTagCompound.getInteger(SEL_Y);
		int sz = stack.stackTagCompound.getInteger(SEL_Z);

		if(sx == rx && sy == ry && sz == rz) {
			clearSelection(stack.stackTagCompound);
			player.addChatMessage(new ChatComponentText("Selection cleared."));
			return true;
		}

		TileEntityCustomElevatorStop first = getStopByRep(world, sx, sy, sz);
		TileEntityCustomElevatorStop second = getStopByRep(world, rx, ry, rz);
		if(first == null || second == null) {
			clearSelection(stack.stackTagCompound);
			player.addChatMessage(new ChatComponentText("ESF link error: one stop is no longer valid."));
			return true;
		}

		int[] firstRep = first.getRepPos();
		int[] secondRep = second.getRepPos();

		if(first.isLinkedTo(secondRep[0], secondRep[1], secondRep[2])) {
			first.removeLink(secondRep[0], secondRep[1], secondRep[2]);
			second.removeLink(firstRep[0], firstRep[1], firstRep[2]);
			normalizeFrom(first);
			normalizeFrom(second);
			player.addChatMessage(new ChatComponentText("ESF link removed. Stops now: " + collectComponentKeys(first).size()));
			clearSelection(stack.stackTagCompound);
			return true;
		}

		Set<String> a = collectComponentKeys(first);
		Set<String> bKeys = collectComponentKeys(second);
		boolean sameSystem = a.contains(key(secondRep[0], secondRep[1], secondRep[2]));

		if(!sameSystem && (a.size() + bKeys.size()) > 9) {
			player.addChatMessage(new ChatComponentText("ESF link error: a system can have at most 9 stops."));
			clearSelection(stack.stackTagCompound);
			return true;
		}

		if(!sameSystem && hasPlatformConflict(first, second)) {
			player.addChatMessage(new ChatComponentText("ESF link error: both systems already have a platform."));
			clearSelection(stack.stackTagCompound);
			return true;
		}

		first.addLink(secondRep[0], secondRep[1], secondRep[2]);
		second.addLink(firstRep[0], firstRep[1], firstRep[2]);
		normalizeFrom(first);
		player.addChatMessage(new ChatComponentText("ESF linked. Stops in system: " + collectComponentKeys(first).size()));
		clearSelection(stack.stackTagCompound);
		return true;
	}


	private void breakStop(World world, int x, int y, int z, EntityPlayer player) {
		TileEntity te = world.getTileEntity(x, y, z);
		if(!(te instanceof TileEntityCustomElevatorStop)) return;

		TileEntityCustomElevatorStop stop = (TileEntityCustomElevatorStop) te;
		int[] rep = stop.getRepPos();

		List<TileEntityCustomElevatorStop> neighbors = new ArrayList<TileEntityCustomElevatorStop>();
		for(int[] link : stop.links) {
			TileEntityCustomElevatorStop other = stop.getLinkedStop(link);
			if(other != null) {

				other.removeLink(rep[0], rep[1], rep[2]);
				neighbors.add(other);
			}
		}

		for(int ix = stop.minX; ix <= stop.maxX; ix++) {
			for(int iz = stop.minZ; iz <= stop.maxZ; iz++) {
				if(world.getBlock(ix, y, iz) instanceof BlockCustomElevatorStopFrame) {
					world.removeTileEntity(ix, y, iz);
				}
			}
		}

		for(TileEntityCustomElevatorStop n : neighbors) {
			normalizeFrom(n);
		}

		player.addChatMessage(new ChatComponentText("ESF dismantled into ESFB blocks."));
	}


	public static TileEntityCustomElevatorStop getStopAt(World world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(x, y, z);
		if(!(te instanceof TileEntityCustomElevatorStop)) return null;
		return (TileEntityCustomElevatorStop) te;
	}


	public static TileEntityCustomElevatorStop getStopByRep(World world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(x, y, z);
		if(!(te instanceof TileEntityCustomElevatorStop)) return null;
		TileEntityCustomElevatorStop stop = (TileEntityCustomElevatorStop) te;
		return (stop.getRepX() == x && stop.getRepY() == y && stop.getRepZ() == z) ? stop : null;
	}


	public static Set<String> collectComponentKeys(TileEntityCustomElevatorStop start) {
		Set<String> out = new HashSet<String>();
		if(start == null) return out;
		for(TileEntityCustomElevatorStop s : collectComponentStops(start)) {
			int[] p = s.getRepPos();
			out.add(key(p[0], p[1], p[2]));
		}
		return out;
	}


	public static List<TileEntityCustomElevatorStop> collectComponentStops(TileEntityCustomElevatorStop start) {
		List<TileEntityCustomElevatorStop> out = new ArrayList<TileEntityCustomElevatorStop>();
		if(start == null) return out;

		Set<String> visited = new HashSet<String>();
		ArrayDeque<TileEntityCustomElevatorStop> q = new ArrayDeque<TileEntityCustomElevatorStop>();
		q.add(start);

		while(!q.isEmpty()) {
			TileEntityCustomElevatorStop s = q.poll();
			if(s == null || s.getWorldObj() == null) continue;

			int[] rep = s.getRepPos();
			String k = key(rep[0], rep[1], rep[2]);
			if(visited.contains(k)) continue;
			visited.add(k);
			out.add(s);

			for(int[] link : s.links) {
				TileEntityCustomElevatorStop other = s.getLinkedStop(link);
				if(other != null) q.add(other);
			}
		}

		return out;
	}


	public static void normalizeFrom(TileEntityCustomElevatorStop start) {
		List<TileEntityCustomElevatorStop> comp = collectComponentStops(start);
		if(comp.isEmpty()) return;

		Map<String, Integer> platforms = new HashMap<String, Integer>();
		for(TileEntityCustomElevatorStop s : comp) {
			if(s.platformType <= 0) continue;
			String pKey = key(s.platformX, s.platformY, s.platformZ);
			if(!platforms.containsKey(pKey)) {
				platforms.put(pKey, Integer.valueOf(s.platformType));
			}
		}

		String chosenPlatform = null;
		int chosenType = 0;
		for(String k : platforms.keySet()) {
			if(chosenPlatform == null || k.compareTo(chosenPlatform) < 0) {
				chosenPlatform = k;
				chosenType = platforms.get(k).intValue();
			}
		}

		int id = nextSystemId++;
		for(TileEntityCustomElevatorStop s : comp) {
			s.systemId = id;
			if(chosenPlatform != null) {
				int[] p = parseKey(chosenPlatform);
				s.setPlatform(chosenType, p[0], p[1], p[2]);
			} else {
				s.clearPlatform();
			}
			s.markDirty();
		}
	}


	public static boolean hasPlatformConflict(TileEntityCustomElevatorStop a, TileEntityCustomElevatorStop b) {
		String pa = findPlatformRepKey(a);
		String pb = findPlatformRepKey(b);
		return pa != null && pb != null && !pa.equals(pb);
	}


	public static String findPlatformRepKey(TileEntityCustomElevatorStop start) {
		for(TileEntityCustomElevatorStop s : collectComponentStops(start)) {
			if(s.platformType > 0) {
				return key(s.platformX, s.platformY, s.platformZ);
			}
		}
		return null;
	}


	public static List<int[]> findRoute(TileEntityCustomElevatorStop start, TileEntityCustomElevatorStop target) {
		List<int[]> route = new ArrayList<>();
		if(start == null || target == null || start.getWorldObj() != target.getWorldObj()) return route;

		int[] sRep = start.getRepPos();
		int[] tRep = target.getRepPos();
		String sKey = key(sRep[0], sRep[1], sRep[2]);
		String tKey = key(tRep[0], tRep[1], tRep[2]);
		if(sKey.equals(tKey)) {
			route.add(sRep);
			return route;
		}

		Map<String, String> parent = new HashMap<String, String>();
		ArrayDeque<TileEntityCustomElevatorStop> q = new ArrayDeque<TileEntityCustomElevatorStop>();
		Set<String> visited = new HashSet<String>();

		q.add(start);
		visited.add(sKey);

		while(!q.isEmpty()) {
			TileEntityCustomElevatorStop cur = q.poll();
			int[] cRep = cur.getRepPos();
			String cKey = key(cRep[0], cRep[1], cRep[2]);

			for(int[] link : cur.links) {
				TileEntityCustomElevatorStop next = cur.getLinkedStop(link);
				if(next == null) continue;
				int[] nRep = next.getRepPos();
				String nKey = key(nRep[0], nRep[1], nRep[2]);
				if(visited.contains(nKey)) continue;
				visited.add(nKey);
				parent.put(nKey, cKey);
				if(nKey.equals(tKey)) {
					String walk = tKey;
					while(walk != null) {
						route.add(0, parseKey(walk));
						walk = parent.get(walk);
					}
					return route;
				}
				q.add(next);
			}
		}

		return route;
	}


	private static boolean hasSelection(NBTTagCompound tag) {
		return tag.hasKey(SEL_X) && tag.hasKey(SEL_Y) && tag.hasKey(SEL_Z);
	}


	private static void setSelection(NBTTagCompound tag, int x, int y, int z) {
		tag.setInteger(SEL_X, x);
		tag.setInteger(SEL_Y, y);
		tag.setInteger(SEL_Z, z);
	}


	private static void clearSelection(NBTTagCompound tag) {
		tag.removeTag(SEL_X);
		tag.removeTag(SEL_Y);
		tag.removeTag(SEL_Z);
	}


	private static String key(int x, int y, int z) {
		return x + "," + y + "," + z;
	}

	private static int[] parseKey(String k) {
		String[] p = k.split(",");
		return new int[] {Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])};
	}


	@Override
	@SuppressWarnings("unchecked")
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean ext) {
		NBTTagCompound tag = stack.stackTagCompound;
		if(tag != null && hasSelection(tag)) {
			list.add("ESF selected: " + tag.getInteger(SEL_X) + ", " + tag.getInteger(SEL_Y) + ", " + tag.getInteger(SEL_Z));
			list.add("Right-click another ESF to connect/disconnect");
		} else {
			list.add("Right-click ESFB to finalize frame");
			list.add("Right-click ESF twice to link/unlink");
			list.add("Shift+Right-click ESF twice to dismantle");
		}
	}


	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean inHand) {
		if(!world.isRemote || stack.stackTagCompound == null || !hasSelection(stack.stackTagCompound)) return;

		Vec3 vec = Vec3.createVectorHelper(
			entity.posX - stack.stackTagCompound.getInteger(SEL_X),
			entity.posY - stack.stackTagCompound.getInteger(SEL_Y),
			entity.posZ - stack.stackTagCompound.getInteger(SEL_Z)
		);
		MainRegistry.proxy.displayTooltip(stack.getDisplayName() + ": selected ESF " + ((int) vec.lengthVector()) + "m", MainRegistry.proxy.ID_WRENCH);
	}
}
