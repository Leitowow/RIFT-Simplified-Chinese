import json
import pathlib
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed


OUTPUT_FILE = pathlib.Path("src/main/composeResources/files/blueprints_zh.tsv")
BASE_URL = "https://ref-data.everef.net"


def get_json(path: str, timeout: int = 20) -> dict:
    with urllib.request.urlopen(BASE_URL + path, timeout=timeout) as response:
        return json.load(response)


def _load_group_rows(group_id: int) -> list[tuple[int, str, str]]:
    bundle = get_json(f"/groups/{group_id}/bundle")
    rows: list[tuple[int, str, str]] = []
    for type_data in bundle.get("types", {}).values():
        names = type_data.get("name", {})
        en_name = (names.get("en") or "").strip()
        zh_name = (names.get("zh") or "").strip()
        if not en_name or not zh_name:
            continue
        if "Blueprint" not in en_name:
            continue
        type_id = int(type_data["type_id"])
        rows.append((type_id, en_name, zh_name))
    return rows


def load_blueprint_names() -> list[tuple[int, str, str]]:
    category = get_json("/categories/9")
    group_ids = list(category.get("group_ids", []))
    rows: list[tuple[int, str, str]] = []
    failed_groups: list[int] = []

    with ThreadPoolExecutor(max_workers=16) as executor:
        futures = {executor.submit(_load_group_rows, group_id): group_id for group_id in group_ids}
        for index, future in enumerate(as_completed(futures), start=1):
            group_id = futures[future]
            try:
                rows.extend(future.result())
            except Exception as exc:
                failed_groups.append(group_id)
                print(f"[warn] group {group_id} failed: {exc}")
            if index % 20 == 0:
                print(f"[info] processed {index}/{len(group_ids)} groups")

    if failed_groups:
        print(f"[warn] {len(failed_groups)} groups failed")

    rows.sort(key=lambda it: (it[1].lower(), it[0]))
    return rows


def main() -> None:
    rows = load_blueprint_names()
    OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    with OUTPUT_FILE.open("w", encoding="utf-8", newline="\n") as f:
        f.write("# typeId\\tenName\\tzhName\n")
        for type_id, en_name, zh_name in rows:
            f.write(f"{type_id}\t{en_name}\t{zh_name}\n")
    print(f"Wrote {len(rows)} blueprint translations to {OUTPUT_FILE}")


if __name__ == "__main__":
    main()
