@file:JvmName("PropertyTreeUtils")
@file:JvmMultifileClass

package org.github.ajkettun.propertytree

import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.jvm.JvmStatic

val EMPTY_NODE: PropertyTree = EmptyNode()

private val EMPTY_PROPERTY: Property = Property(EMPTY, null, linkedSetOf())

data class PropertyName(val value: String)

fun byName(name: PropertyName): (PropertyTree) -> Boolean = { it.name == name.value }

fun byName(name: String): (PropertyTree) -> Boolean = { it.name == name }

data class Property(
    val name: PropertyName,
    val description: String? = null,
    val data: Set<Any?> = linkedSetOf()
) {

    val isSet get() = singleBoolean ?: false

    val isNotSet get() = singleBoolean?.let { !it } ?: false

    val singleBoolean
        get() = data
            .singleOrNull()?.let { if (it is Boolean) it else null }

    val singleString
        get() = data
            .singleOrNull()?.let { if (it is String) it else null }

    val singleLong
        get() = data
            .singleOrNull()?.let { if (it is Long) it else null }

    val singleInt
        get() = data
            .singleOrNull()?.let { if (it is Int) it else null }

    @Suppress("UNCHECKED_CAST")
    val singleObject
        get() = data
            .singleOrNull()?.let { if (it is Map<*, *>) it as Map<String, Any?> else null }

    @Suppress("UNCHECKED_CAST")
    val singleComparable
        get() = data
            .singleOrNull()?.let { if (it is Comparable<*>) it as Comparable<Any> else null }

    val strings
        get() = data
            .filter { it is String }.map { it as String }.toSet()

    val longs
        get() = data
            .filter { it is Long }.map { it as Long }.toSet()

    val ints
        get() = data
            .filter { it is Int }.map { it as Int }.toSet()

    @Suppress("UNCHECKED_CAST")
    val objects
        get() = data
            .filter { it is Map<*, *> }.map { it as Map<String, Any?> }.toSet()

    fun update(data: (Set<Any?>) -> Set<Any?> = { it }): Property {
        val updatedData = data(this.data)
        return if (updatedData != this.data) this.copy(data = updatedData) else this
    }
}

sealed class PropertyTree {
    abstract val property: Property
    abstract val children: List<PropertyTree>

    val name get() = property.name.value
    val description get() = property.description
    val data get() = property.data

    companion object {
        @JvmStatic
        fun empty(): PropertyTree = EMPTY_NODE

        @JvmStatic
        fun propertyNodeOf(
            name: String,
            vararg children: PropertyTree
        ): PropertyTree = propertyNodeOf(
            name = name, description = null, data = linkedSetOf(),
            children.asList()
        )

        @JvmStatic
        fun propertyNodeOf(
            name: String,
            data: Set<Any?> = linkedSetOf(),
            vararg children: PropertyTree
        ): PropertyTree = propertyNodeOf(
            name = name, description = null, data = data,
            children.asList()
        )

        @JvmStatic
        fun propertyNodeOf(
            name: String,
            description: String? = null,
            data: Set<Any?> = linkedSetOf(),
            vararg children: PropertyTree
        ): PropertyTree = propertyNodeOf(
            name = name, description = description, data = data,
            children.asList()
        )

        @JvmStatic
        fun propertyNodeOf(
            name: String,
            description: String? = null,
            data: Set<Any?> = linkedSetOf(),
            children: List<PropertyTree> = listOf()
        ): PropertyTree =
            PropertyNode(Property(
                PropertyName(name), description,
                data
            ),
                children.filter { it.notEmpty })

        @JvmStatic
        fun propertyNodeOf(
            name: PropertyName,
            vararg children: PropertyTree
        ): PropertyTree = propertyNodeOf(
            name = name, description = null, data = linkedSetOf(),
            children.asList()
        )

        @JvmStatic
        fun propertyNodeOf(
            name: PropertyName,
            data: Set<Any?> = linkedSetOf(),
            vararg children: PropertyTree
        ): PropertyTree = propertyNodeOf(
            name = name, description = null, data = data,
            children.asList()
        )

        @JvmStatic
        fun propertyNodeOf(
            name: PropertyName,
            description: String? = null,
            data: Set<Any?> = linkedSetOf(),
            vararg children: PropertyTree
        ): PropertyTree = propertyNodeOf(
            name = name, description = description, data = data,
            children.asList()
        )

        @JvmStatic
        fun propertyNodeOf(
            name: PropertyName,
            description: String? = null,
            data: Set<Any?> = linkedSetOf(),
            children: List<PropertyTree> = listOf()
        ): PropertyTree =
            PropertyNode(Property(name, description, if (data is LinkedHashSet) data else linkedSetOf(data)),
                children.filter { it.notEmpty })
    }

