package terrarium;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.DesertPyramidPiece;
import net.minecraft.world.level.levelgen.structure.structures.JungleTemplePiece;
import net.minecraft.world.level.levelgen.structure.structures.EndCityPieces.EndCityPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

public final class Boxes {
    private static final int GROUND=64;
    private static final int REGION_CHUNKS = 500;
    private enum Scene { VILLAGE, ORES, MUSHROOM, OUTPOST, JUNGLE, TEMPLE, DARK, END_CITY }
    private static final Scene[] SCENES = Scene.values();
    private static final Block[] ORES = {Blocks.COAL_ORE, Blocks.COPPER_ORE, Blocks.IRON_ORE, Blocks.GOLD_ORE,
        Blocks.REDSTONE_ORE, Blocks.LAPIS_ORE, Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE};
    private static final Block[] DEEP_ORES = {Blocks.DEEPSLATE_COAL_ORE, Blocks.DEEPSLATE_COPPER_ORE,
        Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
        Blocks.DEEPSLATE_LAPIS_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DEEPSLATE_EMERALD_ORE};
    public static boolean enabled(WorldGenContext ctx) {
        var d=ctx.level().dimension();
        return (d==Level.OVERWORLD || d==Level.NETHER) && ctx.generator() instanceof NoiseBasedChunkGenerator;
    }
    private static int region(ChunkPos p) {
        return Math.floorDiv(p.x()+REGION_CHUNKS/2,REGION_CHUNKS)+3*Math.floorDiv(p.z()+REGION_CHUNKS/2,REGION_CHUNKS);
    }
    private static Scene scene(ChunkPos p) { return SCENES[Math.floorMod(region(p),SCENES.length)]; }
    private static boolean nether(WorldGenContext ctx) { return ctx.level().dimension()==Level.NETHER; }

    public static void biomes(WorldGenContext ctx,ChunkAccess c) {
        var key=nether(ctx)?switch(Math.floorMod(region(c.getPos()),3)) {
            case 1 -> Biomes.SOUL_SAND_VALLEY; case 2 -> Biomes.BASALT_DELTAS; default -> Biomes.NETHER_WASTES;
        }:switch(scene(c.getPos())) {
            case MUSHROOM -> Biomes.MUSHROOM_FIELDS; case TEMPLE -> Biomes.DESERT; case JUNGLE -> Biomes.JUNGLE; case ORES -> Biomes.STONY_PEAKS; case OUTPOST -> Biomes.TAIGA; case DARK -> Biomes.DEEP_DARK; default -> Biomes.PLAINS;
        };
        var biome=ctx.level().registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(key);
        c.fillBiomesFromNoise((x,y,z)->biome);
    }

