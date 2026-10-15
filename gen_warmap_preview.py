"""生成最终作战图 HTML 预览（Excel式矩阵 + 工序人员调整路线图），与 App 输出一致"""
steps = [
    (1,"前车架打磨","打磨工",15.75,4,4.84,-715.0,3,"冗余1"),
    (2,"后车架人工补焊","焊工",5.71,1,5.71,88.2,1,"匹配"),
    (3,"后车架总成焊接","焊工",4.73,1,4.73,166.6,1,"匹配"),
    (4,"后车架一次补焊","焊工",4.14,1,4.14,213.8,1,"匹配"),
    (5,"后车架一次焊接","焊工",4.14,1,4.14,213.8,1,"匹配"),
    (6,"后车架总成组对","组对工",6.12,0,6.12,55.4,1,"缺1"),
    (7,"后车架二次组对","组对工",4.40,1,4.40,193.0,1,"匹配"),
    (8,"后车架一次组对","组对工",4.40,1,4.40,193.0,1,"匹配"),
    (9,"扭力筒组对","组对工",4.40,1,4.40,193.0,1,"匹配"),
    (10,"前车架总成组对","组对工",3.66,2,3.66,252.2,1,"冗余1"),
    (11,"前车架基础组对","组对工",3.66,1,3.66,252.2,1,"匹配"),
    (12,"前车架总成焊接","焊工",3.66,1,3.66,252.2,1,"匹配"),
    (13,"前车架焊接","焊工",3.66,1,3.66,252.2,1,"匹配"),
]

def esc(s): return s.replace("&","&amp;").replace("<","&lt;")

# 表头
heads = "".join(f'<th style="min-width:46px;padding:6px 4px;font-size:11px;font-weight:500;white-space:nowrap">{esc(n[:5])}</th>' for _,n,_,_,_,_,_,_,_ in steps)

def row(label, getter, color_fn=None):
    cells=""
    for i,(sid,name,sk,t,n,cyc,diff,req,st) in enumerate(steps):
        val=getter(i)
        bg="#fff"; tc="#2c3e50"
        if color_fn: bg,tc=color_fn(i)
        cells+=f'<td style="padding:6px 4px;text-align:center;font-size:11px;background:{bg};color:{tc};white-space:nowrap">{val}</td>'
    return f'<tr><td style="padding:6px 6px;font-size:12px;font-weight:500;background:#eef2f7;white-space:nowrap">{label}</td>{cells}</tr>'

def diff_color(i):
    d=steps[i][6]
    return ("#fdecea","#a32d2d") if d<-0.5 else (("#eaf3de","#3b6d11") if d>0.5 else ("#fff","#2c3e50"))
def req_color(i):
    st=steps[i][8]
    return ("#fdecea","#a32d2d") if "缺" in st else (("#eaf3de","#3b6d11") if "冗余" in st else ("#fff","#2c3e50"))

rows = "".join([
    row("单人单台时间", lambda i:f"{steps[i][3]:.2f}"),
    row("N台时间", lambda i:f"{steps[i][5]:.2f}"),
    row("日可用时间", lambda i:"545"),
    row("差异时间", lambda i:f"{steps[i][6]:.0f}", diff_color),
    row("需要人数", lambda i:steps[i][7]),
    row("人员配置", lambda i:steps[i][8], req_color),
])

# 协作流程图：竖向节点 + 右侧时间/人数信息轨道（与用户手绘一致）
supports = [(10, 6, "10:51-19:00", 1)]  # from_step_id, to_step_id, support_window, count
related_ids = sorted({s[0] for s in supports} | {s[1] for s in supports}, key=lambda sid: next(i for i, st in enumerate(steps) if st[0] == sid))
id_to_name = {s[0]: s[1] for s in steps}
node_r = 6
svg_w = 360
svg_h = 180
node_x = 24
rail_x = 240
top_y = 24
bottom_y = svg_h - 36
node_count = len(related_ids)
node_spacing = (bottom_y - top_y) / max(1, node_count - 1)

def is_receiver(sid): return any(s[1] == sid for s in supports)
def is_donor(sid): return any(s[0] == sid for s in supports)

def node_color(sid):
    if is_receiver(sid): return "#a32d2d"   # 被支援：红
    if is_donor(sid): return "#4caf50"      # 支援方：绿
    return "#1976d2"

node_centers = [(node_x, top_y + i * node_spacing) for i, _ in enumerate(related_ids)]

circles = "".join(
    f'<circle cx="{node_centers[i][0]}" cy="{node_centers[i][1]}" r="{node_r}" fill="{node_color(sid)}"/>'
    for i, sid in enumerate(related_ids)
)

# 右侧信息轨道竖线
rail_line = f'<line x1="{rail_x}" y1="{top_y}" x2="{rail_x}" y2="{bottom_y}" stroke="#1976d2" stroke-width="2"/>'

# 直角折线：donor -> 右 -> 沿轨道 -> receiver，箭头指向 receiver
arrows = ""
for fr_id, to_id, window, cnt in supports:
    fr_idx = related_ids.index(fr_id)
    to_idx = related_ids.index(to_id)
    x1, y1 = node_centers[fr_idx]
    x2, y2 = node_centers[to_idx]
    start_x = x1 + node_r
    end_x = x2 + node_r
    arrows += (
        f'<polyline points="{start_x},{y1} {rail_x},{y1} {rail_x},{y2} {end_x},{y2}" fill="none" stroke="#1976d2" stroke-width="2"/>'
        f'<polygon points="{end_x},{y2} {end_x - 8},{y2 - 4} {end_x - 8},{y2 + 4}" fill="#1976d2"/>'
    )

