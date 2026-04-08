package com.relyon.parkhere.repository;

import com.relyon.parkhere.model.ParkingSpot;
import com.relyon.parkhere.model.enums.SpotType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

public class ParkingSpotRepositoryCustomImpl implements ParkingSpotRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public Page<ParkingSpot> searchWithFilters(double lat, double lng, double radiusMeters,
                                                SpotType type, Double maxPrice,
                                                Boolean requiresBooking, Double minTrustScore,
                                                String query, Pageable pageable) {
        var conditions = new ArrayList<String>();
        conditions.add("ps.active = true");
        conditions.add("ST_DWithin(ps.location::geography, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)");

        if (type != null) conditions.add("ps.type = :type");
        if (maxPrice != null) conditions.add("ps.price_max <= :maxPrice");
        if (requiresBooking != null) conditions.add("ps.requires_booking = :requiresBooking");
        if (minTrustScore != null) conditions.add("ps.trust_score >= :minTrustScore");
        if (query != null && !query.isBlank()) conditions.add("(LOWER(ps.name) LIKE LOWER(:query) OR LOWER(ps.address) LIKE LOWER(:query))");

        var whereClause = String.join(" AND ", conditions);

        var dataSql = "SELECT ps.* FROM parking_spots ps WHERE " + whereClause
                + " ORDER BY ST_Distance(ps.location::geography, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography)";
        var countSql = "SELECT COUNT(*) FROM parking_spots ps WHERE " + whereClause;

        var dataQuery = entityManager.createNativeQuery(dataSql, ParkingSpot.class);
        var countQuery = entityManager.createNativeQuery(countSql);

        dataQuery.setParameter("lat", lat);
        dataQuery.setParameter("lng", lng);
        dataQuery.setParameter("radiusMeters", radiusMeters);
        countQuery.setParameter("lat", lat);
        countQuery.setParameter("lng", lng);
        countQuery.setParameter("radiusMeters", radiusMeters);

        if (type != null) {
            dataQuery.setParameter("type", type.name());
            countQuery.setParameter("type", type.name());
        }
        if (maxPrice != null) {
            dataQuery.setParameter("maxPrice", maxPrice);
            countQuery.setParameter("maxPrice", maxPrice);
        }
        if (requiresBooking != null) {
            dataQuery.setParameter("requiresBooking", requiresBooking);
            countQuery.setParameter("requiresBooking", requiresBooking);
        }
        if (minTrustScore != null) {
            dataQuery.setParameter("minTrustScore", minTrustScore);
            countQuery.setParameter("minTrustScore", minTrustScore);
        }
        if (query != null && !query.isBlank()) {
            var queryParam = "%" + query + "%";
            dataQuery.setParameter("query", queryParam);
            countQuery.setParameter("query", queryParam);
        }

        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());

        List<ParkingSpot> results = dataQuery.getResultList();
        var total = ((Number) countQuery.getSingleResult()).longValue();

        return new PageImpl<>(results, pageable, total);
    }
}
