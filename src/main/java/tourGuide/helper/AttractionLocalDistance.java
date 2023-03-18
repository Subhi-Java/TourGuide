package tourGuide.helper;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import tourGuide.service.RewardsService;

public class AttractionLocalDistance extends Attraction {
    private final double distance;
    public AttractionLocalDistance(Attraction attraction, VisitedLocation visitedLocation, RewardsService rewardsService) {
        super(attraction.attractionName, attraction.city, attraction.state, attraction.latitude, attraction.longitude);
        this.distance = rewardsService.getDistance(attraction, visitedLocation.location);
    }
    public double getDistance() {
        return this.distance;
    }

}
