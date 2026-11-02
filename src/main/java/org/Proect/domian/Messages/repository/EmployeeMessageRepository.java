package org.Proect.domian.Messages.repository;

import org.Proect.domian.Messages.model.EmployeeMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeMessageRepository extends JpaRepository<EmployeeMessage, Long> {
    List<EmployeeMessage> findByEmployeeIdOrderBySentAtAscIdAsc(Integer employeeId);
}