    public static void terrain(WorldGenContext ctx,ChunkAccess c) {
        boolean hell=nether(ctx);
        Scene theme=scene(c.getPos());
        int pattern=hell?Math.floorMod(region(c.getPos()),3):theme.ordinal();
        var surface=hell?switch(pattern){case 1->Blocks.SOUL_SOIL; case 2->Blocks.BLACKSTONE; default->Blocks.NETHERRACK;}
            :switch(theme){
                case TEMPLE -> Blocks.SAND; case MUSHROOM -> Blocks.MYCELIUM; case DARK -> Blocks.SCULK;
                case END_CITY -> Blocks.END_STONE; case OUTPOST -> Blocks.COARSE_DIRT;
                case JUNGLE -> Blocks.MOSS_BLOCK; default -> Blocks.GRASS_BLOCK;
            };
        int min=c.getMinY(), top=hell?Math.min(127,c.getMaxY()):GROUND;
        for (int y=min;y<=top;y++) {
            var section=c.getSection(c.getSectionIndex(y));
            for(int z=0;z<16;z++) for(int x=0;x<16;x++) {
                int u=(c.getPos().x()&1)*16+x, v=(c.getPos().z()&1)*16+z;
                int a=(u+pattern*3)&31, b=(v+pattern*5)&31;
                Block block;
                if (y==min || hell && y==top) block=Blocks.BEDROCK;
                else if (!hell && theme==Scene.ORES) {
                    int index=((u/4) + 3*(v/4) + Math.floorDiv(GROUND-y,4)) & 7;
                    block=(y<0?DEEP_ORES:ORES)[index];
                }
                else if (!hell && y==GROUND || hell && y==54) block=surface;
                else if (hell && y>54 && y<110) continue;
                else if (hell) {
                    if(y>16 && y<23 && b>8 && b<16) continue;
                    block=(a/3==3 && b/3==2 && y>=30 && y<33)?Blocks.NETHER_QUARTZ_ORE:
                          (a/3==5 && b/3==7 && y>=14 && y<16)?Blocks.NETHER_GOLD_ORE:
                          (a==3 && b==18 && y==14)?Blocks.ANCIENT_DEBRIS:Blocks.NETHERRACK;
                } else if(y>=GROUND-3) block=(surface==Blocks.SAND)?Blocks.SANDSTONE:(theme==Scene.DARK?Blocks.DEEPSLATE:theme==Scene.END_CITY?Blocks.END_STONE:Blocks.DIRT);
                else {
                    int cy=-23+Math.abs(a-16)/4;
                    if ((b-16)*(b-16)+(y-cy)*(y-cy)<=16 || (a-8)*(a-8)+(y-16)*(y-16)<=12) continue;
                    block=y<0?Blocks.DEEPSLATE:Blocks.STONE;
                    if (a/3==4 && b/3==6 && y>=-48 && y<=-47) block=Blocks.DEEPSLATE_DIAMOND_ORE;
                    if (a/4==1 && b/4==2 && y>=10 && y<=12) block=Blocks.IRON_ORE;
                    if (a/4==5 && b/4==3 && y>=29 && y<=31) block=Blocks.COAL_ORE;
                    if (a/3==1 && b/3==3 && y>=-27 && y<=-25) block=Blocks.DEEPSLATE_REDSTONE_ORE;
                }
                section.setBlockState(x,y&15,z,block.defaultBlockState(),false);
            }
        }
        Heightmap.primeHeightmaps(c,EnumSet.allOf(Heightmap.Types.class));
    }

