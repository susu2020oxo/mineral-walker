"""Boot the built mod, inspect persisted blocks/entities across every habitat."""
import io,struct,sys,gzip,zlib,time,subprocess
from pathlib import Path

run=Path('build/run')
(run/'eula.txt').write_text('eula=true\n')
(run/'server.properties').write_text('online-mode=false\nserver-ip=127.0.0.1\nlevel-seed=24867713\nview-distance=3\nsimulation-distance=3\npause-when-empty-seconds=-1\n')
log=run/'test.log'
def wait_line(s,timeout=100):
    deadline=time.monotonic()+timeout
    while time.monotonic()<deadline:
        if s in log.read_text(errors='replace'): return
        if p.poll() is not None: break
        time.sleep(.4)
    raise RuntimeError('Server never reported '+s)
def command(s):
    p.stdin.write(s+'\n');p.stdin.flush()
with log.open('w') as output:
    p=subprocess.Popen(['java','-Xmx2400M','-XX:ActiveProcessorCount=4','-jar','fabric.jar','nogui'],cwd=run,stdin=subprocess.PIPE,stdout=output,stderr=subprocess.STDOUT,text=True)
    try:
        wait_line('Done (',150)
        command('tick freeze')
        for i in range(7):
            x=i*16000
            command(f'forceload add {x} 0 {x+63} 31')
        command('forceload add -8032 0 -7969 31')
        command('execute in minecraft:the_nether run forceload add 0 0 63 31')
        checks={'VILLAGE':'if block 8 65 8 minecraft:oak_log','DESERT':'if block 16008 65 8 minecraft:cactus','MUSHROOM':'if block 32009 65 8 minecraft:mushroom_stem','PIG':'if block 48020 65 19 minecraft:crafting_table','POOL':'if block 64015 65 15 minecraft:chest','TEMPLE':'if block 80000 64 0 minecraft:sand','DARK':'if block 96014 65 13 minecraft:sculk_shrieker[can_summon=true]','EDGE':'if block -8032 64 0 minecraft:sculk if block -8000 64 0 minecraft:grass_block','NETHER':'in minecraft:the_nether if block 8 54 8 minecraft:netherrack'}
        for _ in range(65):
            for name,criteria in checks.items(): command('execute '+criteria+' run say PASS_'+name)
            time.sleep(1)
            logtext=log.read_text(errors='replace')
            if all('PASS_'+k in logtext for k in checks):break
        else:raise RuntimeError('Missing checkpoints '+str([k for k in checks if 'PASS_'+k not in logtext]))
        command('save-all flush');wait_line('Saved the game')
        command('stop');p.wait(timeout=120)
    except BaseException:
        p.kill();print(log.read_text(errors='replace')[-14000:]);raise

def nbt(stream):
    def number(fmt):return struct.unpack('>'+fmt,stream.read(struct.calcsize('>'+fmt)))[0]
    def string():return stream.read(number('H')).decode(errors='replace')
    def value(t):
        if t in range(1,7):return number(['','b','h','i','q','f','d'][t])
        if t in (7,11,12):return [number({7:'b',11:'i',12:'q'}[t]) for _ in range(number('i'))]
        if t==8:return string()
        if t==9:
            kind=number('B')
            return [value(kind) for _ in range(number('i'))]
        if t==10:
            data={}
            while (kind:=number('B')):
                key=string();data[key]=value(kind)
            return data
        raise ValueError(t)
    kind=number('B');string()
    return value(kind)
def chunk(x,z,kind='region',dim='overworld'):
    folder=run/'world/dimensions/minecraft'/dim/kind
    path=folder/f'r.{x//32}.{z//32}.mca'
    if not path.exists():return {}
    with path.open('rb') as st:
        st.seek(4*(x%32+32*(z%32)));off=int.from_bytes(st.read(3),'big')
        if not off:return {}
        st.seek(4096*off);size=int.from_bytes(st.read(4),'big');codec=st.read(1);buf=st.read(size-1)
        return nbt(io.BytesIO(zlib.decompress(buf) if codec==b'\x02' else gzip.decompress(buf)))
def name(s):return s if isinstance(s,str) else s.get('id',s.get('',''))
def state(c,x,y,z):
    sec=next(s for s in c['sections'] if s['Y']==y//16)
    blocks=sec.get('block_states',{'palette':['minecraft:air']})
    pal=blocks['palette']
    if len(pal)==1:return name(pal[0])
    bits=max(4,(len(pal)-1).bit_length());per=64//bits
    idx=(y%16)*256+(z%16)*16+x%16
    return name(pal[(blocks['data'][idx//per]>>((idx%per)*bits))&((1<<bits)-1)])
for i in range(7):
    tile={};animals=[];contents=[]
    for dx in range(2):
        for dz in range(2):
            cx=i*1000+dx
            c=chunk(cx,dz)
            assert c['Status']=='minecraft:full'
            tile[dx,dz]=c
            contents+=c.get('block_entities',[])
            animals+=chunk(cx,dz,'entities').get('Entities',[])
            repeated=chunk(cx+2,dz)
            assert state(c,2,-23,3)==state(repeated,2,-23,3)
            assert state(c,2,64,2)==state(repeated,2,64,2)
    def at(x,y,z):return state(tile[x//16,z//16],x,y,z)
    ids=[e.get('id') for e in animals]
    if i==0:assert ids.count('minecraft:villager')==1 and any(at(x,y,z)=='minecraft:oak_door' for x in range(18,31) for z in range(3,16) for y in range(65,72))
    if i==1:assert any('door' in at(x,y,z) for x in range(18,31) for z in range(3,16) for y in range(65,72))
    if i==2:assert ids.count('minecraft:mooshroom')>=1 and at(9,70,8)=='minecraft:red_mushroom_block'
    if i==3:assert any(e.get('id')=='minecraft:pig' and (e.get('NoAI') or e.get('no_ai')) for e in animals),str(animals)[:5000]
    if i==4:assert sum(at(x,64,z)=='minecraft:water' for x in range(32) for z in range(32))==4 and len([b for b in contents if b.get('id')=='minecraft:chest'])==1
    if i==5:
        assert len([b for b in contents if b.get('id')=='minecraft:chest'])==4
        assert any(at(x,y,z)=='minecraft:tnt' for x in range(10,22) for z in range(10,22) for y in range(44,63))
        assert any(at(x,y,z)=='minecraft:stone_pressure_plate' for x in range(10,22) for z in range(10,22) for y in range(44,63))
    print('Verified 32×32 exhibit',i,flush=True)
