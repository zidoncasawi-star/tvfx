package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NetflixRed

enum class SortOption(val label: String) {
    DEFAULT("الافتراضي"),
    RATING("الأعلى تقييماً"),
    YEAR("الأحدث"),
    NAME("الاسم أ-ي")
}

@Composable
private fun DropdownPill(
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isActive) NetflixRed.copy(alpha = 0.18f) else Color(0xFF18181C))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text = label,
            color = if (isActive) NetflixRed else Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = if (isActive) NetflixRed else Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun DropdownFilterBar(
    categories: List<String>,
    selectedCategory: String,
    categoryNames: Map<String, String>? = null,
    onCategorySelect: (String) -> Unit,
    sortOption: SortOption,
    onSortSelect: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val hasActiveFilters = selectedCategory != "الكل" || sortOption != SortOption.DEFAULT

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box {
            DropdownPill(
                label = categoryNames?.get(selectedCategory) ?: selectedCategory,
                isActive = selectedCategory != "الكل",
                onClick = { categoryMenuExpanded = true }
            )
            DropdownMenu(
                expanded = categoryMenuExpanded,
                onDismissRequest = { categoryMenuExpanded = false },
                modifier = Modifier.background(Color(0xFF1B1B1B))
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = categoryNames?.get(category) ?: category,
                                color = if (category == selectedCategory) NetflixRed else Color.White
                            )
                        },
                        onClick = {
                            onCategorySelect(category)
                            categoryMenuExpanded = false
                        }
                    )
                }
            }
        }

        Box {
            DropdownPill(
                label = sortOption.label,
                isActive = sortOption != SortOption.DEFAULT,
                onClick = { sortMenuExpanded = true }
            )
            DropdownMenu(
                expanded = sortMenuExpanded,
                onDismissRequest = { sortMenuExpanded = false },
                modifier = Modifier.background(Color(0xFF1B1B1B))
            ) {
                SortOption.values().forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option.label,
                                color = if (option == sortOption) NetflixRed else Color.White
                            )
                        },
                        onClick = {
                            onSortSelect(option)
                            sortMenuExpanded = false
                        }
                    )
                }
            }
        }

        if (hasActiveFilters) {
            Text(
                text = "إعادة ضبط",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable {
                    onCategorySelect("الكل")
                    onSortSelect(SortOption.DEFAULT)
                }
            )
        }
    }
}
