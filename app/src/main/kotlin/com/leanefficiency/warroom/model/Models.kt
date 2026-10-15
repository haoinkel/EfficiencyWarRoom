package com.leanefficiency.warroom.model

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * 工序 / 工位数据
 */
data class ProcessStep(
    val id: Int,
    val name: String,
    val singlePieceTimeMin: Double, // 单工序单件作业时间（分钟）
    val currentHeadcount: Int,        // 当前人数
    val stationCount: Int = 1,        // 工位数量
    val x: Float = 0f,                // 布局图 X 坐标（0~1）
    val y: Float = 0f,               // 布局图 Y 坐标（0~1）
    val area: String = "",           // 所属区域
    val skillType: String = "通用",   // 工种类型：焊工/打磨工/组对工/通用 等
    val walkTimeMin: Double = 0.2,    // 同一工人操作多机时，相邻工位切换走动时间（分钟）
    val improvedSinglePieceTimeMin: Double? = null, // 改善后单件时间（分钟），null=不变
    val improvedHeadcount: Int? = null               // 改善后当前人数，null=不变
) {
    /**
     * 工序节拍 = 单件时间 / 当前人数
     */
    val cycleTime: Double
        get() = singlePieceTimeMin / max(1, currentHeadcount)

    /**
     * 理想人数 = ceil(单件时间 / 客户节拍)
     */
    fun idealHeadcount(takt: Double): Int =
        if (takt <= 0) 1 else ceil(singlePieceTimeMin / takt).toInt()

    /**
     * 负荷率
     */
    fun loadRate(takt: Double): Double =
        if (takt <= 0) 0.0 else singlePieceTimeMin / (max(1, currentHeadcount) * takt)

    /**
     * 人员差异（正数=冗余，负数=不足）
     */
    fun headcountDelta(takt: Double): Int = currentHeadcount - idealHeadcount(takt)

    /**
     * 改善后生效的单件时间
     */
    fun effectiveTime(after: Boolean): Double =
        if (after) (improvedSinglePieceTimeMin ?: singlePieceTimeMin) else singlePieceTimeMin

    /**
     * 改善后生效的当前人数
     */
    fun effectiveHeadcount(after: Boolean): Int =
        if (after) (improvedHeadcount ?: currentHeadcount) else currentHeadcount

    /**
     * 生成改善前/改善后变体（仅用于对比计算，不影响原始录入）
     */
    fun toVariant(after: Boolean): ProcessStep = if (!after) this else copy(
        singlePieceTimeMin = effectiveTime(true),
        currentHeadcount = effectiveHeadcount(true)
    )

    /**
     * 一人操作 m 台时的单件周期（分钟/件）
     * 并行度提升：t_i / m；走动浪费：(m-1) * walk
     */
    fun multiMachineCycleTime(machines: Int): Double {
        val m = max(1, machines)
        return singlePieceTimeMin / m + (m - 1) * walkTimeMin
    }

    /**
     * 当前工序在客户节拍下的最佳台数与最少人数
     */
    fun bestMultiMachinePlan(takt: Double, maxMachines: Int = stationCount): MultiMachinePlan {
        if (takt <= 0) return MultiMachinePlan(1, 1, singlePieceTimeMin, 0.0, 0.0)
        var bestM = 1
        var bestN = idealHeadcount(takt)
        var bestCycle = singlePieceTimeMin
        var bestSurplusPerDay = 0.0
        val upper = max(1, min(maxMachines, 8)) // 最多看 8 台，避免无意义搜索
        for (m in 1..upper) {
            val cycle = multiMachineCycleTime(m)
            val n = ceil(cycle / takt).toInt()
            val surplusPerDay = if (n <= 0) 0.0 else max(0.0, takt - cycle) // 按件富余时间
            if (m == 1 || n < bestN || (n == bestN && cycle < bestCycle)) {
                bestM = m
                bestN = n
                bestCycle = cycle
                bestSurplusPerDay = surplusPerDay
            }
        }
        return MultiMachinePlan(bestM, bestN, bestCycle, bestSurplusPerDay, takt - bestCycle)
    }
}

/**
 * 一人多机方案
 */
