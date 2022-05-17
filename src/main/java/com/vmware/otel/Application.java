package com.vmware.otel;

import java.time.Duration;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.proto.metrics.v1.AggregationTemporality;
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
    System.setProperty("otel.resource.attributes", "service.name=OtlpExporterExample");
    MeterProvider meterProvider = initOpenTelemetryMetrics();

    LongHistogram longHistogram =
        meterProvider
            .get("io.opentelemetry.example")
            .histogramBuilder("testHistogram")
            .setDescription("description")
            .setUnit("ms")
            .ofLongs()
            .build();

    longHistogram.record(10L);
    longHistogram.record(20L);
    longHistogram.record(30L);
    longHistogram.record(40L);
    longHistogram.record(50L);

    longHistogram.record(10L);
    longHistogram.record(40L);
    longHistogram.record(50L);

    longHistogram.record(10L);
    longHistogram.record(20L);
  }

  private static MeterProvider initOpenTelemetryMetrics() {
    OtlpGrpcMetricExporter metricExporter = OtlpGrpcMetricExporter.getDefault();

    MetricReader periodicReader =
        PeriodicMetricReader.builder(metricExporter).setInterval(Duration.ofMillis(1000)).build();

    SdkMeterProvider sdkMeterProvider =
        SdkMeterProvider.builder()
            .setResource(RESOURCE)
            .registerMetricReader(periodicReader)
            .registerView(
                InstrumentSelector.builder().setType(InstrumentType.HISTOGRAM).build(),
                View.builder()
                    .setAggregation(ExponentialHistogramAggregation.create(-1, 10))
                    .build())
            .build();

    Runtime.getRuntime().addShutdownHook(new Thread(sdkMeterProvider::shutdown));
    return sdkMeterProvider;
  }
}
