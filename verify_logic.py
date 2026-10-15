"""
验证人效作战沙盘核心计算逻辑（生产调度人员匹配 / 同工种自动支援 / 改善前后对比）
模型：需要人数 = ceil(单件时间 / 客户节拍) = ceil(单件时间 × 日产台数 / 日可用)
     差异时间 = 日可用 − 单件时间 × 日产台数（单人日负荷）
"""
import math

steps = [
    {"id": 1, "name": "前车架打磨", "t": 15.75, "n": 4, "skill": "打磨工", "walk": 0.3, "stations": 4, "imp_t": 14.0, "imp_n": 3},
    {"id": 2, "name": "后车架人工补焊", "t": 5.71, "n": 1, "skill": "焊工", "walk": 0.2, "stations": 1, "imp_t": 0.0, "imp_n": 0},
    {"id": 3, "name": "后车架总成焊接", "t": 4.73, "n": 1, "skill": "焊工", "walk": 0.2, "stations": 1, "imp_t": None, "imp_n": None},
    {"id": 4, "name": "后车架一次补焊", "t": 4.14, "n": 1, "skill": "焊工", "walk": 0.2, "stations": 1, "imp_t": None, "imp_n": None},
    {"id": 5, "name": "后车架一次焊接", "t": 4.14, "n": 1, "skill": "焊工", "walk": 0.2, "stations": 1, "imp_t": None, "imp_n": None},
    {"id": 6, "name": "后车架总成组对", "t": 6.12, "n": 0, "skill": "组对工", "walk": 0.25, "stations": 1, "imp_t": 4.5, "imp_n": 1},
    {"id": 7, "name": "后车架二次组对", "t": 4.40, "n": 1, "skill": "组对工", "walk": 0.25, "stations": 1, "imp_t": None, "imp_n": None},
    {"id": 8, "name": "后车架一次组对", "t": 4.40, "n": 1, "skill": "组对工", "walk": 0.25, "stations": 1, "imp_t": None, "imp_n": None},
    {"id": 9, "name": "扭力筒组对", "t": 4.40, "n": 1, "skill": "组对工", "walk": 0.25, "stations": 1, "imp_t": None, "imp_n": None},
    {"id": 10, "name": "前车架总成组对", "t": 3.66, "n": 2, "skill": "组对工", "walk": 0.25, "stations": 1, "imp_t": None, "imp_n": None},
    {"id": 11, "name": "前车架基础组对", "t": 3.66, "n": 1, "skill": "组对工", "walk": 0.25, "stations": 1, "imp_t": None, "imp_n": None},
    {"id": 12, "name": "前车架总成焊接", "t": 3.66, "n": 1, "skill": "焊工", "walk": 0.2, "stations": 1, "imp_t": None, "imp_n": None},
    {"id": 13, "name": "前车架焊接", "t": 3.66, "n": 1, "skill": "焊工", "walk": 0.2, "stations": 1, "imp_t": None, "imp_n": None},
]

params = {
    "demand": 80,
}

# 工作时段（与 App 默认三段一致）：8:10-10:00 / 10:15-11:30 / 13:00-19:00
segments = [("08:10", "10:00"), ("10:15", "11:30"), ("13:00", "19:00")]


def _to_min(hhmm):
    p = hhmm.split(":")
    return int(p[0]) * 60 + int(p[1])


def seg_min(s):
    return max(0, _to_min(s[1]) - _to_min(s[0]))


