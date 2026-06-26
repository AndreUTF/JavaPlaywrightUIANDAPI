#!/usr/bin/env python3
"""Summarize a JaCoCo jacoco.xml report into a plain-text table.

Usage: python parse_jacoco_report.py [path/to/jacoco.xml]
Default path: target/site/jacoco/jacoco.xml (relative to cwd)
"""
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def counter_dict(elem):
    counters = {}
    for c in elem.findall("counter"):
        counters[c.get("type")] = (int(c.get("missed")), int(c.get("covered")))
    return counters


def pct(missed, covered):
    total = missed + covered
    if total == 0:
        return None
    return round(100 * covered / total, 1)


def main():
    path = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("target/site/jacoco/jacoco.xml")

    if not path.exists():
        print(f"NO_REPORT: {path} does not exist. Either `mvn test` hasn't been run yet with the "
              f"JaCoCo plugin attached, or -- very common for UI/API test projects -- there is no "
              f"target/classes directory because the project has no src/main/java application code. "
              f"JaCoCo's report goal silently skips ('Skipping JaCoCo execution due to missing classes "
              f"directory.') rather than writing an empty report in that case. Treat code coverage as "
              f"not-yet-applicable and rely on the scenario-coverage scan instead.")
        sys.exit(0)

    root = ET.parse(path).getroot()
    classes = []
    for package in root.findall("package"):
        for cls in package.findall("class"):
            counters = counter_dict(cls)
            line = counters.get("LINE", (0, 0))
            branch = counters.get("BRANCH", (0, 0))
            classes.append({
                "name": cls.get("name").replace("/", "."),
                "line_pct": pct(*line),
                "line_missed": line[0],
                "line_covered": line[1],
                "branch_pct": pct(*branch),
                "branch_missed": branch[0],
                "branch_covered": branch[1],
            })

    if not classes:
        print("NO_CLASSES: jacoco.xml parsed but contains no instrumented classes. "
              "This is expected if the project has no src/main/java application code yet — "
              "JaCoCo only instruments compiled main classes, not test classes. "
              "Treat code coverage as not-yet-applicable and rely on the scenario-coverage scan instead.")
        sys.exit(0)

    overall = counter_dict(root)
    line = overall.get("LINE", (0, 0))
    branch = overall.get("BRANCH", (0, 0))

    print("OVERALL")
    print(f"  line coverage:   {pct(*line)}%  ({line[1]} covered / {line[0]} missed)")
    print(f"  branch coverage: {pct(*branch)}%  ({branch[1]} covered / {branch[0]} missed)")
    print()
    print("PER CLASS (sorted by line coverage, worst first)")
    for c in sorted(classes, key=lambda x: (x["line_pct"] if x["line_pct"] is not None else -1)):
        print(f"  {c['name']}: line={c['line_pct']}% ({c['line_missed']} missed lines), "
              f"branch={c['branch_pct']}% ({c['branch_missed']} missed branches)")


if __name__ == "__main__":
    main()
