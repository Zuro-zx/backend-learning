#!/usr/bin/env bash
# 08-security 实测脚本：覆盖六条链 + 五个练习
BASE=http://127.0.0.1:8008
J='Content-Type: application/json'

tok() { python3 -c "import json,sys;print(json.load(open(sys.argv[1]))['data']['token'])" "$1"; }
code() { curl -s -o /tmp/body.txt -w "%{http_code}" "$@"; }
hr() { echo; echo "===== $* ====="; }

hr "1 登录 alice"
curl -s -X POST "$BASE/api/auth/login" -H "$J" -d '{"username":"alice","password":"123456"}' > /tmp/login.json
python3 -m json.tool --no-ensure-ascii /tmp/login.json
ALICE=$(tok /tmp/login.json)

hr "2 匿名访问 /api/hello"
echo "HTTP $(code "$BASE/api/hello") $(cat /tmp/body.txt)"

hr "3 alice 访问 /api/hello"
echo "HTTP $(code "$BASE/api/hello" -H "Authorization: Bearer $ALICE") $(cat /tmp/body.txt)"

hr "4 alice 访问 /api/admin/dashboard"
echo "HTTP $(code "$BASE/api/admin/dashboard" -H "Authorization: Bearer $ALICE") $(cat /tmp/body.txt)"

hr "5 bob 访问 /api/admin/dashboard"
curl -s -X POST "$BASE/api/auth/login" -H "$J" -d '{"username":"bob","password":"123456"}' > /tmp/bob.json
BOB=$(tok /tmp/bob.json)
echo "HTTP $(code "$BASE/api/admin/dashboard" -H "Authorization: Bearer $BOB") $(cat /tmp/body.txt)"

hr "6 密码错误 / 禁用 / 锁定"
echo "密码错误  HTTP $(code -X POST "$BASE/api/auth/login" -H "$J" -d '{"username":"alice","password":"bad"}') $(cat /tmp/body.txt)"
echo "禁用 dave HTTP $(code -X POST "$BASE/api/auth/login" -H "$J" -d '{"username":"dave","password":"123456"}') $(cat /tmp/body.txt)"
echo "锁定 frank HTTP $(code -X POST "$BASE/api/auth/login" -H "$J" -d '{"username":"frank","password":"123456"}') $(cat /tmp/body.txt)"

hr "7 权限点：DELETE /api/articles/1001"
echo "alice HTTP $(code -X DELETE "$BASE/api/articles/1001" -H "Authorization: Bearer $ALICE") $(cat /tmp/body.txt)"
echo "bob   HTTP $(code -X DELETE "$BASE/api/articles/1001" -H "Authorization: Bearer $BOB") $(cat /tmp/body.txt)"

hr "8 stage1 全拦截（Basic）"
echo "匿名  HTTP $(code "$BASE/stage1/hello") $(cat /tmp/body.txt)"
echo "Basic HTTP $(code "$BASE/stage1/hello" -u alice:123456) $(cat /tmp/body.txt)"

hr "9 stage2 读公开写受保护"
echo "GET    HTTP $(code "$BASE/stage2/articles/1") $(cat /tmp/body.txt)"
echo "DELETE HTTP $(code -X DELETE "$BASE/stage2/articles/1") $(cat /tmp/body.txt)"

hr "10 stage3 角色"
echo "audit carol（只有 ADMIN）HTTP $(code "$BASE/stage3/audit/logs" -u carol:123456) $(cat /tmp/body.txt)"
echo "audit bob（ADMIN+AUDITOR） HTTP $(code "$BASE/stage3/audit/logs" -u bob:123456) $(cat /tmp/body.txt)"
echo "ops   erin（只有 AUDITOR）HTTP $(code "$BASE/stage3/ops/metrics" -u erin:123456) $(cat /tmp/body.txt)"

hr "11 stage5（练习 1）"
echo "匿名           HTTP $(code "$BASE/stage5/hello") $(cat /tmp/body.txt)"
echo "alice hello    HTTP $(code "$BASE/stage5/hello" -H "Authorization: Bearer $ALICE") $(cat /tmp/body.txt)"
echo "alice admin    HTTP $(code "$BASE/stage5/admin/dashboard" -H "Authorization: Bearer $ALICE") $(cat /tmp/body.txt)"
echo "bob   admin    HTTP $(code "$BASE/stage5/admin/dashboard" -H "Authorization: Bearer $BOB") $(cat /tmp/body.txt)"

