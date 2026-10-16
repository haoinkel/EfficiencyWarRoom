package com.leanefficiency.warroom

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leanefficiency.warroom.model.EfficiencyCalculator
import com.leanefficiency.warroom.model.ComparisonMetric
import com.leanefficiency.warroom.model.ComparisonResult
import com.leanefficiency.warroom.model.EfficiencySuggestion
import com.leanefficiency.warroom.model.GlobalParams
import com.leanefficiency.warroom.model.LayoutSuggestion
import com.leanefficiency.warroom.model.PersonnelSuggestion
import com.leanefficiency.warroom.model.ProcessCompareRow
import com.leanefficiency.warroom.model.ProcessStep
import com.leanefficiency.warroom.model.SupportAssignment
import com.leanefficiency.warroom.model.WarMapCell
import com.leanefficiency.warroom.model.WarMapResult
import com.leanefficiency.warroom.model.WarRoomResult
import com.leanefficiency.warroom.model.WorkSegment
import com.leanefficiency.warroom.ui.theme.BottleneckRed
import com.leanefficiency.warroom.ui.theme.EfficiencyWarRoomTheme
import com.leanefficiency.warroom.ui.theme.ShortageAmber
import com.leanefficiency.warroom.ui.theme.SurplusGreen
import com.leanefficiency.warroom.ui.theme.TaktBlue
import com.leanefficiency.warroom.viewmodel.WarRoomViewModel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EfficiencyWarRoomTheme {
                WarRoomApp()
            }
        }
    }
}

sealed class Screen(val route: String, val label: String) {
    object Input : Screen("input", "数据录入")
    object WarMap : Screen("war_map", "调度大屏")
    object Sandbox : Screen("sandbox", "作战沙盘")
    object Compare : Screen("compare", "改善对比")
    object Layout : Screen("layout", "布局调整")
    object Personnel : Screen("personnel", "人员调配")
    object Improvement : Screen("improvement", "提升方案")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarRoomApp(viewModel: WarRoomViewModel = viewModel()) {
    val screens = listOf(
        Screen.Input, Screen.WarMap, Screen.Sandbox,
        Screen.Compare, Screen.Layout, Screen.Personnel, Screen.Improvement
    )
    var selected by remember { mutableStateOf<Screen>(Screen.WarMap) }
    val isWide = LocalConfiguration.current.screenWidthDp >= 600

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selected.label, style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                ),
                actions = {
                    if (selected == Screen.Input) {
                        IconButton(onClick = { viewModel.resetToSample() }) {
                            Text("重置示例", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!isWide) {
                NavigationBar {
                    screens.forEach { screen -> NavItem(screen, selected, false, Modifier.weight(1f)) { selected = screen } }
                }
            }
        }
    ) { paddingValues ->
        if (isWide) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                    screens.forEach { screen -> NavItem(screen, selected, true, Modifier.fillMaxWidth()) { selected = screen } }
                }
                Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                    RenderScreen(selected, viewModel)
                }
            }
        } else {
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                RenderScreen(selected, viewModel)
            }
        }
    }
}

@Composable
fun NavItem(
    screen: Screen,
    selected: Screen,
    isWide: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isSelected = selected == screen
    val contentColor =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val icon: @Composable (Color) -> Unit = { tint ->
        when (screen) {
            Screen.Input -> Icon(Icons.Default.Edit, contentDescription = null, tint = tint)
            Screen.WarMap -> Icon(Icons.Default.Menu, contentDescription = null, tint = tint)
            Screen.Sandbox -> Icon(Icons.Default.Home, contentDescription = null, tint = tint)
            Screen.Compare -> Icon(Icons.Default.List, contentDescription = null, tint = tint)
            Screen.Layout -> Icon(Icons.Default.Place, contentDescription = null, tint = tint)
            Screen.Personnel -> Icon(Icons.Default.Person, contentDescription = null, tint = tint)
            Screen.Improvement -> Icon(Icons.Default.ThumbUp, contentDescription = null, tint = tint)
        }
    }
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = if (isWide) 12.dp else 8.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon(contentColor)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            screen.label,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = contentColor
        )
    }
}

@Composable
fun RenderScreen(selected: Screen, viewModel: WarRoomViewModel) {
    when (selected) {
        Screen.Input -> InputScreen(viewModel)
        Screen.WarMap -> WarMapScreen(viewModel)
        Screen.Sandbox -> SandboxScreen(viewModel)
        Screen.Compare -> CompareScreen(viewModel)
        Screen.Layout -> LayoutScreen(viewModel)
        Screen.Personnel -> PersonnelScreen(viewModel)
        Screen.Improvement -> ImprovementScreen(viewModel)
    }
}

