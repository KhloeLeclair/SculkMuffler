package dev.khloeleclair.skulkmuffler.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.khloeleclair.skulkmuffler.client.models.MufflerBlockModel;
import dev.khloeleclair.skulkmuffler.common.blockentities.MufflerBlockEntity;
import dev.khloeleclair.skulkmuffler.common.utilities.Constants;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class MufflerBlockEntityRenderer extends GeoBlockRenderer<MufflerBlockEntity> {

    public MufflerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(new MufflerBlockModel());
    }

    public static final float SIDE_OFF = 0.005f;
    public static final float MID_OFF = 0.010f;
    public static final float SIDE_BLOCK = 1f - SIDE_OFF;
    public static final float MID_BLOCK = 1f - MID_OFF;

    private int cR;
    private int cG;
    private int cB;

    private void setColor(int packed) {
        cR = FastColor.ARGB32.red(packed);
        cG = FastColor.ARGB32.green(packed);
        cB = FastColor.ARGB32.blue(packed);
    }

    private void AddVertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z) {
        consumer.addVertex(pose, x, y, z).setColor(cR, cG, cB, 50);
    }

    @Override
    public boolean shouldRenderOffScreen(MufflerBlockEntity blockEntity) {
        return blockEntity.getDebug() != -1;
    }

    @Override
    public @NotNull AABB getRenderBoundingBox(MufflerBlockEntity block) {
        if (block.getDebug() != -1) {
            final var center = block.getCenter();
            final float x = center.getX();
            final float y = center.getY();
            final float z = center.getZ();
            final int radius = Math.max(1, block.getRange());

            return new AABB(x - radius, y - radius, z - radius, x + radius + 1, y + radius + 1, z + radius + 1);
        }

        return super.getRenderBoundingBox(block);
    }

    public void renderDebug(@NotNull MufflerBlockEntity block, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource) {
        poseStack.pushPose();

        var pose = poseStack.last().pose();

        final var pos = block.getBlockPos();
        final var center = block.getCenter();
        final float x = center.getX() - pos.getX();
        final float y = center.getY() - pos.getY();
        final float z = center.getZ() - pos.getZ();
        final int radius = block.getRange();

        // First, the quads
        var consumer = bufferSource.getBuffer(RenderType.DEBUG_QUADS);

        // Bottom and Top (negative/positive y)
        for(int dx = -radius; dx <= radius; dx++) {
            for(int dz = -radius; dz <= radius; dz++) {
                // Bottom
                AddVertex(consumer, pose, x + dx + MID_OFF, y - radius + SIDE_OFF, z + dz + MID_OFF);
                AddVertex(consumer, pose, x + dx + MID_OFF, y - radius + SIDE_OFF, z + dz + MID_BLOCK);
                AddVertex(consumer, pose, x + dx + MID_BLOCK, y - radius + SIDE_OFF, z + dz + MID_BLOCK);
                AddVertex(consumer, pose, x + dx + MID_BLOCK, y - radius + SIDE_OFF, z + dz + MID_OFF);

                // Top
                AddVertex(consumer, pose, x + dx + MID_OFF, y + radius + SIDE_BLOCK, z + dz + MID_OFF);
                AddVertex(consumer, pose, x + dx + MID_OFF, y + radius + SIDE_BLOCK, z + dz + MID_BLOCK);
                AddVertex(consumer, pose, x + dx + MID_BLOCK, y + radius + SIDE_BLOCK, z + dz + MID_BLOCK);
                AddVertex(consumer, pose, x + dx + MID_BLOCK, y + radius + SIDE_BLOCK, z + dz + MID_OFF);
            }
        }

        // North and South (negative/positive z)
        for(int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                // North
                AddVertex(consumer, pose, x + dx + MID_OFF, y + dy + MID_OFF, z - radius + SIDE_OFF);
                AddVertex(consumer, pose, x + dx + MID_OFF, y + dy + MID_BLOCK, z - radius + SIDE_OFF);
                AddVertex(consumer, pose, x + dx + MID_BLOCK, y + dy + MID_BLOCK, z - radius + SIDE_OFF);
                AddVertex(consumer, pose, x + dx + MID_BLOCK, y + dy + MID_OFF, z - radius + SIDE_OFF);

                // South
                AddVertex(consumer, pose, x + dx + MID_OFF, y + dy + MID_OFF, z + radius + SIDE_BLOCK);
                AddVertex(consumer, pose, x + dx + MID_OFF, y + dy + MID_BLOCK, z + radius + SIDE_BLOCK);
                AddVertex(consumer, pose, x + dx + MID_BLOCK, y + dy + MID_BLOCK, z + radius + SIDE_BLOCK);
                AddVertex(consumer, pose, x + dx + MID_BLOCK, y + dy + MID_OFF, z + radius + SIDE_BLOCK);
            }
        }

        // West and East (negative/positive x)
        for(int dz = -radius; dz <= radius; dz++) {
            for(int dy = -radius; dy <= radius; dy++) {
                // West
                AddVertex(consumer, pose, x - radius + SIDE_OFF, y + dy + MID_OFF, z + dz + MID_OFF);
                AddVertex(consumer, pose, x - radius + SIDE_OFF, y + dy + MID_OFF, z + dz + MID_BLOCK);
                AddVertex(consumer, pose, x - radius + SIDE_OFF, y + dy + MID_BLOCK, z + dz + MID_BLOCK);
                AddVertex(consumer, pose, x - radius + SIDE_OFF, y + dy + MID_BLOCK, z + dz + MID_OFF);

                // East
                AddVertex(consumer, pose, x + radius + SIDE_BLOCK, y + dy + MID_OFF, z + dz + MID_OFF);
                AddVertex(consumer, pose, x + radius + SIDE_BLOCK, y + dy + MID_OFF, z + dz + MID_BLOCK);
                AddVertex(consumer, pose, x + radius + SIDE_BLOCK, y + dy + MID_BLOCK, z + dz + MID_BLOCK);
                AddVertex(consumer, pose, x + radius + SIDE_BLOCK, y + dy + MID_BLOCK, z + dz + MID_OFF);
            }
        }

        poseStack.popPose();
    }

    @Override
    public void render(
            @NotNull MufflerBlockEntity block,
            float partialTick,
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        super.render(block, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        int debug = block.getDebug();
        if (debug != -1) {
            setColor(Constants.AREAS[debug]);
            renderDebug(block, poseStack, bufferSource);
        }
    }

    @Override
    protected Direction getFacing(MufflerBlockEntity block) {
        var state = block.getBlockState();
        return switch (state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE)) {
            case CEILING -> Direction.DOWN;
            case WALL -> state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING);
            default -> Direction.UP;
        };
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        switch(facing) {
            case NORTH:
                poseStack.rotateAround(Axis.XP.rotationDegrees(270), 0f, 0.5f, 0f);
                break;
            case SOUTH:
                poseStack.rotateAround(Axis.XP.rotationDegrees(90), 0f, 0.5f, 0f);
                break;
            case EAST:
                poseStack.rotateAround(Axis.ZP.rotationDegrees(270), 0f, 0.5f, 0f);
                break;
            case WEST:
                poseStack.rotateAround(Axis.ZP.rotationDegrees(90), 0f, 0.5f, 0f);
                break;
            case DOWN:
                poseStack.rotateAround(Axis.XP.rotationDegrees(180), 0f, 0.5f, 0f);
                break;
            case UP:
            default:
                poseStack.mulPose(Axis.YP.rotationDegrees(0));
                break;
        }
    }
}
