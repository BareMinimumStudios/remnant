"""Validate Remnant's changelog and optionally extract the current release notes."""

import argparse
import re
from datetime import date
from pathlib import Path

VERSION = r"(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)"
RELEASE = re.compile(rf"## \[({VERSION})\] - (\d{{4}}-\d{{2}}-\d{{2}})")
CATEGORIES = {"Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"}


def release_notes(changelog, current_version):
    sections = re.split(r"(?m)(?=^## )", changelog)
    if len(sections) < 3 or sections[1].splitlines()[0] != "## [Unreleased]":
        raise ValueError("CHANGELOG.md must start its release history with ## [Unreleased].")

    versions = []
    previous_date = date.max
    notes = None
    for section in sections[2:]:
        heading, _, body = section.partition("\n")
        match = RELEASE.fullmatch(heading)
        if not match:
            raise ValueError(f"Expected a dated release heading, found {heading!r}.")
        version, release_date = match.groups()
        parsed_date = date.fromisoformat(release_date)
        if parsed_date > previous_date or version in versions:
            raise ValueError("Release headings must be unique and ordered newest first.")
        previous_date = parsed_date
        versions.append(version)
        body = re.split(r"(?m)^\[(?:Unreleased|" + VERSION + r")\]:", body, maxsplit=1)[0]
        categories = re.findall(r"(?m)^### (.+)$", body)
        if not categories or set(categories) - CATEGORIES or not re.search(r"(?m)^- \S", body):
            raise ValueError(f"[{version}] needs categorized changelog entries.")
        if version == current_version:
            notes = f"## {version} - {release_date}\n\n{body.strip()}\n"

    if versions[0] != current_version:
        raise ValueError(f"The first dated release must match mod_version ({current_version}).")
    for label in ["Unreleased", *versions]:
        if not re.search(rf"(?m)^\[{re.escape(label)}\]:\s+https://\S+", changelog):
            raise ValueError(f"Missing changelog link for [{label}].")
    return notes


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    try:
        properties = Path("gradle.properties").read_text(encoding="utf-8")
        versions = re.findall(r"(?m)^mod_version=(.*)$", properties)
        if len(versions) != 1 or not re.fullmatch(VERSION, versions[0]):
            raise ValueError("gradle.properties needs one stable mod_version, such as 3.0.1.")
        notes = release_notes(Path("CHANGELOG.md").read_text(encoding="utf-8"), versions[0])
        if args.output:
            args.output.parent.mkdir(parents=True, exist_ok=True)
            args.output.write_text(notes, encoding="utf-8")
        print(f"Validated changelog for {versions[0]}.")
    except (OSError, ValueError) as error:
        parser.exit(1, f"{error}\n")


if __name__ == "__main__":
    main()
