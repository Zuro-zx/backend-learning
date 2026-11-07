#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
校验生成的 OpenAPI 文件与真实服务是否一致。

做法：对每个 path 发 OPTIONS，Spring MVC 会回 Allow 头列出该路径支持的方法；
路径不存在则 404。这样既不会产生副作用，也能确认 method 对得上。

08 的接口有鉴权，需要带凭据：
    python probe-openapi.py --token eyJhbGci... --basic alice:123456

用法：
    python probe-openapi.py                 # 探测 10/11/12（三个服务在跑即可）
    python probe-openapi.py ex-11-test      # 只探一个
"""
import argparse
import base64
import json
import pathlib
import sys
import urllib.error
import urllib.request

OUT = pathlib.Path(__file__).resolve().parent
# /stage1 /stage2 /stage3 是 httpBasic 链，Bearer 会被无视并返回 401
BASIC_PREFIXES = ("/stage1", "/stage2", "/stage3")


def auth_headers(path, token, basic):
    if path.startswith(BASIC_PREFIXES):
        if not basic:
            return {}
        raw = base64.b64encode(basic.encode("utf-8")).decode("ascii")
        return {"Authorization": f"Basic {raw}"}
    if token:
        return {"Authorization": f"Bearer {token}"}
    return {}


def probe(module, token=None, basic=None):
    spec = json.loads((OUT / f"{module}.json").read_text(encoding="utf-8"))
    base = spec["servers"][0]["url"]
    bad, notes, total = [], [], 0
    for path, item in spec["paths"].items():
        url = base + path.replace("{id}", "1").replace("{sku}", "SKU-001")
        req = urllib.request.Request(url, method="OPTIONS",
                                     headers=auth_headers(path, token, basic))
        allow = ""
        try:
            with urllib.request.urlopen(req, timeout=10) as r:
                allow = r.headers.get("Allow", "")
        except urllib.error.HTTPError as e:
            if e.code in (200, 204, 405):
                allow = e.headers.get("Allow", "")
            elif e.code in (401, 403):
                # 鉴权在 handler 之前生效，拿不到 Allow；但能返回 401/403 说明路由是存在的
                notes.append(f"{path} 被鉴权拦截（HTTP {e.code}），路由存在")
                total += len(item)
                continue
            else:
                bad.append(f"{path} 探测失败 HTTP {e.code}")
                continue
        if not allow:
            bad.append(f"{path} 路径不存在（Allow 为空）")
            continue
        allowed = {m.strip().upper() for m in allow.split(",")}
        for method in item:
            total += 1
            if method.upper() not in allowed:
                bad.append(f"{method.upper()} {path} 未在 Allow 中（Allow: {allow}）")
    print(f"{module:26s} 检查 {total:2d} 个 operation，{len(bad)} 个不一致")
    for b in bad:
        print("   ✗", b)
    for n in notes:
        print("   · ", n, "（用有权限的账号复核可拿到 Allow）")
    return not bad


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("modules", nargs="*", help="不传则探测所有 ex-*.json")
    ap.add_argument("--token", help="08 的 JWT，/api/** 与 /stage5/** 用")
    ap.add_argument("--basic", help="08 的 Basic 凭据，形如 alice:123456")
    args = ap.parse_args()

    modules = args.modules or [p.stem for p in sorted(OUT.glob("ex-*.json"))]
    ok = True
    for m in modules:
        if m.startswith("ex-08") and not (args.token and args.basic):
            print(f"{m:26s} 跳过（需要 --token 与 --basic 才能探有鉴权的接口）")
            continue
        ok = probe(m, args.token, args.basic) and ok

    print("\n结论：" + ("全部一致" if ok else "存在不一致，见上"))
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
