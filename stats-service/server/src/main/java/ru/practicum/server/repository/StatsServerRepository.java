package ru.practicum.server.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import ru.practicum.server.model.*;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StatsServerRepository extends JpaRepository<EndpointHit, Long> {

    @Query("SELECT new ru.practicum.server.model.StatHits(e.app, e.uri, COUNT(DISTINCT e.ip)) " +
            "FROM EndpointHit AS e " +
            "WHERE e.timestamp BETWEEN ?1 AND ?2 AND (e.uri IN ?3 OR ?3 = null) " +
            "GROUP BY e.app, e.uri " +
            "ORDER BY COUNT(DISTINCT e.ip) DESC")
    List<StatHits> findUniqueStatsWithUris(LocalDateTime start, LocalDateTime end, List<String> uris);

    @Query("SELECT new ru.practicum.server.model.StatHits(e.app, e.uri, COUNT(e.ip)) " +
            "FROM EndpointHit as e " +
            "WHERE e.timestamp BETWEEN ?1 AND ?2 AND (e.uri IN ?3 OR ?3 = null) " +
            "GROUP BY e.app, e.uri " +
            "ORDER BY COUNT(e.ip) DESC")
    List<StatHits> findAllStatsWithUris(LocalDateTime start, LocalDateTime end, List<String> uris);
}