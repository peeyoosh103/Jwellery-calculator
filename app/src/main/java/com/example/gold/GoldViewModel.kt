package com.example.gold

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calculation.JewelleryCalculator
import com.example.database.CalculationHistoryEntity
import com.example.database.CalculationHistoryRepository
import com.example.models.CalculationInput
import com.example.models.CalculationResult
import com.example.models.GoldPurity
import com.example.models.MetalType
import com.example.settings.AppSettings
import com.example.settings.SettingsRepository
import com.example.sharing.CalculationShareHelper
import com.example.tts.TextToSpeechHelper
import com.example.validation.CalculationValidator
import com.example.validation.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GoldUiState(
    val gramsText: String = "",
    val milligramsText: String = "",
    val selectedPurity: GoldPurity = GoldPurity.K22,
    val marketRateText: String = "",
    val makingChargeText: String = "5.0",
    val wastageText: String = "0.0",
    val gstText: String = "3.0",
    val result: CalculationResult? = null,
    val errorMessage: String? = null,
    val isSaved: Boolean = false,
    val showResult: Boolean = false
)

class GoldViewModel(
    private val historyRepository: CalculationHistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val ttsHelper: TextToSpeechHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoldUiState())
    val uiState: StateFlow<GoldUiState> = _uiState.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settings

    init {
        // Initialize default GST from settings
        val defaultGst = settingsRepository.settings.value.defaultGst
        _uiState.value = _uiState.value.copy(gstText = defaultGst.toString())
    }

    fun onGramsChange(value: String) {
        _uiState.value = _uiState.value.copy(gramsText = value, errorMessage = null)
    }

    fun onMilligramsChange(value: String) {
        _uiState.value = _uiState.value.copy(milligramsText = value, errorMessage = null)
    }

    fun onPuritySelect(purity: GoldPurity) {
        _uiState.value = _uiState.value.copy(selectedPurity = purity, errorMessage = null)
    }

    fun onMarketRateChange(value: String) {
        _uiState.value = _uiState.value.copy(marketRateText = value, errorMessage = null)
    }

    fun onMakingChargeChange(value: String) {
        _uiState.value = _uiState.value.copy(makingChargeText = value, errorMessage = null)
    }

    fun onWastageChange(value: String) {
        _uiState.value = _uiState.value.copy(wastageText = value, errorMessage = null)
    }

    fun onGstChange(value: String) {
        _uiState.value = _uiState.value.copy(gstText = value, errorMessage = null)
    }

    fun calculateGoldPrice() {
        val state = _uiState.value
        val validation = CalculationValidator.validateInput(
            gramsText = state.gramsText,
            milligramsText = state.milligramsText,
            marketRateText = state.marketRateText,
            makingChargeText = state.makingChargeText,
            wastageText = state.wastageText,
            gstText = state.gstText
        )

        when (validation) {
            is ValidationResult.Invalid -> {
                _uiState.value = _uiState.value.copy(errorMessage = validation.errorMessage)
            }
            is ValidationResult.Valid -> {
                val grams = state.gramsText.toDoubleOrNull() ?: 0.0
                val mg = state.milligramsText.toDoubleOrNull() ?: 0.0
                val marketRate = state.marketRateText.toDoubleOrNull() ?: 0.0
                val making = state.makingChargeText.toDoubleOrNull() ?: 0.0
                val wastage = state.wastageText.toDoubleOrNull() ?: 0.0
                val gst = state.gstText.toDoubleOrNull() ?: 0.0

                val input = CalculationInput(
                    metalType = MetalType.GOLD,
                    grams = grams,
                    milligrams = mg,
                    purityLabel = state.selectedPurity.label,
                    purityFineness = state.selectedPurity.fineness,
                    purityFactor = state.selectedPurity.purityFactor,
                    marketRate = marketRate,
                    makingChargePercent = making,
                    wastagePercent = wastage,
                    gstPercent = gst
                )

                val result = JewelleryCalculator.calculate(input)
                _uiState.value = _uiState.value.copy(
                    result = result,
                    showResult = true,
                    isSaved = false,
                    errorMessage = null
                )

                // Auto-save if enabled
                if (settings.value.autoSaveHistory) {
                    saveCalculation()
                }

                // Automatic voice read if enabled
                if (settings.value.isTtsEnabled) {
                    speakResult()
                }
            }
        }
    }

    fun speakResult() {
        val result = _uiState.value.result ?: return
        val currentSettings = settings.value
        ttsHelper.speakResult(result, currentSettings.language, currentSettings.speechSpeed)
    }

    fun saveCalculation() {
        val result = _uiState.value.result ?: return
        if (_uiState.value.isSaved) return

        viewModelScope.launch {
            val entity = CalculationHistoryEntity.fromCalculationResult(result)
            historyRepository.insert(entity)
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }

    fun shareCalculation(context: Context) {
        val result = _uiState.value.result ?: return
        CalculationShareHelper.shareCalculation(context, result)
    }

    fun resetCalculation() {
        _uiState.value = _uiState.value.copy(
            gramsText = "",
            milligramsText = "",
            marketRateText = "",
            result = null,
            showResult = false,
            errorMessage = null,
            isSaved = false
        )
    }

    fun editCalculation() {
        _uiState.value = _uiState.value.copy(showResult = false)
    }
}
