package org.github.ajkettun.propertytree

val EMPTY_NODE: PropertyTree = EmptyNode()

private val EMPTY_PROPERTY: Property = Property(EMPTY, null, linkedSetOf())

data class PropertyName(val value: String)

fun byName(name: String): (PropertyTree) -> Boolean = { it.name == name }

data class Property(
    val name: String,
    val description: String? = null,
    val data: Set<Any?> = linkedSetOf()
) {

    val set get() = singleBoolean ?: false

    val unset get() = singleBoolean?.let { !it } ?: false

    val singleBoolean
        get() = data
            .singleOrNull()?.let { if (it is Boolean) it else null }

    val singleComparable
        get() = data
            .singleOrNull()?.let { if (it is Comparable<*>) it else null }

    val strings
        get() = data
            .filter { it is String }.map { it as String }

    val longs
        get() = data
            .filter { it is String }.map { it as Long }

    fun update(data: (Set<Any?>) -> Set<Any?> = { it }): Property {
        val updatedData = data(this.data);
        return if (updatedData != this.data) this.copy(data = updatedData) else this;
    }
}

sealed class PropertyTree {
    abstract val property: Property
    abstract val children: List<PropertyTree>

    val name get() = property.name
    val description get() = property.description
    val data get() = property.data

    companion object {
        fun empty(): PropertyTree = EMPTY_NODE

        fun propertyNodeOf(
            name: String,
            vararg children: PropertyTree
        ): PropertyTree = propertyNodeOf(
            name = name, description = null, data = linkedSetOf(),
            children.asList()
        )

        fun propertyNodeOf(
            name: String,
            data: Set<Any?> = linkedSetOf(),
            vararg children: PropertyTree
        ): PropertyTree = propertyNodeOf(
            name = name, description = null, data = data,
            children.asList()
        )

        fun propertyNodeOf(
            name: String,
            description: String? = null,
            data: Set<Any?> = linkedSetOf(),
            vararg children: PropertyTree
        ): PropertyTree = propertyNodeOf(
            name = name, description = description, data = data,
            children.asList()
        )

        fun propertyNodeOf(
            name: String,
            description: String? = null,
            data: Set<Any?> = linkedSetOf(),
            children: List<PropertyTree> = listOf()
        ): PropertyTree =
            PropertyNode(Property(name, description, if (data is LinkedHashSet) data else linkedSetOf(data)),
                children.filter { it.notEmpty })
    }

    val empty get() = this == EMPTY_NODE

    val notEmpty get() = !empty;

    fun find(predicate: (PropertyTree) -> Boolean) = traverse().find(predicate) ?: EMPTY_NODE

    fun first(predicate: (PropertyTree) -> Boolean) = traverse().first(predicate)

    fun filter(predicate: (PropertyTree) -> Boolean) = traverse().filter(predicate)

    /**
     * Traverses the tree lazily in pre-order.
     */
    fun traverse(): Sequence<PropertyTree> = sequence {
        yield(this@PropertyTree)
        yieldAll(children.flatMap { it.traverse() })
    }

    fun prune(predicate: (PropertyTree) -> Boolean): PropertyTree {
        return this.replaceNode(EMPTY_NODE, predicate)
    }

    fun overwriteChildren(replacement: PropertyTree, name: String): PropertyTree =
        overwriteChildren(replacement, byName(name))

    fun overwriteChildren(replacement: PropertyTree, predicate: (PropertyTree) -> Boolean): PropertyTree =
        if (this.empty)
            this
        else updateChildren { it.filter(predicate) + replacement }

    fun updateNode(name: String, updater: (PropertyTree) -> PropertyTree) =
        find(byName(name)).let { if (it.empty) this else replaceNode(it, updater(it)) }

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

        return transformer(this, children);
    }

    fun updateChildren(
        children: (List<PropertyTree>) -> List<PropertyTree> = { it }
    ): PropertyTree {
        return with(children = children(this.children));
    }

    abstract fun with(
        property: Property = this.property,
        children: List<PropertyTree> = this.children
    ): PropertyTree;

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

    protected abstract fun drawNode(indent: String?, isLast: Boolean): String;
}

private class EmptyNode : PropertyTree() {
    override val property = EMPTY_PROPERTY;
    override val children = listOf<PropertyTree>()

    override fun toString(): String {
        return draw()
    }

    override fun with(property: Property, children: List<PropertyTree>): PropertyTree {
        return this;
    }

    override fun drawNode(indent: String?, isLast: Boolean) = "null";
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
