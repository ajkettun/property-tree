package org.github.ajkettun.propertytree;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.github.ajkettun.propertytree.PropertyTree.propertyNodeOf;
import static org.github.ajkettun.propertytree.PropertyTreeUtils.byCriteria;
import static org.github.ajkettun.propertytree.PropertyTreeUtils.byName;
import static org.junit.jupiter.api.Assertions.*;

class PropertyTreeJavaInteropTest {
    @Test
    void construct() {
        var propertyTree = propertyNodeOf(
                "root", "desc", Set.of(1, 2, 3),
                propertyNodeOf("child1", Set.of(1)),
                propertyNodeOf("child2"));
        assertNotNull(propertyTree);
    }

    @Test
    void find() {
        var propertyTree = propertyNodeOf(
                "root", "desc", Set.of(1, 2, 3),
                propertyNodeOf("child1", Set.of(true)),
                propertyNodeOf("child2"));

        assertTrue(propertyTree.find(byName("child1")).getProperty().isSet());
        assertFalse(propertyTree.find(byName("child2")).getProperty().isSet());
        assertFalse(propertyTree.find(byName("child3")).getProperty().isSet());
    }

    @Test
    void findChild() {
        var propertyTree = propertyNodeOf(
                "root", "desc", Set.of(1, 2, 3),
                propertyNodeOf("child1", Set.of(true)),
                propertyNodeOf("child2"));

        propertyTree.findChild(byName("foo")).getProperty().isSet();
    }

    @Test
    void excludeInclude() {
        var propertyTree = propertyNodeOf(
                "root", "desc", Set.of(1, 2, 3),
                propertyNodeOf("child1", Set.of(true)),
                propertyNodeOf("child2"));

        var criteria = new Criteria(
                new Criterion("foo", 1),
                new Criterion("bar", "sf", "34"),
                new Criterion("car", 1));
        propertyTree.filter(byCriteria(criteria));
    }
}
