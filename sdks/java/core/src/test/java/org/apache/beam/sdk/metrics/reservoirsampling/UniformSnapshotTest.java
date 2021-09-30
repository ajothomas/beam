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
package org.apache.beam.sdk.metrics.reservoirsampling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import org.apache.beam.vendor.guava.v26_0_jre.com.google.common.primitives.Longs;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link UniformSnapshot}.*/
@RunWith(JUnit4.class)
public class UniformSnapshotTest {
  private final Snapshot snapshot = new UniformSnapshot(new long[]{5, 1, 2, 3, 4});

  @Test
  public void smallQuantilesAreTheFirstValue() {
    assertThat(snapshot.getValue(0.0))
        .isEqualTo(1, offset(0.1));
  }

  @Test
  public void bigQuantilesAreTheLastValue() {
    assertThat(snapshot.getValue(1.0))
        .isEqualTo(5, offset(0.1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void disallowsNotANumberQuantile() {
    snapshot.getValue(Double.NaN);
  }

  @Test(expected = IllegalArgumentException.class)
  public void disallowsNegativeQuantile() {
    snapshot.getValue(-0.5);
  }

  @Test(expected = IllegalArgumentException.class)
  public void disallowsQuantileOverOne() {
    snapshot.getValue(1.5);
  }

  @Test
  public void hasValues() {
    assertThat(snapshot.getValues()).containsOnly(1, 2, 3, 4, 5);
  }

  @Test
  public void hasValidSize() {
    assertThat(snapshot.size()).isEqualTo(5);
  }

  @Test
  public void canAlsoBeCreatedFromACollectionOfLongs() {
    final Snapshot other = new UniformSnapshot(Longs.asList(5L, 1L, 2L, 3L, 4L));

    assertThat(other.getValues())
        .containsOnly(1, 2, 3, 4, 5);
  }
}
