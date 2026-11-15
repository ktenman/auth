#!/bin/sh
# Runs inside the client container: checks the service still behaves, then sends a fixed load.
set -eu
APP=http://authbench-app:8083
JSON='Accept: application/json'

fail() { echo "CHECK FAILED: $*" >&2; exit 1; }
headers() { curl -s -o /dev/null -D - "$@" | tr -d '\r'; }
code() { head -1 | cut -d' ' -f2; }
header() { grep -i "^$1: " | tail -1 | cut -d' ' -f2-; }
session() { grep -i '^set-cookie: AUTHSESSION=' | tail -1 | sed 's/^[^=]*=//; s/;.*//'; }

# Walks the Google sign-in against the mock provider and prints the signed-in session id
login() {
  h=$(headers "$APP/oauth2/authorization/google?redirect=/radio/x")
  s=$(echo "$h" | session)
  callback=$(headers "$(echo "$h" | header location)" | header location)
  h=$(headers -H "Cookie: AUTHSESSION=$s" "$callback")
  [ "$(echo "$h" | header location)" = https://fov.ee/radio/x ] || fail "sign-in landed on '$(echo "$h" | header location)'"
  fresh=$(echo "$h" | session)
  echo "${fresh:-$s}"
}

h=$(headers -H "$JSON" -H 'X-Forwarded-Uri: /radio/x' $APP/validate)
[ "$(echo "$h" | code)" = 302 ] && echo "$h" | header location | grep -q '/login?redirect=%2Fradio%2Fx' || fail "signed-out validate"
curl -s "$APP/login?redirect=/radio/x" | grep -q 'name="redirect" value="/radio/x"' || fail "login page"
S=$(login)
h=$(headers -H "$JSON" -H "Cookie: AUTHSESSION=$S" $APP/validate)
[ "$(echo "$h" | code)" = 200 ] && [ "$(echo "$h" | header x-user-id)" = bench@example.com ] || fail "signed-in validate"
L=$(login)
[ "$(headers -X POST -H "Cookie: AUTHSESSION=$L" $APP/logout | code)" = 302 ] || fail "logout"
[ "$(headers -H "$JSON" -H "Cookie: AUTHSESSION=$L" $APP/validate | code)" = 302 ] || fail "validate after logout"
# The rest of the pages, and the requests that would leave a CSRF token or an error in the session
curl -s -H "Cookie: AUTHSESSION=$S" $APP/user | grep -q '"email":"bench@example.com"' || fail "user"
curl -s -H "Cookie: AUTHSESSION=$S" $APP/home | grep -q 'Dashboard' || fail "home page"
curl -s -H "Cookie: AUTHSESSION=$S" $APP/missing | grep -q '<p>404</p>' || fail "error page"
C=$(login)
[ "$(headers -X POST -H "Cookie: AUTHSESSION=$C" $APP/validate | code)" = 403 ] || fail "POST without a CSRF token"
h=$(headers $APP/home)
[ "$(echo "$h" | code)" = 302 ] && ! echo "$h" | session | grep -q . || fail "signed-out page opened a session"
h=$(headers $APP/oauth2/authorization/google)
state=$(echo "$h" | header location | sed 's/.*[?&]state=\([^&]*\).*/\1/')
h=$(headers -H "Cookie: AUTHSESSION=$(echo "$h" | session)" "$APP/login/oauth2/code/google?error=access_denied&state=$state")
[ "$(echo "$h" | code)" = 302 ] && echo "$h" | header location | grep -q '/login?error' || fail "cancelled sign-in"
echo "checks: ok"

for i in $(seq 18); do login >/dev/null; done
total=0
load() {
  out=$(ab -q -k "$@" 2>&1) || fail "ab $*: $out"
  echo "$out" | grep -q '^Failed requests: *0$' || fail "failed requests in ab $*"
  total=$(echo "$out" | awk -v t="$total" '/^Time taken for tests/ { printf "%.2f", t + $5 }')
}
# Caddy's forward_auth: every request to a protected site asks /validate with the visitor's cookie
load -n 20000 -c 8 -H "$JSON" -C "AUTHSESSION=$S" $APP/validate
echo "$out" | grep -q '^Non-2xx' && fail "signed-in validate answered non-2xx under load"
load -n 4000 -c 8 -H "$JSON" -H 'X-Forwarded-Uri: /radio/x' $APP/validate
load -n 2000 -c 4 "$APP/login?redirect=/radio/x"
load -n 1000 -c 2 $APP/actuator/health
echo "load_s: $total"
