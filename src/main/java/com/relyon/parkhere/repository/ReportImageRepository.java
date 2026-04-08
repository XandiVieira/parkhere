package com.relyon.parkhere.repository;

import com.relyon.parkhere.model.ReportImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportImageRepository extends JpaRepository<ReportImage, UUID> {

    List<ReportImage> findByReportId(UUID reportId);
}
