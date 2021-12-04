package org.github.ajkettun.propertytree

import org.github.ajkettun.propertytree.PropertyTree.Companion.propertyNodeOf

data class PropertyNodeBuilder(
    var name: String? = null,
    var description: String? = null,
    var data: MutableSet<Any?> = mutableSetOf(),
    private var children: MutableList<PropertyNodeBuilder> = mutableListOf()
) {
    fun propertyNode(init: PropertyNodeBuilder.() -> Unit): PropertyNodeBuilder {
        val node = PropertyNodeBuilder();
        init(node);
        children.add(node)
        return node;
    }

    fun build(): PropertyTree = propertyNodeOf(checkNotNull(name) { "Property name required" },
        description, data, children.map { it.build() })
}

fun propertyNode(init: PropertyNodeBuilder.() -> Unit): PropertyTree {
    val node = PropertyNodeBuilder();
    init(node);
    return node.build();
}