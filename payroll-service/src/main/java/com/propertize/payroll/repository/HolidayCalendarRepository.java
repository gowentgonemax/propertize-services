package com.propertize.payroll.repository;

import com.propertize.payroll.entity.HolidayCalendarEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface HolidayCalendarRepository extends JpaRepository<HolidayCalendarEntity, UUID> {

    List<HolidayCalendarEntity> findByClientId(UUID clientId);

    @Query("SELECT hc FROM HolidayCalendarEntity hc WHERE hc.client.id = :clientId AND hc.holidayDate BETWEEN :startDate AND :endDate")
    List<HolidayCalendarEntity> findByClientIdAndDateRange(
        @Param("clientId") UUID clientId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    @Query("SELECT hc FROM HolidayCalendarEntity hc WHERE hc.client.id = :clientId AND hc.holidayDate = :date")
    List<HolidayCalendarEntity> findByClientIdAndDate(
        @Param("clientId") UUID clientId,
        @Param("date") LocalDate date);

    @Query("SELECT hc FROM HolidayCalendarEntity hc WHERE hc.client.id = :clientId AND YEAR(hc.holidayDate) = :year ORDER BY hc.holidayDate")
    List<HolidayCalendarEntity> findByClientIdAndYear(
        @Param("clientId") UUID clientId,
        @Param("year") Integer year);

    @Query("SELECT CASE WHEN COUNT(hc) > 0 THEN true ELSE false END FROM HolidayCalendarEntity hc WHERE hc.client.id = :clientId AND hc.holidayDate = :date")
    boolean isHoliday(@Param("clientId") UUID clientId, @Param("date") LocalDate date);
}
