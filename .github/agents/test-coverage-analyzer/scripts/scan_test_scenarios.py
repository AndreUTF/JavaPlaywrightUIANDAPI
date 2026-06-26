#!/usr/bin/env python3
"""Scan Java test sources for Playwright/JUnit5 test methods and extract the
HTTP calls, UI interactions, and assertions each one makes.

This is a heuristic regex scan, not a real Java parser -- it's meant to hand
Claude structured raw material to reason over, not to be the final word on
what's tested. It will miss helper-method indirection (e.g. a test that calls
a shared `doLogin()` helper instead of inlining the calls) and constructed
URLs, so always sanity-check surprising gaps by reading the file directly.

Usage: python scan_test_scenarios.py [src/test/java]
Outputs JSON to stdout: a list of {file, class, method, kind, calls, assertions}
"""
import json
import re
import sys
from pathlib import Path

TEST_ANNOTATION = re.compile(r"@(Test|ParameterizedTest)\b")
CLASS_DECL = re.compile(r"\bclass\s+(\w+)")
METHOD_DECL = re.compile(
    r"^\s*(?:public|private|protected|static|\s)*\s*\w[\w<>\[\],\s]*\s+(\w+)\s*\([^)]*\)\s*(?:throws[^{]+)?\{",
    re.MULTILINE,
)

HTTP_CALL = re.compile(r"\.(get|post|put|patch|delete)\s*\(\s*\"([^\"]+)\"")
NAVIGATE_CALL = re.compile(r"\.navigate\s*\(\s*\"([^\"]+)\"")
UI_ACTION = re.compile(r"\.(click|fill|check|uncheck|selectOption|press|hover|type)\s*\(\s*\"([^\"]*)\"")
ASSERTION_LINE = re.compile(r".*\bassert\w*\s*\(.*\);?\s*$", re.IGNORECASE)


def extract_method_bodies(text):
    """Yield (start_index, method_name, body_text) for each brace-delimited method."""
    results = []
    for m in METHOD_DECL.finditer(text):
        name = m.group(1)
        if name in ("if", "for", "while", "switch", "catch", "return"):
            continue
        brace_start = text.index("{", m.start())
        depth = 0
        i = brace_start
        for i in range(brace_start, len(text)):
            if text[i] == "{":
                depth += 1
            elif text[i] == "}":
                depth -= 1
                if depth == 0:
                    break
        body = text[brace_start:i + 1]
        results.append((m.start(), name, body))
    return results


def scan_file(path: Path):
    text = path.read_text(encoding="utf-8", errors="ignore")
    class_match = CLASS_DECL.search(text)
    class_name = class_match.group(1) if class_match else path.stem

    test_annotation_positions = [m.start() for m in TEST_ANNOTATION.finditer(text)]
    entries = []

    for start, name, body in extract_method_bodies(text):
        is_test = any(pos < start and text[pos:start].count("\n") <= 3 for pos in test_annotation_positions)
        if not is_test:
            continue

        calls = []
        for m in HTTP_CALL.finditer(body):
            target = m.group(2)
            # Filter out false positives like body.get("id") (JSON field access,
            # not an HTTP call) by requiring a URL-shaped argument.
            if target.startswith("http://") or target.startswith("https://") or target.startswith("/"):
                calls.append({"type": "http", "method": m.group(1).upper(), "target": target})
        for m in NAVIGATE_CALL.finditer(body):
            calls.append({"type": "navigate", "method": "GET", "target": m.group(1)})
        for m in UI_ACTION.finditer(body):
            calls.append({"type": "ui_action", "method": m.group(1), "target": m.group(2)})

        assertions = [line.strip() for line in body.splitlines() if ASSERTION_LINE.match(line)]

        if any(c["type"] == "http" or c["type"] == "navigate" and c["target"] for c in calls if c["type"] == "http"):
            kind = "API"
        elif any(c["type"] in ("navigate", "ui_action") for c in calls):
            kind = "UI"
        else:
            kind = "unknown"

        entries.append({
            "file": str(path),
            "class": class_name,
            "method": name,
            "kind": kind,
            "calls": calls,
            "assertions": assertions,
        })

    return entries


def main():
    root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("src/test/java")
    if not root.exists():
        print(json.dumps({"error": f"{root} does not exist"}))
        sys.exit(0)

    all_entries = []
    for java_file in sorted(root.rglob("*.java")):
        all_entries.extend(scan_file(java_file))

    print(json.dumps(all_entries, indent=2))


if __name__ == "__main__":
    main()
