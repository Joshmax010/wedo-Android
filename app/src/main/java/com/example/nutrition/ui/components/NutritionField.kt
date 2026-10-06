package com.example.nutrition.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.nutrition.ui.theme.Error
import com.example.nutrition.ui.theme.TextSecondary
import com.example.nutrition.ui.theme.nutritionFieldColors

/** Shared complete fields for diet, body, target and template forms. */
@Composable
fun NutritionField(
    label: String,
    unit: String = "",
    value: String,
    onValueChange: (String) -> Unit,
    required: Boolean = false,
    placeholder: String = "选填",
    keyboardType: KeyboardType = KeyboardType.Decimal,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row {
            Text(label + if (unit.isNotEmpty()) " · $unit" else "", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            if (required) Text(" *", style = MaterialTheme.typography.bodySmall, color = Error)
        }
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text(if (required && placeholder == "选填") "必填" else placeholder) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = nutritionFieldColors(), shape = RoundedCornerShape(12.dp)
        )
    }
}
