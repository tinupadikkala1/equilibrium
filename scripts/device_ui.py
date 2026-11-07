"""Small ADB helper for inspecting and exercising the installed Equilibrium UI."""
import argparse
import pathlib
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser()
parser.add_argument("action", choices=["dump", "click", "capture"])
parser.add_argument("value", nargs="?")
parser.add_argument("--serial", required=True)
parser.add_argument("--adb", default=shutil.which("adb") or "adb")
args = parser.parse_args()
command = [args.adb, "-s", args.serial]

def adb(*parts):
    return subprocess.check_output(command + list(parts), timeout=30)

def nodes():
    result = adb("shell", "uiautomator", "dump", "/sdcard/eq-check.xml")
    if b"dumped to" not in result:
        raise SystemExit("Fresh UI dump unavailable; no interaction performed")
    raw = adb("shell", "cat", "/sdcard/eq-check.xml")
    root = ET.fromstring(raw)
    if not any(n.get("package") == "com.aistudio.equilibrium.zypqnx" for n in root.iter("node")):
        raise SystemExit("Equilibrium is not in the foreground; no interaction performed")
    return list(root.iter("node"))

if args.action == "capture":
    nodes()
    target = pathlib.Path(args.value)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(adb("exec-out", "screencap", "-p"))
    print(target)
else:
    items = nodes()
    if args.action == "dump":
        for n in items:
            label = n.get("text") or n.get("content-desc")
            if label:
                print(label, n.get("bounds"), "selected=" + str(n.get("selected")))
    else:
        matches = [n for n in items if args.value in (n.get("text"), n.get("content-desc"))]
        if len(matches) != 1:
            raise SystemExit(f"Expected one visible match for {args.value!r}; got {len(matches)}")
        bounds = list(map(int, re.findall(r"\d+", matches[0].get("bounds"))))
        x = (bounds[0] + bounds[2]) // 2
        y = (bounds[1] + bounds[3]) // 2
        adb("shell", "input", "tap", str(x), str(y))
        print("Tapped", args.value, x, y)
