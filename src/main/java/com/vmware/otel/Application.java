package com.vmware.otel;

import java.time.Duration;

import io.grpc.ManagedChannel;
import io.grpc.okhttp.OkHttpChannelBuilder;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.metrics.internal.view.ExponentialHistogramAggregation;
import io.opentelemetry.sdk.resources.Resource;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

/**
 * @author Sumit Deo (deosu@vmware.com)
 */
public class Application {
  private static final Resource RESOURCE =
      Resource.create(Attributes.of(stringKey("resource_key"), "resource_value"));
  public static void main(String[] args) {
    ManagedChannel managedChannel = managedChannel();
    OtlpGrpcMetricExporter metricExporter =
        OtlpGrpcMetricExporter.builder().setChannel(managedChannel).build();

    MetricReader periodicReader =
        PeriodicMetricReader.builder(metricExporter).setInterval(Duration.ofMillis(1000)).build();

    SdkMeterProvider sdkMeterProvider =
        SdkMeterProvider.builder()
            .setResource(RESOURCE)
            .registerMetricReader(periodicReader)
            .registerView(
                InstrumentSelector.builder().setType(InstrumentType.HISTOGRAM).build(),
                View.builder()
                    .setAggregation(ExponentialHistogramAggregation.create(-1, 5))
                    .build())
            .build();
    LongHistogram longHistogram =
        sdkMeterProvider
            .get(Application.class.getName())
            .histogramBuilder("testHistogram")
            .setDescription("description")
            .setUnit("ms")
            .ofLongs()
            .build();

    longHistogram.record(12L, Attributes.builder().put("key", "value").build());
    longHistogram.record(12L);
    longHistogram.record(13L);
  }

  private static ManagedChannel managedChannel() {
    return OkHttpChannelBuilder.forAddress("localhost", 4317).usePlaintext().build();
  }
}
