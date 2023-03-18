package tourGuide.dto;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.helper.AttractionLocalDistance;

public class NearAttractionDTO {
    private String attractionName;
    private double attractionLatitude;
    private double attractionLongitude;
    private double userLocationLatitude;
    private double userLocationLongitude;
    private double attractionDistance;
    private int rewardsPoints;

    public NearAttractionDTO(Attraction attraction,
                             VisitedLocation visitedLocation,
                             AttractionLocalDistance attractionLocalDistance,
                             RewardCentral rewardCentral) {
        this.attractionName = attraction.attractionName;
        this.attractionLatitude = attraction.latitude;
        this.attractionLongitude = attraction.longitude;
        this.userLocationLatitude = visitedLocation.location.latitude;
        this.userLocationLongitude = visitedLocation.location.longitude;
        this.attractionDistance = attractionLocalDistance.getDistance();
        this.rewardsPoints = rewardCentral.getAttractionRewardPoints(attraction.attractionId, visitedLocation.userId);

    }

    public String getAttractionName() {
        return attractionName;
    }

    public void setAttractionName(String attractionName) {
        this.attractionName = attractionName;
    }

    public double getAttractionLatitude() {
        return attractionLatitude;
    }

    public void setAttractionLatitude(double attractionLatitude) {
        this.attractionLatitude = attractionLatitude;
    }

    public double getAttractionLongitude() {
        return attractionLongitude;
    }

    public void setAttractionLongitude(double attractionLongitude) {
        this.attractionLongitude = attractionLongitude;
    }

    public double getUserLocationLatitude() {
        return userLocationLatitude;
    }

    public void setUserLocationLatitude(double userLocationLatitude) {
        this.userLocationLatitude = userLocationLatitude;
    }

    public double getUserLocationLongitude() {
        return userLocationLongitude;
    }

    public void setUserLocationLongitude(double userLocationLongitude) {
        this.userLocationLongitude = userLocationLongitude;
    }

    public double getAttractionDistance() {
        return attractionDistance;
    }

    public void setAttractionDistance(double attractionDistance) {
        this.attractionDistance = attractionDistance;
    }

    public int getRewardsPoints() {
        return rewardsPoints;
    }

    public void setRewardsPoints(int rewardsPoints) {
        this.rewardsPoints = rewardsPoints;
    }
}
