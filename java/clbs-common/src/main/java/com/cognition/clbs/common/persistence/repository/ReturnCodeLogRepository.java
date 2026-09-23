package com.cognition.clbs.common.persistence.repository;

import com.cognition.clbs.common.persistence.entity.ReturnCodeLog;
import com.cognition.clbs.common.persistence.entity.ReturnCodeLogKey;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link ReturnCodeLog}. */
public interface ReturnCodeLogRepository extends JpaRepository<ReturnCodeLog, ReturnCodeLogKey> {}
