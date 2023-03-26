package tourGuide.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.dto.NearAttractionDTO;
import tourGuide.exception.UserAlreadyExistException;
import tourGuide.exception.UserNotFoundException;
import tourGuide.helper.InternalTestHelper;
import tourGuide.helper.AttractionLocalDistance;
import tourGuide.tracker.Tracker;
import tourGuide.user.User;
import tourGuide.user.UserReward;
import tripPricer.Provider;
import tripPricer.TripPricer;



import static java.util.stream.Collectors.toMap;


@Service
public class TourGuideService {
    private final Logger logger = LoggerFactory.getLogger(TourGuideService.class);
    private final GpsUtil gpsUtil;
    private final RewardsService rewardsService;
    private final TripPricer tripPricer = new TripPricer();
    public final Tracker tracker;
    boolean testMode = true;
    private final ExecutorService trackUserExecutorService = Executors.newFixedThreadPool(100);



    public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
        this.gpsUtil = gpsUtil;
        this.rewardsService = rewardsService;

        if (testMode) {
            logger.info("TestMode enabled");
            logger.debug("Initializing users");
            initializeInternalUsers();
            logger.debug("Finished initializing users");
        }
        tracker = new Tracker(this);
        addShutDownHook();
    }

    public List<UserReward> getUserRewards(User user) {
        return user.getUserRewards();
    }

    public VisitedLocation getUserLocation(User user) throws ExecutionException, InterruptedException {
        logger.debug("getLocation methode starts here, from TourGuideService");
        VisitedLocation visitedLocation =
                (user.getVisitedLocations().size() > 0) ? user.getLastVisitedLocation() : trackUserLocation(user).get();
        logger.info("Retrieve successfully user lastVisitedLocation if exists, if not track user current location");
        return visitedLocation;
    }
    /**
     * Retrieves a user by their username from the internal user map.
     * @param userName the username of the user to retrieve
     * @return the User object corresponding to the given username
     * @throws UserNotFoundException if a user with the given username does not exist in the internal user map
     */
    public User getUser(String userName) {
        User user = internalUserMap.get(userName);
        if (user == null) {
            logger.debug("This user doesn't exist in the DB with this username:{}, getUser method", userName);
            throw new UserNotFoundException("This user doesn't exist in the DB with this username: " + userName);
        }
        logger.info("User is retrieved by username: {}, from TourGuideService", user.getUserName());
        return user;
    }

    /**
     * Retrieve all Users
     *
     * @return A User List
     */
    public List<User> getAllUsers() {
        //List<User> users = internalUserMap.values().stream().collect(Collectors.toList());
        List<User> users = new ArrayList<>(internalUserMap.values());
        logger.info("{} Users are successfully retrieved , from TourGuideService", users.size());
        return users;
    }

    /**
     * Create a New User if Username doesn't exist in the DB!
     *
     * @param user User
     */
    public void addUser(User user) {
        if (!internalUserMap.containsKey(user.getUserName())) {
            internalUserMap.put(user.getUserName(), user);
            logger.info("New User successfully added, username:{}, from TourGuideService", user.getUserName());
        } else {
            logger.debug("Add this User with username: {} is rejected with some reason, i.e, already exists in the DB!", user.getUserName());
            throw new UserAlreadyExistException("This user with username: " + user.getUserName() + " already exist in the DB!");
        }
    }



    /**
     * Return the Map of all current locations of all users. Key is userId, value is Location
     *
     * @return CompletableFuture
     */
    public Map<String, Location> getAllCurrentLocations() {
        logger.debug("AllCurrentLocations starts here, from TourGuideService");
        return getAllUsers().stream().collect(toMap(user -> user.getUserId().toString(),
                user -> new Location(user.getLastVisitedLocation().location.longitude, user.getLastVisitedLocation().location.latitude)));
    }

    /**
     *
     * Gets trip deals for the specified user based on their preferences and reward points.
     * Calls tripPricer service, to retrieve available trip deals.
     * @param user User
     * @return A list of Provider objects containing information about available trip deals.
     */
    public List<Provider> getTripDeals(User user) {
        int cumulativeRewardPoints = user.getUserRewards().stream().mapToInt(UserReward::getRewardPoints).sum();

        List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(), user.getUserPreferences().getNumberOfAdults(),
                user.getUserPreferences().getNumberOfChildren(), user.getUserPreferences().getTripDuration(), cumulativeRewardPoints);

        user.setTripDeals(providers);
        return providers;
    }

    /**
     * Asynchronously tracks a user's location and updates the visited locations list.
     * It also calculates the user's rewards for visiting the location and returns the current visited location.
     * @param user the user whose location is being tracked
     * @return CompletableFuture VisitedLocation that supplies the user's current visited location
     */
    public CompletableFuture<VisitedLocation> trackUserLocation(User user) {
        //logger.debug("trackUserLocation from TourGuideService, user:{}, currentThread: {}", user.getUserName(), Thread.currentThread().getName());
        return CompletableFuture.supplyAsync(() -> {
                   // logger.debug(Thread.currentThread().getName() + " is working");
           return gpsUtil.getUserLocation(user.getUserId());
                        }, trackUserExecutorService)

                .thenApplyAsync(visitedLocation -> {
                    //logger.debug("addToVisitedLocation, user: {}, currentThread: {}", user.getUserName(), Thread.currentThread().getName());
                    user.addToVisitedLocations(visitedLocation);
                        rewardsService.calculateRewards(user);
                    return visitedLocation;
                }, trackUserExecutorService);
    }
    /**
     * Get the list of the nearest 5 attractions from a given location
     * @param visitedLocation the location visited by the user
     * @return the list of the 5 nearest attractions
     */
    public List<NearAttractionDTO> getNearByAttractions(VisitedLocation visitedLocation) {
        int attractionsNearest = 5;
        return gpsUtil.getAttractions()
                .stream()
                .map(attraction -> new AttractionLocalDistance(attraction, visitedLocation, rewardsService))
                .sorted(Comparator.comparing(AttractionLocalDistance::getDistance))
                .limit(attractionsNearest)
                .map(attraction -> new NearAttractionDTO(attraction, visitedLocation, attraction, new RewardCentral()))
                .collect(Collectors.toList());
    }


    private void addShutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(tracker::stopTracking));
    }

    /**********************************************************************************
     * <p>
     * Methods Below: For Internal Testing
     *
     **********************************************************************************/
    private static final String tripPricerApiKey = "test-server-api-key";
    // Database connection will be used for external users, but for testing purposes internal users are provided and stored in memory
    private final Map<String, User> internalUserMap = new HashMap<>();

    private void initializeInternalUsers() {
        IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
            String userName = "internalUser" + i;
            String phone = "000";
            String email = userName + "@tourGuide.com";
            User user = new User(UUID.randomUUID(), userName, phone, email);
            generateUserLocationHistory(user);

            internalUserMap.put(userName, user);
        });
        logger.info("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
        logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
    }

    private void generateUserLocationHistory(User user) {
        IntStream.range(0, 3).forEach(i -> user.addToVisitedLocations(new VisitedLocation(user.getUserId(), new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime())));
    }

    private double generateRandomLongitude() {
        double leftLimit = -180;
        double rightLimit = 180;
        // Random().nextDouble() retourne un nombre aléatoire à virgule flottante supérieur ou égal à 0,0 et inférieur à 1,0.
        return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
    }

    private double generateRandomLatitude() {
        double leftLimit = -85.05112878;
        double rightLimit = 85.05112878;
        return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
    }

    private Date getRandomTime() {
        // java.util.Random.nextInt(int n) : The nextInt(int n) is used to get a random number between 0(inclusive) and the number passed in this argument(n), exclusive
        LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
        return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
    }

}
