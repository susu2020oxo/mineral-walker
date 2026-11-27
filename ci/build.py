"""Build against Mojang's 26.3 server and the exact libraries installed by Fabric."""
from pathlib import Path
from urllib.request import urlopen
from zipfile import ZipFile
import hashlib, json, os, subprocess

root = Path.cwd()
work = root / 'build'
run = work / 'run'
run.mkdir(parents=True, exist_ok=True)
def fetch(url): 
    with urlopen(url, timeout=120) as response: return response.read()
def meta(url): return json.loads(fetch(url))
manifest=meta('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json')
version=meta(next(v['url'] for v in manifest['versions'] if v['id']=='26.3'))
item=version['downloads']['server']
bundle=fetch(item['url'])
assert hashlib.sha1(bundle).hexdigest()==item['sha1']
server=run/'.fabric/server/26.3-server.jar'
server.parent.mkdir(parents=True,exist_ok=True)
server.write_bytes(bundle)
(run/'server.jar').write_bytes(bundle)
with ZipFile(server) as z:
    for path in z.namelist():
        if path.endswith('.jar') and path.startswith(('META-INF/libraries/','META-INF/versions/')):
            z.extract(path,work/'mc')
loader='0.20.1'
installer=meta('https://meta.fabricmc.net/v2/versions/installer')[0]['version']
launcher=run/'fabric.jar'
launcher.write_bytes(fetch(f'https://meta.fabricmc.net/v2/versions/loader/26.3/{loader}/{installer}/server/jar'))
subprocess.run(['java','-Xmx1G','-jar','fabric.jar','nogui'],cwd=run,check=True,timeout=240)
classpath=os.pathsep.join(str(p) for base in [work/'mc',run/'libraries'] for p in base.rglob('*.jar'))
out=work/'classes'
out.mkdir(exist_ok=True)
sources=[str(p) for p in (root/'src').rglob('*.java')]
subprocess.run(['javac','-encoding','UTF-8','-proc:none','-cp',classpath,'-d',str(out),*sources],check=True)
dist=root/'dist'
dist.mkdir(exist_ok=True)
jar=dist/'terrarium-26.3-fabric-1.1.0.jar'
subprocess.run(['jar','cf',str(jar),'-C','resources','.','-C',str(out),'.'],check=True)
(run/'mods').mkdir(exist_ok=True)
(run/'mods'/jar.name).write_bytes(jar.read_bytes())
