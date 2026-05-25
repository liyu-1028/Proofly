package com.lyllink.proofly.config;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Objects;

/**
 * 基础设施健康检查配置
 * 在应用启动阶段检查 MySQL、Redis 和 MinIO 是否可用
 */
@Slf4j
@Configuration
public class InfrastructureHealthChecker {

    private final StringRedisTemplate redisTemplate;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final JdbcTemplate jdbcTemplate;

    public InfrastructureHealthChecker(
            StringRedisTemplate redisTemplate,
            MinioClient minioClient,
            MinioProperties minioProperties,
            JdbcTemplate jdbcTemplate) {
        this.redisTemplate = redisTemplate;
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void check() {
        log.info("开始检查基础设施依赖...");

        // 1. 检查 MySQL
        try {
            jdbcTemplate.execute("SELECT 1");
            log.info("✔ MySQL 连接正常");
        } catch (Exception e) {
            log.error("✘ MySQL 连接失败: {}", e.getMessage());
            throw new RuntimeException("基础设施检查失败: MySQL 不可用", e);
        }

        // 2. 检查 Redis
        try {
            String pong = redisTemplate.execute(RedisConnection::ping);
            if (Objects.equals(pong, "PONG")) {
                log.info("✔ Redis 连接正常");
            } else {
                log.warn("! Redis 返回非预期响应: {}", pong);
            }
        } catch (Exception e) {
            log.error("✘ Redis 连接失败: {}", e.getMessage());
            throw new RuntimeException("基础设施检查失败: Redis 不可用", e);
        }

        // 3. 检查 MinIO
        try {
            minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .build());
            log.info("✔ MinIO 连接正常 (Bucket: {})", minioProperties.getBucket());
        } catch (Exception e) {
            log.error("✘ MinIO 连接失败: {}", e.getMessage());
            throw new RuntimeException("基础设施检查失败: MinIO 不可用", e);
        }

        log.info("所有基础设施依赖检查通过。");
    }
}