def offset_to_clock(offset_min, segs):
    """把"可用时间表内的偏移分钟"映射为时钟（跳过段间休息）"""
    remaining = max(0.0, offset_min)
    for s in segs:
        dur = seg_min(s)
        if remaining <= dur:
            return "%02d:%02d" % ((_to_min(s[0]) + int(remaining)) // 60, (_to_min(s[0]) + int(remaining)) % 60)
        remaining -= dur
    last = segs[-1]
    return "%02d:%02d" % (_to_min(last[1]) // 60, _to_min(last[1]) % 60)


def required(t, demand, available):
    """需要人数 = ceil(单件时间 × 日产台数 / 日可用)"""
    if available <= 0:
        return 1
    return max(1, math.ceil(t * demand / available))


def multi_machine_cycle(t, walk, m):
    return t / m + (m - 1) * walk


def best_plan(step, tk):
    """单人看多台周期（仅作 N台时间 信息展示）"""
    best_m = 1
    best_cycle = step["t"]
    upper = max(1, min(step["stations"], 8))
    for m in range(1, upper + 1):
        cycle = multi_machine_cycle(step["t"], step["walk"], m)
        if cycle < best_cycle:
            best_m = m
            best_cycle = cycle
    return best_m, best_cycle


def compute_war_map(step_list, demand, available):
    tk = available / demand if demand > 0 else 0
    nodes = {}
    for s in step_list:
        req = required(s["t"], demand, available)
        diff = available - s["t"] * demand
        nodes[s["id"]] = {"step": s, "req": req, "diff": diff, "vc": s["n"]}
    assignments = []
    groups = {}
    for s in step_list:
        groups.setdefault(s["skill"], []).append(s)
    for skill, group in groups.items():
        deficits = sorted([s for s in group if nodes[s["id"]]["vc"] < nodes[s["id"]]["req"]],
                          key=lambda s: nodes[s["id"]]["req"] - nodes[s["id"]]["vc"], reverse=True)
        surpluses = sorted([s for s in group if nodes[s["id"]]["vc"] > nodes[s["id"]]["req"]],
                           key=lambda s: nodes[s["id"]]["vc"] - nodes[s["id"]]["req"], reverse=True)
        for d in deficits:
            need = nodes[d["id"]]["req"] - nodes[d["id"]]["vc"]
            for donor in surpluses:
                if donor["id"] == d["id"]:
                    continue
                if need <= 0:
                    break
                can_give = nodes[donor["id"]]["vc"] - nodes[donor["id"]]["req"]
                give = min(need, can_give)
                if give <= 0:
                    continue
                nodes[donor["id"]]["vc"] -= give
                nodes[d["id"]]["vc"] += give
                need -= give
                donor_busy = donor["t"] * demand / max(1, donor["n"])
                s_start = offset_to_clock(donor_busy, segments)
                s_end = "%02d:%02d" % (_to_min(segments[-1][1]) // 60, _to_min(segments[-1][1]) % 60)
                buffer_min = donor_busy * 0.2
                assignments.append({
                    "from": donor["name"], "to": d["name"], "count": give, "skill": skill,
                    "reason": f"{donor['name']} 同工种({skill})可富余 {can_give} 人，调 {give} 人支援 {d['name']}",
                    "window": f"{s_start}-{s_end}",
                    "buffer": buffer_min,
                })
    return nodes, assignments


# 默认场景（demand = 80）
demand = params["demand"]
available = sum(seg_min(s) for s in segments)
takt = available / demand
nodes80, assignments80 = compute_war_map(steps, demand, available)
total_current = sum(s["n"] for s in steps)
total_min = sum(nodes80[s["id"]]["req"] for s in steps)

line_ct = max(s["t"] / max(1, s["n"]) for s in steps)
bottleneck = max(steps, key=lambda s: s["t"] / max(1, s["n"]))
total_work = sum(s["t"] for s in steps)
balance_rate = (total_work / (line_ct * len(steps))) * 100
estimated_output = available / line_ct

print("=" * 78)
print("人效作战沙盘计算验证（基线：日产 %d 台）" % demand)
print("=" * 78)
print(f"日可用时间: {available} min   客户节拍 Takt: {takt:.3f} min/件")
print(f"产线节拍: {line_ct:.2f} min/件   瓶颈: {bottleneck['name']} (CT={bottleneck['t']/max(1,bottleneck['n']):.2f})")
print(f"平衡率: {balance_rate:.1f}%   预计日产量: {estimated_output:.0f} 件")
print(f"当前总人数: {total_current} 人   需要总人数: {total_min} 人")
print()
print("工序明细（需要人数 / 差异时间 / N台时间）")
print("-" * 110)
print(f"{'工序':<16}{'工种':<8}{'单台':>7}{'需要':>6}{'差异时间':>10}{'N台时间':>9}{'当前':>6}{'状态':>10}")
for s in steps:
    req = nodes80[s["id"]]["req"]
    diff = nodes80[s["id"]]["diff"]
    m, cyc = best_plan(s, takt)
    status = "匹配" if s["n"] == req else ("冗余%d" % (s["n"] - req) if s["n"] > req else "缺%d" % (req - s["n"]))
    print(f"{s['name']:<16}{s['skill']:<8}{s['t']:>7.2f}{req:>6}{diff:>10.1f}{cyc:>9.2f}{s['n']:>6}{status:>10}")
print("-" * 110)
print()
print("自动支援方案（同工种）")
print("-" * 110)
if not assignments80:
    print("当前配置无需跨工序支援，所有工种已自足或无人富余。")
else:
    for a in assignments80:
        print(f"{a['from']} 派 {a['count']} 人 → {a['to']} | 每天支援时段 {a['window']}（含20%弹性≈{a['buffer']:.0f}min，误差≤20%）")
print("-" * 110)
print()


# ---------- 生产调度场景：日产台数 → 人员配置 ----------
print("=" * 78)
print("生产调度场景（日产台数 → 需要人数）")
print("=" * 78)
print(f"{'日产台数':>8}{'客户节拍':>10}{'需要人数':>10}{'当前人数':>10}{'缺口':>8}")
for d in [1, 2, 3, 5, 10, 20, 50, 80]:
    tk = available / d
    tot_min = sum(required(s["t"], d, available) for s in steps)
    print(f"{d:>8}{tk:>10.2f}{tot_min:>10}{total_current:>10}{max(0, tot_min - total_current):>8}")
print("-" * 78)
print()


# ---------- 改善前 / 改善后 指标对比 ----------
def make_variant(improved):
    out = []
    for s in steps:
        t = s["imp_t"] if (improved and s["imp_t"] is not None) else s["t"]
        n = s["imp_n"] if (improved and s["imp_n"] is not None) else s["n"]
        out.append({**s, "t": t, "n": n})
    return out


def aggregate(step_list, demand):
    tk = available / demand
    total_work = sum(s["t"] for s in step_list)
    cts = [s["t"] / max(1, s["n"]) for s in step_list]
    line_ct = max(cts) if cts else 0
    balance = (total_work / (line_ct * len(step_list))) * 100 if line_ct > 0 else 0
    total_n = sum(s["n"] for s in step_list)
    est = available / line_ct if line_ct > 0 else 0
    return tk, line_ct, balance, total_n, est, total_work


before = make_variant(False)
after = make_variant(True)
b = aggregate(before, demand)
a = aggregate(after, demand)

print("=" * 78)
print("改善前 / 改善后 指标对比（示例改善方案）")
print("=" * 78)
print(f"{'指标':<16}{'改善前':>12}{'改善后':>12}{'变化':>12}  结论")
def crow(label, bv, av, higher_better, digits=2):
    delta = av - bv
    improved = (delta > 0) if higher_better else (delta < 0)
    flag = "✔ 更优" if improved else ("— 无变化" if delta == 0 else "△ 需关注")
    print(f"{label:<16}{bv:>10.{digits}f}{av:>12.{digits}f}{delta:>+10.{digits}f}  {flag}")
crow("当前总人数(人)", b[3], a[3], False, 0)
crow("产线节拍(min)", b[1], a[1], False)
crow("平衡率(%)", b[2], a[2], True, 1)
crow("预计日产量(件)", b[4], a[4], True, 0)
crow("标准工时(min/台)", b[5], a[5], False, 1)
print("-" * 78)
print("工序级改善（时间 min / 人数 人）:")
for s_b, s_a in zip(before, after):
    if s_b["t"] != s_a["t"] or s_b["n"] != s_a["n"]:
        print(f"  {s_b['name']:<16} 时间 {s_b['t']:.2f}→{s_a['t']:.2f}   人数 {s_b['n']}→{s_a['n']}")
print("-" * 78)
print("\n验证完成。基线需要总人数 = %d 人（当前 %d 人）。" % (total_min, total_current))
