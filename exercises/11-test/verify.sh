#!/usr/bin/env bash
# 模块 11 的真实接口验证：先 mvn package，再 java -jar 起服务，然后跑这个脚本。
# 用法：bash verify.sh [端口]
set -u

PORT="${1:-8011}"
BASE="http://127.0.0.1:${PORT}"
PASS=0
FAIL=0

# check <描述> <期望状态码> <期望片段> <curl 参数...>
check() {
  local desc="$1" wantCode="$2" wantBody="$3"
  shift 3
  local out code body
  out=$(curl -s -w '\n%{http_code}' "$@" 2>/dev/null)
  code=$(printf '%s' "$out" | tail -n1)
  body=$(printf '%s' "$out" | sed '$d')

  if [ "$code" = "$wantCode" ] && printf '%s' "$body" | grep -q -- "$wantBody"; then
    printf '  [OK]   %-34s %s\n' "$desc" "$code"
    PASS=$((PASS + 1))
  else
    printf '  [FAIL] %-34s 期望 %s / %s，实际 %s / %s\n' "$desc" "$wantCode" "$wantBody" "$code" "$body"
    FAIL=$((FAIL + 1))
  fi
}

echo "== 正常路径 =="
check "hello" 200 '"data":"hello, testing"' "${BASE}/orders/hello"
check "300 元金卡 → 247" 200 '"payable":247.00' \
  -X POST "${BASE}/orders/calculate" -H 'Content-Type: application/json' \
  -d '{"orderNo":"V-001","amount":300,"goldCard":true,"sku":"SKU-1","quantity":2}'
check "100 元普卡 → 90" 200 '"payable":90.00' \
  -X POST "${BASE}/orders/calculate" -H 'Content-Type: application/json' \
  -d '{"orderNo":"V-002","amount":100}'
check "quick 100 金卡 → 85.50" 200 '"payable":85.50' \
  "${BASE}/orders/quick-calculate?amount=100&goldCard=true"
check "触底 0.01 金卡 → 0.01" 200 '"触底0.01"' \
  "${BASE}/orders/quick-calculate?amount=0.01&goldCard=true"

echo "== 错误路径 =="
check "库存不足 → 409" 409 '"code":20409' \
  -X POST "${BASE}/orders/calculate" -H 'Content-Type: application/json' \
  -d '{"orderNo":"V-003","amount":100,"quantity":101}'
check "金额缺失 → 400" 400 '金额不能为空' \
  -X POST "${BASE}/orders/calculate" -H 'Content-Type: application/json' \
  -d '{"orderNo":"V-004"}'
check "金额 0.001 → 400" 400 '金额不能小于 0.01' \
  -X POST "${BASE}/orders/calculate" -H 'Content-Type: application/json' \
  -d '{"orderNo":"V-005","amount":0.001}'
check "orderNo 空白 → 400" 400 '订单号不能为空' \
  -X POST "${BASE}/orders/calculate" -H 'Content-Type: application/json' \
  -d '{"orderNo":"  ","amount":100}'
check "非法 JSON → 400" 400 '请求体不是合法的 JSON' \
  -X POST "${BASE}/orders/calculate" -H 'Content-Type: application/json' -d '{"orderNo": '
check "amount=abc → 400" 400 '类型不正确' "${BASE}/orders/quick-calculate?amount=abc"
check "缺 amount → 400" 400 '缺少必填参数 amount' "${BASE}/orders/quick-calculate"

echo
echo "通过 ${PASS} 项，失败 ${FAIL} 项"
[ "$FAIL" -eq 0 ]
