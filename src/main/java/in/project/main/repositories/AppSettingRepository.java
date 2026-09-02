package in.project.main.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import in.project.main.entities.AppSetting;

@Repository
public interface AppSettingRepository extends JpaRepository<AppSetting, Long> {

    Optional<AppSetting> findBySettingKey(String settingKey);

    List<AppSetting> findBySettingCategoryOrderBySettingKeyAsc(String settingCategory);

    boolean existsBySettingKey(String settingKey);
}