    val empty get() = this == EMPTY_NODE

    val notEmpty get() = !empty

    fun find(predicate: (PropertyTree) -> Boolean) = traverse().find(predicate) ?: EMPTY_NODE

    fun findAll(predicate: (PropertyTree) -> Boolean) = traverse().filter(predicate)

    fun first(predicate: (PropertyTree) -> Boolean) = traverse().first(predicate)

    fun findChild(predicate: (PropertyTree) -> Boolean) = children.find(predicate) ?: EMPTY_NODE

    fun findChildren(predicate: (PropertyTree) -> Boolean) = children.filter(predicate)

    fun firstChild(predicate: (PropertyTree) -> Boolean) = children.first(predicate)

    /**
     * Traverses the tree lazily in pre-order.
     */
    fun traverse(): Sequence<PropertyTree> = sequence {
        yield(this@PropertyTree)
        yieldAll(children.flatMap { it.traverse() })
    }

    fun filter(predicate: (PropertyTree) -> Boolean) = prune { !predicate(it) }

    fun prune(predicate: (PropertyTree) -> Boolean): PropertyTree {
        return this.replaceNode(EMPTY_NODE, predicate)
    }

    fun overwriteChildren(replacement: PropertyTree, name: PropertyName): PropertyTree =
        overwriteChildren(replacement, byName(name))

    fun overwriteChildren(replacement: PropertyTree, name: String): PropertyTree =
        overwriteChildren(replacement, byName(name))

    fun overwriteChildren(replacement: PropertyTree, predicate: (PropertyTree) -> Boolean): PropertyTree =
        if (this.empty)
            this
        else updateChildren { it.filter(predicate) + replacement }

    fun updateNode(name: PropertyName, updater: (PropertyTree) -> PropertyTree) =
        find(byName(name)).let { if (it.empty) this else replaceNode(it, updater(it)) }

    fun updateNode(name: String, updater: (PropertyTree) -> PropertyTree) = updateNode(PropertyName(name), updater)

    fun replaceNode(target: PropertyTree, replacement: PropertyTree): PropertyTree =
        replaceNode(replacement) { it == target }

    fun replaceNode(replacement: PropertyTree, predicate: (PropertyTree) -> Boolean): PropertyTree =
        if (this.empty)
            this
        else transform { node, children -> if (predicate(node)) replacement else node.with(children = children) }

    fun transform(
        transformer: (
            propertyTree: PropertyTree,
            children: List<PropertyTree>
        ) -> PropertyTree
    ): PropertyTree {
        val children = this.children
            .map { it.transform(transformer) }
            .filter { it.notEmpty }

        return transformer(this, children)
    }

    fun updateChildren(
        children: (List<PropertyTree>) -> List<PropertyTree> = { it }
    ): PropertyTree {
        return with(children = children(this.children))
    }

    abstract fun with(
        property: Property = this.property,
        children: List<PropertyTree> = this.children
    ): PropertyTree

    fun draw(indent: String = "", isLast: Boolean = false): String {
        val result = StringBuilder(drawNode(indent, isLast))
        for ((i, child) in children.withIndex()) {
            val childIsLast = i == this.children.size - 1
            val hasGrandChildren = child.children.isNotEmpty()
            result.append('\n').append(indent).append(if (childIsLast) "└──" else "├──")
            result.append(child.draw(indent + if (childIsLast) "   " else "│  ", !hasGrandChildren))
        }
        return result.toString()
    }

    protected abstract fun drawNode(indent: String?, isLast: Boolean): String
}

private class EmptyNode : PropertyTree() {
    override val property = EMPTY_PROPERTY
    override val children = listOf<PropertyTree>()

    override fun toString(): String {
        return draw()
    }

    override fun with(property: Property, children: List<PropertyTree>): PropertyTree {
        return this
    }

    override fun drawNode(indent: String?, isLast: Boolean) = "null"
}

private data class PropertyNode(
    override val property: Property,
    override val children: List<PropertyTree>
) : PropertyTree() {

    override fun toString(): String {
        return draw()
    }

    override fun with(property: Property, children: List<PropertyTree>): PropertyTree =
        if (this.property != property || this.children != children)
            copy(property = property, children = children)
        else this

    override fun drawNode(indent: String?, isLast: Boolean): String {
        var result: String = this.name
        if (!this.data.isEmpty()) {
            result += if (this.data.size == 1) {
                ": " + this.data.first()
            } else {
                ": [" + this.data.map { value -> value?.toString() }.joinToString(",") + "]"
            }
        }
        return result
    }
}
