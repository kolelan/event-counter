package com.eventcounter.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GroupTreeTest {
    private val groups = listOf(
        Group(id = 1, parentGroupId = null, name = "Дом", iconKey = IconKeys.FOLDER),
        Group(id = 2, parentGroupId = 1, name = "Кухня", iconKey = IconKeys.FOLDER),
        Group(id = 3, parentGroupId = 2, name = "Фильтры", iconKey = IconKeys.FOLDER),
        Group(id = 4, parentGroupId = null, name = "Авто", iconKey = IconKeys.FOLDER),
    )

    @Test
    fun descendantIds_includesNested() {
        assertEquals(setOf(2L, 3L), GroupTree.descendantIds(groups, 1))
    }

    @Test
    fun choices_skipExcludedSubtree() {
        val choices = GroupTree.choices(groups, excludedIds = setOf(1))
        assertEquals(listOf(4L), choices.map { it.id })
    }

    @Test
    fun choices_indentNestedGroups() {
        val choices = GroupTree.choices(groups)
        assertEquals(listOf("Авто", "Дом", "Кухня", "Фильтры"), choices.map { it.name })
        assertEquals(listOf(0, 0, 1, 2), choices.map { it.depth })
    }
}
