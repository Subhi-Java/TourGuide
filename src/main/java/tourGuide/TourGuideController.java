package tourGuide;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import gpsUtil.location.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.jsoniter.output.JsonStream;

import gpsUtil.location.VisitedLocation;
import tourGuide.service.TourGuideService;
import tourGuide.user.User;
import tripPricer.Provider;

@RestController
public class TourGuideController {
    private static final Logger logger = LoggerFactory.getLogger(TourGuideService.class);

    private final TourGuideService tourGuideService;

    @Autowired
    public TourGuideController(TourGuideService tourGuideService) {
        this.tourGuideService = tourGuideService;
    }

    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }

    @RequestMapping(value = "/getLocation", produces = "application/json")
    public String getLocation(@RequestParam String userName) throws ExecutionException, InterruptedException {
        VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));
        logger.info("Get successfully the location of an user, username: {}, from TourGuideController", userName);
        return JsonStream.serialize(visitedLocation.location);
    }

    @RequestMapping("/getNearbyAttractions")
    public String getNearbyAttractions(@RequestParam String userName) throws ExecutionException, InterruptedException {
        logger.debug("getNearByAttractions starts here.");
        VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));

        return JsonStream.serialize(tourGuideService.getNearByAttractions(visitedLocation));
    }

    @RequestMapping("/getRewards")
    public String getRewards(@RequestParam String userName) {
        return JsonStream.serialize(tourGuideService.getUserRewards(getUser(userName)));
    }

    @RequestMapping(value = "/getAllCurrentLocations", produces = "application/json")
    public String getAllCurrentLocations() {
        logger.debug("getAllCurrentLocations method starts here, form TourGuideController");

        Map<String, Location> allCurrentLocations = tourGuideService.getAllCurrentLocations();
        logger.info("AllCurrentLocations({} total) have been retrieved successfully, from TourGuideController", allCurrentLocations.size());
        return JsonStream.serialize(allCurrentLocations);
    }

    @RequestMapping("/getTripDeals")
    public String getTripDeals(@RequestParam String userName) {
        List<Provider> providers = tourGuideService.getTripDeals(getUser(userName));
        return JsonStream.serialize(providers);
    }

    private User getUser(String userName) {
        User userByUserName = tourGuideService.getUser(userName);
        logger.info("User is successfully retrieved by username: {}, from TourGuideController", userName);
        return userByUserName;
    }
}