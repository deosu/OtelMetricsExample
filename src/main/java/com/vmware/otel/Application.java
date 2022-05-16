package com.vmware.otel;

import java.util.Arrays;
import java.util.Collections;

import io.grpc.ManagedChannel;
import io.grpc.okhttp.OkHttpChannelBuilder;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc;
import io.opentelemetry.proto.metrics.v1.AggregationTemporality;
import io.opentelemetry.proto.metrics.v1.Histogram;
import io.opentelemetry.proto.metrics.v1.HistogramDataPoint;
import io.opentelemetry.proto.metrics.v1.InstrumentationLibraryMetrics;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;

/**
 * @author Sumit Deo (deosu@vmware.com)
 */
public class Application {
  public static void main(String[] args) {
    ManagedChannel managedChannel = managedChannel();
    MetricsServiceGrpc.MetricsServiceBlockingStub metricService =
        MetricsServiceGrpc.newBlockingStub(managedChannel);
    HistogramDataPoint point = HistogramDataPoint.newBuilder()
        .addAllExplicitBounds(Arrays.asList(1.0, 2.0, 3.0))
        .addAllBucketCounts(Arrays.asList(1L, 2L, 3L, 4L))
        .setTimeUnixNano(1515151515L)
        .build();

    Histogram otelHistogram = Histogram.newBuilder()
        .setAggregationTemporality(AggregationTemporality.AGGREGATION_TEMPORALITY_DELTA)
        .addAllDataPoints(Collections.singletonList(point))
        .build();

    Metric otelMetric = Metric.newBuilder()
        .setHistogram(otelHistogram)
        .setName("test-delta-histogram")
        .build();
    ResourceMetrics resourceMetrics = ResourceMetrics.newBuilder().addInstrumentationLibraryMetrics(InstrumentationLibraryMetrics.newBuilder().addMetrics(otelMetric).build()).build();
    ExportMetricsServiceRequest request = ExportMetricsServiceRequest.newBuilder().addResourceMetrics(resourceMetrics).build();
    metricService.export(request);
  }

  private static ManagedChannel managedChannel() {
    return OkHttpChannelBuilder.forAddress("localhost", 4317).usePlaintext().build();
  }
}
