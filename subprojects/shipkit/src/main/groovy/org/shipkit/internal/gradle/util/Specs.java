package org.shipkit.internal.gradle.util;

import org.gradle.api.Task;
import org.gradle.api.specs.Spec;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static edu.emory.mathcs.backport.java.util.Arrays.asList;

/**
 * Helper methods for Gradle Spec objects
 */
public class Specs {

    /**
     * Task that matches any of the provided names
     */
    public static Spec<Task> withName(final String ... names) {
        Set<String> namesSet = new HashSet<>(asList(names));
        return new Spec<Task>() {
            @Override
            public boolean isSatisfiedBy(Task t) {
                return namesSet.contains(t.getName());
            }
        };
    }

    /**
     * Spec that checks if file exists using {@link File#isFile()} method.
     */
    public static Spec<Task> fileExists(final File file) {
        return new Spec<Task>() {
            @Override
            public boolean isSatisfiedBy(Task task) {
                return file.isFile();
            }
        };
    }
}
