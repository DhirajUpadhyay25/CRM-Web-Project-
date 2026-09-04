package in.project.main.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import in.project.main.entities.Media;

public interface MediaRepository extends JpaRepository<Media, Long>, JpaSpecificationExecutor<Media> {

    Page<Media> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Media> findByMimeTypeStartingWith(String mimeTypePrefix);

    Page<Media> findByMimeTypeStartingWith(String mimeTypePrefix, Pageable pageable);

    List<Media> findByUploadedBy(Long uploadedBy);

    List<Media> findByUsageType(String usageType);

    List<Media> findByFolder(String folder);

    @Query("SELECT DISTINCT m.folder FROM Media m WHERE m.folder IS NOT NULL ORDER BY m.folder")
    List<String> findAllFolders();

    @Query("SELECT m FROM Media m WHERE LOWER(m.fileName) LIKE LOWER(CONCAT('%',:keyword,'%')) OR LOWER(m.originalName) LIKE LOWER(CONCAT('%',:keyword,'%')) OR LOWER(m.altText) LIKE LOWER(CONCAT('%',:keyword,'%')) ORDER BY m.createdAt DESC")
    Page<Media> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT SUM(m.fileSize) FROM Media m WHERE m.fileSize IS NOT NULL")
    Long getTotalStorageUsed();

    long count();
}
