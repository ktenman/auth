# Stand-in for Google's OAuth2 endpoints: approves every sign-in as bench@example.com
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlencode, urlparse

USER = {"sub": "bench", "email": "bench@example.com", "name": "Bench User",
        "given_name": "Bench", "family_name": "User", "picture": "https://example.com/bench.png"}


class Google(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_GET(self):
        url = urlparse(self.path)
        if url.path == "/authorize":
            q = parse_qs(url.query)
            self.send_response(302)
            self.send_header("Location", q["redirect_uri"][0] + "?" + urlencode({"code": "bench", "state": q["state"][0]}))
            self.send_header("Content-Length", "0")
            self.end_headers()
        else:
            self.reply(USER)

    def do_POST(self):
        self.rfile.read(int(self.headers.get("Content-Length", 0)))
        self.reply({"access_token": "bench", "token_type": "Bearer", "expires_in": 3600, "scope": "email profile"})

    def reply(self, body):
        data = json.dumps(body).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, *args):
        pass


ThreadingHTTPServer(("", 9000), Google).serve_forever()
