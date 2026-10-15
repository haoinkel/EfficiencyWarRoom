package com.leanefficiency.warroom.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.leanefficiency.warroom.model.EfficiencyCalculator
import com.leanefficiency.warroom.model.GlobalParams
import com.leanefficiency.warroom.model.ProcessStep
import com.leanefficiency.warroom.model.WarMapResult
import com.leanefficiency.warroom.model.WarRoomResult
import com.leanefficiency.warroom.model.ComparisonResult
import com.leanefficiency.warroom.model.sampleProcessSteps

class WarRoomViewModel : ViewModel() {

    private val _steps = mutableStateListOf<ProcessStep>().apply { addAll(sampleProcessSteps()) }
    val steps: List<ProcessStep> get() = _steps

    private val _params = mutableStateOf(GlobalParams())
    val params: State<GlobalParams> = _params

    private val _result = mutableStateOf(EfficiencyCalculator.calculate(_steps.toList(), _params.value))
    val result: State<WarRoomResult> = _result

    private val _warMap = mutableStateOf(EfficiencyCalculator.computeWarMap(_steps.toList(), _params.value))
    val warMap: State<WarMapResult> = _warMap

    private val _comparison = mutableStateOf(
        EfficiencyCalculator.compareBeforeAfter(_steps.toList(), _params.value)
    )
    val comparison: State<ComparisonResult> = _comparison

    init {
        recalculate()
    }

    fun updateParams(newParams: GlobalParams) {
        _params.value = newParams
        recalculate()
    }

    fun updateStep(index: Int, newStep: ProcessStep) {
        if (index in _steps.indices) {
            _steps[index] = newStep
            recalculate()
        }
    }

    fun addStep(step: ProcessStep = ProcessStep(
        id = (_steps.maxOfOrNull { it.id } ?: 0) + 1,
        name = "新工序 ${_steps.size + 1}",
        singlePieceTimeMin = 1.0,
        currentHeadcount = 1
    )) {
        _steps.add(step)
        recalculate()
    }

    fun removeStep(index: Int) {
        if (index in _steps.indices && _steps.size > 1) {
            _steps.removeAt(index)
            recalculate()
        }
    }

    fun resetToSample() {
        _steps.clear()
        _steps.addAll(sampleProcessSteps())
        _params.value = GlobalParams()
        recalculate()
    }

    fun clearAll() {
        _steps.clear()
        _steps.add(ProcessStep(1, "工序 1", 0.0, 1))
        recalculate()
    }

    private fun recalculate() {
        _result.value = EfficiencyCalculator.calculate(_steps.toList(), _params.value)
        _warMap.value = EfficiencyCalculator.computeWarMap(_steps.toList(), _params.value)
        _comparison.value = EfficiencyCalculator.compareBeforeAfter(_steps.toList(), _params.value)
    }
}
