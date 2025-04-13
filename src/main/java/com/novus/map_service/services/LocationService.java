package com.novus.map_service.services;

import com.novus.map_service.UuidProvider;
import com.novus.map_service.configuration.DateConfiguration;
import com.novus.map_service.dao.LocationDaoUtils;
import com.novus.map_service.dao.UserDaoUtils;
import com.novus.map_service.utils.LogUtils;
import com.novus.shared_models.GeoPoint;
import com.novus.shared_models.common.Kafka.KafkaMessage;
import com.novus.shared_models.common.Location.Location;
import com.novus.shared_models.common.Location.LocationType;
import com.novus.shared_models.common.Log.HttpMethod;
import com.novus.shared_models.common.Log.LogLevel;
import com.novus.shared_models.common.User.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private final LogUtils logUtils;
    private final LocationDaoUtils locationDaoUtils;
    private final UserDaoUtils userDaoUtils;
    private final UuidProvider uuidProvider;
    private final DateConfiguration dateConfiguration;

    public void processGetUserFavoriteLocations(KafkaMessage kafkaMessage) {
        User authenticatedUser = kafkaMessage.getAuthenticatedUser();
        log.info("Starting to process get user favorite locations request for user: {}", authenticatedUser.getId());

        try {
            authenticatedUser.setLastActivityDate(dateConfiguration.newDate());
            userDaoUtils.save(authenticatedUser);

            logUtils.buildAndSaveLog(
                    LogLevel.INFO,
                    "GET_USER_FAVORITE_LOCATIONS_SUCCESS",
                    kafkaMessage.getIpAddress(),
                    String.format("User with ID '%s' retrieved their favorite locations", authenticatedUser.getId()),
                    HttpMethod.GET,
                    "/private/map/favorite/locations",
                    "map-service",
                    null,
                    authenticatedUser.getId()
            );
            log.info("Favorite locations successfully retrieved for user: {}", authenticatedUser.getId());
        } catch (Exception e) {
            log.error("Error occurred while processing get user favorite locations request: {}", e.getMessage());
            logError(e, kafkaMessage, "GET_USER_FAVORITE_LOCATIONS_ERROR",
                    "Error processing get user favorite locations request",
                    HttpMethod.GET, "/private/map/favorite/locations", authenticatedUser);
        }
    }

    public void processSaveNewUserFavoriteLocation(KafkaMessage kafkaMessage) {
        User authenticatedUser = kafkaMessage.getAuthenticatedUser();
        Map<String, String> request = kafkaMessage.getRequest();
        log.info("Starting to process save new user favorite location request for user: {}", authenticatedUser.getId());

        try {
            String locationType = request.get("locationType");
            String name = request.get("name");
            String city = request.get("city");
            String country = request.get("country");
            String street = request.get("street");
            String formattedAddress = request.get("formattedAddress");
            String postalCode = request.get("postalCode");
            double latitude = Double.parseDouble(request.get("latitude"));
            double longitude = Double.parseDouble(request.get("longitude"));

            GeoPoint geoPoint = GeoPoint.builder()
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();

            Location location = Location.builder()
                    .id(uuidProvider.generateUuid())
                    .name(name)
                    .locationType(LocationType.valueOf(locationType))
                    .city(city)
                    .country(country)
                    .street(street)
                    .formattedAddress(formattedAddress)
                    .postalCode(postalCode)
                    .coordinates(geoPoint)
                    .build();

            authenticatedUser.setLastActivityDate(dateConfiguration.newDate());

            List<String> favoriteLocationIds = authenticatedUser.getFavoriteLocationIds();
            if (favoriteLocationIds == null) {
                favoriteLocationIds = new ArrayList<>();
                authenticatedUser.setFavoriteLocationIds(favoriteLocationIds);
            }
            favoriteLocationIds.add(location.getId());

            locationDaoUtils.save(location);
            userDaoUtils.save(authenticatedUser);

            logUtils.buildAndSaveLog(
                    LogLevel.INFO,
                    "SAVE_NEW_USER_FAVORITE_LOCATION_SUCCESS",
                    kafkaMessage.getIpAddress(),
                    String.format("User with ID '%s' saved a new favorite location '%s'",
                            authenticatedUser.getId(), name),
                    HttpMethod.POST,
                    "/private/map/favorite/location",
                    "map-service",
                    null,
                    authenticatedUser.getId()
            );
            log.info("New favorite location '{}' successfully saved for user: {}", name, authenticatedUser.getId());
        } catch (Exception e) {
            log.error("Error occurred while processing save new user favorite location request: {}", e.getMessage());
            logError(e, kafkaMessage, "SAVE_NEW_USER_FAVORITE_LOCATION_ERROR",
                    "Error processing save new user favorite location request",
                    HttpMethod.POST, "/private/map/favorite/location", authenticatedUser);
        }
    }

    public void processDeleteUserFavoriteLocation(KafkaMessage kafkaMessage) {
        User authenticatedUser = kafkaMessage.getAuthenticatedUser();
        Map<String, String> request = kafkaMessage.getRequest();
        log.info("Starting to process delete user favorite location request for user: {}", authenticatedUser.getId());

        try {
            String locationId = request.get("locationId");

            if (!authenticatedUser.getFavoriteLocationIds().contains(locationId)) {
                String errorMessage = String.format("Location with ID '%s' not found in user's favorites", locationId);
                throw new ResourceNotFoundException(errorMessage);
            }

            authenticatedUser.getFavoriteLocationIds().remove(locationId);
            authenticatedUser.setLastActivityDate(dateConfiguration.newDate());

            userDaoUtils.save(authenticatedUser);

            Optional<Location> optionalLocation = locationDaoUtils.findById(locationId);
            if (optionalLocation.isEmpty()) {
                String errorMessage = String.format("Location with ID '%s' not found in database", locationId);
                throw new ResourceNotFoundException(errorMessage);
            }

            locationDaoUtils.delete(optionalLocation.get());

            logUtils.buildAndSaveLog(
                    LogLevel.INFO,
                    "DELETE_USER_FAVORITE_LOCATION_SUCCESS",
                    kafkaMessage.getIpAddress(),
                    String.format("User with ID '%s' deleted favorite location with ID '%s'",
                            authenticatedUser.getId(), locationId),
                    HttpMethod.DELETE,
                    "/private/map/favorite/location/{id}" + locationId,
                    "map-service",
                    null,
                    authenticatedUser.getId()
            );
            log.info("Favorite location with ID {} successfully deleted for user: {}", locationId, authenticatedUser.getId());
        } catch (ResourceNotFoundException e) {
            log.error("Error occurred while processing delete user favorite location request: {}", e.getMessage());
            logError(e, kafkaMessage, "DELETE_USER_FAVORITE_LOCATION_ERROR",
                    e.getMessage(),
                    HttpMethod.DELETE, "/private/map/favorite/location/{id}" + request.get("locationId"), authenticatedUser);
        } catch (Exception e) {
            log.error("Error occurred while processing delete user favorite location request: {}", e.getMessage());
            logError(e, kafkaMessage, "DELETE_USER_FAVORITE_LOCATION_ERROR",
                    "Error processing delete user favorite location request",
                    HttpMethod.DELETE, "/private/map/favorite/location/{id}" + request.get("locationId"), authenticatedUser);
        }
    }

    public void processUpdateUserFavoriteLocation(KafkaMessage kafkaMessage) {
        User authenticatedUser = kafkaMessage.getAuthenticatedUser();
        Map<String, String> request = kafkaMessage.getRequest();
        log.info("Starting to process update user favorite location request for user: {}", authenticatedUser.getId());

        try {
            String locationId = request.get("locationId");
            String locationType = request.get("locationType");
            String name = request.get("name");
            String city = request.get("city");
            String country = request.get("country");
            String street = request.get("street");
            String formattedAddress = request.get("formattedAddress");
            String postalCode = request.get("postalCode");
            double latitude = Double.parseDouble(request.get("latitude"));
            double longitude = Double.parseDouble(request.get("longitude"));

            Optional<Location> optionalLocation = locationDaoUtils.findById(locationId);
            if (optionalLocation.isEmpty()) {
                String errorMessage = String.format("Location with ID '%s' not found in database", locationId);
                throw new ResourceNotFoundException(errorMessage);
            }

            GeoPoint geoPoint = GeoPoint.builder()
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();

            Location location = optionalLocation.get();
            location.setName(name);
            location.setLocationType(LocationType.valueOf(locationType));
            location.setCity(city);
            location.setCountry(country);
            location.setStreet(street);
            location.setFormattedAddress(formattedAddress);
            location.setPostalCode(postalCode);
            location.setCoordinates(geoPoint);

            location.setUpdatedAt(dateConfiguration.newDate());

            authenticatedUser.setLastActivityDate(dateConfiguration.newDate());

            userDaoUtils.save(authenticatedUser);
            locationDaoUtils.save(optionalLocation.get());

            logUtils.buildAndSaveLog(
                    LogLevel.INFO,
                    "UPDATE_USER_FAVORITE_LOCATION_SUCCESS",
                    kafkaMessage.getIpAddress(),
                    String.format("User with ID '%s' updated favorite location with ID '%s'",
                            authenticatedUser.getId(), locationId),
                    HttpMethod.PUT,
                    "/private/map/favorite/location/{id}" + locationId,
                    "map-service",
                    null,
                    authenticatedUser.getId()
            );
            log.info("Favorite location with ID {} successfully updated for user: {}", locationId, authenticatedUser.getId());
        } catch (Exception e) {
            log.error("Error occurred while processing update user favorite location request: {}", e.getMessage());
            logError(e, kafkaMessage, "UPDATE_USER_FAVORITE_LOCATION_ERROR",
                    "Error processing update user favorite location request",
                    HttpMethod.PUT, "/private/map/favorite/location/{id}" + request.get("locationId"), authenticatedUser);
        }
    }

    private void logError(Exception e, KafkaMessage kafkaMessage, String errorCode,
                          String message, HttpMethod httpMethod, String endpoint, User user) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();

        logUtils.buildAndSaveLog(
                LogLevel.ERROR,
                errorCode,
                kafkaMessage.getIpAddress(),
                message + ": " + e.getMessage(),
                httpMethod,
                endpoint,
                "map-service",
                stackTrace,
                user != null ? user.getId() : null
        );
        throw new RuntimeException(message + ": " + e.getMessage(), e);
    }
}