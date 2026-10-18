#!/usr/bin/env python3
"""Validate a REAL generated Android debug APK, never a mock ZIP."""
import hashlib
import sys
import zipfile
from pathlib import Path


def verify(path: Path) -> str:
    if not path.is_file() or path.stat().st_size < 100_000:
        raise ValueError(f"APK missing or unexpectedly small: {path}")
    with zipfile.ZipFile(path) as apk:
        corruption = apk.testzip()
        if corruption is not None:
            raise ValueError(f"APK ZIP checksum mismatch: {corruption}")
        paths = set(apk.namelist())
        essentials = {"AndroidManifest.xml", "classes.dex", "assets/index.html",
                      "assets/core.js", "assets/world.js", "assets/game.js"}
        missing = sorted(essentials - paths)
        if missing:
            raise ValueError(f"Required APK entries absent: {missing}")
        maps = {f"assets/assets/maps/map_{i:03d}.png" for i in range(41)}
        missing_maps = sorted(maps - paths)
        if missing_maps:
            raise ValueError(f"Maps absent from APK: {missing_maps}")
        if apk.read("assets/index.html").strip() == b"":
            raise ValueError("Packaged HTML empty")
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    print(f"PASS real APK ZIP structure: {path.stat().st_size} bytes, 41 arena assets")
    print(f"SHA-256: {digest}")
    print("NOTE: an APK must additionally pass apksigner and device testing.")
    return digest


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit("Usage: python tools/verify_apk.py path/to/app-debug.apk")
    try:
        verify(Path(sys.argv[1]))
    except (ValueError, zipfile.BadZipFile) as exc:
        sys.exit(str(exc))
