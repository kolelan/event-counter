package com.eventcounter.domain

object GroupTree {
    fun choices(groups: List<Group>, excludedIds: Set<Long> = emptySet()): List<GroupChoice> {
        val byParent = groups.groupBy { it.parentGroupId }
        val result = mutableListOf<GroupChoice>()
        fun walk(parentId: Long?, depth: Int) {
            val children = byParent[parentId].orEmpty().sortedWith(
                compareBy<Group> { it.sortOrder }.thenBy { it.name.lowercase() },
            )
            for (group in children) {
                if (group.id in excludedIds) continue
                result += GroupChoice(group.id, group.name, depth)
                walk(group.id, depth + 1)
            }
        }
        walk(null, 0)
        return result
    }

    fun descendantIds(groups: List<Group>, rootId: Long): Set<Long> {
        val byParent = groups.groupBy { it.parentGroupId }
        val result = mutableSetOf<Long>()
        val stack = ArrayDeque<Long>()
        stack.add(rootId)
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            for (child in byParent[current].orEmpty()) {
                if (result.add(child.id)) stack.add(child.id)
            }
        }
        return result
    }
}
