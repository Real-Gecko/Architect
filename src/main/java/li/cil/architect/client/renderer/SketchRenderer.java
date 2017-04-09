package li.cil.architect.client.renderer;

import li.cil.architect.api.BlueprintAPI;
import li.cil.architect.common.init.Items;
import li.cil.architect.common.item.ItemSketch;
import li.cil.architect.common.item.data.SketchData;
import li.cil.architect.util.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.stream.Stream;

import static li.cil.architect.client.renderer.OverlayRendererUtils.*;

public enum SketchRenderer {
    INSTANCE;

    private static final float SELECTION_GROWTH = 0.05f;

    @SubscribeEvent
    public void onWorldRender(final RenderWorldLastEvent event) {
        final Minecraft mc = Minecraft.getMinecraft();
        final EntityPlayer player = mc.player;

        final ItemStack stack = player.getHeldItem(EnumHand.MAIN_HAND);
        if (!Items.isSketch(stack)) {
            return;
        }

        final boolean hasRangeSelection = ItemSketch.hasRangeSelection(stack);
        final SketchData data = ItemSketch.getData(stack);
        final AxisAlignedBB potentialBounds = data.getPotentialBounds();

        final BlockPos hitPos;
        final RayTraceResult hit = mc.objectMouseOver;
        if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK) {
            hitPos = hit.getBlockPos();
        } else if (player.isSneaking() || hasRangeSelection) {
            hitPos = PlayerUtils.getLookAtPos(player);
        } else {
            hitPos = null;
        }

        doPositionPrologue(event);
        doOverlayPrologue();

        if (hitPos != null) {
            if (hasRangeSelection) {
                renderRangeSelection(player, ItemSketch.getRangeSelection(stack, hitPos));
            } else {
                renderBlockSelection(player, hitPos, potentialBounds);
            }
        }

        if (!data.isEmpty()) {
            GlStateManager.color(0.4f, 0.7f, 0.9f, 1f);
            renderCubeGrid(potentialBounds);

            final float dt = computeScaleOffset();

            GlStateManager.color(0.2f, 0.4f, 0.9f, 0.15f);
            renderBlocks(data.getBlocks(), dt);

            if (hitPos != null && data.isSet(hitPos)) {
                GlStateManager.color(0.2f, 0.4f, 0.9f, 0.3f);
                renderFocusHighlight(hitPos, dt);

                doWirePrologue();
                GlStateManager.color(0.2f, 0.4f, 0.9f, 0.5f);
                renderFocusHighlight(hitPos, dt);
                doWireEpilogue();
            }
        }

        doOverlayEpilogue();
        doPositionEpilogue();
    }

    private static void renderBlockSelection(final EntityPlayer player, final BlockPos hitPos, final AxisAlignedBB potentialBounds) {
        final AxisAlignedBB hitBounds = new AxisAlignedBB(hitPos);
        if (player.isSneaking()) {
            if (potentialBounds.intersectsWith(hitBounds)) {
                GlStateManager.color(0.2f, 0.9f, 0.4f, 0.5f);
            } else {
                GlStateManager.color(0.9f, 0.4f, 0.2f, 0.5f);
            }
            renderCubeWire(hitPos, MIN - SELECTION_GROWTH, MAX + SELECTION_GROWTH);
        } else {
            if (potentialBounds.intersectsWith(hitBounds) && BlueprintAPI.canSerialize(player.getEntityWorld(), hitPos)) {
                GlStateManager.color(0.2f, 0.9f, 0.4f, 0.5f);
            } else {
                GlStateManager.color(0.9f, 0.4f, 0.2f, 0.5f);
            }
            renderCube(hitPos, MIN - SELECTION_GROWTH, MAX + SELECTION_GROWTH);
        }
    }

    private static void renderRangeSelection(final EntityPlayer player, @Nullable final AxisAlignedBB rangeBounds) {
        if (rangeBounds == null) {
            return;
        }

        if (player.isSneaking()) {
            GlStateManager.color(0.9f, 0.4f, 0.2f, 0.2f);
        } else {
            GlStateManager.color(0.2f, 0.9f, 0.4f, 0.2f);
        }
        renderCube(rangeBounds);

        if (player.isSneaking()) {
            GlStateManager.color(0.9f, 0.4f, 0.2f, 0.7f);
        } else {
            GlStateManager.color(0.2f, 0.9f, 0.4f, 0.7f);
        }
        renderCubeWire(rangeBounds);
    }

    private static void renderBlocks(final Stream<BlockPos> blocks, final float dt) {
        final Tessellator t = Tessellator.getInstance();
        final VertexBuffer buffer = t.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

        blocks.forEach(pos -> drawCube(pos, buffer, dt));

        t.draw();
    }

    private static void renderFocusHighlight(final BlockPos hitPos, final float dt) {
        final Tessellator t = Tessellator.getInstance();
        final VertexBuffer buffer = t.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

        drawCube(hitPos, buffer, dt);

        t.draw();
    }
}