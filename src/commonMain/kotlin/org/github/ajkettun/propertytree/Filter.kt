package org.github.ajkettun.propertytree

const val INCLUDE_KEY_SUFFIX = "Include"
const val EXCLUDE_KEY_SUFFIX = "Exclude"
const val INTERVAL_START_KEY_SUFFIX = "Start"
const val INTERVAL_END_KEY_SUFFIX = "End"

val CRITERIA_SUFFIXES: Set<String> = hashSetOf(
    INCLUDE_KEY_SUFFIX, EXCLUDE_KEY_SUFFIX,
    INTERVAL_START_KEY_SUFFIX, INTERVAL_END_KEY_SUFFIX
)

fun PropertyName.getBaseProperty() =
    CRITERIA_SUFFIXES.reduce { name, suffix -> name.replace(suffix, "") }

fun PropertyName.isCriteria() =
    CRITERIA_SUFFIXES.any { suffix -> value.endsWith(suffix) }

fun PropertyName.isExcludeIncludeCriteria() = isIncludeCriteria() || isExcludeCriteria()

fun PropertyName.isIncludeCriteria() = value.endsWith(INCLUDE_KEY_SUFFIX)

fun PropertyName.isExcludeCriteria() = value.endsWith(EXCLUDE_KEY_SUFFIX)

fun PropertyName.isIntervalCriteria() = isIntervalStartCriteria() || isIntervalEndCriteria()

fun PropertyName.isIntervalStartCriteria() = value.endsWith(INTERVAL_START_KEY_SUFFIX)

fun PropertyName.isIntervalEndCriteria() = value.endsWith(INTERVAL_END_KEY_SUFFIX)

fun PropertyName.getEndCriteriaForStart() = getBaseProperty() + INTERVAL_END_KEY_SUFFIX

fun PropertyTree.filter(criteria: Criteria) = criteria.satisfiedBy(this)

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
