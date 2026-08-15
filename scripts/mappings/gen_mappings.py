#!/usr/bin/env python3
"""Per-version mapping generation: intermediary<->obf (from Loom's cached
intermediary-v2.tiny) + yarn->obf (composed with Loom's cached yarn mappings).

Outputs into the mappings dir next to this script:
  inter2obf-<ver>.json        intermediary class/member -> obfuscated
  yarn2obf-full-<ver>.json    yarn class names (dotted+slashed) -> obfuscated
  refmap-obf-<ver>.json       obf-rewritten refmap extracted from the jar
"""
import json, os, sys, zipfile

MAP_DIR = os.path.dirname(os.path.abspath(__file__))
LOOM = os.path.expanduser(r"~\.gradle\caches\fabric-loom")


def tiny_rows(path):
    """Yield rows from a tiny (v1 or v2) file, normalized to (kind, cols)."""
    with open(path, encoding="utf-8") as f:
        header = f.readline()
        v2 = header.startswith("tiny\t2")
        for line in f:
            p = line.rstrip("\n").split("\t")
            if v2:
                if p[0] in ("c", ""):
                    yield p
            else:
                if p[0] in ("CLASS", "FIELD", "METHOD"):
                    yield p


def build(ver):
    inter_tiny = os.path.join(LOOM, ver, "intermediary-v2.tiny")
    if not os.path.exists(inter_tiny):
        print(f"[{ver}] SKIP: no intermediary tiny"); return None

    classes, members = {}, {}
    for p in tiny_rows(inter_tiny):
        if p[0] == "c" and len(p) >= 3:
            # c <official> <intermediary>
            classes[p[2]] = p[1]
        elif p[0] == "" and len(p) >= 5 and p[1] in ("m", "f"):
            # "" m/f <desc> <official> <intermediary>
            members[p[4]] = (p[2], p[3])
    with open(os.path.join(MAP_DIR, f"inter2obf-{ver}.json"), "w") as f:
        json.dump({"classes": classes, "members": members}, f)

    # yarn tiny: any "net.fabricmc.yarn.*" subdir of this version
    ver_dir = os.path.join(LOOM, ver)
    yarn_tiny = None
    for d in os.listdir(ver_dir):
        if d.startswith("net.fabricmc.yarn"):
            cand = os.path.join(ver_dir, d, "mappings.tiny")
            if os.path.exists(cand):
                yarn_tiny = cand; break
    yarn2obf = {}
    if yarn_tiny:
        for p in tiny_rows(yarn_tiny):
            if p[0] == "c" and len(p) >= 4:
                inter = p[2]
                named = p[3]
                if inter.startswith("net/minecraft/class_") and inter in classes:
                    obf = classes[inter]
                    if named:
                        yarn2obf[named] = obf
                        yarn2obf[named.replace("/", ".")] = obf
    with open(os.path.join(MAP_DIR, f"yarn2obf-full-{ver}.json"), "w") as f:
        json.dump(yarn2obf, f)
    print(f"[{ver}] classes={len(classes)} members={len(members)} yarn={len(yarn2obf)}")
    return True


if __name__ == "__main__":
    for ver in sys.argv[1:]:
        build(ver)
