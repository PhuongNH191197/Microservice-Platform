package com.platform.crbtcommunitylibrary.repository;

import com.platform.crbtcommunitylibrary.entity.Ringtone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RingtoneRepository extends JpaRepository<Ringtone, Long>, JpaSpecificationExecutor<Ringtone> {
}
