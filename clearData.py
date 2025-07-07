from __future__ import annotations

import argparse
import sys
import time
from pathlib import Path

CYAN = "\033[96m" if sys.stdout.isatty() else ""
RESET = "\033[0m" if sys.stdout.isatty() else ""


def debug(msg: str) -> None:
    timestamp = time.strftime("%H:%M:%S")
    print(f"{CYAN}[DEBUG {timestamp}]{RESET} {msg}")


def read_users(filepath: Path) -> None:
    # Read the credential file and list all usernames found.
    debug(f"Reading from {filepath}")
    if not filepath.exists():
        print(f"File '{filepath}' not found.")
        return

    with filepath.open("r", encoding="utf-8") as f:
        lines = f.readlines()

    users = [line.split(".username=")[1].strip() for line in lines
             if not line.startswith("#") and line.endswith("\n") and ".username=" in line]

    if users:
        print("Found users:")
        for user in users:
            print(f"  • {user}")
    else:
        print("No users found.")


def delete_file(filepath: Path, force: bool = False) -> None:
    # Delete the credential file after optional confirmation.
    if not filepath.exists():
        print(f"File '{filepath}' does not exist, nothing to delete.")
        return

    if not force:
        ans = input(f" Are you sure you want to delete '{filepath}'? [y/N] ").lower()
        if ans != "y":
            print("Aborted.")
            return

    debug("Deleting file…")
    filepath.unlink()
    print("🗑️  File deleted.")


def main() -> None:
    default_path = (Path.home() / "libraries" / "apache-tomcat-10.1.41" / "bin" / "data" /
                    "userDatabase.properties")

    parser = argparse.ArgumentParser(
        description="Inspect or delete the Tomcat credential store.")
    parser.add_argument("--read", action="store_true", help="Read usernames in credential file")
    parser.add_argument("--delete", action="store_true", help="Delete credential file")
    parser.add_argument("--force", action="store_true", help="Skip confirmation prompts")
    parser.add_argument("-f", "--file", type=Path, default=default_path,
                        help="Path to userDatabase.properties (default: %(default)s)")

    args = parser.parse_args()

    # Ensure exactly one of --read / --delete is chosen.
    if args.read == args.delete:
        parser.error("Choose exactly one of --read or --delete.")

    filepath = args.file.expanduser().resolve()
    debug(f"Resolved credential file path: {filepath}")

    if args.read:
        read_users(filepath)
    else:
        delete_file(filepath, args.force)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nInterrupted by user.")
