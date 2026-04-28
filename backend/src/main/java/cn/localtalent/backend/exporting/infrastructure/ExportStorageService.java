package cn.localtalent.backend.exporting.infrastructure;

import cn.localtalent.backend.common.exception.ApiException;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http.Method;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ExportStorageService {

    private final MinioClient minioClient;
    private final String bucket;

    public ExportStorageService(
            @Value("${localtalent.minio.endpoint}") String endpoint,
            @Value("${localtalent.minio.access-key}") String accessKey,
            @Value("${localtalent.minio.secret-key}") String secretKey,
            @Value("${localtalent.minio.bucket}") String bucket
    ) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(5))
                .writeTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(10))
                .callTimeout(Duration.ofSeconds(20))
                .build();
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .httpClient(httpClient)
                .build();
        this.bucket = bucket;
    }

    public void putCsv(String objectKey, byte[] content) {
        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(content), (long) content.length, -1L)
                    .contentType("text/csv; charset=utf-8")
                    .build());
        } catch (Exception exception) {
            throw storageError("failed to write export file");
        }
    }

    public String presignedGetUrl(String objectKey, int ttlSeconds) {
        try {
            ensureBucket();
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(ttlSeconds, TimeUnit.SECONDS)
                    .build());
        } catch (Exception exception) {
            throw storageError("failed to create download url");
        }
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private ApiException storageError(String message) {
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SYS_500", message);
    }
}
