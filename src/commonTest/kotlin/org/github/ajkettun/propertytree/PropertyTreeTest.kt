package org.github.ajkettun.propertytree

import org.github.ajkettun.propertytree.PropertyTree.Companion.propertyNodeOf
import kotlin.test.Test
import kotlin.test.assertEquals

class PropertyTreeTest {
    companion object {
        val testPropertyTree = propertyNodeOf(
            "root", "description", linkedSetOf(1, 2),
            propertyNodeOf("child1", linkedSetOf("foo")),
            propertyNodeOf(
                "child2",
                propertyNodeOf("child3", linkedSetOf(linkedMapOf(Pair("foo", "bar"))))
            )
        )
    }

    @Test
    fun construct() {
        val expected = propertyNode {
            name = "root"
            description = "description"
            data = linkedSetOf(1, 2)

            propertyNode {
                name = "child1"
                data = linkedSetOf("foo")
            }

            propertyNode {
                name = "child2"

                propertyNode {
                    name = "child3"
                }
            }
        }

        val actual =
            propertyNodeOf(
                "root", "description", linkedSetOf(1, 2),
                propertyNodeOf("child1", linkedSetOf("foo")),
                propertyNodeOf(
                    "child2",
                    propertyNodeOf("child3")
                )
            )

        assertEquals(expected, actual)
    }

    @Test
    fun constructWithTypeSafeBuilder() {
        val expected = propertyNodeOf(
            "root", "description", linkedSetOf(1, 2),
            propertyNodeOf("child1", linkedSetOf("foo")),
            propertyNodeOf(
                "child2",
                propertyNodeOf("child3")
            )
        )

        val actual = propertyNode {
            name = "root"
            description = "description"
            data = linkedSetOf(1, 2)

            propertyNode {
                name = "child1"
                data = linkedSetOf("foo")
            }

            propertyNode {
                name = "child2"

                propertyNode {
                    name = "child3"
                }
            }
        }
        assertEquals(expected, actual)
    }

    @Test
    fun traverse() {
        val traversal = testPropertyTree.traverse().map { it.name }
        assertEquals(listOf("root", "child1", "child2", "child3"), traversal.toList())
    }

    @Test
    fun findAll() {
        val traversal = testPropertyTree.findAll { it.name.startsWith("child") }.map { it.name }
        assertEquals(listOf("child1", "child2", "child3"), traversal.toList())
    }
}