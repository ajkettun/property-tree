@file:JvmName("PropertyTreeUtils")
@file:JvmMultifileClass

package org.github.ajkettun.propertytree

import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

const val INCLUDE_KEY_SUFFIX = "Include"
const val EXCLUDE_KEY_SUFFIX = "Exclude"
const val INTERVAL_START_KEY_SUFFIX = "Start"
const val INTERVAL_END_KEY_SUFFIX = "End"

val CRITERIA_SUFFIXES: Set<String> = hashSetOf(
    INCLUDE_KEY_SUFFIX, EXCLUDE_KEY_SUFFIX,
    INTERVAL_START_KEY_SUFFIX, INTERVAL_END_KEY_SUFFIX
)

val PropertyName.criterionBaseProperty
    get() = CRITERIA_SUFFIXES.reduce { name, suffix -> name.replace(suffix, "") }

val PropertyName.isCriterion
    get() = CRITERIA_SUFFIXES.any { suffix -> value.endsWith(suffix) }

val PropertyName.isExcludeIncludeCriterion get() = isIncludeCriterion || isExcludeCriterion

val PropertyName.isIncludeCriterion get() = value.endsWith(INCLUDE_KEY_SUFFIX)

val PropertyName.isExcludeCriterion get() = value.endsWith(EXCLUDE_KEY_SUFFIX)

val PropertyName.isIntervalCriterion get() = isIntervalStartCriterion || isIntervalEndCriterion

val PropertyName.isIntervalStartCriterion get() = value.endsWith(INTERVAL_START_KEY_SUFFIX)

val PropertyName.isIntervalEndCriterion get() = value.endsWith(INTERVAL_END_KEY_SUFFIX)

val PropertyName.isEndCriterionForStart get() = criterionBaseProperty + INTERVAL_END_KEY_SUFFIX

fun byCriteria(criteria: Criteria): (PropertyTree) -> Boolean = { criteria.satisfiedBy(it) }

data class Criterion(val name: PropertyName, val values: Set<Any>) {
    constructor(name: PropertyName, vararg value: Any) : this(name, setOf(value.toSet()))
    constructor(name: String, vararg value: Any) : this(PropertyName(name), value.toSet())
    constructor(name: String, values: Set<Any>) : this(PropertyName(name), values)
}

data class Criteria(val criteria: Iterable<Criterion>) {
    constructor(vararg criteria: Criterion) : this(criteria.toList())

    fun satisfiedBy(propertyTree: PropertyTree): Boolean {
        val excluded = isExcluded(propertyTree)
        val included = isIncluded(propertyTree)
        val includedInterval = isIncludedInterval(propertyTree)
        return !excluded && included && includedInterval
    }

    private fun isExcluded(propertyTree: PropertyTree): Boolean {
        return criteria.any { (criterionName, criterionValues) ->
            propertyTree.children.filter(byName(criterionName.value + EXCLUDE_KEY_SUFFIX))
                .flatMap { node -> node.property.data }
                .any { value -> criterionValues.contains(value) }
        }
    }

    private fun isIncluded(propertyTree: PropertyTree): Boolean {
        return criteria
            .all { (criterionName, criterionValues) ->
                val properties = propertyTree.children.filter(byName(criterionName.value + INCLUDE_KEY_SUFFIX))
                val values = properties.flatMap { it.data }
                return properties.isEmpty() || criterionValues.any { value -> values.contains(value) }
            }
    }

    private fun isIncludedInterval(propertyTree: PropertyTree): Boolean {
        return criteria
            .all { (criterionName, criterionValues) ->
                val start = propertyTree.children.find(
                    byName(criterionName.value + INTERVAL_START_KEY_SUFFIX)
                )?.property?.singleComparable

                return if (start == null) true else {
                    val end = propertyTree.children.find(
                        byName(criterionName.value + INTERVAL_END_KEY_SUFFIX)
                    )?.property?.singleComparable

                    val range = if (end != null) start..end else null

                    criterionValues.all {
                        check(it is Comparable<*>) { "Criterion value not comparable ${criterionName.value}: $criterionValues" }
                        @Suppress("UNCHECKED_CAST")
                        val comparable = it as Comparable<Any>
                        range?.contains(comparable) ?: (start <= comparable)
                    }
                }
            }
    }
}
