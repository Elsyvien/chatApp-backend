from __future__ import annotations

import argparse
import sys
import time
from datetime import datetime
from pathlib import Path

CYAN = "\033[96m" if sys.stdout.isatty() else ""
RESET = "\033[0m" if sys.stdout.isatty() else ""


def debug(msg: str) -> None:
    timestamp = time.strftime("%H:%M:%S")
    print(f"{CYAN}[DEBUG {timestamp}]{RESET} {msg}")


def format_unix_timestamp(timestamp_str: str) -> str:
    """Convert Unix timestamp (in milliseconds) to readable format."""
    try:
        # Convert from milliseconds to seconds
        timestamp_seconds = int(timestamp_str) / 1000
        dt = datetime.fromtimestamp(timestamp_seconds)
        return dt.strftime("%d-%m-%Y %H:%M:%S")
    except (ValueError, TypeError):
        return f"Invalid timestamp: {timestamp_str}"


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


def read_users_with_keys(filepath: Path) -> None:
    # Read the credential file and list all usernames with their stored keys.
    debug(f"Reading from {filepath}")
    if not filepath.exists():
        print(f"File '{filepath}' not found.")
        return

    with filepath.open("r", encoding="utf-8") as f:
        lines = f.readlines()

    # Parse the file to extract user data
    user_data = {}
    for line in lines:
        line = line.strip()
        if line.startswith("#") or not line:
            continue
        
        if "user." in line and "=" in line:
            # Extract user hash and property
            parts = line.split("=", 1)
            key_part = parts[0]
            value = parts[1]
            
            # Extract user hash from key like "user.{hash}.{property}"
            key_sections = key_part.split(".")
            if len(key_sections) >= 3 and key_sections[0] == "user":
                user_hash = key_sections[1]
                property_name = key_sections[2]
                
                if user_hash not in user_data:
                    user_data[user_hash] = {}
                
                user_data[user_hash][property_name] = value

    if user_data:
        print("Found users with keys:")
        for user_hash, data in user_data.items():
            username = data.get("username", "Unknown")
            public_key_e = data.get("publicKeyE", "Not found")
            public_key_n = data.get("publicKeyN", "Not found")
            registration_time = data.get("registrationTime", "Not found")
            
            # Format registration time if it's a valid timestamp
            if registration_time != "Not found":
                formatted_time = format_unix_timestamp(registration_time)
            else:
                formatted_time = "Not found"
            
            print(f"\n  • User: {username}")
            print(f"    Hash: {user_hash}")
            print(f"    PublicKeyE: {public_key_e}")
            print(f"    PublicKeyN: {public_key_n}")
            print(f"    Registration Time: {formatted_time}")
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
    print("File deleted.")


def create_empty_properties_file(properties_path: Path) -> None:
    """Erstellt eine leere userDatabase.properties-Datei im Standardpfad, falls sie nicht existiert."""
    if properties_path.exists():
        debug(f"Properties-Datei existiert bereits: {properties_path}")
        return
    try:
        properties_path.parent.mkdir(parents=True, exist_ok=True)
        with properties_path.open("w", encoding="utf-8") as f:
            f.write("# Leere userDatabase.properties Datei\n")
        debug(f"Leere Properties-Datei erstellt: {properties_path}")
    except Exception as e:
        print(f"Fehler beim Erstellen der Properties-Datei: {e}")


def main() -> None:
    default_path = (Path.home() / "libraries" / "apache-tomcat-10.1.41" / "bin" / "data" /
                    "userDatabase.properties")

    parser = argparse.ArgumentParser(
        description="Inspect or delete the Tomcat credential store.")
    parser.add_argument("--read", action="store_true", help="Read usernames in credential file")
    parser.add_argument("--read-keys", action="store_true", help="Read usernames with their stored keys")
    parser.add_argument("--delete", action="store_true", help="Delete credential file")
    parser.add_argument("--force", action="store_true", help="Skip confirmation prompts")
    parser.add_argument("-f", "--file", type=Path, default=default_path,
                        help="Path to userDatabase.properties (default: %(default)s)")
    parser.add_argument('--init-db', action='store_true', help='Leere userDatabase.properties im Standardpfad anlegen')

    args = parser.parse_args()

    if args.init_db:
        create_empty_properties_file(default_path)
        return

    # Ensure exactly one action is chosen.
    actions = [args.read, args.read_keys, args.delete]
    if sum(actions) != 1:
        parser.error("Choose exactly one of --read, --read-keys, or --delete.")

    filepath = args.file.expanduser().resolve()
    debug(f"Resolved credential file path: {filepath}")

    if args.read:
        read_users(filepath)
    elif args.read_keys:
        read_users_with_keys(filepath)
    else:
        delete_file(filepath, args.force)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nInterrupted by user.")