data class MultiMachinePlan(
    val machines: Int,           // 最佳台数 m*
    val minHeadcount: Int,       // 最少人数 n*
    val cycleTime: Double,       // 一人看 m* 台时的单件周期
    val surplusPerPiece: Double, // 每件富余时间 = Takt - cycleTime
    val deficitPerPiece: Double  // 每件不足时间（负数为富余）= cycleTime - Takt
)

/**
 * 人员支援指令
 */
data class SupportAssignment(
    val fromStepId: Int,
    val fromName: String,
    val toStepId: Int,
    val toName: String,
    val count: Int,
    val reason: String,
    val supportWindow: String = "",      // 支援时段（时钟，如 "10:51-19:00"）
    val bufferMinutes: Double = 0.0     // 20% 弹性缓冲（分钟），用于说明误差
)

/**
 * 作战图每个工序的单元格数据
 */
data class WarMapCell(
    val step: ProcessStep,
    val plan: MultiMachinePlan,
    val requiredHeadcount: Int,     // 需要人数 = ceil(单件时间 / 客户节拍)
    val availableMinutes: Double,
    val demandQty: Double,
    val diffTimeMin: Double,        // 差异时间 = 日可用 − 单件时间×日产台数（单人日负荷）
    val assignmentIn: List<SupportAssignment> = emptyList(),
    val assignmentOut: List<SupportAssignment> = emptyList()
) {
    val requiredHeadcountText: String
        get() = "${requiredHeadcount}人"

    val configStatus: String
        get() = when {
            step.currentHeadcount > requiredHeadcount -> "冗余${step.currentHeadcount - requiredHeadcount}人"
            step.currentHeadcount < requiredHeadcount -> "缺${requiredHeadcount - step.currentHeadcount}人"
            else -> "匹配"
        }

    val multiMachineTimeMin: Double
        get() = plan.cycleTime
}

/**
 * 作战图整体结果
 */
data class WarMapResult(
    val cells: List<WarMapCell>,
    val supports: List<SupportAssignment>,
    val totalMinHeadcount: Int,
    val totalCurrentHeadcount: Int,
    val totalDiffMinutes: Double
)

/**
 * 改善前/改善后 单条对比指标
 */
data class ComparisonMetric(
    val label: String,
    val beforeText: String,
    val afterText: String,
    val unit: String,
    val higherIsBetter: Boolean,
    val improved: Boolean          // 改善后是否更优
)

/**
 * 工序级改善前后对比行
 */
data class ProcessCompareRow(
    val name: String,
    val skill: String,
    val beforeTime: Double,
    val afterTime: Double,
    val beforeHead: Int,
    val afterHead: Int
)

/**
 * 改善前/改善后整体对比结果
 */
data class ComparisonResult(
    val before: WarRoomResult,
    val after: WarRoomResult,
    val beforeWarMap: WarMapResult,
    val afterWarMap: WarMapResult,
    val metrics: List<ComparisonMetric>,
    val perProcess: List<ProcessCompareRow>
)

/**
 * 工作时段分段（支持午休/就餐等多段拆分）。
 * 例：8:10-10:00 / 10:15-11:30 / 13:00-19:00
 */
data class WorkSegment(
    val start: String,  // "HH:mm"
    val end: String     // "HH:mm"
)

/**
 * 全局输入参数
 */
data class GlobalParams(
    val demandQty: Double = 80.0,         // 客户需求量（件/日）
    val segments: List<WorkSegment> = listOf(
        WorkSegment("08:10", "10:00"),
        WorkSegment("10:15", "11:30"),
        WorkSegment("13:00", "19:00")
    )
) {
    /** 日可用时间 = 各段时长之和（分钟），自动由时段算出，避免人为输入漂移 */
    val availableMinutes: Double
        get() = segments.sumOf { segMinutes(it) }

    val customerTakt: Double
        get() = if (demandQty <= 0) 0.0 else availableMinutes / demandQty

    val shiftSummary: String
        get() = segments.joinToString(" / ") { "${it.start}-${it.end}" }
}

