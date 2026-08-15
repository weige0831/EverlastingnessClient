#!/usr/bin/env python3
"""Rewrite the refmap JSON in <workdir> to obfuscated names for <ver>."""
import json, os, re, sys

workdir, mapdir, ver = sys.argv[1], sys.argv[2], sys.argv[3]
inter = json.load(open(os.path.join(mapdir, f"inter2obf-{ver}.json")))
cls, mem = inter["classes"], inter["members"]

def map_desc(desc):
    return re.sub(r"L([^;]+);", lambda m: "L%s;" % cls.get(m.group(1), m.group(1)), desc)

def map_class_ref(ref):
    m = re.match(r"^L([^;]+);$", ref)
    inner = m.group(1) if m else ref
    new = cls.get(inner, inner)
    return ("L%s;" % new) if m else new

def map_member_ref(ref):
    m = re.match(r"^(L[^;]+;)([^:(]+)(.*)$", ref)
    if not m:
        return ref
    owner, name, tail = m.group(1), m.group(2), m.group(3)
    newowner = map_class_ref(owner)
    mm = mem.get(name)
    return newowner + (mm[1] if mm else name) + map_desc(tail)

for fn in os.listdir(workdir):
    if fn.endswith(".json") and "refmap" in fn:
        path = os.path.join(workdir, fn)
        data = json.load(open(path))
        for mixin, entries in data.get("mappings", {}).items():
            ne = {}
            for k, v in entries.items():
                if re.match(r"^L[^;]+;[^:(]+", v):
                    nv = map_member_ref(v)
                elif v.startswith("L") or "/" in v:
                    nv = map_class_ref(v)
                else:
                    nv = v
                ne[k] = nv
            data["mappings"][mixin] = ne
        out = os.path.join(mapdir, f"refmap-obf-{ver}.json")
        json.dump(data, open(out, "w"))
        print(f"refmap-obf-{ver}.json written from {fn}")
        break
