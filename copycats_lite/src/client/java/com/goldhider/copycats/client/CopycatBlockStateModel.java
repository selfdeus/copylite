package com.goldhider.copycats.client;

import com.goldhider.copycats.client.assembly.CopycatRenderContext;
import com.goldhider.copycats.client.model.CopycatCores;
import com.goldhider.copycats.client.model.CopycatModelCore;
import com.goldhider.copycats.client.model.MeshRenderContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.blockgetter.v2.FabricBlockGetter;
import net.fabricmc.fabric.api.client.model.loading.v1.CustomUnbakedBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Modelo del copycat. Toma los quads del modelo del material copiado (o del modelo base si no hay material)
 * y, si el bloque tiene una forma ("shape"), los recorta y reensambla con el motor de piezas.
 *
 * Limitación conocida: el material no recibe tinte de bioma (césped, hojas).
 */
public class CopycatBlockStateModel implements BlockStateModel {
	private final BlockStateModel baseModel;
	private final String shape;
	@Nullable
	private final CopycatModelCore core;

	public CopycatBlockStateModel(BlockStateModel baseModel, String shape) {
		this.baseModel = baseModel;
		this.shape = shape;
		this.core = CopycatCores.get(shape);
	}

	/** Lee el material desde los datos de render (inmutables) de la block entity. */
	private static @Nullable BlockState getMaterial(BlockAndTintGetter level, BlockPos pos) {
		if (((FabricBlockGetter) level).getBlockEntityRenderData(pos) instanceof BlockState material) {
			return material;
		}
		return null;
	}

	private static BlockStateModel modelOf(BlockState state) {
		return Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest) {
		BlockState material = getMaterial(level, pos);
		BlockStateModel source = material == null ? baseModel : modelOf(material);
		BlockState sourceState = material == null ? state : material;

		if (core == null) {
			// Cubo completo: se emite el modelo de origen sin cambios.
			source.emitQuads(emitter, level, pos, sourceState, random, cullTest);
			return;
		}

		// Se capturan los quads del cubo de origen (sin descartar ninguno) y se reensamblan con la forma del bloque.
		MutableMesh mesh = Renderer.get().mutableMesh();
		source.emitQuads(mesh.emitter(), level, pos, sourceState, random, face -> false);

		CopycatRenderContext context = new MeshRenderContext(mesh, emitter, CopycatModelCore.MATERIAL_KEY);
		core.emitCopycatQuads(CopycatModelCore.MATERIAL_KEY, state, context, sourceState);
	}

	private record GeometryKey(Object sub, BlockState state) {
	}

	@Override
	@Nullable
	public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		BlockState material = getMaterial(level, pos);
		if (material == null) {
			return new GeometryKey(this, state);
		}

		Object subkey = modelOf(material).createGeometryKey(level, pos, material, random);
		if (subkey == null) {
			return null;
		}
		return new GeometryKey(subkey, state);
	}

	@Override
	public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		BlockState material = getMaterial(level, pos);
		if (material == null) {
			return baseModel.particleMaterial(level, pos, state);
		}
		return modelOf(material).particleMaterial(level, pos, material);
	}

	@Override
	@BakedQuad.MaterialFlags
	public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		@BakedQuad.MaterialFlags int flags = baseModel.materialFlags(level, pos, state, random);

		BlockState material = getMaterial(level, pos);
		if (material == null) {
			return flags;
		}
		return flags | modelOf(material).materialFlags(level, pos, material, random);
	}

	@Override
	public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
		// La Renderer API de Fabric hace esto innecesario: no se añaden partes.
	}

	@Override
	public Material.Baked particleMaterial() {
		return baseModel.particleMaterial();
	}

	@Override
	public @BakedQuad.MaterialFlags int materialFlags() {
		// Puede renderizar cualquier submodelo (animado o translúcido), así que se declara el caso general.
		return BakedQuad.FLAG_ANIMATED | BakedQuad.FLAG_TRANSLUCENT;
	}

	public record Unbaked(BlockStateModel.Unbaked baseModel, String shape) implements CustomUnbakedBlockStateModel {
		public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				BlockStateModel.Unbaked.CODEC.fieldOf("base_model").forGetter(Unbaked::baseModel),
				Codec.STRING.optionalFieldOf("shape", "block").forGetter(Unbaked::shape)
		).apply(instance, Unbaked::new));

		@Override
		public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
			return CODEC;
		}

		@Override
		public void resolveDependencies(Resolver resolver) {
			baseModel.resolveDependencies(resolver);
		}

		@Override
		public BlockStateModel bake(ModelBaker baker) {
			return new CopycatBlockStateModel(baseModel.bake(baker), shape);
		}
	}
}
