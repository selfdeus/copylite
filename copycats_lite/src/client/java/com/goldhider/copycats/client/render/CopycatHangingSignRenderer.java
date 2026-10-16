package com.goldhider.copycats.client.render;

import com.goldhider.copycats.block.CopycatHangingSignBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Dibuja solo el texto del letrero colgante copycat (la tabla y las cadenas son parte del modelo del bloque).
 * Basado en el renderizador de texto de los letreros colgantes de vanilla.
 */
public final class CopycatHangingSignRenderer implements BlockEntityRenderer<CopycatHangingSignBlockEntity, CopycatSignRenderState> {
    private static final float TEXT_RENDER_SCALE = 0.9F;
    private static final Vector3f TEXT_OFFSET = new Vector3f(0.1F / 16F, -5.12F / 16F, 1.024F / 16F);

    private final Font font;

    public CopycatHangingSignRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.font();
    }

    /** El contorno del texto brillante solo se dibuja a menos de 16 bloques de la cámara (como en vanilla). */
    private static boolean isOutlineVisible(CopycatHangingSignBlockEntity blockEntity) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        return camera != null && camera.distanceToSqr(Vec3.atCenterOf(blockEntity.getBlockPos())) < 256.0D;
    }

    @Override
    public CopycatSignRenderState createRenderState() {
        return new CopycatSignRenderState();
    }

    @Override
    public void extractRenderState(
            CopycatHangingSignBlockEntity blockEntity,
            CopycatSignRenderState renderState,
            float partialTick,
            Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPos, crumblingOverlay);

        BlockState state = blockEntity.getBlockState();
        if (!(state.getBlock() instanceof SignBlock signBlock)) {
            return;
        }

        renderState.yRot = -signBlock.getYRotationDegrees(state);
        renderState.frontText = blockEntity.getSideText(true);
        renderState.backText = blockEntity.getSideText(false);
        renderState.lineHeight = blockEntity.getTextLineHeight();
        renderState.lineWidth = blockEntity.getMaxTextLineWidth();
        renderState.outline = isOutlineVisible(blockEntity);
    }

    @Override
    public void submit(CopycatSignRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (renderState.frontText == null || renderState.backText == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.9375D, 0.5D);
        poseStack.mulPose(new Matrix4f().rotationY((float) Math.toRadians(renderState.yRot)));
        poseStack.translate(0.0F, -0.3125F, 0.0F);
        renderText(renderState.frontText, poseStack, submitNodeCollector, renderState, true);
        renderText(renderState.backText, poseStack, submitNodeCollector, renderState, false);
        poseStack.popPose();
    }

    private void renderText(SignText text, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CopycatSignRenderState renderState, boolean front) {
        int lineHeight = renderState.lineHeight;
        int lineWidth = renderState.lineWidth;

        poseStack.pushPose();
        if (!front) {
            poseStack.mulPose(new Matrix4f().rotationY((float) Math.PI));
        }
        poseStack.translate(TEXT_OFFSET.x, TEXT_OFFSET.y, TEXT_OFFSET.z);
        float scale = 0.015625F * TEXT_RENDER_SCALE;
        poseStack.scale(scale, -scale, scale);

        int darkColor = AbstractSignRenderer.getDarkColor(text);
        int textColor;
        boolean outline;
        int textLight;
        if (text.hasGlowingText()) {
            textColor = text.getColor().getTextColor();
            outline = textColor == DyeColor.BLACK.getTextColor() || renderState.outline;
            textLight = LightCoordsUtil.FULL_BRIGHT;
        } else {
            textColor = darkColor;
            outline = false;
            textLight = renderState.lightCoords;
        }

        boolean filter = Minecraft.getInstance().isTextFilteringEnabled();
        FormattedCharSequence[] lines = text.getRenderMessages(filter, line -> {
            List<FormattedCharSequence> parts = font.split(line, lineWidth);
            return parts.isEmpty() ? FormattedCharSequence.EMPTY : parts.getFirst();
        });

        int centerY = 4 * lineHeight / 2;
        for (int idx = 0; idx < 4; ++idx) {
            FormattedCharSequence line = lines[idx];
            float textX = (float) -font.width(line) / 2;
            float textY = idx * lineHeight - centerY;
            submitNodeCollector.submitText(poseStack, textX, textY, line, false, Font.DisplayMode.POLYGON_OFFSET, textLight, textColor, 0, outline ? darkColor : 0);
        }

        poseStack.popPose();
    }
}
