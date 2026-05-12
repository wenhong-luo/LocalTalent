package cn.localtalent.backend.company.workbench.infrastructure;

import cn.localtalent.backend.common.exception.ApiException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CompanyStyleImageStorageService {

    private final MinioClient minioClient;
    private final String bucket;

    public CompanyStyleImageStorageService(
            @Value("${localtalent.minio.endpoint}") String endpoint,
            @Value("${localtalent.minio.access-key}") String accessKey,
            @Value("${localtalent.minio.secret-key}") String secretKey,
            @Value("${localtalent.minio.bucket}") String bucket
    ) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(5))
                .writeTimeout(Duration.ofSeconds(15))
                .readTimeout(Duration.ofSeconds(20))
                .callTimeout(Duration.ofSeconds(30))
                .build();
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .httpClient(httpClient)
                .build();
        this.bucket = bucket;
    }

    public void put(String objectKey, byte[] content, String contentType) {
        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(content), (long) content.length, -1L)
                    .contentType(contentType)
                    .build());
        } catch (Exception exception) {
            throw storageError("failed to write company style image");
        }
    }

    public byte[] get(String objectKey) {
        try {
            ensureBucket();
            try (var object = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build())) {
                return object.readAllBytes();
            }
        } catch (Exception exception) {
            throw storageError("failed to read company style image");
        }
    }

    public void deleteQuietly(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            ensureBucket();
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception ignored) {
            // Object keys are sensitive; intentionally avoid logging them.
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
