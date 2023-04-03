package tourGuide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.user.User;
import tourGuide.user.UserReward;

@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;
    private static final Logger logger = LoggerFactory.getLogger(RewardsService.class);

    // proximity in miles
    private final int defaultProximityBuffer = 10;
    private int proximityBuffer = defaultProximityBuffer; // 5000, 10000;
    private final int attractionProximityRange = 200;
    private final GpsUtil gpsUtil;
    private final RewardCentral rewardsCentral;

    public ExecutorService getCalculateExecutorService() {
        return calculateExecutorService;
    }

    private final ExecutorService calculateExecutorService = Executors.newFixedThreadPool(100);

    public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
        this.gpsUtil = gpsUtil;
        this.rewardsCentral = rewardCentral;
    }

    public void setProximityBuffer(int proximityBuffer) {
        this.proximityBuffer = proximityBuffer;
    }

    public void setDefaultProximityBuffer() {
        proximityBuffer = defaultProximityBuffer;
    }

    /**
     * Calculates rewards for a given user based on their visited locations and proximity to attractions.
     * @param user  User for which to calculate rewards.
     * It retrieves the user's visited locations and the list of all attractions
     * It then iterates over each attraction and visited location, checking if the attraction is within proximity of the location.
     * If it is, a UserReward object is created for that attraction and location and the reward points for that attraction.
     * The UserReward object is then added to the user's list of rewards.
     * All of these operations are performed asynchronously using CompletableFutures.
     * Finally, the method waits for all the CompletableFuture objects to complete before returning.
     */
    public void calculateRewards(User user) {
        //logger.debug("calculateRewards starts here, from RewardsService");
        List<VisitedLocation> userLocations = user.getVisitedLocations();

        List<Attraction> attractions = gpsUtil.getAttractions();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Attraction attraction : attractions) {
            if (user.getUserRewards().stream().anyMatch(r -> r.attraction.attractionName.equals(attraction.attractionName))) {
                continue;
            }
            for (VisitedLocation visitedLocation : userLocations) {
                if (nearAttraction(visitedLocation, attraction)) {
                    CompletableFuture<Void> futureUserReward = CompletableFuture.runAsync(() -> {
                        UserReward userReward = new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user));
                        user.addUserReward(userReward);
                    }, calculateExecutorService);
                    futures.add(futureUserReward);
                    break;
                }
            }
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    /**
     * Checks if the given location is within the proximity range of the attraction.
     *
     * @param attraction the attraction to check proximity for
     * @param location the location to check
     * @return true if the location is within the proximity range, false otherwise
     */
    public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
        return !(getDistance(attraction, location) > attractionProximityRange);
    }
    /**
     * Checks if a visited location is within proximity to an attraction.
     * @param visitedLocation the visited location to check
     * @param attraction the attraction to compare against
     * @return true if the visited location is within proximity to the attraction, false otherwise
     */
    private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
        return !(getDistance(attraction, visitedLocation.location) > proximityBuffer);
    }


    /**
     * Calculates the reward points for a user visiting an attraction based on the attraction's ID and the user's ID.
     * @param attraction The attraction for which the reward points are being calculated.
     * @param user The user for whom the reward points are being calculated.
     * @return The reward points calculated for the user visiting the attraction.
     */
    private int getRewardPoints(Attraction attraction, User user) {
        return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
    }

    public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        return statuteMiles;
    }

}
