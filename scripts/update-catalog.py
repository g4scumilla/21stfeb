#!/usr/bin/env python3
import argparse
import datetime
import json
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser(description="Update My Store catalog.json entry.")
    parser.add_argument("--catalog", required=True)
    parser.add_argument("--package-name", required=True)
    parser.add_argument("--version-name", required=True)
    parser.add_argument("--version-code", type=int, required=True)
    parser.add_argument("--apk-url", required=True)
    parser.add_argument("--release-url", required=True)
    parser.add_argument("--sha256", required=True)
    parser.add_argument("--size-bytes", type=int, required=True)
    parser.add_argument("--release-notes", default="")
    args = parser.parse_args()

    catalog_path = Path(args.catalog)
    data = json.loads(catalog_path.read_text(encoding="utf-8"))
    now_iso = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    today = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d")

    data["updatedAt"] = now_iso
    updated = False
    for app in data.get("apps", []):
        if app.get("packageName") == args.package_name:
            app["versionName"] = args.version_name
            app["versionCode"] = args.version_code
            app["apkUrl"] = args.apk_url
            app["releaseUrl"] = args.release_url
            app["sha256"] = args.sha256
            app["apkSizeBytes"] = args.size_bytes
            app["releaseDate"] = today
            if args.release_notes:
                app["releaseNotes"] = args.release_notes
            updated = True
            break

    if not updated:
        raise SystemExit(f"Package {args.package_name} not found in {args.catalog}")

    catalog_path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    print(f"Updated {args.package_name} -> v{args.version_name} ({args.version_code}) in {args.catalog}")


if __name__ == "__main__":
    main()
