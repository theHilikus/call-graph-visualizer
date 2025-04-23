package com.github.thehilikus.call_graph.run;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * @param include Collection of classes to include. If null, include everything
 * @param exclude Collection of classes to exclude
 */
public record AnalysisFilter(@Nullable Set<String> include, @Nonnull Set<String> exclude) {
}
