package company.aura.erp.dto.storage;

import java.time.Instant;

public record R2FileInfo(
        String key,
        Long size,
        Instant lastModified,
        String eTag
) {}
