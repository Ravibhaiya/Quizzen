#!/usr/bin/env python3
"""Creates app/google-services.json for CI builds.

The Google Services Gradle plugin refuses to build without this file, and the real one is not stored in the repository.

* If the GOOGLE_SERVICES_JSON environment variable is set (GitHub secret of the same name, the full file content),
  it is written as is. The result is a fully working Firebase build.
* Otherwise, and only without --require-real, a placeholder is written so tests, lint and the compile check can run.
  An app built with the placeholder starts, but cannot talk to the real Firestore database.
* With --require-real a missing secret is an error, so a placeholder APK can never be published by mistake.
"""
import json
import os
import sys

TARGET = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "google-services.json")

PLACEHOLDER = {
    "project_info": {
        "project_number": "000000000000",
        "project_id": "quizzen-ci-placeholder",
        "storage_bucket": "quizzen-ci-placeholder.appspot.com",
    },
    "client": [
        {
            "client_info": {
                "mobilesdk_app_id": "1:000000000000:android:0000000000000000000000",
                "android_client_info": {"package_name": "com.ravibhaiya.quizzen"},
            },
            "api_key": [{"current_key": "placeholder-not-a-real-key"}],
        }
    ],
    "configuration_version": "1",
}


def main() -> int:
    require_real = "--require-real" in sys.argv[1:]
    secret = os.environ.get("GOOGLE_SERVICES_JSON", "").strip()
    if secret:
        try:
            json.loads(secret)
        except ValueError as exc:
            print(f"::error::GOOGLE_SERVICES_JSON is not valid JSON: {exc}")
            return 1
        content = secret
        print("Using google-services.json from the GOOGLE_SERVICES_JSON secret.")
    elif require_real:
        print(
            "::error::The GOOGLE_SERVICES_JSON secret is missing. Add it under Settings > Secrets and variables > Actions "
            "(paste the whole content of app/google-services.json from the Firebase console)."
        )
        return 1
    else:
        content = json.dumps(PLACEHOLDER, indent=2)
        print("::warning::GOOGLE_SERVICES_JSON secret not set; using a placeholder google-services.json (CI check only).")
    with open(TARGET, "w", encoding="utf-8") as fh:
        fh.write(content + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
