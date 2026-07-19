package com.hbm.render.tileentity;

import org.lwjgl.opengl.GL11;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.TileEntityCustomElevatorStop;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

public class RenderCustomElevatorStop extends TileEntitySpecialRenderer {

	@Override
	public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float interp) {
		if(!(tile instanceof TileEntityCustomElevatorStop)) return;
		TileEntityCustomElevatorStop stop = (TileEntityCustomElevatorStop) tile;

		boolean atMinX = tile.xCoord == stop.minX;
		boolean atMaxX = tile.xCoord == stop.maxX;
		boolean atMinZ = tile.zCoord == stop.minZ;
		boolean atMaxZ = tile.zCoord == stop.maxZ;

		boolean isPerimeter = atMinX || atMaxX || atMinZ || atMaxZ;
		if(!isPerimeter) return;

		boolean isCorner = (atMinX || atMaxX) && (atMinZ || atMaxZ);

		GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5D, y, z + 0.5D);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glColor4f(1F, 1F, 1F, 1F);

		if(isCorner) {
			this.bindTexture(ResourceManager.mdl_frame_corner_tex);

			// push model outward so it sits outside footprint
			//double ox = atMinX ? -0.00125D : 0.00125D;
			//double oz = atMinZ ? -0.00125D : 0.00125D;
			//GL11.glTranslated(ox, 0D, oz);

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
	}
}
