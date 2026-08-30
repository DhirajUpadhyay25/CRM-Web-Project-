package in.project.main.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import in.project.main.entities.Certificate;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    java.util.Optional<Certificate> findByEnrollmentId(String enrollmentId);
    java.util.Optional<Certificate> findByCertificateCode(String certificateCode);
}
