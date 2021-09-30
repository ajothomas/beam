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

import java.io.Serializable;

/** A statistical snapshot of a {@link Snapshot}. */
public abstract class Snapshot implements Serializable {

  /**
   * Returns the value at the given quantile.
   *
   * @param quantile a given quantile, in {@code [0..1]}
   * @return the value in the distribution at {@code quantile}
   */
  public abstract double getValue(double quantile);

  /**
   * Returns the entire set of values in the snapshot.
   *
   * @return the entire set of values
   */
  public abstract long[] getValues();

  /**
   * Returns the number of values in the snapshot.
   *
   * @return the number of values
   */
  public abstract int size();
}
