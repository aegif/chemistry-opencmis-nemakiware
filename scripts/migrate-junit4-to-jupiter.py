#!/usr/bin/env python3
"""Mechanically migrate JUnit 4 test sources to JUnit Jupiter."""

from __future__ import annotations

import argparse
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

ASSERT_METHODS = [
    "assertEquals",
    "assertNotEquals",
    "assertTrue",
    "assertFalse",
    "assertNull",
    "assertNotNull",
    "assertSame",
    "assertNotSame",
    "assertArrayEquals",
    "fail",
]


def needs_migration(text: str) -> bool:
    if "org.junit.jupiter" in text and "org.junit.Test" not in text and "junit.framework" not in text:
        # already jupiter-only, unless still has org.junit.Before without Each
        if any(
            x in text
            for x in (
                "org.junit.Before;",
                "org.junit.After;",
                "org.junit.BeforeClass",
                "org.junit.AfterClass",
                "org.junit.Ignore",
                "org.junit.Assert",
            )
        ):
            return True
        return False
    return any(
        x in text
        for x in (
            "junit.framework",
            "org.junit.Test",
            "org.junit.Assert",
            "org.junit.Before",
            "org.junit.After",
            "org.junit.Ignore",
            "org.junit.BeforeClass",
            "org.junit.AfterClass",
        )
    )


def migrate(text: str) -> str:
    original = text
    extends_testcase = bool(
        re.search(r"extends\s+(junit\.framework\.)?TestCase\b", text)
    )

    # imports / annotations
    text = text.replace("import junit.framework.TestCase;\n", "")
    text = text.replace("import junit.framework.Assert;\n", "")
    text = re.sub(
        r"\bextends\s+(junit\.framework\.)?TestCase\b",
        "",
        text,
    )
    # clean double spaces in class decl
    text = re.sub(r"class\s+(\w+)\s+\{\{", r"class \1 {", text)
    text = re.sub(r"class\s+(\w+)\s+\n", r"class \1\n", text)
    text = re.sub(r"public class (\w+)\s+\{", r"public class \1 {", text)
    text = re.sub(r"public class (\w+)  \{", r"public class \1 {", text)
    text = re.sub(r"public class (\w+)\s+\n\s*\{", r"public class \1 {", text)

    replacements = [
        ("import static org.junit.Assert.", "import static org.junit.jupiter.api.Assertions."),
        ("import org.junit.Assert;\n", "import org.junit.jupiter.api.Assertions;\n"),
        ("import org.junit.Test;\n", "import org.junit.jupiter.api.Test;\n"),
        ("import org.junit.Before;\n", "import org.junit.jupiter.api.BeforeEach;\n"),
        ("import org.junit.After;\n", "import org.junit.jupiter.api.AfterEach;\n"),
        ("import org.junit.BeforeClass;\n", "import org.junit.jupiter.api.BeforeAll;\n"),
        ("import org.junit.AfterClass;\n", "import org.junit.jupiter.api.AfterAll;\n"),
        ("import org.junit.Ignore;\n", "import org.junit.jupiter.api.Disabled;\n"),
        ("@BeforeClass", "@BeforeAll"),
        ("@AfterClass", "@AfterAll"),
        ("@Before\n", "@BeforeEach\n"),
        ("@After\n", "@AfterEach\n"),
        ("@Ignore", "@Disabled"),
    ]
    for old, new in replacements:
        text = text.replace(old, new)

    # Assert.xxx -> Assertions.xxx when fully qualified / class-qualified
    text = re.sub(r"\borg\.junit\.Assert\.", "org.junit.jupiter.api.Assertions.", text)
    text = re.sub(r"(?<!Assertions)\bAssert\.(assert|fail)", r"Assertions.\1", text)

    # If still extends leftover whitespace issues like "public class Foo  {"
    text = re.sub(r"(public class \w+)\s+\{", r"\1 {", text)

    # Convert JUnit3-style setUp/tearDown without annotations
    if extends_testcase or "void setUp(" in text or "void tearDown(" in text:
        text = annotate_lifecycle(text, "setUp", "BeforeEach")
        text = annotate_lifecycle(text, "tearDown", "AfterEach")

    # Ensure Assertions static imports for inherited assert* usage
    used = sorted({m for m in ASSERT_METHODS if re.search(rf"\b{m}\s*\(", text)})
    if used:
        text = ensure_static_assertion_imports(text, used)

    # Ensure jupiter Test import if @Test present
    if "@Test" in text and "import org.junit.jupiter.api.Test;" not in text:
        text = insert_import(text, "import org.junit.jupiter.api.Test;")

    # Ensure BeforeEach/AfterEach imports if annotations present
    if "@BeforeEach" in text and "import org.junit.jupiter.api.BeforeEach;" not in text:
        text = insert_import(text, "import org.junit.jupiter.api.BeforeEach;")
    if "@AfterEach" in text and "import org.junit.jupiter.api.AfterEach;" not in text:
        text = insert_import(text, "import org.junit.jupiter.api.AfterEach;")
    if "@BeforeAll" in text and "import org.junit.jupiter.api.BeforeAll;" not in text:
        text = insert_import(text, "import org.junit.jupiter.api.BeforeAll;")
    if "@AfterAll" in text and "import org.junit.jupiter.api.AfterAll;" not in text:
        text = insert_import(text, "import org.junit.jupiter.api.AfterAll;")
    if "@Disabled" in text and "import org.junit.jupiter.api.Disabled;" not in text:
        text = insert_import(text, "import org.junit.jupiter.api.Disabled;")

    # Add @Test on public void testXxx() if missing (JUnit3 style)
    text = add_missing_test_annotations(text)

    if text != original and "junit.framework" in text:
        # leave marker for manual review if residual framework refs remain
        pass

    return text


