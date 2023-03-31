package tourGuide;

import com.jsoniter.output.JsonStream;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tourGuide.dto.UserPreferencesDTO;
import tourGuide.service.TourGuideService;
import tourGuide.user.User;
import tourGuide.user.UserReward;
import tripPricer.Provider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;


@RestController
public class TourGuideController {
    private static final Logger logger = LoggerFactory.getLogger(TourGuideService.class);

    private final TourGuideService tourGuideService;

    @Autowired
    public TourGuideController(TourGuideService tourGuideService) {
        this.tourGuideService = tourGuideService;
    }

    /**
     * @return a message of greetings
     */
    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }
    /**
     * Returns the location of a user in JSON format based on username.
     @param userName The username
     @return The location of the user in JSON format.
     @throws ExecutionException If there is an execution error while getting the user location.
     @throws InterruptedException If the thread is interrupted while getting the user location.
     */
    @RequestMapping(value = "/getLocation", produces = "application/json")
    public String getLocation(@RequestParam String userName) throws ExecutionException, InterruptedException {
        VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));
        logger.info("Get successfully the location of an user, username: {}, from TourGuideController", userName);
        return JsonStream.serialize(visitedLocation.location);
    }
    /**
     * Method handles the request for getting the 5 nearby attractions of a user based on their visited location.
     * @param userName username
     * @return a JSON string of the nearby attractions.
     * @throws ExecutionException if an execution error occurs while getting the user location.
     * @throws InterruptedException if the thread is interrupted while getting the user location.
     */
    @RequestMapping("/getNearbyAttractions")
    public String getNearbyAttractions(@RequestParam String userName) throws ExecutionException, InterruptedException {
        logger.debug("getNearByAttractions starts here.");
        VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));

        return JsonStream.serialize(tourGuideService.getNearByAttractions(visitedLocation));
    }
    /**
     * Returns the rewards of a given user in JSON format.
     * @param userName the userName
     * @return a JSON string representing the rewards of the user
     */
  /*  @RequestMapping("/getRewards")
    public String getRewards(@RequestParam String userName) {
        logger.debug("getRewards method starts here, form TourGuideController");
        return JsonStream.serialize(tourGuideService.getUserRewards(getUser(userName)));
    }*/
    @RequestMapping("/getRewards")
    public List<UserReward> getRewards(@RequestParam String userName) {
        logger.debug("getRewards method starts here, form TourGuideController");
        return tourGuideService.getUserRewards(getUser(userName));
    }
    /**
     * Method returns a JSON string representation of all current locations of all users.
     * @return a JSON string representation of all current locations of all users
     */
    @RequestMapping(value = "/getAllCurrentLocations", produces = "application/json")
    public String getAllCurrentLocations() {
        logger.debug("getAllCurrentLocations method starts here, form TourGuideController");

        Map<String, Location> allCurrentLocations = tourGuideService.getAllCurrentLocations();
        logger.info("AllCurrentLocations({} total) have been retrieved successfully, from TourGuideController", allCurrentLocations.size());
        return JsonStream.serialize(allCurrentLocations);
    }
    /**
     * Returns the trip deals for a user as a JSON string.
     * @param userName the UserName
     * @return a JSON string containing the trip deals for the user
     */
   /* @RequestMapping( "/getTripDeals")
    public String getTripDeals(@RequestParam String userName) {
        logger.debug("getTripDeals method starts here, form TourGuideController");
        List<Provider> providers = tourGuideService.getTripDeals(getUser(userName));
        return JsonStream.serialize(providers);
    }*/
    @RequestMapping("/getTripDeals")
    public List<Provider> getTripDeals(@RequestParam String userName) {
        logger.debug("getTripDeals method starts here, form TourGuideController");
        List<Provider> providers = tourGuideService.getTripDeals(getUser(userName));
        return providers;
    }
    /**
     * Updates the user preferences for a given user.
     * @param userName username
     * @param userPreferencesDTO the UserPreferencesDTO object containing the updated user preferences
     */
    @RequestMapping("/update-user_preference")
    public void updateUserPreference(@RequestParam String userName, @RequestBody UserPreferencesDTO userPreferencesDTO) {
        logger.debug("updateUserPreference method starts here, form TourGuideController");
        tourGuideService.updateUserPreferences(userName, userPreferencesDTO);
    }

    /**
     * Retrieve a user by its username.
     * @param userName the userName
     * @return the User object corresponding to the given username
     */
    @RequestMapping("/users/user")
    private User getUser(@RequestParam String userName) {
        User userByUserName = tourGuideService.getUser(userName);
        logger.info("User is successfully retrieved by username: {}, from TourGuideController", userName);
        return userByUserName;
    }
    @RequestMapping("/users")
    private List<User> getAllUser() {
        List<User> users = tourGuideService.getAllUsers();
        logger.info("Users are successfully retrieved, from TourGuideController");
        return users;
    }
}