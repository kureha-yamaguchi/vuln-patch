"""Locate or download the Jazzer API jar used to compile harnesses."""
import os
import sys
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))
import config


class JazzerEnvironment:
    """Resolves Jazzer jars for compiling and running harnesses."""

    def __init__(self,
                 api_jar_path: str = config.JAZZER_API_JAR,
                 api_url: str = config.JAZZER_API_URL,
                 standalone_jar_path: str = config.JAZZER_STANDALONE_JAR,
                 standalone_url: str = config.JAZZER_STANDALONE_URL):
        self.api_jar_path = api_jar_path
        self.api_url = api_url
        self.standalone_jar_path = standalone_jar_path
        self.standalone_url = standalone_url

    def ensure(self) -> str:
        """Download jazzer-api.jar if not present. Returns its path."""
        return self._ensure_jar(self.api_jar_path, self.api_url,
                                "Jazzer API")

    def ensure_driver(self) -> str:
        """Download the Jazzer standalone jar if not present. Returns its path."""
        return self._ensure_jar(self.standalone_jar_path, self.standalone_url,
                                "Jazzer driver")

    def _ensure_jar(self, jar_path: str, url: str, label: str) -> str:
        if os.path.isfile(jar_path):
            return jar_path
        os.makedirs(os.path.dirname(jar_path), exist_ok=True)
        print(f"Downloading {label} from {url}")
        urllib.request.urlretrieve(url, jar_path)
        return jar_path