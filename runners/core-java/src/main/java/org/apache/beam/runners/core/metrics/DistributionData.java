/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.beam.runners.core.metrics;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.apache.beam.sdk.metrics.DistributionResult;
import org.apache.beam.sdk.metrics.reservoirsampling.Reservoir;
import org.apache.beam.sdk.metrics.reservoirsampling.Snapshot;
import org.apache.beam.sdk.metrics.reservoirsampling.UniformReservoir;
import org.apache.beam.vendor.guava.v26_0_jre.com.google.common.annotations.VisibleForTesting;
import org.apache.beam.vendor.guava.v26_0_jre.com.google.common.collect.ImmutableMap;

/**
 * Data describing the the distribution. This should retain enough detail that it can be combined
 * with other {@link DistributionData}.
 *
 * <p>This is kept distinct from {@link DistributionResult} since this may be extended to include
 * data necessary to approximate quantiles, etc. while {@link DistributionResult} would just include
 * the approximate value of those quantiles.
 */
public class DistributionData implements Serializable {
  private static final Map<Double, Function<Snapshot, Double>> DEFAULT_PERCENTILE_EXTRACTOR =
      ImmutableMap.<Double, Function<Snapshot, Double>>builder()
          .put(25.0D, new PercentileExtractor(25.0D))
          .put(50.0D, new PercentileExtractor(50.0D))
          .put(75.0D, new PercentileExtractor(75.0D))
          .put(90.0D, new PercentileExtractor(90.0D))
          .put(95.0D, new PercentileExtractor(95.0D))
          .put(99.0D, new PercentileExtractor(99.0D))
          .build();

  private Reservoir reservoir;
  private long sum;
  private long count;
  private long min;
  private long max;
  private Map<Double, Double> percentiles;

  /** Backward compatible static factory method. */
  public static DistributionData empty() {
    return new DistributionData();
  }

  @VisibleForTesting
  public static DistributionData create(long sum, long count, long min, long max) {
    return create(sum, count, min, max, ImmutableMap.of());
  }

  @VisibleForTesting
  public static DistributionData create(
      long sum, long count, long min, long max, Map<Double, Double> percentiles) {
    return new DistributionData(sum, count, min, max, percentiles);
  }

  /**
   * Constructor primarily used for testing. No reservoir is created, percentiles are directly
   * assigned.
   */
  private DistributionData(
      long sum, long count, long min, long max, Map<Double, Double> percentiles) {
    this.reservoir = null;
    this.sum = sum;
    this.count = count;
    this.min = min;
    this.max = max;
    this.percentiles = percentiles;
  }

  private DistributionData() {
    this.reservoir = new UniformReservoir();
    this.sum = 0L;
    this.count = 0L;
    this.min = Long.MAX_VALUE;
    this.max = Long.MIN_VALUE;
    this.percentiles = ImmutableMap.of();
  }

  public static DistributionData singleton(long value) {
    final DistributionData distributionData = new DistributionData();
    distributionData.update(value);
    return distributionData;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Getters

  public long sum() {
    return sum;
  }

  public long count() {
    return count;
  }

  public long min() {
    return min;
  }

  public long max() {
    return max;
  }

  public Map<Double, Double> percentiles() {
    return percentiles;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////

  public DistributionResult extractResult() {
    return DistributionResult.create(sum(), count(), min(), max(), percentiles());
  }

  // Function to extract a percentile value from the Histogram snapshot
  private static class PercentileExtractor implements Function<Snapshot, Double> {
    private final Double percentile;

    PercentileExtractor(Double percentile) {
      this.percentile = percentile;
    }

    @Override
    public Double apply(Snapshot snapshot) {
      return snapshot.getValue(percentile / 100);
    }
  }

  public void update(long value) {
    ++count;
    min = Math.min(min, value);
    max = Math.max(max, value);
    sum += value;
    if (reservoir != null) {
      reservoir.update(value);
    }
    percentiles = computePercentiles();
  }

  /** Gets the value percentiles as a map. */
  public Map<Double, Double> computePercentiles() {
    if (reservoir == null || reservoir.size() == 0) {
      // if there is no reservoir assigned or the reservoir is empty, skip computation and return
      // the existing value
      return percentiles;
    }
    final Snapshot snapshot = reservoir.getSnapshot();
    return DEFAULT_PERCENTILE_EXTRACTOR.entrySet().stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Map.Entry::getKey,
                kv -> {
                  final Function<Snapshot, Double> metricExtractor = kv.getValue();
                  return metricExtractor.apply(snapshot);
                }));
  }

  public DistributionData combine(DistributionData other) {
    if (reservoir != null && other.reservoir != null) {
      // keep only the larger reservoir
      reservoir = reservoir.size() > other.reservoir.size() ? reservoir : other.reservoir;
      percentiles = computePercentiles();
    }
    sum += other.sum;
    count += other.count;
    max = Math.max(max, other.max);
    min = Math.min(min, other.min);
    return this;
  }

  public DistributionData reset() {
    if (reservoir != null) {
      this.reservoir = new UniformReservoir();
    }
    this.sum = 0L;
    this.count = 0L;
    this.min = Long.MAX_VALUE;
    this.max = Long.MIN_VALUE;
    this.percentiles = ImmutableMap.of();
    return this;
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof DistributionData) {
      DistributionData other = (DistributionData) object;
      return Objects.equals(reservoir, other.reservoir)
          && Objects.equals(max, other.max())
          && Objects.equals(min, other.min())
          && Objects.equals(count, other.count())
          && Objects.equals(sum, other.sum())
          && Objects.equals(percentiles, other.percentiles());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(min, max, sum, count, percentiles, reservoir);
  }

  @Override
  public String toString() {
    return "DistributionData{"
        + "sum="
        + sum
        + ", "
        + "count="
        + count
        + ", "
        + "min="
        + min
        + ", "
        + "max="
        + max
        + ", "
        + "percentiles="
        + percentiles
        + "}";
  }
}