def annotate_lifecycle(text: str, method: str, annotation: str) -> str:
    pattern = re.compile(
        rf"(^[ \t]*)((?:public|protected)\s+void\s+{method}\s*\()",
        re.MULTILINE,
    )

    def repl(m: re.Match[str]) -> str:
        indent, sig = m.group(1), m.group(2)
        # already annotated immediately above?
        start = m.start()
        before = m.string[max(0, start - 80) : start]
        if f"@{annotation}" in before.splitlines()[-3:]:
            return m.group(0)
        # if @Before/@After old form somehow remains on previous line
        return f"{indent}@{annotation}\n{indent}{sig}"

    return pattern.sub(repl, text)


def ensure_static_assertion_imports(text: str, methods: list[str]) -> str:
    existing = set(
        re.findall(
            r"import static org\.junit\.jupiter\.api\.Assertions\.(\w+);",
            text,
        )
    )
    missing = [m for m in methods if m not in existing]
    if not missing:
        return text
    lines = [f"import static org.junit.jupiter.api.Assertions.{m};" for m in missing]
    return insert_import_block(text, "\n".join(lines) + "\n")


def insert_import(text: str, import_line: str) -> str:
    return insert_import_block(text, import_line if import_line.endswith("\n") else import_line + "\n")


def insert_import_block(text: str, block: str) -> str:
    # after package and existing imports
    m = re.search(r"(package .*?;\n)", text, re.DOTALL)
    if not m:
        return block + text
    idx = m.end()
    # skip blank lines then find last import
    rest = text[idx:]
    imports = list(re.finditer(r"^(?:import .*;\n)+", rest, re.MULTILINE))
    if imports:
        # insert after first contiguous import region following package blanks
        # Find all import lines from start of rest
        pos = 0
        while pos < len(rest) and rest[pos] == "\n":
            pos += 1
        end = pos
        while True:
            m2 = re.match(r"import .*;\n", rest[end:])
            if not m2:
                break
            end += m2.end()
        return text[: idx + end] + block + text[idx + end :]
    return text[:idx] + "\n" + block + text[idx:]


def add_missing_test_annotations(text: str) -> str:
    # only for methods named test* that lack @Test in preceding lines
    pattern = re.compile(
        r"(^[ \t]*)(public\s+void\s+(test\w+)\s*\([^;]*\)\s*(?:throws\s+[^{]+)?\{)",
        re.MULTILINE,
    )

    def repl(m: re.Match[str]) -> str:
        indent, sig = m.group(1), m.group(2)
        start = m.start()
        before = m.string[max(0, start - 120) : start]
        recent = "\n".join(before.splitlines()[-4:])
        if "@Test" in recent or "@Disabled" in recent:
            return m.group(0)
        # skip if this looks like a helper somehow already annotated elsewhere - keep simple
        return f"{indent}@Test\n{indent}{sig}"

    return pattern.sub(repl, text)


def process_file(path: Path, write: bool) -> bool:
    text = path.read_text(encoding="utf-8")
    if not needs_migration(text):
        return False
    new = migrate(text)
    if new == text:
        return False
    if write:
        path.write_text(new, encoding="utf-8")
    return True


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("paths", nargs="+", help="files or directories under repo")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    files: list[Path] = []
    for p in args.paths:
        path = Path(p)
        if not path.is_absolute():
            path = ROOT / path
        if path.is_dir():
            files.extend(path.rglob("src/test/java/**/*.java"))
        else:
            files.append(path)

    changed = []
    for f in sorted(set(files)):
        if "target" in f.parts:
            continue
        if process_file(f, write=not args.dry_run):
            changed.append(f.relative_to(ROOT))

    print(f"{'would change' if args.dry_run else 'changed'}: {len(changed)}")
    for c in changed:
        print(c)
    # Heuristic warnings for JUnit4 message-first asserts that need manual review
    warn = []
    for c in changed:
        txt = (ROOT / c).read_text(encoding='utf-8')
        if re.search(r'assertEquals\(\s*name\b', txt) or re.search(r'assertTrue\(\s*name\b', txt):
            warn.append(str(c))
    if warn:
        print('WARN message-first assert candidates:')
        for w in warn:
            print(' ', w)


if __name__ == "__main__":
    main()
