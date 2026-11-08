"""Overlay compiled nightmare classes and audio onto a release jar."""
import argparse
from pathlib import Path
import zipfile

p = argparse.ArgumentParser()
p.add_argument('base', type=Path)
p.add_argument('classes', type=Path)
p.add_argument('output', type=Path)
a = p.parse_args()
assets = Path(__file__).parent / 'assets'
add = {str(f.relative_to(a.classes)): f.read_bytes() for f in a.classes.rglob('*.class')}
add.update({str(f.relative_to(assets)): f.read_bytes() for f in assets.rglob('*') if f.is_file()})
assert len(add) == 5, sorted(add)
assert all(n.startswith(('com/idir/magiclives/nightmare/StrungDream',
                        'com/idir/magiclives/client/nightmare/StrungClient'))
           or n == 'assets/magiclives/sounds/nightmare_scream.ogg' for n in add)
with zipfile.ZipFile(a.base) as src, zipfile.ZipFile(a.output, 'w', zipfile.ZIP_DEFLATED) as dst:
    for info in src.infolist():
        dst.writestr(info, add.pop(info.filename, src.read(info.filename)))
    for name, data in add.items():
        dst.writestr(name, data)
