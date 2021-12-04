package org.github.ajkettun.propertytree

const val INCLUDE_KEY_SUFFIX = "Include"
const val EXCLUDE_KEY_SUFFIX = "Exclude"
const val INTERVAL_START_KEY_SUFFIX = "Start"
const val INTERVAL_END_KEY_SUFFIX = "End"

val CRITERIA_SUFFIXES: Set<String> = hashSetOf(
    INCLUDE_KEY_SUFFIX, EXCLUDE_KEY_SUFFIX,
    INTERVAL_START_KEY_SUFFIX, INTERVAL_END_KEY_SUFFIX
)

fun getBaseProperty(): String? {
    return getBaseProperty(getPropertyName())
}

fun isCriteria(): Boolean {
    return isCriteria(getPropertyName())
}

fun isExcludeIncludeCriteria(): Boolean {
    return isExcludeIncludeCriteria(getPropertyName())
}

fun isIncludeCriteria(): Boolean {
    return isIncludeCriteria(getPropertyName())
}

fun isExcludeCriteria(): Boolean {
    return isExcludeCriteria(getPropertyName())
}

fun isIntervalCriteria(): Boolean {
    return isIntervalCriteria(getPropertyName())
}

fun isIntervalStartCriteria(): Boolean {
    return isIntervalStartCriteria(getPropertyName())
}

fun isIntervalEndCriteria(): Boolean {
    return isIntervalEndCriteria(getPropertyName())
}

fun getEndCriteriaForStart(): String? {
    return getEndCriteriaForStart(getPropertyName())
}

fun getBaseProperty(propertyName: String?): String {
    return fi.ruokavirasto.hyrra.core.propertytree.domain.PropertyName.CRITERIA_SUFFIXES.foldLeft(
        propertyName
    ) { name, suffix -> name.replace(suffix, "") }
}

fun isCriteria(propertyName: String): Boolean {
    return fi.ruokavirasto.hyrra.core.propertytree.domain.PropertyName.CRITERIA_SUFFIXES.exists { suffix: String? ->
        propertyName.endsWith(
            suffix!!
        )
    }
}

fun isExcludeIncludeCriteria(name: String): Boolean {
    return isIncludeCriteria(name) || isExcludeCriteria(name)
}

fun isIncludeCriteria(propertyName: String): Boolean {
    return propertyName.endsWith(fi.ruokavirasto.hyrra.core.propertytree.domain.PropertyName.INCLUDE_KEY_SUFFIX)
}

fun isExcludeCriteria(propertyName: String): Boolean {
    return propertyName.endsWith(fi.ruokavirasto.hyrra.core.propertytree.domain.PropertyName.EXCLUDE_KEY_SUFFIX)
}

fun isIntervalCriteria(propertyName: String): Boolean {
    return isIntervalStartCriteria(propertyName) || isIntervalEndCriteria(propertyName)
}

fun isIntervalStartCriteria(propertyName: String): Boolean {
    return propertyName.endsWith(fi.ruokavirasto.hyrra.core.propertytree.domain.PropertyName.INTERVAL_START_KEY_SUFFIX)
}

fun isIntervalEndCriteria(propertyName: String): Boolean {
    return propertyName.endsWith(fi.ruokavirasto.hyrra.core.propertytree.domain.PropertyName.INTERVAL_END_KEY_SUFFIX)
}

fun getEndCriteriaForStart(propertyName: String?): String? {
    return getBaseProperty(propertyName) + fi.ruokavirasto.hyrra.core.propertytree.domain.PropertyName.INTERVAL_END_KEY_SUFFIX
}

data class Criteria(val value: Map<String, Any>)

fun PropertyTree.excludeInclude(criteria: Criteria): PropertyTree? {
    return prune { node -> !node.isNodeIncluded(criteria) }
}

private fun PropertyTree.isNodeIncluded(criteria: Criteria): Boolean {
    val excluded = isExcluded(criteria)
    val included = isIncluded(criteria)
    val includedInterval = isIncludedInterval(criteria)
    return !excluded && included && includedInterval
}

private fun PropertyTree.isExcluded(criteria: Criteria): Boolean {
    return criteria.value.entries.any { (name, data) ->
        children.filter(byName())
        findChildPropertiesByName(
            fi.ruokavirasto.hyrra.core.propertytree.domain.PropertyTree.toCriteriaPropertyName(
                entry._1,
                EXCLUDE_KEY_SUFFIX
            )
        )
            .flatMap(Property::getValues)
            .exists { value -> containsCheckTypes(unwrapAndMakeTraversable(entry._2), value) }
    }
}

private fun PropertyTree.isIncluded(critera: Criteria): Boolean {
    return criteriaData
        .forAll { entry ->
            val properties: Seq<Property> = findChildPropertiesByName(
                fi.ruokavirasto.hyrra.core.propertytree.domain.PropertyTree.toCriteriaPropertyName(
                    entry._1,
                    INCLUDE_KEY_SUFFIX
                )
            )
            val values: Seq<Any> = properties.flatMap(Property::getValues)
            properties.isEmpty() || unwrapAndMakeTraversable(entry._2).exists { value ->
                containsCheckTypes(
                    values,
                    value
                )
            }
        }
}

private fun PropertyTree.containsCheckTypes(container: Traversable<Any>, element: Any?): Boolean {
    if (element != null) {
        val invalidType: Option<String> = container.filter { obj: Any? -> Objects.nonNull(obj) }
            .map { obj: Any -> obj.javaClass }
            .find { c -> !c.equals(element.javaClass) }
            .map { obj: java.lang.Class -> obj.getSimpleName() }
        checkArgument(
            invalidType.isEmpty(),
            "excludeInclude value types mismatch: %s !== %s",
            element.javaClass.getSimpleName(),
            invalidType.getOrNull()
        )
    }
    return container.contains(element)
}

private fun PropertyTree.isIncludedInterval(criteriaData: Map<*, *>): Boolean {
    return criteriaData
        .mapKeys { propertName ->
            findFirstChildPropertyByName(
                fi.ruokavirasto.hyrra.core.propertytree.domain.PropertyTree.toCriteriaPropertyName(
                    propertName,
                    INTERVAL_START_KEY_SUFFIX
                )
            )
                .flatMap(Property::getSingleComparable)
                .map { start ->
                    IntervalClosedEnd.ended(
                        start,
                        findFirstChildPropertyByName(
                            fi.ruokavirasto.hyrra.core.propertytree.domain.PropertyTree.toCriteriaPropertyName(
                                propertName,
                                INTERVAL_END_KEY_SUFFIX
                            )
                        )
                            .flatMap(Property::getSingleComparable).getOrNull()
                    )
                }
        }
        .forAll { entry ->
            if (entry._1.isEmpty()) {
                return@forAll true
            } else if (entry._2 is Comparable<*>) {
                return@forAll entry._1.get().contains(entry._2)
            } else {
                if (entry._2 == null) {
                    return@forAll false
                } else {
                    throw IllegalArgumentException(
                        java.lang.String.format(
                            "Not comparable %s:%s", entry._2, entry._2.getClass().getSimpleName()
                        )
                    )
                }
            }
        }
}