    public static void decorate(WorldGenContext ctx,WorldGenRegion world,ChunkAccess c) {
        if (nether(ctx)) return;
        ChunkPos p=c.getPos();
        int x=(p.x()&~1)*16, z=(p.z()&~1)*16, y=GROUND+1;
        boolean west=(p.x()&1)==0, north=(p.z()&1)==0;
        switch(scene(p)) {
            case VILLAGE -> {
                int px=p.getMinBlockX(), pz=p.getMinBlockZ();
                int lane=west?px+15:px;
                int cross=north?pz+15:pz;
                for (int i=0;i<16;i++) {
                    put(world,lane,GROUND,pz+i,Blocks.DIRT_PATH);
                    put(world,px+i,GROUND,cross,Blocks.DIRT_PATH);
                }
                var start=new BlockPos(px+5,y,pz+5);
                var rot=west?Rotation.CLOCKWISE_180:Rotation.NONE;
                int variant=1+(west?0:1)+(north?0:2);
                template(ctx,world,c,"village/plains/houses/plains_small_house_"+variant,start,rot,new BlockPos(3,0,3));
                for(int u=west?12:1;u<=(west?14:4);u++) put(world,px+u,GROUND,pz+8,Blocks.DIRT_PATH);
                resident(world,EntityTypes.VILLAGER,start.offset(3,1,3));
            }
            case MUSHROOM -> {
                if(west && north) mushroom(world,x+9,y,z+8);
                if(!west && !north) resident(world,EntityTypes.MOOSHROOM,new BlockPos(x+21,y,z+21));
            }
            case ORES -> { /* The deposit itself is the exhibit. */ }
            case TEMPLE -> piece(ctx,world,c,new DesertPyramidPiece(RandomSource.create(31L),x+5,z+5));
            case JUNGLE -> piece(ctx,world,c,new JungleTemplePiece(RandomSource.create(31L),x+9,z+9));
            case OUTPOST -> {
                template(ctx,world,c,"pillager_outpost/watchtower",new BlockPos(x+8,GROUND,z+8),
                    Rotation.NONE,BlockPos.ZERO);
                if (!west && !north) resident(world,EntityTypes.PILLAGER,new BlockPos(x+16,y,z+24));
            }
            case END_CITY -> {
                // The vanilla house floors overlap according to the End City assembly offsets.
                city(ctx,world,c,"base_floor",x+11,y,z+11,true);
                city(ctx,world,c,"second_floor_1",x+10,y,z+10,false);
                city(ctx,world,c,"third_floor_2",x+9,y+4,z+9,false);
                city(ctx,world,c,"third_roof",x+8,y+12,z+8,true);
                city(ctx,world,c,"tower_base",x+12,y+9,z+12,true);
                city(ctx,world,c,"tower_piece",x+12,y+16,z+12,true);
                city(ctx,world,c,"tower_piece",x+12,y+20,z+12,true);
                city(ctx,world,c,"tower_top",x+11,y+24,z+11,true);
            }
            case DARK -> {
                if(west && north) {
                    put(world,x+11,y,z+11,Blocks.SCULK_SENSOR);
                    world.setBlock(new BlockPos(x+14,y,z+13),Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON,true),2);
                }
                if(!west && !north) put(world,x+20,y,z+18,Blocks.SCULK_SENSOR);
            }
        }
    }

    private static BoundingBox clip(ChunkAccess c) {
        var p=c.getPos();
        return new BoundingBox(p.getMinBlockX(),c.getMinY(),p.getMinBlockZ(),p.getMaxBlockX(),c.getMaxY(),p.getMaxBlockZ());
    }
    private static void template(WorldGenContext ctx,WorldGenRegion world,ChunkAccess c,String id,BlockPos origin,
                                 Rotation rotation,BlockPos pivot) {
        var settings=new StructurePlaceSettings().setBoundingBox(clip(c)).setKnownShape(true).setIgnoreEntities(true)
            .setRotation(rotation).setRotationPivot(pivot).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
            .addProcessor(JigsawReplacementProcessor.INSTANCE);
        ctx.structureManager().get(Identifier.withDefaultNamespace(id)).orElseThrow()
            .placeInWorld(world,origin,origin,settings,RandomSource.create(747L),2);
    }
    private static void piece(WorldGenContext ctx,WorldGenRegion world,ChunkAccess c,StructurePiece piece) {
        var bounds=clip(c);
        if(piece.getBoundingBox().intersects(bounds))
            piece.postProcess(world,ctx.level().structureManager().forWorldGenRegion(world),ctx.generator(),
                RandomSource.create(417L),bounds,c.getPos(),piece.getBoundingBox().getCenter());
    }
    private static void city(WorldGenContext ctx,WorldGenRegion world,ChunkAccess c,String id,int x,int y,int z,boolean overwrite) {
        piece(ctx,world,c,new EndCityPiece(ctx.structureManager(),id,new BlockPos(x,y,z),Rotation.NONE,overwrite));
    }

    private static void resident(WorldGenRegion world,EntityType<? extends Mob> type,BlockPos pos) {
        var mob=type.create(world.getLevel(),EntitySpawnReason.STRUCTURE);
        if(mob==null) return;
        mob.snapTo(pos,90,0);
        mob.finalizeSpawn(world,world.getCurrentDifficultyAt(pos),EntitySpawnReason.STRUCTURE,null);
        mob.setPersistenceRequired();
        world.addFreshEntity(mob);
    }

    private static void put(WorldGenRegion world,int x,int y,int z,Block block) {
        world.setBlock(new BlockPos(x,y,z),block.defaultBlockState(),2);
    }
    private static void mushroom(WorldGenRegion w,int x,int y,int z) {
        for(int h=0;h<5;h++) put(w,x,y+h,z,Blocks.MUSHROOM_STEM);
        for(int h=2;h<=5;h++) for(int dx=-2;dx<=2;dx++) for(int dz=-2;dz<=2;dz++) {
            int ax=Math.abs(dx),az=Math.abs(dz);
            if(h==5?ax<=1 && az<=1:(ax==2 && az<2 || az==2 && ax<2))
                put(w,x+dx,y+h,z+dz,Blocks.RED_MUSHROOM_BLOCK);
        }
    }
}
