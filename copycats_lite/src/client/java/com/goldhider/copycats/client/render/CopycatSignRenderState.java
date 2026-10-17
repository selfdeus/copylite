package com.goldhider.copycats.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.SignText;
import org.jspecify.annotations.Nullable;

public final class CopycatSignRenderState extends BlockEntityRenderState {
    public float yRot;
    @Nullable
    public SignText frontText;
    @Nullable
    public SignText backText;
    public int lineHeight;
    public int lineWidth;
    public boolean outline;
}