@Composable
fun InputScreen(viewModel: WarRoomViewModel) {
    val steps = viewModel.steps
    val params = viewModel.params.value
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("全局参数", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(
                        label = "日需求(件/台)",
                        value = params.demandQty,
                        onValueChange = { viewModel.updateParams(params.copy(demandQty = it)) },
                        modifier = Modifier.weight(1f)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("日可用时间(min)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(
                            String.format("%.0f", params.availableMinutes),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("由工时三段自动累加", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("工作时段（可增删，自动累加可用时间）", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                params.segments.forEachIndexed { idx, seg ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = seg.start,
                            onValueChange = { newStart ->
                                val newSegs = params.segments.toMutableList()
                                    .also { it[idx] = it[idx].copy(start = newStart) }
                                viewModel.updateParams(params.copy(segments = newSegs))
                            },
                            label = { Text("开始") }, singleLine = true, modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = seg.end,
                            onValueChange = { newEnd ->
                                val newSegs = params.segments.toMutableList()
                                    .also { it[idx] = it[idx].copy(end = newEnd) }
                                viewModel.updateParams(params.copy(segments = newSegs))
                            },
                            label = { Text("结束") }, singleLine = true, modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (params.segments.size > 1) {
                                    val newSegs = params.segments.toMutableList().also { it.removeAt(idx) }
                                    viewModel.updateParams(params.copy(segments = newSegs))
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "删除该段")
                        }
                    }
                }
                TextButton(
                    onClick = {
                        val last = params.segments.last()
                        val newSegs = params.segments + WorkSegment(last.end, last.end)
                        viewModel.updateParams(params.copy(segments = newSegs))
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("添加工作时段")
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "工时安排：${params.shiftSummary}　|　日可用 ${String.format("%.0f", params.availableMinutes)} min　|　客户节拍 Takt = ${String.format("%.2f", params.customerTakt)} 分钟/件",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TaktBlue
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("工序清单 (${steps.size})", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("添加工序")
            }
        }
        Spacer(Modifier.height(8.dp))

        steps.forEachIndexed { index, step ->
            StepEditableCard(index = index, step = step, onUpdate = { viewModel.updateStep(index, it) }, onDelete = { viewModel.removeStep(index) })
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(80.dp))
    }

    if (showAddDialog) {
        AddStepDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, time, count, skill, walk ->
                viewModel.addStep(
                    ProcessStep(
                        id = (steps.maxOfOrNull { it.id } ?: 0) + 1,
                        name = name,
                        singlePieceTimeMin = time,
                        currentHeadcount = count,
                        skillType = skill,
                        walkTimeMin = walk
                    )
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
fun StepEditableCard(index: Int, step: ProcessStep, onUpdate: (ProcessStep) -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("#${index + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Clear, contentDescription = "删除", tint = Color.Gray)
                }
            }
            OutlinedTextField(
                value = step.name,
                onValueChange = { onUpdate(step.copy(name = it)) },
                label = { Text("工序名称") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    label = "单件时间(min)",
                    value = step.singlePieceTimeMin,
                    onValueChange = { onUpdate(step.copy(singlePieceTimeMin = it)) },
                    modifier = Modifier.weight(1f)
                )
                NumberField(
                    label = "当前人数",
                    value = step.currentHeadcount.toDouble(),
                    onValueChange = { onUpdate(step.copy(currentHeadcount = max(1, it.toInt()))) },
                    modifier = Modifier.weight(1f),
                    isInteger = true
                )
                NumberField(
                    label = "工位数",
                    value = step.stationCount.toDouble(),
                    onValueChange = { onUpdate(step.copy(stationCount = max(1, it.toInt()))) },
                    modifier = Modifier.weight(1f),
                    isInteger = true
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    label = "布局 X",
                    value = step.x.toDouble(),
                    onValueChange = { onUpdate(step.copy(x = it.toFloat())) },
                    modifier = Modifier.weight(1f)
                )
                NumberField(
                    label = "布局 Y",
                    value = step.y.toDouble(),
                    onValueChange = { onUpdate(step.copy(y = it.toFloat())) },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = step.area,
                    onValueChange = { onUpdate(step.copy(area = it)) },
                    label = { Text("区域") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = step.skillType,
                    onValueChange = { onUpdate(step.copy(skillType = it)) },
                    label = { Text("工种类型") },
                    modifier = Modifier.weight(1f)
                )
                NumberField(
                    label = "走动时间(min)",
                    value = step.walkTimeMin,
                    onValueChange = { onUpdate(step.copy(walkTimeMin = it)) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("改善后（留空=不变，用于改善对比页）", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = if (step.improvedSinglePieceTimeMin == null) "" else step.improvedSinglePieceTimeMin.toString(),
                    onValueChange = { v -> onUpdate(step.copy(improvedSinglePieceTimeMin = v.toDoubleOrNull())) },
                    label = { Text("改善后单件(min)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = if (step.improvedHeadcount == null) "" else step.improvedHeadcount.toString(),
                    onValueChange = { v -> onUpdate(step.copy(improvedHeadcount = v.toIntOrNull())) },
                    label = { Text("改善后人数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun NumberField(label: String, value: Double, onValueChange: (Double) -> Unit, modifier: Modifier = Modifier, isInteger: Boolean = false) {
    OutlinedTextField(
        value = if (value == 0.0) "" else value.toString().removeSuffix(".0"),
        onValueChange = {
            val v = it.toDoubleOrNull() ?: 0.0
            onValueChange(if (isInteger) v.toDouble() else v)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

@Composable
fun AddStepDialog(onDismiss: () -> Unit, onConfirm: (String, Double, Int, String, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("1.0") }
    var count by remember { mutableStateOf("1") }
    var skill by remember { mutableStateOf("通用") }
    var walk by remember { mutableStateOf("0.2") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加工序") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("工序名称") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("单件时间(min)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = count, onValueChange = { count = it }, label = { Text("当前人数") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = skill, onValueChange = { skill = it }, label = { Text("工种类型") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = walk, onValueChange = { walk = it }, label = { Text("走动时间(min)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    name,
                    time.toDoubleOrNull() ?: 1.0,
                    count.toIntOrNull() ?: 1,
                    skill,
                    walk.toDoubleOrNull() ?: 0.2
                )
            }) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun SandboxScreen(viewModel: WarRoomViewModel) {
    val result = viewModel.result.value
    val params = result.params
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        KpiCards(result)
        Spacer(Modifier.height(16.dp))
        Text("节拍 vs 客户节拍", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        CycleTimeBarChart(result)
        Spacer(Modifier.height(16.dp))
        Text("负荷率", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LoadRateBarChart(result)
        Spacer(Modifier.height(16.dp))
        Text("人员差异", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        HeadcountDeltaChart(result)
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun KpiCards(result: WarRoomResult) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard(
                title = "产线节拍",
                value = String.format("%.2f", result.lineCycleTime),
                unit = "min/件",
                color = BottleneckRed,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "平衡率",
                value = String.format("%.1f", result.balanceRate),
                unit = "%",
                color = TaktBlue,
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard(
                title = "瓶颈工序",
                value = result.bottleneckName,
                unit = "",
                color = ShortageAmber,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "预计日产量",
                value = String.format("%.0f", result.estimatedDailyOutput),
                unit = "件",
                color = SurplusGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun KpiCard(title: String, value: String, unit: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = color)
            if (unit.isNotEmpty()) {
                Text(unit, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun CycleTimeBarChart(result: WarRoomResult) {
    val takt = result.params.customerTakt
    val maxVal = max(result.lineCycleTime, takt) * 1.1
    if (maxVal <= 0) return
    val primaryColor = MaterialTheme.colorScheme.primary
    Card(modifier = Modifier.fillMaxWidth().height(260.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidth = size.width / (result.steps.size * 2 + 1)
                    val baselineY = size.height - 24.dp.toPx()
                    drawLine(
                        color = TaktBlue,
                        start = Offset(0f, baselineY - (takt / maxVal * baselineY).toFloat()),
                        end = Offset(size.width, baselineY - (takt / maxVal * baselineY).toFloat()),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    result.steps.forEachIndexed { index, step ->
                        val h = (step.cycleTime / maxVal * baselineY).toFloat()
                        val color = if (step.id == result.bottleneckStepId) BottleneckRed else primaryColor
                        drawRect(
                            color = color,
                            topLeft = Offset(barWidth * (index * 2 + 1), baselineY - h),
                            size = Size(barWidth, h)
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("蓝色虚线 = 客户节拍", style = MaterialTheme.typography.labelSmall, color = TaktBlue)
                Text(String.format("Takt=%.2f", takt), style = MaterialTheme.typography.labelSmall, color = TaktBlue)
            }
            LazyColumn(modifier = Modifier.height(40.dp)) {
                itemsIndexed(result.steps) { index, step ->
                    Text(
                        "#${index + 1} ${step.name}: ${String.format("%.2f", step.cycleTime)}",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun LoadRateBarChart(result: WarRoomResult) {
    val takt = result.params.customerTakt
    Card(modifier = Modifier.fillMaxWidth().height(220.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidth = size.width / (result.steps.size * 2 + 1)
                    val baselineY = size.height
                    drawLine(
                        color = TaktBlue,
                        start = Offset(0f, baselineY - baselineY * 1.0f),
                        end = Offset(size.width, baselineY - baselineY * 1.0f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    result.steps.forEachIndexed { index, step ->
                        val rate = min(step.loadRate(takt), 1.2)
                        val h = (rate * baselineY).toFloat()
                        val color = when {
                            rate > 0.95 -> BottleneckRed
                            rate > 0.8 -> ShortageAmber
                            else -> SurplusGreen
                        }
                        drawRect(
                            color = color,
                            topLeft = Offset(barWidth * (index * 2 + 1), baselineY - h),
                            size = Size(barWidth, h)
                        )
                    }
                }
            }
            Text("绿<80% 黄80~95% 红>95%（相对客户节拍）", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun HeadcountDeltaChart(result: WarRoomResult) {
    val takt = result.params.customerTakt
    Card(modifier = Modifier.fillMaxWidth().height(220.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val baselineY = size.height / 2
                    drawLine(
                        color = Color.LightGray,
                        start = Offset(0f, baselineY),
                        end = Offset(size.width, baselineY),
                        strokeWidth = 1.dp.toPx()
                    )
                    val maxDelta = max(result.steps.maxOfOrNull { abs(it.headcountDelta(takt)) } ?: 1, 1).toFloat()
                    val barWidth = size.width / (result.steps.size * 2 + 1)
                    result.steps.forEachIndexed { index, step ->
                        val delta = step.headcountDelta(takt).toFloat()
                        val h = (abs(delta) / maxDelta * baselineY).toFloat()
                        val color = if (delta > 0) SurplusGreen else if (delta < 0) BottleneckRed else Color.Gray
                        val top = if (delta >= 0) baselineY - h else baselineY
                        drawRect(
                            color = color,
                            topLeft = Offset(barWidth * (index * 2 + 1), top),
                            size = Size(barWidth, h)
                        )
                    }
                }
            }
            Text("正值=冗余，负值=不足", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun WarMapProcessTable(
    warMap: WarMapResult,
    result: WarRoomResult,
    modifier: Modifier = Modifier
) {
    val cells = warMap.cells
    if (cells.isEmpty()) return

    val cellWidth = 88.dp
    val rowHeight = 76.dp
    val headerHeight = 56.dp
    val labelWidth = 96.dp
    val scrollState = rememberScrollState()
    val supports = warMap.supports

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Text(
                "← 左右拖动查看全部工序 →",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                // 固定左侧标签列
                Column(modifier = Modifier.width(labelWidth)) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(headerHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("指标 \\ 工序", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight)
                            .padding(4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text("布局图", style = MaterialTheme.typography.labelMedium, color = Color.DarkGray)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight)
                            .padding(4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text("协作流程图", style = MaterialTheme.typography.labelMedium, color = Color.DarkGray)
                    }
                }

                // 可横向滚动的工序列
                Column {
                    // 表头
                    Row {
                        cells.forEachIndexed { index, cell ->
                            Box(
                                modifier = Modifier
                                    .width(cellWidth)
                                    .height(headerHeight)
                                    .padding(2.dp)
                                    .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp))
                                    .border(0.5.dp, Color.LightGray, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "工序${index + 1}\n${cell.step.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    // 布局图行
                    Row {
                        cells.forEach { cell ->
                            val color = when {
                                cell.step.id == result.bottleneckStepId -> BottleneckRed
                                cell.step.loadRate(result.params.customerTakt) > 0.9 -> ShortageAmber
                                cell.step.headcountDelta(result.params.customerTakt) > 0 -> SurplusGreen
                                else -> MaterialTheme.colorScheme.primary
                            }
                            Box(
                                modifier = Modifier
                                    .width(cellWidth)
                                    .height(rowHeight)
                                    .padding(2.dp)
                                    .border(0.5.dp, Color.LightGray, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .width(48.dp)
                                            .height(28.dp)
                                            .background(color, RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${cell.step.stationCount}工位",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "${cell.step.currentHeadcount}人",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                    // 协作流程图行
                    Box(
                        modifier = Modifier
                            .width(cellWidth * cells.size)
                            .height(rowHeight)
                    ) {
                        if (supports.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("当前无跨工序支援需求", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        } else {
                            val density = androidx.compose.ui.platform.LocalDensity.current
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stepPx = with(density) { cellWidth.toPx() }
                                val rowPx = with(density) { rowHeight.toPx() }
                                val lineStroke = with(density) { 2.dp.toPx() }
                                val arrowLen = with(density) { 6.dp.toPx() }
                                val arrowWing = with(density) { 3.dp.toPx() }

                                // 列分隔虚线
                                for (i in 0..cells.size) {
                                    val x = i * stepPx
                                    drawLine(
                                        color = Color(0xFFE0E0E0),
                                        start = Offset(x, 0f),
                                        end = Offset(x, rowPx),
                                        strokeWidth = with(density) { 0.5.dp.toPx() }
                                    )
                                }

                                supports.forEachIndexed { idx, support ->
                                    val fromIndex = cells.indexOfFirst { it.step.id == support.fromStepId }
                                    val toIndex = cells.indexOfFirst { it.step.id == support.toStepId }
                                    if (fromIndex < 0 || toIndex < 0) return@forEachIndexed

                                    val fromX = fromIndex * stepPx + stepPx / 2
                                    val toX = toIndex * stepPx + stepPx / 2
                                    val color = if (idx % 2 == 0) Color(0xFFE53935) else Color(0xFF1976D2)

                                    // 错开高度，避免多条支援线重叠
                                    val lane = idx % 3
                                    val y = rowPx * (0.3f + lane * 0.22f)

                                    val leftX = minOf(fromX, toX)
                                    val rightX = maxOf(fromX, toX)
                                    drawLine(color, Offset(leftX, y), Offset(rightX, y), lineStroke)

                                    // 箭头指向 to 列
                                    val dir = if (toX > fromX) 1f else -1f
                                    val tipX = toX
                                    drawLine(color, Offset(tipX - dir * arrowLen, y - arrowWing), Offset(tipX, y), lineStroke)
                                    drawLine(color, Offset(tipX - dir * arrowLen, y + arrowWing), Offset(tipX, y), lineStroke)
                                }
                            }

                            // 支援标签覆盖层
                            supports.forEachIndexed { idx, support ->
                                val fromIndex = cells.indexOfFirst { it.step.id == support.fromStepId }
                                val toIndex = cells.indexOfFirst { it.step.id == support.toStepId }
                                if (fromIndex < 0 || toIndex < 0) return@forEachIndexed

                                val midIndex = (fromIndex + toIndex) / 2
                                val lane = idx % 3
                                val xCenter = cellWidth * midIndex + cellWidth / 2
                                val y = rowHeight * (0.3f + lane * 0.22f)

                                Box(
                                    modifier = Modifier
                                        .offset(
                                            x = xCenter - 60.dp,
                                            y = y - 24.dp
                                        )
                                        .width(120.dp)
                                        .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(4.dp))
                                        .border(0.5.dp, Color.LightGray, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${support.fromName}派${support.count}人\n${support.supportWindow}支援${support.toName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF2C3E50),
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WarMapScreen(viewModel: WarRoomViewModel) {
    val warMap = viewModel.warMap.value
    val params = viewModel.params.value
    val result = viewModel.result.value
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFE8F4FD)).padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("生产调度大屏（人效作战图）", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        ScenarioSelector(viewModel)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard(title = "客户节拍", value = String.format("%.2f", params.customerTakt), unit = "min/件", color = TaktBlue, modifier = Modifier.weight(1f))
            KpiCard(title = "日产台数", value = String.format("%.0f", params.demandQty), unit = "台/日", color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard(title = "最少人数", value = warMap.totalMinHeadcount.toString(), unit = "人", color = BottleneckRed, modifier = Modifier.weight(1f))
            KpiCard(title = "当前人数", value = warMap.totalCurrentHeadcount.toString(), unit = "人", color = ShortageAmber, modifier = Modifier.weight(1f))
            KpiCard(title = "瓶颈工序", value = result.bottleneckName, unit = "", color = SurplusGreen, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        WarMapProcessTable(warMap, result)
        Spacer(Modifier.height(16.dp))
        Text("自动支援方案", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (warMap.supports.isEmpty()) {
            Text("当前配置无需跨工序支援，所有工种已自足或无人富余。", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        } else {
            warMap.supports.forEach { support ->
                SupportCard(support)
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun ScenarioSelector(viewModel: WarRoomViewModel) {
    val params = viewModel.params.value
    val qty = params.demandQty
    var manualText by remember(qty) { mutableStateOf(qty.toInt().toString()) }
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("生产调度场景（日产台数）", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.updateParams(params.copy(demandQty = max(1.0, qty - 1))) }) {
                        Text("－", style = MaterialTheme.typography.titleLarge)
                    }
                    Text(String.format("%.0f", qty), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = { viewModel.updateParams(params.copy(demandQty = qty + 1)) }) {
                        Text("＋", style = MaterialTheme.typography.titleLarge)
                    }
                    Text("台/日", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = manualText,
                    onValueChange = { input ->
                        manualText = input.filter { it.isDigit() }.take(6)
                        val v = manualText.toIntOrNull()
                        if (v != null && v >= 1) viewModel.updateParams(params.copy(demandQty = v.toDouble()))
                    },
                    label = { Text("手动输入台数") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Text("回车/失焦即生效，可填任意正整数。", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1, 2, 3, 5, 10, 20, 50, 80).forEach { preset ->
                    val sel = qty.toInt() == preset
                    Button(
                        onClick = {
                            manualText = preset.toString()
                            viewModel.updateParams(params.copy(demandQty = preset.toDouble()))
                        },
                        colors = if (sel) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        else ButtonDefaults.outlinedButtonColors(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("${preset}台", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "切换日产台数将实时重算客户节拍、最少人数与同工种支援方案，可直接作为车间大屏生产调度看板。",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun WarMapMatrix(warMap: WarMapResult, result: WarRoomResult, scrollState: ScrollState = rememberScrollState()) {
    val cells = warMap.cells
    if (cells.isEmpty()) return
    val cellWidth = 88.dp
    val rowHeight = 44.dp
    val labelWidth = 120.dp

    Card(modifier = Modifier.fillMaxWidth().height(380.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            // 表头
            WarMapHeaderRow(cells, cellWidth, labelWidth, rowHeight, scrollState)
            // 数据行
            WarMapDataRow("单人单台时间", cells.map { String.format("%.2f", it.step.singlePieceTimeMin) }, cellWidth, labelWidth, rowHeight, scrollState)
            WarMapDataRow("N台时间", cells.map { String.format("%.2f", it.multiMachineTimeMin) }, cellWidth, labelWidth, rowHeight, scrollState)
            WarMapDataRow("日可用时间", cells.map { String.format("%.0f", it.availableMinutes) }, cellWidth, labelWidth, rowHeight, scrollState)
            WarMapDataRow("差异时间", cells.map { String.format("%.2f", it.diffTimeMin) }, cellWidth, labelWidth, rowHeight, scrollState, colorize = true)
            WarMapDataRow("需要人数", cells.map { it.requiredHeadcountText }, cellWidth, labelWidth, rowHeight, scrollState)
            WarMapDataRow("人员配置", cells.map { it.configStatus }, cellWidth, labelWidth, rowHeight, scrollState)
        }
    }
}

@Composable
fun WarMapHeaderRow(cells: List<WarMapCell>, cellWidth: Dp, labelWidth: Dp, rowHeight: Dp, scrollState: ScrollState) {
    Row(Modifier.horizontalScroll(scrollState).height(rowHeight)) {
        Box(modifier = Modifier.width(labelWidth).height(rowHeight).padding(4.dp), contentAlignment = Alignment.CenterStart) {
            Text("指标 \\ 工序", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        cells.forEach { cell ->
            Box(modifier = Modifier.width(cellWidth).height(rowHeight).padding(4.dp), contentAlignment = Alignment.Center) {
                Text(cell.step.name, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun WarMapDataRow(
    label: String,
    values: List<String>,
    cellWidth: Dp,
    labelWidth: Dp,
    rowHeight: Dp,
    scrollState: ScrollState,
    colorize: Boolean = false
) {
    Row(Modifier.horizontalScroll(scrollState).height(rowHeight)) {
        Box(modifier = Modifier.width(labelWidth).height(rowHeight).padding(4.dp), contentAlignment = Alignment.CenterStart) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
        }
        values.forEachIndexed { index, value ->
            val color = if (colorize && value.toDoubleOrNull() != null) {
                val v = value.toDouble()
                if (v > 0.5) SurplusGreen.copy(alpha = 0.15f)
                else if (v < -0.5) BottleneckRed.copy(alpha = 0.15f)
                else Color.Transparent
            } else Color.Transparent
            Box(
                modifier = Modifier
                    .width(cellWidth)
                    .height(rowHeight)
                    .padding(2.dp)
                    .background(color, RoundedCornerShape(4.dp))
                    .border(0.5.dp, Color.LightGray, RoundedCornerShape(4.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(value, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun WarMapFlowDiagram(
    warMap: WarMapResult,
    result: WarRoomResult,
    modifier: Modifier = Modifier
) {
    val supports = warMap.supports

    // 无支援时给出明确提示（淡蓝底卡片）
    if (supports.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F4FD))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.background(Color(0xFFE8E8E8), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("协作流程图", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "当前无跨工序支援需求",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5B6B7B)
                )
            }
        }
        return
    }

    // 只保留参与支援的节点，按工艺顺序纵向排列
    val relatedIds = supports.flatMap { listOf(it.fromStepId, it.toStepId) }.distinct()
    val sortedCells = warMap.cells.filter { relatedIds.contains(it.step.id) }
        .sortedBy { warMap.cells.indexOf(it) }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F4FD))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(16.dp)
        ) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val nodeRadiusDp = 8.dp
            val nodeRadiusPx = with(density) { nodeRadiusDp.toPx() }
            val topPadding = 24.dp
            val bottomPadding = 32.dp
            val leftX = 24.dp
            val railRightPadding = 88.dp
            val diagramHeight = maxHeight - topPadding - bottomPadding
            val nodeCount = sortedCells.size
            val nodeSpacing = if (nodeCount > 1) diagramHeight / (nodeCount - 1) else 0.dp
            val primaryColor = MaterialTheme.colorScheme.primary

            Canvas(modifier = Modifier.fillMaxSize()) {
                val railX = size.width - with(density) { railRightPadding.toPx() }
                val startY = with(density) { topPadding.toPx() }
                val endY = size.height - with(density) { bottomPadding.toPx() }

                val nodeCenters = List(nodeCount) { index ->
                    val y = if (nodeCount > 1) {
                        startY + index * (endY - startY) / (nodeCount - 1)
                    } else {
                        (startY + endY) / 2f
                    }
                    Offset(with(density) { leftX.toPx() }, y)
                }

                // 右侧信息轨道竖线
                drawLine(
                    color = Color(0xFF1976D2),
                    start = Offset(railX, startY),
                    end = Offset(railX, endY),
                    strokeWidth = 2.dp.toPx()
                )

                // 节点：红色=被支援，绿色=支援方
                nodeCenters.forEachIndexed { index, center ->
                    val cell = sortedCells[index]
                    val color = when {
                        supports.any { it.toStepId == cell.step.id } -> BottleneckRed
                        supports.any { it.fromStepId == cell.step.id } -> SurplusGreen
                        else -> primaryColor
                    }
                    drawCircle(color, radius = nodeRadiusPx, center = center)
                }

                // 直角折线：donor → 右 → 沿轨道 → receiver
                supports.forEach { support ->
                    val fromIndex = sortedCells.indexOfFirst { it.step.id == support.fromStepId }
                    val toIndex = sortedCells.indexOfFirst { it.step.id == support.toStepId }
                    if (fromIndex >= 0 && toIndex >= 0) {
                        val fromY = nodeCenters[fromIndex].y
                        val toY = nodeCenters[toIndex].y
                        val start = Offset(nodeCenters[fromIndex].x + nodeRadiusPx, fromY)
                        val elbow1 = Offset(railX, fromY)
                        val elbow2 = Offset(railX, toY)
                        val end = Offset(nodeCenters[toIndex].x + nodeRadiusPx, toY)
                        val lineColor = Color(0xFF1976D2)
                        val stroke = 2.dp.toPx()
                        drawLine(lineColor, start, elbow1, stroke)
                        drawLine(lineColor, elbow1, elbow2, stroke)
                        drawLine(lineColor, elbow2, end, stroke)

                        // 箭头在 receiver 侧指向左
                        val arrowLen = 8.dp.toPx()
                        val wing = 4.dp.toPx()
                        drawLine(lineColor, Offset(end.x - arrowLen, end.y - wing), end, stroke)
                        drawLine(lineColor, Offset(end.x - arrowLen, end.y + wing), end, stroke)
                    }
                }
            }

            // 左侧节点名称与身份标签
            sortedCells.forEachIndexed { index, cell ->
                val y = topPadding + nodeSpacing * index
                val isReceiver = supports.any { it.toStepId == cell.step.id }
                val isDonor = supports.any { it.fromStepId == cell.step.id }
                val role = when {
                    isReceiver -> "被支援"
                    isDonor -> "支援方"
                    else -> ""
                }
                val roleColor = if (isReceiver) BottleneckRed else if (isDonor) SurplusGreen else Color.Gray
                Box(
                    modifier = Modifier.offset(x = 44.dp, y = y - 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            cell.step.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF2C3E50)
                        )
                        if (role.isNotEmpty()) {
                            Text(
                                role,
                                style = MaterialTheme.typography.labelSmall,
                                color = roleColor
                            )
                        }
                    }
                }
            }

            // 右侧轨道上的时间与人数标签
            supports.forEach { support ->
                val fromIndex = sortedCells.indexOfFirst { it.step.id == support.fromStepId }
                val toIndex = sortedCells.indexOfFirst { it.step.id == support.toStepId }
                if (fromIndex >= 0 && toIndex >= 0) {
                    val fromY = topPadding + nodeSpacing * fromIndex
                    val toY = topPadding + nodeSpacing * toIndex
                    val midY = (fromY + toY) / 2
                    Box(
                        modifier = Modifier.offset(
                            x = maxWidth - railRightPadding + 8.dp,
                            y = midY - 14.dp
                        ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Text(
                                support.supportWindow,
                                style = MaterialTheme.typography.labelMedium,
                                color = BottleneckRed
                            )
                            Text(
                                "支援 ${support.count} 人",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF1565C0)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SupportCard(support: SupportAssignment) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(TaktBlue).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text("支援", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text("${support.fromName} 派 ${support.count} 人 → ${support.toName}", style = MaterialTheme.typography.bodyLarge)
                if (support.supportWindow.isNotEmpty()) {
                    Text(
                        "每天支援时段 ${support.supportWindow}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BottleneckRed
                    )
                }
                Text(support.reason, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                if (support.bufferMinutes > 0) {
                    Text(
                        "（含 20% 弹性缓冲 ≈ ${String.format("%.0f", support.bufferMinutes)} 分钟，实际到岗误差控制在 20% 以内）",
                        style = MaterialTheme.typography.labelSmall,
                        color = ShortageAmber
                    )
                }
            }
        }
    }
}

@Composable
fun LayoutScreen(viewModel: WarRoomViewModel) {
    val result = viewModel.result.value
    val suggestions = remember(result) { EfficiencyCalculator.layoutSuggestions(result) }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("工位布局沙盘", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LayoutCanvas(result)
        Spacer(Modifier.height(16.dp))
        Text("布局调整建议", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        suggestions.forEach { suggestion ->
            SuggestionCard(title = suggestion.title, description = suggestion.description, priority = suggestion.priority)
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun LayoutCanvas(result: WarRoomResult) {
    Card(modifier = Modifier.fillMaxWidth().height(320.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            val primaryColor = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.fillMaxSize()) {
                // channel
                drawRect(color = Color(0xFFE0E0E0), topLeft = Offset(0f, height * 0.45f), size = Size(width, height * 0.1f))
                result.steps.forEach { step ->
                    val cx = step.x.coerceIn(0f, 1f) * width
                    val cy = step.y.coerceIn(0f, 1f) * height
                    val color = when {
                        step.id == result.bottleneckStepId -> BottleneckRed
                        step.loadRate(result.params.customerTakt) > 0.9 -> ShortageAmber
                        step.headcountDelta(result.params.customerTakt) > 0 -> SurplusGreen
                        else -> primaryColor
                    }
                    drawCircle(color = color, radius = 14.dp.toPx(), center = Offset(cx, cy))
                    drawCircle(color = Color.White, radius = 14.dp.toPx(), center = Offset(cx, cy), style = Stroke(width = 2.dp.toPx()))
                }
                // arrows between consecutive steps
                result.steps.zipWithNext().forEach { (a, b) ->
                    val start = Offset(a.x.coerceIn(0f, 1f) * width, a.y.coerceIn(0f, 1f) * height)
                    val end = Offset(b.x.coerceIn(0f, 1f) * width, b.y.coerceIn(0f, 1f) * height)
                    drawLine(color = Color.DarkGray, start = start, end = end, strokeWidth = 1.dp.toPx())
                }
            }
            // Labels overlay
            val density = androidx.compose.ui.platform.LocalDensity.current
            result.steps.forEach { step ->
                val labelX = with(density) { (step.x.coerceIn(0f, 1f) * width).toDp() } - 16.dp
                val labelY = with(density) { (step.y.coerceIn(0f, 1f) * height).toDp() } - 10.dp
                Box(
                    modifier = Modifier
                        .padding(start = labelX, top = labelY)
                ) {
                    Text(
                        text = step.name.take(4),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PersonnelScreen(viewModel: WarRoomViewModel) {
    val result = viewModel.result.value
    val suggestions = remember(result) { EfficiencyCalculator.personnelSuggestions(result) }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("人员总览", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    NumberBadge(label = "当前总人数", value = result.totalHeadcount, color = MaterialTheme.colorScheme.primary)
                    NumberBadge(label = "理想总人数", value = result.idealTotalHeadcount, color = TaktBlue)
                    NumberBadge(label = "差异", value = result.totalHeadcount - result.idealTotalHeadcount, color = if (result.totalHeadcount >= result.idealTotalHeadcount) SurplusGreen else BottleneckRed)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("工序级人员调配", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        suggestions.forEach { sug ->
            PersonnelCard(sug)
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun NumberBadge(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Spacer(Modifier.height(4.dp))
        Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            Text(value.toString(), style = MaterialTheme.typography.headlineSmall, color = Color.White)
        }
    }
}

@Composable
fun PersonnelCard(suggestion: PersonnelSuggestion) {
    val color = when {
        suggestion.delta > 0 -> SurplusGreen
        suggestion.delta < 0 -> BottleneckRed
        else -> Color.Gray
    }
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(suggestion.processName, style = MaterialTheme.typography.bodyLarge)
                Text(suggestion.action, style = MaterialTheme.typography.bodySmall, color = color)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${suggestion.current}", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Text("${suggestion.ideal}", style = MaterialTheme.typography.titleMedium, color = TaktBlue)
            }
        }
    }
}

@Composable
fun ImprovementScreen(viewModel: WarRoomViewModel) {
    val result = viewModel.result.value
    val suggestions = remember(result) { EfficiencyCalculator.efficiencySuggestions(result) }
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("人效提升方案", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { shareReport(context, result, suggestions) }) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("分享")
            }
        }
        Spacer(Modifier.height(8.dp))
        suggestions.forEach { sug ->
            EfficiencyCard(sug)
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun EfficiencyCard(suggestion: EfficiencySuggestion) {
    val color = when (suggestion.priority) {
        "高" -> BottleneckRed
        "中" -> ShortageAmber
        else -> SurplusGreen
    }
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(color).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(suggestion.priority, color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.width(8.dp))
                Text(suggestion.title, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(4.dp))
            Text(suggestion.expectedBenefit, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
        }
    }
}

@Composable
fun SuggestionCard(title: String, description: String, priority: String) {
    val color = when (priority) {
        "高" -> BottleneckRed
        "中" -> ShortageAmber
        else -> SurplusGreen
    }
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.padding(top = 2.dp).clip(RoundedCornerShape(4.dp)).background(color).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text(priority, color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(description, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
            }
        }
    }
}

@Composable
fun CompareScreen(viewModel: WarRoomViewModel) {
    val comp = viewModel.comparison.value
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("改善前 / 改善后 指标对比", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { shareComparison(context, comp) }) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("分享")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "说明：在「数据录入」页为每个工序填写「改善后单件/人数」后，下方指标与表格自动刷新。",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
        Spacer(Modifier.height(12.dp))
        Text("关键指标", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        comp.metrics.forEach { MetricCompareCard(it) }
        Spacer(Modifier.height(16.dp))
        Text("工序级对比（横向滚动）", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        CompareTable(comp.perProcess)
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun MetricCompareCard(metric: ComparisonMetric) {
    val accent = if (metric.improved) SurplusGreen
    else if (metric.beforeText == metric.afterText) Color.Gray else BottleneckRed
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1.4f)) {
                Text(metric.label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (metric.improved) "改善方向 ✔ 更优" else if (metric.beforeText == metric.afterText) "无变化" else "需关注",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("改善前", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("${metric.beforeText}${metric.unit}", style = MaterialTheme.typography.bodyLarge, color = Color.DarkGray)
                }
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text("改善后", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("${metric.afterText}${metric.unit}", style = MaterialTheme.typography.titleMedium, color = accent)
                }
            }
        }
    }
}

@Composable
fun CompareTable(rows: List<ProcessCompareRow>) {
    val cellWidth = 88.dp
    val labelWidth = 110.dp
    val rowHeight = 40.dp
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()).height(rowHeight)) {
                Box(modifier = Modifier.width(labelWidth).height(rowHeight).padding(4.dp), contentAlignment = Alignment.CenterStart) {
                    Text("工序\\指标", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                listOf("改善前时间", "改善后时间", "改善前人数", "改善后人数").forEach { h ->
                    Box(modifier = Modifier.width(cellWidth).height(rowHeight).padding(4.dp), contentAlignment = Alignment.Center) {
                        Text(h, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                    }
                }
            }
            rows.forEach { row ->
                Row(Modifier.horizontalScroll(rememberScrollState()).height(rowHeight)) {
                    Box(modifier = Modifier.width(labelWidth).height(rowHeight).padding(4.dp), contentAlignment = Alignment.CenterStart) {
                        Text("${row.name}\n(${row.skill})", style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    CompareCell(String.format("%.2f", row.beforeTime), cellWidth, rowHeight)
                    CompareCell(String.format("%.2f", row.afterTime), cellWidth, rowHeight, highlight = row.afterTime != row.beforeTime)
                    CompareCell(row.beforeHead.toString(), cellWidth, rowHeight)
                    CompareCell(row.afterHead.toString(), cellWidth, rowHeight, highlight = row.afterHead != row.beforeHead)
                }
            }
        }
    }
}

@Composable
fun CompareCell(text: String, width: Dp, height: Dp, highlight: Boolean = false) {
    Box(
        modifier = Modifier.width(width).height(height).padding(2.dp)
            .background(if (highlight) SurplusGreen.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(4.dp))
            .border(0.5.dp, Color.LightGray, RoundedCornerShape(4.dp)).padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, maxLines = 1)
    }
}

private fun shareComparison(context: android.content.Context, comp: ComparisonResult) {
    val text = buildString {
        appendLine("人效改善前/改善后对比报告")
        appendLine()
        comp.metrics.forEach { m ->
            appendLine("${m.label}: ${m.beforeText} → ${m.afterText} ${m.unit}  ${if (m.improved) "[改善✔]" else ""}")
        }
        appendLine()
        appendLine("工序级:")
        comp.perProcess.forEach { r ->
            appendLine("- ${r.name}(${r.skill}): 时间 ${String.format("%.2f", r.beforeTime)}→${String.format("%.2f", r.afterTime)} min, 人数 ${r.beforeHead}→${r.afterHead}")
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "分享改善对比报告"))
}

private fun shareReport(context: android.content.Context, result: WarRoomResult, suggestions: List<EfficiencySuggestion>) {
    val text = buildString {
        appendLine("人效提升作战沙盘报告")
        appendLine("产线节拍: ${String.format("%.2f", result.lineCycleTime)} min/件")
        appendLine("平衡率: ${String.format("%.1f", result.balanceRate)}%")
        appendLine("瓶颈工序: ${result.bottleneckName}")
        appendLine("预计日产量: ${String.format("%.0f", result.estimatedDailyOutput)} 件")
        appendLine("当前/理想人数: ${result.totalHeadcount}/${result.idealTotalHeadcount}")
        appendLine()
        appendLine("提升方案:")
        suggestions.forEachIndexed { index, sug ->
            appendLine("${index + 1}. [${sug.priority}] ${sug.title}")
            appendLine("   ${sug.expectedBenefit}")
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "分享作战沙盘报告"))
}