hr "12 短信登录（练习 5：需要图形验证码）"
echo "不带图形验证码 HTTP $(code -X POST "$BASE/api/auth/sms/code?phone=13900009999") $(cat /tmp/body.txt)"
curl -s "$BASE/api/auth/captcha" > /tmp/captcha.json
UUID=$(python3 -c "import json,sys;print(json.load(sys.stdin)['data']['uuid'])" < /tmp/captcha.json)
ANS=$(python3 -c "import json,sys;print(json.load(sys.stdin)['data']['answer'])" < /tmp/captcha.json)
echo "取图 uuid=$UUID 演示答案=$ANS"
echo "发码 HTTP $(code -X POST "$BASE/api/auth/sms/code?phone=13900009999&captchaUuid=$UUID&captchaCode=$ANS") $(cat /tmp/body.txt)"
SMSCODE=$(python3 -c "import json,re,sys;print(re.search(r'(\d{6})',json.load(sys.stdin)['data']).group(1))" < /tmp/body.txt)
echo "验证码 $SMSCODE"
echo "短信登录 HTTP $(code -X POST "$BASE/api/auth/sms/login" -H "$J" -d "{\"phone\":\"13900009999\",\"code\":\"$SMSCODE\"}")"
python3 -m json.tool --no-ensure-ascii < /tmp/body.txt | head -12

hr "13 练习 3：同一账号第二次登录顶掉第一次"
curl -s -X POST "$BASE/api/auth/login" -H "$J" -d '{"username":"carol","password":"123456"}' > /tmp/c1.json
T1=$(tok /tmp/c1.json)
curl -s -X POST "$BASE/api/auth/login" -H "$J" -d '{"username":"carol","password":"123456"}' > /tmp/c2.json
T2=$(tok /tmp/c2.json)
echo "旧 token HTTP $(code "$BASE/api/hello" -H "Authorization: Bearer $T1") $(cat /tmp/body.txt)"
echo "新 token HTTP $(code "$BASE/api/hello" -H "Authorization: Bearer $T2") $(cat /tmp/body.txt)"

hr "14 练习 2：登出后 token 失效"
curl -s -X POST "$BASE/api/auth/login" -H "$J" -d '{"username":"erin","password":"123456"}' > /tmp/c3.json
T3=$(tok /tmp/c3.json)
echo "登出 HTTP $(code -X POST "$BASE/api/auth/logout" -H "Authorization: Bearer $T3") $(cat /tmp/body.txt)"
echo "登出后 HTTP $(code "$BASE/api/hello" -H "Authorization: Bearer $T3") $(cat /tmp/body.txt)"

hr "15 练习 2：改密后旧 token 失效（改完改回）"
curl -s -X POST "$BASE/api/auth/login" -H "$J" -d '{"username":"erin","password":"123456"}' > /tmp/c4.json
T4=$(tok /tmp/c4.json)
echo "改密 HTTP $(code -X PUT "$BASE/api/auth/password" -H "Authorization: Bearer $T4" -H "$J" -d '{"oldPassword":"123456","newPassword":"654321"}') $(cat /tmp/body.txt)"
echo "旧 token 访问 HTTP $(code "$BASE/api/hello" -H "Authorization: Bearer $T4") $(cat /tmp/body.txt)"
curl -s -X POST "$BASE/api/auth/login" -H "$J" -d '{"username":"erin","password":"654321"}' > /tmp/c5.json
T5=$(tok /tmp/c5.json)
echo "新密码登录成功，token 长度 ${#T5}"
echo "改回 HTTP $(code -X PUT "$BASE/api/auth/password" -H "Authorization: Bearer $T5" -H "$J" -d '{"oldPassword":"654321","newPassword":"123456"}') $(cat /tmp/body.txt)"

hr "16 练习 4：Service 层 @PreAuthorize"
echo "alice（无 AUDITOR）HTTP $(code -X POST "$BASE/api/articles/1/review" -H "Authorization: Bearer $ALICE") $(cat /tmp/body.txt)"
echo "bob（AUDITOR）    HTTP $(code -X POST "$BASE/api/articles/1/review" -H "Authorization: Bearer $BOB") $(cat /tmp/body.txt)"
echo "同类内部调用 alice HTTP $(code -X POST "$BASE/api/articles/self-invoke-review" -H "Authorization: Bearer $ALICE") $(cat /tmp/body.txt)"

hr "17 CORS 预检"
curl -s -D - -o /dev/null -X OPTIONS "$BASE/api/hello" -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: GET" | grep -i "^HTTP/\|access-control-allow-origin\|access-control-allow-methods" | head -5

hr "18 stage4 表单登录（curl 走完整 CSRF 流程）"
rm -f /tmp/cookies.txt
CSRF=$(curl -s -c /tmp/cookies.txt "$BASE/stage4/csrf" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")
echo "CSRF token 长度 ${#CSRF}；Cookie 里的 XSRF-TOKEN：$(grep -o 'XSRF-TOKEN[[:space:]].*' /tmp/cookies.txt | head -1 | cut -c1-45)"
STATUS=$(curl -s -o /dev/null -D /tmp/h4.txt -w "%{http_code}" -b /tmp/cookies.txt -c /tmp/cookies.txt \
  -X POST "$BASE/stage4/login" -d "username=alice&password=123456&_csrf=$CSRF")
echo "提交登录 HTTP $STATUS，Location: $(grep -i '^location' /tmp/h4.txt | tr -d '\r')"
curl -s -b /tmp/cookies.txt "$BASE/stage4/me" | grep -E "当前用户|表单登录成功|权限" | sed 's/^ *//'
