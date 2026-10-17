package com.goldhider.copycats.client;

import com.goldhider.copycats.CCRegistry;
import com.goldhider.copycats.CopycatsLite;
import com.goldhider.copycats.client.render.CopycatHangingSignRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.CustomUnbakedBlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class CopycatsLiteClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// El id debe coincidir con "fabric:type" en los blockstates JSON.
		CustomUnbakedBlockStateModel.register(CopycatsLite.id("copycat"), CopycatBlockStateModel.Unbaked.CODEC);
		CopycatTint.register();
		BlockEntityRenderers.register(CCRegistry.COPYCAT_HANGING_SIGN_BE_TYPE, CopycatHangingSignRenderer::new);
	}
}
