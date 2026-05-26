"""Locate or download the Jazzer API jar used to compile harnesses."""
import os
import urllib.request

import config


class JazzerEnvironment:
    """Resolves the jazzer-api jar for compiling Jazzer harnesses against.

    Kept as its own class (rather than a free function) because when we
    eventually want to *run* the harness as well we'll add a method to
    fetch the Jazzer agent and driver here too.
    """

    def __init__(self,
                 api_jar_path: str = config.JAZZER_API_JAR,
                 api_url: str = config.JAZZER_API_URL):
        self.api_jar_path = api_jar_path
        self.api_url = api_url

    def ensure(self) -> str:
        """Download jazzer-api.jar to the cache directory if it isn't
        already present. Returns the absolute path to the jar."""
        if os.path.isfile(self.api_jar_path):
            return self.api_jar_path
        os.makedirs(os.path.dirname(self.api_jar_path), exist_ok=True)
        print(f"Downloading Jazzer API from {self.api_url}")
        urllib.request.urlretrieve(self.api_url, self.api_jar_path)
        return self.api_jar_path