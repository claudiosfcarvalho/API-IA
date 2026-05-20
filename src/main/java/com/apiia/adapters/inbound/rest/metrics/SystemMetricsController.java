package com.apiia.adapters.inbound.rest.metrics;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Endpoint de observabilidade para o dashboard de desenvolvimento.
 *
 * Retorna métricas instantâneas de CPU, memória, JVM, rede e latência
 * do backend para exibição em tempo real no frontend.
 */
@RestController
@RequestMapping("/api/metrics")
public class SystemMetricsController {

    /**
     * Coleta snapshot de métricas do processo backend.
     *
     * @return payload consolidado para dashboard
     */
    @GetMapping(value = "/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public DashboardMetricsResponse dashboardMetrics() {
        long startedAt = System.nanoTime();

        java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        com.sun.management.OperatingSystemMXBean sunOsBean =
            osBean instanceof com.sun.management.OperatingSystemMXBean casted ? casted : null;
        Runtime runtime = Runtime.getRuntime();
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        long heapUsed = runtime.totalMemory() - runtime.freeMemory();
        long heapMax = runtime.maxMemory();
        long systemTotalMemory = sunOsBean != null ? sunOsBean.getTotalMemorySize() : 0L;
        long systemFreeMemory = sunOsBean != null ? sunOsBean.getFreeMemorySize() : 0L;
        double processCpuLoad = sunOsBean != null ? sunOsBean.getProcessCpuLoad() : -1.0;
        double systemCpuLoad = sunOsBean != null ? sunOsBean.getCpuLoad() : -1.0;

        CpuMetrics cpu = new CpuMetrics(
            safePercent(processCpuLoad),
            safePercent(systemCpuLoad),
                osBean.getAvailableProcessors(),
                safePercentFromLoadAverage(osBean.getSystemLoadAverage(), osBean.getAvailableProcessors())
        );

        MemoryMetrics memory = new MemoryMetrics(
                toMb(heapUsed),
                toMb(heapMax),
                heapMax > 0 ? round2((heapUsed * 100.0) / heapMax) : 0.0,
                toMb(systemTotalMemory),
                toMb(systemFreeMemory)
        );

        JvmMetrics jvm = new JvmMetrics(
                runtimeMxBean.getUptime(),
                threadMXBean.getThreadCount(),
                threadMXBean.getPeakThreadCount(),
                runtimeMxBean.getName()
        );

        NetworkMetrics network = captureNetworkMetrics();

        long processingTimeMs = (System.nanoTime() - startedAt) / 1_000_000;
        LatencyMetrics latency = new LatencyMetrics(processingTimeMs);

        return new DashboardMetricsResponse(
                Instant.now().toString(),
                cpu,
                memory,
                jvm,
                network,
                latency
        );
    }

    private static NetworkMetrics captureNetworkMetrics() {
        int interfaces = 0;
        int active = 0;
        int loopback = 0;
        List<String> ipv4 = new ArrayList<>();

        try {
            Enumeration<NetworkInterface> all = NetworkInterface.getNetworkInterfaces();
            if (all == null) {
                return new NetworkMetrics(0, 0, 0, List.of());
            }

            for (NetworkInterface ni : Collections.list(all)) {
                interfaces++;
                if (ni.isUp()) {
                    active++;
                }
                if (ni.isLoopback()) {
                    loopback++;
                }
                Collections.list(ni.getInetAddresses()).stream()
                        .map(a -> a.getHostAddress())
                        .filter(ip -> ip != null && ip.contains(".") && !ip.contains(":"))
                        .limit(2)
                        .forEach(ipv4::add);
            }
        } catch (SocketException ignored) {
            return new NetworkMetrics(0, 0, 0, List.of());
        }

        if (ipv4.size() > 8) {
            ipv4 = ipv4.subList(0, 8);
        }

        return new NetworkMetrics(interfaces, active, loopback, ipv4);
    }

    private static double safePercent(double value) {
        if (Double.isNaN(value) || value < 0) {
            return 0.0;
        }
        return round2(value * 100.0);
    }

    private static double safePercentFromLoadAverage(double loadAverage, int processors) {
        if (Double.isNaN(loadAverage) || loadAverage < 0 || processors <= 0) {
            return 0.0;
        }
        return round2((loadAverage / processors) * 100.0);
    }

    private static long toMb(long bytes) {
        if (bytes <= 0) {
            return 0;
        }
        return bytes / (1024 * 1024);
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public record DashboardMetricsResponse(
            String timestamp,
            CpuMetrics cpu,
            MemoryMetrics memory,
            JvmMetrics jvm,
            NetworkMetrics network,
            LatencyMetrics latency
    ) {
    }

    public record CpuMetrics(
            double processCpuPct,
            double systemCpuPct,
            int availableProcessors,
            double systemLoadPct
    ) {
    }

    public record MemoryMetrics(
            long heapUsedMb,
            long heapMaxMb,
            double heapUsagePct,
            long systemTotalMemoryMb,
            long systemFreeMemoryMb
    ) {
    }

    public record JvmMetrics(
            long uptimeMs,
            int threadCount,
            int peakThreadCount,
            String pid
    ) {
    }

    public record NetworkMetrics(
            int interfaceCount,
            int activeInterfaces,
            int loopbackInterfaces,
            List<String> ipv4Addresses
    ) {
    }

    public record LatencyMetrics(
            long processingTimeMs
    ) {
    }
}