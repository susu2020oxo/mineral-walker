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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.DesertPyramidPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

public final class Boxes {
    private static final int GROUND=64;
    private enum Scene { VILLAGE, DESERT, MUSHROOM, PIG, POOL, TEMPLE, DARK }
    public static boolean enabled(WorldGenContext ctx) {
        var d=ctx.level().dimension();
        return (d==Level.OVERWORLD || d==Level.NETHER) && ctx.generator() instanceof NoiseBasedChunkGenerator;
    }
    private static int region(ChunkPos p) {
        return Math.floorDiv(p.x()+500,1000)+3*Math.floorDiv(p.z()+500,1000);
    }
    private static Scene scene(ChunkPos p) { return Scene.values()[Math.floorMod(region(p),7)]; }
    private static boolean nether(WorldGenContext ctx) { return ctx.level().dimension()==Level.NETHER; }

    public static void biomes(WorldGenContext ctx,ChunkAccess c) {
        var key=nether(ctx)?switch(Math.floorMod(region(c.getPos()),3)) {
            case 1 -> Biomes.SOUL_SAND_VALLEY; case 2 -> Biomes.BASALT_DELTAS; default -> Biomes.NETHER_WASTES;
        }:switch(scene(c.getPos())) {
            case MUSHROOM -> Biomes.MUSHROOM_FIELDS; case DESERT,TEMPLE -> Biomes.DESERT; case DARK -> Biomes.DEEP_DARK; default -> Biomes.PLAINS;
        };
        var biome=ctx.level().registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(key);
        c.fillBiomesFromNoise((x,y,z)->biome);
    }

    public static void terrain(WorldGenContext ctx,ChunkAccess c) {
        boolean hell=nether(ctx);
        int pattern=hell?Math.floorMod(region(c.getPos()),3):scene(c.getPos()).ordinal();
        var surface=hell?switch(pattern){case 1->Blocks.SOUL_SOIL; case 2->Blocks.BLACKSTONE; default->Blocks.NETHERRACK;}
            :switch(scene(c.getPos())){case MUSHROOM->Blocks.MYCELIUM;case DESERT,TEMPLE->Blocks.SAND;case DARK->Blocks.SCULK;default->Blocks.GRASS_BLOCK;};
        int min=c.getMinY(), top=hell?Math.min(127,c.getMaxY()):GROUND;
        for (int y=min;y<=top;y++) {
            var section=c.getSection(c.getSectionIndex(y));
            for(int z=0;z<16;z++) for(int x=0;x<16;x++) {
                int u=(c.getPos().x()&1)*16+x, v=(c.getPos().z()&1)*16+z;
                int a=(u+pattern*3)&31, b=(v+pattern*5)&31;
                Block block;
                if (y==min || hell && y==top) block=Blocks.BEDROCK;
                else if (!hell && y==GROUND || hell && y==54) block=surface;
                else if (hell && y>54 && y<110) continue;
                else if (hell) {
                    if(y>16 && y<23 && b>8 && b<16) continue;
                    block=(a/3==3 && b/3==2 && y>=30 && y<33)?Blocks.NETHER_QUARTZ_ORE:
                          (a/3==5 && b/3==7 && y>=14 && y<16)?Blocks.NETHER_GOLD_ORE:
                          (a==3 && b==18 && y==14)?Blocks.ANCIENT_DEBRIS:Blocks.NETHERRACK;
                } else if(y>=GROUND-3) block=(surface==Blocks.SAND)?Blocks.SANDSTONE:(surface==Blocks.SCULK?Blocks.DEEPSLATE:Blocks.DIRT);
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
            case VILLAGE,DESERT -> {
                boolean desert=scene(p)==Scene.DESERT;
                if(west && north) {
                    if(desert) for(int h=0;h<3;h++) put(world,x+8,y+h,z+8,Blocks.CACTUS);
                    else tree(world,x+8,y,z+8);
                }
                if(!west && north) {
                    var start=new BlockPos(x+19,y,z+4);
                    String house=desert?"village/desert/houses/desert_small_house_1":"village/plains/houses/plains_small_house_1";
                    var template=ctx.structureManager().get(Identifier.withDefaultNamespace(house)).orElseThrow();
                    var opts=new StructurePlaceSettings().setKnownShape(true).setIgnoreEntities(true)
                        .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK).addProcessor(JigsawReplacementProcessor.INSTANCE);
                    template.placeInWorld(world,start,start,opts,RandomSource.create(747L),2);
                    if(!desert) resident(world,EntityTypes.VILLAGER,start.offset(3,1,3),false);
                }
            }
            case MUSHROOM -> {
                if(west && north) mushroom(world,x+9,y,z+8);
                if(!west && !north) resident(world,EntityTypes.MOOSHROOM,new BlockPos(x+21,y,z+21),false);
            }
            case PIG -> {
                if(west && north) tree(world,x+8,y,z+8);
                if(!west && !north) {
                    put(world,x+20,y,z+19,Blocks.CRAFTING_TABLE);
                    resident(world,EntityTypes.PIG,new BlockPos(x+23,y,z+20),true);
                }
            }
            case POOL -> {
                int u=west?15:16, v=north?15:16;
                put(world,x+u,GROUND,z+v,Blocks.WATER);
                if(west && north) put(world,x+15,y,z+15,Blocks.CHEST);
            }
            case TEMPLE -> {
                var pyramid=new DesertPyramidPiece(RandomSource.create(31L),x+5,z+5);
                var clip=new BoundingBox(p.getMinBlockX(),c.getMinY(),p.getMinBlockZ(),p.getMaxBlockX(),c.getMaxY(),p.getMaxBlockZ());
                pyramid.postProcess(world,ctx.level().structureManager().forWorldGenRegion(world),ctx.generator(),
                    RandomSource.create(417L),clip,p,new BlockPos(x+15,GROUND,z+15));
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

    private static void resident(WorldGenRegion world,EntityType<? extends Mob> type,BlockPos pos,boolean still) {
        var mob=type.create(world.getLevel(),EntitySpawnReason.STRUCTURE);
        if(mob==null) return;
        mob.snapTo(pos,90,0);
        mob.finalizeSpawn(world,world.getCurrentDifficultyAt(pos),EntitySpawnReason.STRUCTURE,null);
        mob.setNoAi(still);
        mob.setPersistenceRequired();
        world.addFreshEntity(mob);
    }

    private static void put(WorldGenRegion world,int x,int y,int z,Block block) {
        world.setBlock(new BlockPos(x,y,z),block.defaultBlockState(),2);
    }
    private static void tree(WorldGenRegion w,int x,int y,int z) {
        BlockState leaves=Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true);
        for(int h=2;h<=5;h++) {
            int radius=h<=3?2:1;
            for(int dx=-radius;dx<=radius;dx++) for(int dz=-radius;dz<=radius;dz++) {
                if(Math.abs(dx)==radius && Math.abs(dz)==radius && h!=3) continue;
                w.setBlock(new BlockPos(x+dx,y+h,z+dz),leaves,2);
            }
        }
        for(int h=0;h<5;h++) put(w,x,y+h,z,Blocks.OAK_LOG);
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
