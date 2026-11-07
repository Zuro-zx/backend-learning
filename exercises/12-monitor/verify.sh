#!/usr/bin/env bash
# 模块 12 的真实端点验证：先 mvn package，再 java -jar 起服务，然后跑这个脚本。
# 用法：bash verify.sh [端口]
set -u

PORT="${1:-8012}"
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
    printf '  [OK]   %-38s %s\n' "$desc" "$code"
    PASS=$((PASS + 1))
  else
    printf '  [FAIL] %-38s 期望 %s / %s，实际 %s\n' "$desc" "$wantCode" "$wantBody" "$code"
    FAIL=$((FAIL + 1))
  fi
}

echo "== 端点可用性 =="
check "health 整体 UP" 200 '"status":"UP"' "${BASE}/actuator/health"
check "health 含自定义 inventory 组件" 200 '"skuCount":3' "${BASE}/actuator/health"
check "liveness 探针" 200 '"status":"UP"' "${BASE}/actuator/health/liveness"
check "readiness 探针" 200 '"status":"UP"' "${BASE}/actuator/health/readiness"
check "info 来自 InfoContributor" 200 '"owner":"zsj"' "${BASE}/actuator/info"

echo "== 业务埋点 =="
check "下单成功" 200 '"remainingStock"' \
  -X POST "${BASE}/orders" -H 'Content-Type: application/json' \
  -d '{"sku":"SKU-001","quantity":1,"amount":129.00}'
check "metrics 列表含自定义指标" 200 'biz.order.placed' "${BASE}/actuator/metrics"
check "单指标带 result 标签" 200 '"tag":"result"' "${BASE}/actuator/metrics/biz.order.placed"
check "金额进 DistributionSummary" 200 'biz.order.amount' "${BASE}/actuator/metrics/biz.order.amount"
check "耗时进 Timer" 200 'biz.order.process' "${BASE}/actuator/metrics/biz.order.process"

echo "== Prometheus 抓取格式 =="
check "计数器转 _total 后缀" 200 'biz_order_placed_total' "${BASE}/actuator/prometheus"
check "公共标签 application" 200 'application="12-monitor"' "${BASE}/actuator/prometheus"
check "Timer 有 _bucket" 200 'biz_order_process_seconds_bucket' "${BASE}/actuator/prometheus"

echo "== 状态联动 =="
curl -s -X POST "${BASE}/inventory/SKU-002/stock?value=3" >/dev/null
check "低库存 → Gauge 变 1" 200 '"value":1.0' "${BASE}/actuator/metrics/biz.inventory.low.stock"
check "低库存 → health 仍 UP" 200 '"status":"UP"' "${BASE}/actuator/health"

curl -s -X POST "${BASE}/inventory/SKU-002/stock?value=0" >/dev/null
check "归零 → health 变 503" 503 '"status":"DOWN"' "${BASE}/actuator/health"
check "归零 → 下单 409" 409 '"code":20409' \
  -X POST "${BASE}/orders" -H 'Content-Type: application/json' \
  -d '{"sku":"SKU-002","quantity":5,"amount":50.00}'

curl -s -X POST "${BASE}/inventory/SKU-002/stock?value=30" >/dev/null
check "库存恢复 → health 回 200" 200 '"status":"UP"' "${BASE}/actuator/health"

echo
echo "通过 ${PASS} 项，失败 ${FAIL} 项"
[ "$FAIL" -eq 0 ]
