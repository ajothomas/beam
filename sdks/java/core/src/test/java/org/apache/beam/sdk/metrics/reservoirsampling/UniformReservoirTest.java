package org.apache.beam.sdk.metrics.reservoirsampling;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link UniformReservoir}.*/
@RunWith(JUnit4.class)
public class UniformReservoirTest {
  @Test
  public void testReservoir() {
    final UniformReservoir reservoir = new UniformReservoir(100);
    for (int i = 0; i < 1000; i++) {
      reservoir.update(i);
    }

    final Snapshot snapshot = reservoir.getSnapshot();

    assertThat(reservoir.size())
        .isEqualTo(100);
    assertThat(snapshot.size())
        .isEqualTo(100);
    for (double i : snapshot.getValues()) {
      assertThat(i)
          .isLessThan(1000)
          .isGreaterThanOrEqualTo(0);
    }
  }
}
