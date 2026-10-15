package com.goldhider.copycats.client.model;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.MeshView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.client.renderer.v1.sprite.SpriteFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Dibuja cajas con la textura del cubo copiado (valla, muro, viga, letrero...).
 * <p>
 * Cada cara de la caja toma el sprite, el tinte, el sombreado y el material de render de las caras del cubo de origen
 * que miran en la misma dirección (así también salen las capas extra, como el borde de césped), y se calcula
 * su UV por posición, igual que un modelo JSON normal: la textura queda alineada con el mundo.
 * <p>
 * Todas las coordenadas van en píxeles (0..16). Con {@link #yaw} las cajas se giran alrededor del centro del bloque.
 */
public final class BoxBuilder {
    private final MeshView source;
    private final QuadEmitter dest;
    private final Supplier<@Nullable MeshView> chainSource;
    private double yaw = 0.0;

    public BoxBuilder(MeshView source, QuadEmitter dest, Supplier<@Nullable MeshView> chainSource) {
        this.source = source;
        this.dest = dest;
        this.chainSource = chainSource;
    }

    private static SpriteFinder spriteFinder(QuadView quad) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(quad.atlas().getId()).spriteFinder();
    }

    /** Gira lo que se dibuje después, en grados alrededor del eje Y del centro del bloque (convención de Axis.YP). */
    public BoxBuilder yaw(double degrees) {
        this.yaw = degrees;
        return this;
    }

    /** Dibuja una caja completa salvo las caras indicadas (por ejemplo las que quedan dentro de otra pieza). */
    public void box(double x1, double y1, double z1, double x2, double y2, double z2, Direction... skip) {
        for (Direction dir : Direction.values()) {
            boolean skipped = false;
            for (Direction s : skip) {
                if (s == dir) {
                    skipped = true;
                    break;
                }
            }
            if (!skipped) {
                face(dir, x1, y1, z1, x2, y2, z2);
            }
        }
    }

    private void face(Direction dir, double x1, double y1, double z1, double x2, double y2, double z2) {
        double[][] verts = vertices(dir, x1, y1, z1, x2, y2, z2);
        Direction cull = yaw == 0.0 ? cullFor(dir, x1, y1, z1, x2, y2, z2) : null;

        source.forEach(src -> {
            if (src.lightFace() != dir) {
                return;
            }
            TextureAtlasSprite sprite = spriteFinder(src).find(src);
            dest.copyFrom(src);
            for (int i = 0; i < 4; i++) {
                double[] p = verts[i];
                dest.uv(i, sprite.getU((float) (u(dir, p) / 16.0)), sprite.getV((float) (v(dir, p) / 16.0)));
                place(i, p[0], p[1], p[2]);
            }
            dest.nominalFace(dir);
            dest.cullFace(cull);
            dest.emit();
        });
    }

    /**
     * Una cadena: dos planos cruzados (uno a +angle y otro a -angle) con la textura de la cadena de vanilla.
     * Cada plano usa una franja de 3 px de la textura: {@code slice} 0 = columna izquierda, 1 = derecha.
     */
    public void chainPlane(double cx, double y1, double y2, double angleDeg, int slice) {
        MeshView chain = chainSource.get();
        if (chain == null) {
            return;
        }
        double a = Math.toRadians(angleDeg);
        double dx = 1.5 * Math.cos(a);
        double dz = -1.5 * Math.sin(a);
        double[] max = {cx + dx, 8.0 + dz};
        double[] min = {cx - dx, 8.0 - dz};
        double uLo = slice * 3.0 / 16.0;
        double uHi = uLo + 3.0 / 16.0;
        double vBottom = (y2 - y1) / 16.0;

        boolean[] done = {false};
        chain.forEach(src -> {
            if (done[0]) {
                return;
            }
            done[0] = true;
            TextureAtlasSprite sprite = spriteFinder(src).find(src);

            // Cara delantera (normal hacia -Z local): de derecha a izquierda.
            emitPlaneQuad(src, sprite, Direction.NORTH,
                    max[0], y2, max[1], uLo, 0.0,
                    max[0], y1, max[1], uLo, vBottom,
                    min[0], y1, min[1], uHi, vBottom,
                    min[0], y2, min[1], uHi, 0.0);
            // Cara trasera (normal hacia +Z local).
            emitPlaneQuad(src, sprite, Direction.SOUTH,
                    min[0], y2, min[1], uLo, 0.0,
                    min[0], y1, min[1], uLo, vBottom,
                    max[0], y1, max[1], uHi, vBottom,
                    max[0], y2, max[1], uHi, 0.0);
        });
    }

    private void emitPlaneQuad(QuadView template, TextureAtlasSprite sprite, Direction nominal,
                               double x0, double y0, double z0, double u0, double v0,
                               double x1, double y1, double z1, double u1, double v1,
                               double x2, double y2, double z2, double u2, double v2,
                               double x3, double y3, double z3, double u3, double v3) {
        dest.copyFrom(template);
        double[][] pos = {{x0, y0, z0}, {x1, y1, z1}, {x2, y2, z2}, {x3, y3, z3}};
        double[][] uv = {{u0, v0}, {u1, v1}, {u2, v2}, {u3, v3}};
        for (int i = 0; i < 4; i++) {
            dest.uv(i, sprite.getU((float) uv[i][0]), sprite.getV((float) uv[i][1]));
            place(i, pos[i][0], pos[i][1], pos[i][2]);
        }
        dest.nominalFace(nominal);
        dest.cullFace(null);
        dest.emit();
    }

    private void place(int index, double x, double y, double z) {
        if (yaw != 0.0) {
            double a = Math.toRadians(yaw);
            double cos = Math.cos(a);
            double sin = Math.sin(a);
            double dx = x - 8.0;
            double dz = z - 8.0;
            x = 8.0 + dx * cos + dz * sin;
            z = 8.0 - dx * sin + dz * cos;
        }
        dest.pos(index, (float) (x / 16.0), (float) (y / 16.0), (float) (z / 16.0));
    }

    /** Solo se marca para culling la cara que está justo en el borde del bloque. */
    private static @Nullable Direction cullFor(Direction dir, double x1, double y1, double z1, double x2, double y2, double z2) {
        boolean onEdge = switch (dir) {
            case DOWN -> y1 == 0.0;
            case UP -> y2 == 16.0;
            case NORTH -> z1 == 0.0;
            case SOUTH -> z2 == 16.0;
            case WEST -> x1 == 0.0;
            case EAST -> x2 == 16.0;
        };
        return onEdge ? dir : null;
    }

    /** Vértices de la cara en el mismo orden que los modelos de vanilla. */
    private static double[][] vertices(Direction dir, double x1, double y1, double z1, double x2, double y2, double z2) {
        return switch (dir) {
            case DOWN -> new double[][]{{x1, y1, z2}, {x1, y1, z1}, {x2, y1, z1}, {x2, y1, z2}};
            case UP -> new double[][]{{x1, y2, z1}, {x1, y2, z2}, {x2, y2, z2}, {x2, y2, z1}};
            case NORTH -> new double[][]{{x2, y2, z1}, {x2, y1, z1}, {x1, y1, z1}, {x1, y2, z1}};
            case SOUTH -> new double[][]{{x1, y2, z2}, {x1, y1, z2}, {x2, y1, z2}, {x2, y2, z2}};
            case WEST -> new double[][]{{x1, y2, z1}, {x1, y1, z1}, {x1, y1, z2}, {x1, y2, z2}};
            case EAST -> new double[][]{{x2, y2, z2}, {x2, y1, z2}, {x2, y1, z1}, {x2, y2, z1}};
        };
    }

    /** UV por posición, como el UV automático de los modelos de vanilla (en 0..16). */
    private static double u(Direction dir, double[] p) {
        return switch (dir) {
            case DOWN, UP, SOUTH -> p[0];
            case NORTH -> 16.0 - p[0];
            case WEST -> p[2];
            case EAST -> 16.0 - p[2];
        };
    }

    private static double v(Direction dir, double[] p) {
        return switch (dir) {
            case DOWN -> 16.0 - p[2];
            case UP -> p[2];
            default -> 16.0 - p[1];
        };
    }
}