# 节点名称与身份标签
name_labels = ""
for i, sid in enumerate(related_ids):
    x, y = node_centers[i]
    role = "被支援" if is_receiver(sid) else ("支援方" if is_donor(sid) else "")
    role_color = "#a32d2d" if is_receiver(sid) else ("#4caf50" if is_donor(sid) else "#5b6b7b")
    name_labels += (
        f'<text x="{x + 16}" y="{y - 2}" font-size="12" fill="#2c3e50">{esc(id_to_name[sid][:8])}</text>'
        f'<text x="{x + 16}" y="{y + 11}" font-size="10" fill="{role_color}">{role}</text>'
    )

# 右侧轨道上的时间与人数标签
rail_labels = ""
for fr_id, to_id, window, cnt in supports:
    fr_idx = related_ids.index(fr_id)
    to_idx = related_ids.index(to_id)
    y1 = node_centers[fr_idx][1]
    y2 = node_centers[to_idx][1]
    mid_y = (y1 + y2) / 2
    rail_labels += (
        f'<text x="{rail_x + 8}" y="{mid_y - 2}" font-size="12" fill="#a32d2d">{esc(window)}</text>'
        f'<text x="{rail_x + 8}" y="{mid_y + 12}" font-size="11" fill="#1565c0">支援 {cnt} 人</text>'
    )

flow_svg = f'''<svg width="{svg_w}" height="{svg_h}" style="display:block">{circles}{rail_line}{arrows}{name_labels}{rail_labels}</svg>'''

kpis = [("客户节拍","6.81","min/件","#185fa5"),("日产台数","80","台/日","#0f6e56"),
        ("需要人数","15","人","#a32d2d"),("当前人数","16","人","#ba7517"),("自动支援","1","人","#534ab7")]
kpi_html="".join(
    f'<div style="flex:1;min-width:118px;border:1px solid #dde3ea;border-radius:8px;padding:8px 10px">'
    f'<div style="font-size:12px;color:#5b6b7b">{t}</div>'
    f'<div style="font-size:20px;font-weight:500;color:{c}">{v} <span style="font-size:11px;color:#90a0b0">{u}</span></div></div>'
    for t,v,u,c in kpis)

html = f'''<div style="font-family:'Microsoft YaHei',Segoe UI,sans-serif;color:#2c3e50;font-size:13px;max-width:680px;background:#e8f4fd;padding:16px;border-radius:12px">
<h2 style="font-size:15px;font-weight:500;margin:0 0 4px">人效提升作战图（最终输出 · 日产 80 台）</h2>
<div style="font-size:12px;color:#5b6b7b;margin-bottom:8px">工作时段 08:10-10:00 / 10:15-11:30 / 13:00-19:00　日可用 545 min　客户节拍 6.81 min/件</div>
<div style="display:flex;gap:8px;flex-wrap:wrap;margin-bottom:10px">{kpi_html}</div>

<div style="font-size:13px;font-weight:500;margin:14px 0 4px">工序协作流程图</div>
<div style="display:flex;align-items:center;gap:8px;background:#e8f4fd;border:1px solid #cde3f8;border-radius:8px;padding:12px;overflow-x:auto">
  <span style="padding:6px 10px;border-radius:6px;background:#e8e8e8;color:#4a4a4a;font-size:12px;white-space:nowrap">协作流程图</span>
  <div>{flow_svg}</div>
</div>
<div style="font-size:12px;color:#5b6b7b;margin-top:4px">左侧竖向节点 = 工序；红色 = 被支援工序，绿色 = 支援方工序；右侧轨道标注支援时段与人数。</div>

<div style="font-size:13px;font-weight:500;margin:14px 0 4px">作战图矩阵（横向滚动）</div>
<div style="overflow-x:auto;border:1px solid #dde3ea;border-radius:8px">
<table style="border-collapse:collapse;min-width:680px">
<thead><tr><th style="padding:6px 6px;font-size:12px;font-weight:500;background:#1f3a5f;color:#fff;white-space:nowrap">指标 \\ 工序</th>{heads}</tr></thead>
<tbody>{rows}</tbody>
</table>
</div>

<div style="border:2px solid #ba7517;border-radius:10px;padding:12px;margin-top:12px;background:#fff">
  <div style="font-size:14px;font-weight:500;color:#534ab7;margin-bottom:6px">支援方案（同工种自动调配）</div>
  <div style="font-size:13px">前车架总成组对 派 <b style="color:#a32d2d">1 人</b> -> 后车架总成组对</div>
  <div style="font-size:13px;color:#a32d2d;font-weight:500;margin-top:2px">每天支援时段：10:51 - 19:00</div>
  <div style="font-size:11px;color:#ba7517;margin-top:2px">含 20% 弹性缓冲 ≈ 29 分钟，实际到岗时间误差控制在 20% 以内</div>
</div>
</div>'''
open("D:/workbuddy/精益人效提升作战/EfficiencyWarRoom/warmap_preview.html","w",encoding="utf-8").write(html)
print("HTML written, len=",len(html))
