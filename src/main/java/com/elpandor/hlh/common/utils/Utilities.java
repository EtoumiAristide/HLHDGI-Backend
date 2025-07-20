package com.elpandor.hlh.common.utils;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class Utilities {

    /**
     * Format de retour pour une reponse paginée
     *
     * @param listOfValue
     * @param page
     * @param status
     * @return
     */
    public static Map<String, Object> pagingResponse(Object listOfValue, Page page, boolean status) {

        Map<String, Object> response = new HashMap<>();
        response.put("status", status);
        response.put("data", listOfValue);
        response.put("currentPage", page.getNumber());
        response.put("recordsTotal", page.getTotalElements());
        return response;
    }

    /**
     * Format de retour pour une reponse unique
     *
     * @param object
     * @param status
     * @return
     */
    public static Map<String, Object> singleResponse(Object object, boolean status) {

        Map<String, Object> response = new HashMap<>();
        response.put("status", status);
        response.put("data", object);
        return response;
    }

    /**
     * Format de retour pour une reponse vide
     *
     * @param status
     * @param errorCode
     * @param message
     * @return
     */
    public static Map<String, Object> pagingResponseEmpty(boolean status, String errorCode, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", status);
        response.put("error_code", errorCode);
        response.put("error_message", message);
        return response;
    }

    public static String convertInstantToLocalDateFormattedd(Instant instant, String format) {
        LocalDate localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return localDate.format(formatter);
    }

    public static String convertInstantToLocalDateTimeFormatted(Instant instant, String format) {
        LocalDateTime localDate = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return localDate.format(formatter);
    }

    public static String convertDateToStringFormatted(Date date, String format) {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date);
    }

    public static String convertDateToString(Date date, String format) {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date);
    }

    public static Date convertStringToDate(String dateString, String format) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.parse(dateString);
    }

    public static Instant convertStringToInstant(String dateString) {
        // Utiliser le format ISO 8601 pour analyser la chaîne en Instant
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        return Instant.from(formatter.parse(dateString));
    }

    public static LocalDateTime convertStringToLocalDateTime(String dateString, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        if (!(dateString.length() >= 11)) {
            return LocalDateTime.parse(dateString, formatter);
        }
        return LocalDateTime.parse(dateString, formatter);
    }

    public static ResponseEntity<Map<String, Object>> createSuccessResponse(HttpStatus status, Page<?> page, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("data", page.getContent());
        response.put("message", message);
        response.put("current_page", page.getNumber());
        response.put("total_items", page.getTotalElements());
        response.put("total_pages", page.getTotalPages());
        response.put("page_size", page.getSize());
        return ResponseEntity.status(status).body(response);
    }

    public static <T> ResponseEntity<Map<String, Object>> createSuccessResponse(HttpStatus status, List<T> list, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("data", list);
        response.put("message", message);
        response.put("total_items", list.size());
        return ResponseEntity.status(status).body(response);
    }

    public static ResponseEntity<Map<String, Object>> createSuccessResponse(HttpStatus status, Object data, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("data", data);
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }

    public static ResponseEntity<Map<String, Object>> createErrorResponse(String message, Object data, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", false);
        response.put("data", data);
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }

    public static String getFileUri(String fileName, String link) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("api/v1/" + link + "/")
                .path(fileName)
                .toUriString();
    }
}
