#!/usr/bin/env python3
"""Rewrite intermediary names (class_NNNN, method_NNNN, field_NNNN) inside
mixin-class constant pools to the official obfuscated runtime names, by
composing Fabric's intermediary tiny (obf<->intermediary) table.

Operates directly on the jar's class entries: parses the constant pool and
replaces Utf8 constants matching intermediary patterns with their obfuscated
equivalents. Only touches classes under net/everlastingness/ (mixin classes);
Minecraft annotation references live in Utf8 pool entries both as internal
names (net/minecraft/class_310) and descriptors (Lnet/minecraft/class_310;)
and member names (method_1574).
"""
import json, re, sys, zipfile, shutil, os

MAPPING = sys.argv[1]
SRC_JAR = sys.argv[2]
DST_JAR = sys.argv[3]

with open(MAPPING) as f:
    table = json.load(f)
classes = table["classes"]      # "net/minecraft/class_310" -> "enn"
members = table["members"]      # "method_1574" -> [ownerObf, "s"]
YARN_MAP = MAPPING.replace("inter2obf-", "yarn2obf-full-")
import os
if os.path.exists(YARN_MAP):
    with open(YARN_MAP) as yf:
        yarn2obf = json.load(yf)  # yarn class names (dotted+slashed) -> obf
else:
    yarn2obf = {}

cls_re = re.compile(r"net/minecraft/(class_\d+)(\$class_\d+)?")
mem_re = re.compile(r"^(method|field)_\d+$")

def map_utf8(s):
    changed = False
    # Full-string yarn class name (dotted or slashed), any package
    if s in yarn2obf:
        return yarn2obf[s], True
    # Descriptor-embedded yarn names Lfoo.bar.Baz; -> Lobf;
    def yarn_desc_repl(m):
        nonlocal changed
        inner = m.group(1)
        obf = yarn2obf.get(inner) or yarn2obf.get(inner.replace("/", "."))
        if obf:
            changed = True
            return "L%s;" % obf
        return m.group(0)
    s = re.sub(r"L([^;]+);", yarn_desc_repl, s)
    # Full-string intermediary class name
    def cls_repl(m):
        nonlocal changed
        base = "net/minecraft/" + m.group(1)
        obf = classes.get(base)
        if obf is None:
            return m.group(0)
        inner = m.group(2)
        if inner:
            inner_obf = classes.get(base + inner)
            if inner_obf is None:
                return m.group(0)
            changed = True
            return obf + "$" + inner_obf.split("/")[-1]
        changed = True
        return obf
    s2 = cls_re.sub(cls_repl, s)
    # Bare member names (annotation values for @Inject(method=...))
    if mem_re.match(s2):
        mm = members.get(s2)
        if mm:
            s2 = mm[1]
            changed = True
        return s2, changed
    # Member-with-descriptor form "name(desc)ret" or "name:desc"
    md = re.match(r"^((?:method|field)_\d+)(\(.*\)\S+|:.+)$", s2)
    if md:
        name = md.group(1)
        rest = s2[len(name):]
        mm = members.get(name)
        if mm:
            s2 = mm[1] + rest
            changed = True
    return s2, changed

# --- Constant pool rewriting -------------------------------------------------
CONST_Utf8 = 1

def rewrite_class(data):
    if len(data) < 10 or data[0:4] != b"\xca\xfe\xba\xbe":
        return data, 0
    idx = 10
    count = int.from_bytes(data[8:10], "big")
    entries = [None] * count  # 1-based; entries[i] = (tag, payload-bytes)
    out = bytearray()
    out += data[:10]
    changes = 0
    i = 1
    long_skip = False
    while i < count:
        tag = data[idx]
        if long_skip:
            entries[i] = (0, None)
            long_skip = False
            i += 1
            continue
        if tag == CONST_Utf8:
            ln = int.from_bytes(data[idx+1:idx+3], "big")
            raw = data[idx+3:idx+3+ln]
            try:
                s = raw.decode("utf-8", errors="surrogateescape")
                new, changed = map_utf8(s)
                if changed:
                    nb = new.encode("utf-8", errors="surrogateescape")
                    out += bytes([tag]) + len(nb).to_bytes(2, "big") + nb
                    changes += 1
                    idx += 3 + ln
                    i += 1
                    continue
            except Exception:
                pass
            out += data[idx:idx+3+ln]
            idx += 3 + ln
            i += 1
            continue
        # Other tags: copy verbatim; need lengths to advance
        length = TAG_SIZE.get(tag)
        if length is None:
            # Unknown tag — abort rewriting this class
            return data, 0
        out += data[idx:idx+length]
        idx += length
        if tag == 5 or tag == 6:
            long_skip = True
        i += 1
    # Copy the rest verbatim (from current idx to end)
    out += data[idx:]
    return bytes(out), changes

TAG_SIZE = {
    2: 4, 3: 5, 4: 5, 5: 9, 6: 9, 7: 3, 8: 3, 9: 5, 10: 5, 11: 5, 12: 5,
    15: 4, 16: 2, 17: 5, 18: 5, 19: 3, 20: 3,
}

total = 0
shutil.copy(SRC_JAR, DST_JAR + ".tmp")
zin = zipfile.ZipFile(SRC_JAR, "r")
zout = zipfile.ZipFile(DST_JAR + ".tmp", "a", zipfile.ZIP_DEFLATED)
# Rewrite approach: build a fresh zip instead
zin.close()
zout.close()
os.remove(DST_JAR + ".tmp")

zin = zipfile.ZipFile(SRC_JAR, "r")
zout = zipfile.ZipFile(DST_JAR, "w", zipfile.ZIP_DEFLATED)
for item in zin.infolist():
    data = zin.read(item.filename)
    if item.filename.endswith(".class") and "net/everlastingness/" in item.filename:
        data, n = rewrite_class(data)
        total += n
    zout.writestr(item, data)
zout.close()
zin.close()
print("rewritten constants:", total)
REFOBF = MAPPING.replace("inter2obf-", "refmap-obf-")

# Post-process: rewrite any refmap JSON inside the output jar to obfuscated refs
import shutil as _sh
if os.path.exists(REFOBF):
    refobf_data = json.load(open(REFOBF))
    tmp = DST_JAR + ".refmap.tmp"
    zi = zipfile.ZipFile(DST_JAR, "r")
    zo = zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED)
    for it in zi.infolist():
        d = zi.read(it.filename)
        if it.filename.endswith(".json") and b'"mappings"' in d and it.filename != os.path.basename(REFOBF):
            try:
                data = json.loads(d)
                if "mappings" in data:
                    obfmap = refobf_data.get("mappings", {})
                    for mixin, entries in data["mappings"].items():
                        oe = obfmap.get(mixin)
                        if oe:
                            data["mappings"][mixin] = oe
                    d = json.dumps(data).encode()
            except Exception:
                pass
        zo.writestr(it, d)
    zi.close(); zo.close()
    _sh.move(tmp, DST_JAR)
    print("refmap obf-substituted")

