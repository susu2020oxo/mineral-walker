package terrarium.mixin;

import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.*;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import terrarium.Boxes;

@Mixin(ChunkStatusTasks.class)
abstract class Generation {
    @Inject(method={"generateBiomes","buildTerrain","generateFeatures"}, at=@At("HEAD"), cancellable=true)
    private static void terrarium$generate(WorldGenContext ctx, ChunkStep step, StaticCache2D<GenerationChunkHolder> holders, ChunkAccess c, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cb) {
        if (!Boxes.enabled(ctx)) return;
        if (step.targetStatus()==ChunkStatus.BIOMES) Boxes.biomes(ctx,c);
        else if (step.targetStatus()==ChunkStatus.TERRAIN) Boxes.terrain(ctx,c);
        else Boxes.decorate(ctx,new WorldGenRegion(ctx.level(),holders,step,c),c);
        cb.setReturnValue(CompletableFuture.completedFuture(c));
    }

    @Inject(method="generateStructureStarts",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;onStructureStartsAvailable(Lnet/minecraft/world/level/chunk/ChunkAccess;)V"))
    private static void terrarium$spawnZones(WorldGenContext ctx,ChunkStep step,StaticCache2D<GenerationChunkHolder> cache,ChunkAccess c,CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cb) {
        if (!Boxes.enabled(ctx)) return;
        var zones=new java.util.HashMap<>(c.getAllStarts());
        zones.keySet().removeIf(s -> s.spawnOverrides().isEmpty());
        c.setAllStarts(zones);
    }
}
