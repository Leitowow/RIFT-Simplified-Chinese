import json
import pathlib
import urllib.request

OUTPUT_FILE = pathlib.Path("src/main/kotlin/dev/nohus/rift/pings/PingTextTranslation.kt")

BASE_TERMS = {
    "home defense": "本土防御",
    "standing fleet": "常驻舰队",
    "response fleet": "响应舰队",
    "stratop": "战略行动",
    "formup": "集结",
    "forming": "正在组队",
    "fleet": "舰队",
    "fc": "指挥官",
    "hostiles": "敌对",
    "hostile": "敌对",
    "enemy": "敌人",
    "defense": "防御",
    "attack": "进攻",
    "op": "行动",
    "comms": "语音频道",
    "doctrine": "编队配置",
    "bridge": "跳桥",
    "cyno": "诱导",
    "mumble": "Mumble 语音",
    "pap": "PAP",
    "Booster": "加成船",
    "Support": "电子战船",
    "Else": "其他舰船",
    "Logi": "后勤船",
    "Kikis": "奇奇莫拉级",
    "MWD": "微曲配置",
    "AB": "加力配置",
}


def get(path: str) -> dict:
    with urllib.request.urlopen("https://ref-data.everef.net" + path, timeout=30) as response:
        return json.load(response)


def escape_kotlin_string(value: str) -> str:
    return value.replace("\\", "\\\\").replace("\"", "\\\"")


def load_ship_name_mappings() -> dict[str, str]:
    category = get("/categories/6")
    ships: dict[str, str] = {}
    for group_id in category["group_ids"]:
        bundle = get(f"/groups/{group_id}/bundle")
        for type_data in bundle.get("types", {}).values():
            names = type_data.get("name", {})
            english = names.get("en")
            chinese = names.get("zh")
            if english and chinese:
                ships[english] = chinese
    return ships


def build_output_file(mapping: dict[str, str]) -> str:
    lines: list[str] = []
    lines.append("package dev.nohus.rift.pings")
    lines.append("")
    lines.append("/**")
    lines.append(" * Centralized English -> Chinese mapping for Jabber ping message body text.")
    lines.append(" * Includes common ping vocabulary and all EVE ship names.")
    lines.append(" */")
    lines.append("private val pingTextMapping = linkedMapOf(")
    for english in sorted(mapping.keys(), key=lambda value: (value.lower(), value)):
        chinese = mapping[english]
        lines.append(f"    \"{escape_kotlin_string(english)}\" to \"{escape_kotlin_string(chinese)}\",")
    lines.append(")")
    lines.append("")
    lines.append("private val sortedMappings = pingTextMapping.entries.sortedByDescending { it.key.length }")
    lines.append("")
    lines.append("fun toBilingualPingText(text: String): String {")
    lines.append("    var result = text")
    lines.append("    sortedMappings.forEach { (english, chinese) ->")
    lines.append("        val pattern = Regex(\"\\\\b${Regex.escape(english)}\\\\b\", RegexOption.IGNORE_CASE)")
    lines.append("        result = pattern.replace(result) { match ->")
    lines.append("            \"${match.value}（$chinese）\"")
    lines.append("        }")
    lines.append("    }")
    lines.append("    return result")
    lines.append("}")
    lines.append("")
    return "\n".join(lines)


def main() -> None:
    ship_mappings = load_ship_name_mappings()
    merged_mappings = dict(BASE_TERMS)
    merged_mappings.update(ship_mappings)
    OUTPUT_FILE.write_text(build_output_file(merged_mappings), encoding="utf-8")
    print(f"Wrote {len(merged_mappings)} entries ({len(ship_mappings)} ship names)")


if __name__ == "__main__":
    main()
