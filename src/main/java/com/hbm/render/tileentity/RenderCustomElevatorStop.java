package com.hbm.render.tileentity;

import org.lwjgl.opengl.GL11;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.TileEntityCustomElevatorStop;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

public class RenderCustomElevatorStop extends TileEntitySpecialRenderer {

	private TileEntityCustomElevatorStop getPlatformAnchor(TileEntityCustomElevatorStop s) {
		if(s == null || s.getWorldObj() == null) return null;

		// 1) try local
		if(s.platformType > 0) {
			TileEntity te = s.getWorldObj().getTileEntity(s.platformX, s.platformY, s.platformZ);
			if(te instanceof TileEntityCustomElevatorStop) return (TileEntityCustomElevatorStop) te;
		}

		// 2) fallback: representative TE for this stop
		TileEntity repTe = s.getWorldObj().getTileEntity(s.getRepX(), s.getRepY(), s.getRepZ());
		if(repTe instanceof TileEntityCustomElevatorStop) {
			TileEntityCustomElevatorStop rep = (TileEntityCustomElevatorStop) repTe;
			if(rep.platformType > 0) {
				TileEntity te = s.getWorldObj().getTileEntity(rep.platformX, rep.platformY, rep.platformZ);
				if(te instanceof TileEntityCustomElevatorStop) return (TileEntityCustomElevatorStop) te;
			}
		}

		return null;
	}

	@Override
	public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float interp) {
		if(!(tile instanceof TileEntityCustomElevatorStop)) return;
		TileEntityCustomElevatorStop stop = (TileEntityCustomElevatorStop) tile;

		boolean atMinX = tile.xCoord == stop.minX;
		boolean atMaxX = tile.xCoord == stop.maxX;
		boolean atMinZ = tile.zCoord == stop.minZ;
		boolean atMaxZ = tile.zCoord == stop.maxZ;

		boolean isPerimeter = atMinX || atMaxX || atMinZ || atMaxZ;
		if(!isPerimeter) {
			renderPlatformDistributed(stop, x, y, z);
			return;
		}

		boolean isCorner = (atMinX || atMaxX) && (atMinZ || atMaxZ);

		GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5D, y, z + 0.5D);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glColor4f(1F, 1F, 1F, 1F);

		if(isCorner) {
			this.bindTexture(ResourceManager.mdl_frame_corner_tex);

			// rotate per quadrant
			if(atMaxX && atMinZ) GL11.glRotatef(90F, 0F, 1F, 0F);
			else if(atMinX && atMinZ) GL11.glRotatef(180F, 0F, 1F, 0F);
			else if(atMinX && atMaxZ) GL11.glRotatef(270F, 0F, 1F, 0F);

			ResourceManager.mdl_frame_corner.renderAll();
		} else {
			this.bindTexture(ResourceManager.mdl_frame_side_tex);

			if(atMinZ) {
				GL11.glRotatef(90F, 0F, 1F, 0F);
			} else if(atMaxZ) {
				GL11.glRotatef(270F, 0F, 1F, 0F);
			} else if(atMinX) {
				GL11.glRotatef(180F, 0F, 1F, 0F);
			}

			ResourceManager.mdl_frame_side.renderAll();
		}

		GL11.glPopMatrix();

		renderPlatformDistributed(stop, x, y, z);
	}

	private void renderPlatformDistributed(TileEntityCustomElevatorStop stop, double x, double y, double z) {
		TileEntityCustomElevatorStop anchor = getPlatformAnchor(stop);
		if(anchor == null) return;

		// only render piece that belongs to THIS TE's block coordinate
		int ix = stop.xCoord;
		int iz = stop.zCoord;

		if(ix < anchor.minX || ix > anchor.maxX || iz < anchor.minZ || iz > anchor.maxZ) return;

		boolean minX = ix == anchor.minX;
		boolean maxX = ix == anchor.maxX;
		boolean minZ = iz == anchor.minZ;
		boolean maxZ = iz == anchor.maxZ;

		int edges = (minX ? 1 : 0) + (maxX ? 1 : 0) + (minZ ? 1 : 0) + (maxZ ? 1 : 0);

		double yOff = 0.001D;

		GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5D, y + yOff, z + 0.5D);
		GL11.glColor4f(1F, 1F, 1F, 1F);

		if(edges >= 2) {
			bindTexture(ResourceManager.liftplatcorner_tex);
			if(maxX && minZ) GL11.glRotatef(180F, 0F, 1F, 0F);
			else if(maxX && maxZ) GL11.glRotatef(90F, 0F, 1F, 0F);
			else if(minX && minZ) GL11.glRotatef(270F, 0F, 1F, 0F);
			ResourceManager.liftplatcorner.renderAll();
		} else if(edges == 1) {
			bindTexture(ResourceManager.liftplatside_tex);
			if(minZ) GL11.glRotatef(270F, 0F, 1F, 0F);
			else if(maxZ) GL11.glRotatef(90F, 0F, 1F, 0F);
			else if(maxX) GL11.glRotatef(180F, 0F, 1F, 0F);
			ResourceManager.liftplatside.renderAll();
		} else {
			bindTexture(ResourceManager.liftplatmiddle_tex);
			ResourceManager.liftplatmiddle.renderAll();
		}

		GL11.glPopMatrix();
	}
}
