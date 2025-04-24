package com.github.thehilikus.call_graph.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;

/**
 * Filter to control which classes should be analyzed.
 */
public final class AnalysisFilter {
    private static final Logger LOG = LoggerFactory.getLogger(AnalysisFilter.class);

    @Nullable
    private final Set<String> include;
    @Nonnull
    private final Set<String> exclude;

    /**
     * @param include Collection of classes to include. If null, include everything
     * @param exclude Collection of classes to exclude
     */
    public AnalysisFilter(@Nullable Set<String> include, @Nonnull Set<String> exclude) {
        this.include = include;
        this.exclude = exclude;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (AnalysisFilter) obj;
        return Objects.equals(this.include, that.include) &&
                Objects.equals(this.exclude, that.exclude);
    }

    @Override
    public int hashCode() {
        return Objects.hash(include, exclude);
    }

    @Override
    public String toString() {
        return "AnalysisFilter[" +
                "include=" + include + ", " +
                "exclude=" + exclude + ']';
    }

    public boolean isClassIncluded(String className) {
        for (String prefix : exclude) {
            if (className.startsWith(prefix)) {
                LOG.trace("Excluding class {}", className);
                return false;
            }
        }
        if (include != null) {
            for (String prefix : include) {
                if (className.startsWith(prefix)) {
                    LOG.trace("Including class {}", className);
                    return true;
                }
            }
            return false;
        }

        return true;
    }
}