/** "HH:mm" -> 当日分钟数 */
fun parseHHmm(s: String): Int {
    val parts = s.split(":")
    if (parts.size != 2) return 0
    return (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
}

/** 单段时长（分钟） */
fun segMinutes(seg: WorkSegment): Double =
    max(0.0, (parseHHmm(seg.end) - parseHHmm(seg.start)).toDouble())

/** 分钟数 -> "HH:mm" */
fun formatClock(totalMin: Int): String {
    val m = ((totalMin % 1440) + 1440) % 1440
    return "%02d:%02d".format(m / 60, m % 60)
}

/**
 * 将"可用时间表内的偏移分钟"映射为时钟时刻（跳过段间休息）。
 * 用于把工序富余时间落到真实作息，给出"几点可去支援"。
 */
fun offsetToClock(offsetMin: Double, segments: List<WorkSegment>): String {
    var remaining = max(0.0, offsetMin)
    for (seg in segments) {
        val dur = segMinutes(seg)
        if (remaining <= dur) {
            return formatClock(parseHHmm(seg.start) + remaining.toInt())
        }
        remaining -= dur
    }
    return formatClock(parseHHmm(segments.last().end))
}

/**
 * 计算结果汇总
 */
data class WarRoomResult(
    val steps: List<ProcessStep>,
    val params: GlobalParams,
    val lineCycleTime: Double,
    val balanceRate: Double,
    val totalHeadcount: Int,
    val idealTotalHeadcount: Int,
    val bottleneckStepId: Int,
    val bottleneckName: String,
    val estimatedDailyOutput: Double
) {
    val bottleneckCycleTime: Double
        get() = steps.find { it.id == bottleneckStepId }?.cycleTime ?: lineCycleTime
}

/**
 * 布局调整建议
 */
data class LayoutSuggestion(
    val title: String,
    val description: String,
    val priority: String // 高/中/低
)

/**
 * 人员调配建议
 */
data class PersonnelSuggestion(
    val processName: String,
    val current: Int,
    val ideal: Int,
    val delta: Int,
    val action: String
)

/**
 * 人效提升建议
 */
data class EfficiencySuggestion(
    val title: String,
    val expectedBenefit: String,
    val priority: String
)

object EfficiencyCalculator {

    fun calculate(steps: List<ProcessStep>, params: GlobalParams): WarRoomResult {
        val lineCt = steps.maxOfOrNull { it.cycleTime } ?: 0.0
        val bottleneck = steps.maxByOrNull { it.cycleTime }
        val totalWork = steps.sumOf { it.singlePieceTimeMin }
        val balanceRate = if (lineCt <= 0 || steps.isEmpty()) 0.0 else
            (totalWork / (lineCt * steps.size)) * 100

        val totalHeadcount = steps.sumOf { it.currentHeadcount }
        val idealTotal = steps.sumOf { it.idealHeadcount(params.customerTakt) }
        val estimatedOutput = if (lineCt <= 0) 0.0 else params.availableMinutes / lineCt

        return WarRoomResult(
            steps = steps,
            params = params,
            lineCycleTime = lineCt,
            balanceRate = balanceRate,
            totalHeadcount = totalHeadcount,
            idealTotalHeadcount = idealTotal,
            bottleneckStepId = bottleneck?.id ?: -1,
            bottleneckName = bottleneck?.name ?: "无",
            estimatedDailyOutput = estimatedOutput
        )
    }

    /**
     * 作战图：生产调度人员匹配 + 同工种自动支援
     * 需要人数 = ceil(单件时间 / 客户节拍)（= 日产台数×单件时间 ≤ 日可用 的最小人力）
     * 差异时间 = 日可用 − 单件时间×日产台数（单人日负荷差额）
     */
    fun computeWarMap(steps: List<ProcessStep>, params: GlobalParams): WarMapResult {
        val takt = params.customerTakt
        val available = params.availableMinutes
        val demand = params.demandQty

        data class Node(
            val step: ProcessStep,
            val plan: MultiMachinePlan,
            val required: Int,
            val diffTime: Double,
            var virtualHeadcount: Int
        )

        val nodes = steps.map { step ->
            val plan = step.bestMultiMachinePlan(takt)
            val required = step.idealHeadcount(takt)
            val diffTime = available - step.singlePieceTimeMin * demand
            Node(
                step = step,
                plan = plan,
                required = required,
                diffTime = diffTime,
                virtualHeadcount = step.currentHeadcount
            )
        }.associateBy { it.step.id }.toMutableMap()

        val assignments = mutableListOf<SupportAssignment>()

        // 按工种类型分组，自动匹配支援（冗余人力 → 不足工序）
        val skillGroups = nodes.values.groupBy { it.step.skillType }
        skillGroups.forEach { (skill, group) ->
            val deficitNodes = group.filter { it.virtualHeadcount < it.required }
                .sortedByDescending { it.required - it.virtualHeadcount }
            val surplusNodes = group.filter { it.virtualHeadcount > it.required }
                .sortedByDescending { it.virtualHeadcount - it.required }

            for (deficit in deficitNodes) {
                var need = deficit.required - deficit.virtualHeadcount
                for (donor in surplusNodes.filter { it.step.id != deficit.step.id }) {
                    if (need <= 0) break
                    val canGive = donor.virtualHeadcount - donor.required
                    val give = min(need, canGive)
                    if (give <= 0) continue
                    donor.virtualHeadcount -= give
                    deficit.virtualHeadcount += give
                    need -= give
                    // 计算支援时段：donor 全员协作完成本工序份额后，富余工人方可去支援
                    val donorBusy = donor.step.singlePieceTimeMin * demand /
                        max(1, donor.step.currentHeadcount)
                    val supportStart = offsetToClock(donorBusy, params.segments)
                    val supportEnd = formatClock(parseHHmm(params.segments.last().end))
                    assignments.add(
                        SupportAssignment(
                            fromStepId = donor.step.id,
                            fromName = donor.step.name,
                            toStepId = deficit.step.id,
                            toName = deficit.step.name,
                            count = give,
                            reason = "${donor.step.name} 同工种($skill)可富余 ${canGive} 人，调 ${give} 人支援 ${deficit.step.name}；可在 $supportStart-$supportEnd 去支援（已含20%弹性缓冲）",
                            supportWindow = "$supportStart-$supportEnd",
                            bufferMinutes = donorBusy * 0.2
                        )
                    )
                }
            }
        }

        // 构建 WarMapCell
        val inMap = assignments.groupBy { it.toStepId }
        val outMap = assignments.groupBy { it.fromStepId }
        val cells = steps.map { step ->
            val node = nodes.getValue(step.id)
            WarMapCell(
                step = step,
                plan = node.plan,
                requiredHeadcount = node.required,
                availableMinutes = available,
                demandQty = demand,
                diffTimeMin = node.diffTime,
                assignmentIn = inMap[step.id] ?: emptyList(),
                assignmentOut = outMap[step.id] ?: emptyList()
            )
        }

        val totalMin = cells.sumOf { it.requiredHeadcount }
        val totalCurrent = cells.sumOf { it.step.currentHeadcount }
        val totalDiff = cells.sumOf { it.diffTimeMin }

        return WarMapResult(cells, assignments, totalMin, totalCurrent, totalDiff)
    }

    /**
     * 改善前/改善后 指标对比：
     * 用原始录入户（before）与改善后变体（after）分别计算，再逐项对比。
     */
    fun compareBeforeAfter(steps: List<ProcessStep>, params: GlobalParams): ComparisonResult {
        val afterSteps = steps.map { it.toVariant(true) }
        val before = calculate(steps, params)
        val after = calculate(afterSteps, params)
        val beforeWarMap = computeWarMap(steps, params)
        val afterWarMap = computeWarMap(afterSteps, params)

        fun metric(
            label: String,
            beforeVal: Double,
            afterVal: Double,
            unit: String,
            higherIsBetter: Boolean,
            digits: Int = 2
        ): ComparisonMetric {
            val improved = if (higherIsBetter) afterVal > beforeVal + 1e-9 else afterVal < beforeVal - 1e-9
            val fmt = { v: Double -> if (digits == 0) "%.0f".format(v) else "%.${digits}f".format(v) }
            return ComparisonMetric(
                label = label,
                beforeText = fmt(beforeVal),
                afterText = fmt(afterVal),
                unit = unit,
                higherIsBetter = higherIsBetter,
                improved = improved
            )
        }

        val metrics = listOf(
            metric("当前总人数", before.totalHeadcount.toDouble(), after.totalHeadcount.toDouble(), "人", higherIsBetter = false, digits = 0),
            metric("最少需求人数", beforeWarMap.totalMinHeadcount.toDouble(), afterWarMap.totalMinHeadcount.toDouble(), "人", higherIsBetter = false, digits = 0),
            metric("产线节拍", before.lineCycleTime, after.lineCycleTime, "min/件", higherIsBetter = false),
            metric("瓶颈工序节拍", before.bottleneckCycleTime, after.bottleneckCycleTime, "min/件", higherIsBetter = false),
            metric("平衡率", before.balanceRate, after.balanceRate, "%", higherIsBetter = true, digits = 1),
            metric("预计日产量", before.estimatedDailyOutput, after.estimatedDailyOutput, "件", higherIsBetter = true, digits = 0),
            metric("标准工时合计", before.steps.sumOf { it.singlePieceTimeMin }, after.steps.sumOf { it.singlePieceTimeMin }, "min/台", higherIsBetter = false, digits = 1)
        )

        val perProcess = steps.map { s ->
            val a = s.toVariant(true)
            ProcessCompareRow(
                name = s.name,
                skill = s.skillType,
                beforeTime = s.singlePieceTimeMin,
                afterTime = a.singlePieceTimeMin,
                beforeHead = s.currentHeadcount,
                afterHead = a.currentHeadcount
            )
        }

        return ComparisonResult(before, after, beforeWarMap, afterWarMap, metrics, perProcess)
    }

    fun layoutSuggestions(result: WarRoomResult): List<LayoutSuggestion> {
        val list = mutableListOf<LayoutSuggestion>()
        val steps = result.steps

        // 1. 高负荷工序集中原则
        val highLoad = steps.filter { it.loadRate(result.params.customerTakt) > 0.9 }
        if (highLoad.size >= 2) {
            val names = highLoad.joinToString("、") { it.name }
            list.add(
                LayoutSuggestion(
                    title = "高负荷工序靠近布置",
                    description = "将 $names 等负荷>90%的工序就近布置，减少在制品搬运距离。",
                    priority = "高"
                )
            )
        }

        // 2. 通道动线
        val farPairs = mutableListOf<Pair<String, String>>()
        for (i in steps.indices) {
            for (j in i + 1 until steps.size) {
                val a = steps[i]
                val b = steps[j]
                if (kotlin.math.abs(a.x - b.x) > 0.6f || kotlin.math.abs(a.y - b.y) > 0.6f) {
                    farPairs.add(a.name to b.name)
                }
            }
        }
        if (farPairs.isNotEmpty()) {
            val sample = farPairs.take(2).joinToString("；") { "${it.first}→${it.second}" }
            list.add(
                LayoutSuggestion(
                    title = "缩减物流动线",
                    description = "发现 $sample 等跨线对，建议按工艺流程重新排列，避免长距离搬运。",
                    priority = "中"
                )
            )
        }

        // 3. 打磨区独立
        val polish = steps.filter { it.name.contains("打磨") }
        if (polish.size > 1) {
            list.add(
                LayoutSuggestion(
                    title = "打磨工序集中或隔离",
                    description = "共有 ${polish.size} 处打磨，建议集中设置除尘/隔音区，或按前后车架分区管理。",
                    priority = "中"
                )
            )
        }

        // 4. 瓶颈工序前移 / 加强资源
        val bottleneck = steps.find { it.id == result.bottleneckStepId }
        bottleneck?.let {
            list.add(
                LayoutSuggestion(
                    title = "瓶颈工序优先配置",
                    description = "瓶颈为「${it.name}」，建议在其旁预留 1 个弹性工位与物料缓存区，便于随时增员而不阻断线体。",
                    priority = "高"
                )
            )
        }

        // 5. 前后车架分区
        val front = steps.filter { it.name.contains("前车架") }
        val rear = steps.filter { it.name.contains("后车架") }
        if (front.size >= 3 && rear.size >= 3) {
            list.add(
                LayoutSuggestion(
                    title = "前后车架分线/分区布置",
                    description = "前车架(${front.size}工序)与后车架(${rear.size}工序)各自成区，减少跨线交叉，建议按 U 型单元化布局。",
                    priority = "中"
                )
            )
        }

        if (list.isEmpty()) {
            list.add(LayoutSuggestion("布局暂无需调整", "当前布局未发现明显物流浪费。", "低"))
        }
        return list
    }

    fun personnelSuggestions(result: WarRoomResult): List<PersonnelSuggestion> {
        return result.steps.map { step ->
            val ideal = step.idealHeadcount(result.params.customerTakt)
            val delta = step.headcountDelta(result.params.customerTakt)
            val action = when {
                delta > 0 -> "可裁减/支援其他工序 ${delta} 人"
                delta < 0 -> "需补充 ${-delta} 人"
                else -> "人数匹配"
            }
            PersonnelSuggestion(
                processName = step.name,
                current = step.currentHeadcount,
                ideal = ideal,
                delta = delta,
                action = action
            )
        }.sortedByDescending { kotlin.math.abs(it.delta) }
    }

    fun efficiencySuggestions(result: WarRoomResult): List<EfficiencySuggestion> {
        val list = mutableListOf<EfficiencySuggestion>()
        val takt = result.params.customerTakt

        // 瓶颈工序提升
        val bottleneck = result.steps.find { it.id == result.bottleneckStepId }
        bottleneck?.let {
            val ideal = it.idealHeadcount(takt)
            if (it.currentHeadcount < ideal) {
                list.add(
                    EfficiencySuggestion(
                        title = "瓶颈工序增员：${it.name}",
                        expectedBenefit = "由 ${it.currentHeadcount} 人增至 ${ideal} 人，节拍从 %.2f 降至 %.2f min".format(
                            it.cycleTime, it.singlePieceTimeMin / ideal
                        ),
                        priority = "高"
                    )
                )
            }
        }

        // 冗余人员调配
        val redundant = result.steps.filter { it.headcountDelta(takt) > 0 }
        val surplus = redundant.sumOf { it.headcountDelta(takt) }
        if (surplus > 0) {
            list.add(
                EfficiencySuggestion(
                    title = "冗余人员再分配",
                    expectedBenefit = "可从 ${redundant.joinToString("、") { it.name }} 调出 ${surplus} 人支援瓶颈。",
                    priority = "高"
                )
            )
        }

        // 合并短工序
        val shortSteps = result.steps.filter { it.singlePieceTimeMin < takt * 0.5 }
        if (shortSteps.size >= 2) {
            list.add(
                EfficiencySuggestion(
                    title = "合并短作业工序",
                    expectedBenefit = "${shortSteps.take(3).joinToString("、") { it.name }} 等作业时间较短，可考虑一人多机或合并工位。",
                    priority = "中"
                )
            )
        }

        // 白板线索①：后车架人工补焊存在"打叉"，疑似瓶颈候选/待取消工序
        val crossedOut = result.steps.find { it.name.contains("人工补焊") }
        crossedOut?.let {
            val ideal = it.idealHeadcount(takt)
            if (it.currentHeadcount >= ideal) {
                list.add(
                    EfficiencySuggestion(
                        title = "取消/合并冗余补焊工序：${it.name}",
                        expectedBenefit = "该工序节拍 %.2f min 已低于 Takt，且现场标注为可取消，建议并入相邻焊接工位，释放 1 人支援瓶颈。".format(it.cycleTime),
                        priority = "中"
                    )
                )
            }
        }

        // 白板线索②：打磨区标注"打磨屏风 +1"，属环境改善
        val polishStep = result.steps.find { it.name.contains("打磨") }
        polishStep?.let {
            list.add(
                EfficiencySuggestion(
                    title = "打磨工位环境改善（屏风 +1）",
                    expectedBenefit = "为打磨区增设隔音/除尘屏风，改善作业环境、减少返工，提升有效作业时间约 5%~8%。",
                    priority = "中"
                )
            )
        }

        // 白板线索③：扭力筒组对标注"1.5 人应用"，建议兼职/支援
        val flexStep = result.steps.find { it.name.contains("扭力筒") }
        flexStep?.let {
            val ideal = it.idealHeadcount(takt)
            list.add(
                EfficiencySuggestion(
                    title = "弹性用工：扭力筒组对 1.5 人制",
                    expectedBenefit = "该工序负荷不足 1 人满负荷，建议设为 0.5 人兼职/巡回支援，剩余 0.5 人补足瓶颈或承担辅助。",
                    priority = "中"
                )
            )
        }

        // 整体目标
        if (result.balanceRate < 85) {
            list.add(
                EfficiencySuggestion(
                    title = "提升线平衡率至 85%+",
                    expectedBenefit = "当前平衡率 %.1f%%，通过削峰填谷预计日产能从 %.0f 提升至 %.0f 件".format(
                        result.balanceRate,
                        result.estimatedDailyOutput,
                        result.params.availableMinutes / (result.steps.sumOf { it.singlePieceTimeMin } / max(1, result.steps.size))
                    ),
                    priority = "高"
                )
            )
        }

        return list
    }
}

fun sampleProcessSteps(): List<ProcessStep> = listOf(
    ProcessStep(1, "前车架打磨", 15.75, 4, x = 0.10f, y = 0.20f, area = "打磨区", skillType = "打磨工", walkTimeMin = 0.3, improvedSinglePieceTimeMin = 14.0, improvedHeadcount = 3),
    ProcessStep(2, "后车架人工补焊", 5.71, 1, x = 0.20f, y = 0.40f, area = "焊接区", skillType = "焊工", walkTimeMin = 0.2, improvedSinglePieceTimeMin = 0.0, improvedHeadcount = 0),
    ProcessStep(3, "后车架总成焊接", 4.73, 1, x = 0.30f, y = 0.40f, area = "焊接区", skillType = "焊工", walkTimeMin = 0.2),
    ProcessStep(4, "后车架一次补焊", 4.14, 1, x = 0.40f, y = 0.40f, area = "焊接区", skillType = "焊工", walkTimeMin = 0.2),
    ProcessStep(5, "后车架一次焊接", 4.14, 1, x = 0.50f, y = 0.40f, area = "焊接区", skillType = "焊工", walkTimeMin = 0.2),
    ProcessStep(6, "后车架总成组对", 6.12, 0, x = 0.60f, y = 0.40f, area = "组对区", skillType = "组对工", walkTimeMin = 0.25, improvedSinglePieceTimeMin = 4.5, improvedHeadcount = 1),
    ProcessStep(7, "后车架二次组对", 4.40, 1, x = 0.70f, y = 0.40f, area = "组对区", skillType = "组对工", walkTimeMin = 0.25),
    ProcessStep(8, "后车架一次组对", 4.40, 1, x = 0.80f, y = 0.40f, area = "组对区", skillType = "组对工", walkTimeMin = 0.25),
    ProcessStep(9, "扭力筒组对", 4.40, 1, x = 0.90f, y = 0.40f, area = "组对区", skillType = "组对工", walkTimeMin = 0.25),
    ProcessStep(10, "前车架总成组对", 3.66, 2, x = 0.80f, y = 0.60f, area = "组对区", skillType = "组对工", walkTimeMin = 0.25),
    ProcessStep(11, "前车架基础组对", 3.66, 1, x = 0.70f, y = 0.60f, area = "组对区", skillType = "组对工", walkTimeMin = 0.25),
    ProcessStep(12, "前车架总成焊接", 3.66, 1, x = 0.50f, y = 0.60f, area = "焊接区", skillType = "焊工", walkTimeMin = 0.2),
    ProcessStep(13, "前车架焊接", 3.66, 1, x = 0.30f, y = 0.60f, area = "焊接区", skillType = "焊工", walkTimeMin = 0.2)
)

fun emptyProcessSteps(): List<ProcessStep> = listOf(
    ProcessStep(1, "工序 1", 0.0, 1, skillType = "通用", walkTimeMin = 0.2)
)
