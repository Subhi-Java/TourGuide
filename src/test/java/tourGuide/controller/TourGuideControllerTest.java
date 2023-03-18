package tourGuide.controller;

import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tourGuide.TourGuideController;
import tourGuide.dto.NearAttractionDTO;
import tourGuide.helper.InternalTestHelper;
import tourGuide.service.TourGuideService;
import tourGuide.user.User;
import tourGuide.user.UserReward;

import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TourGuideController.class)
class TourGuideControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext context;
    @MockBean
    private TourGuideService tourGuideService;


    @BeforeEach
    public void init() {
        Locale.setDefault(new Locale("en", "US"));
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void index() throws Exception {
        String greeting = "Greetings from TourGuide!";
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(greeting));
    }

    @Test
    void getLocation() throws Exception {
        InternalTestHelper.setInternalUserNumber(0);
        UUID userID = UUID.randomUUID();
        User user = new User(userID, "USER", "000", "userEmail");
        Location location = new Location(20.5, 30.54);
        VisitedLocation visitedLocation = new VisitedLocation(userID, location, new Date());
        user.addToVisitedLocations(visitedLocation);

        when(tourGuideService.getUser(user.getUserName())).thenReturn(user);
        when(tourGuideService.getUserLocation(user)).thenReturn(visitedLocation);

        mockMvc.perform(get("/getLocation")
                        .param("userName", user.getUserName()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size()", is(2)))
                .andExpect(jsonPath("$.latitude", is(20.5)));

    }

    @Test
    void getNearbyAttractions() throws Exception {
        InternalTestHelper.setInternalUserNumber(0);
        UUID userID = UUID.randomUUID();
        User user = new User(userID, "USER", "000", "userEmail");
        Location location = new Location(20.5, 30.54);
        VisitedLocation visitedLocation = new VisitedLocation(userID, location, new Date());
        user.addToVisitedLocations(visitedLocation);

        /* Attraction attraction = new Attraction("TourEiffel", "Paris", "France", location.latitude, location.longitude);
        AttractionLocalDistance attractionLocalDistance = new AttractionLocalDistance(attraction, visitedLocation, rewardsService);
        NearAttractionDTO nearAttractionDTO = new NearAttractionDTO(attraction, visitedLocation, attractionLocalDistance, rewardCentral);*/
        List<NearAttractionDTO> nearAttractionDTOS = new ArrayList<>(); // List.of(nearAttractionDTO)


        when(tourGuideService.getUser(user.getUserName())).thenReturn(user);
        when(tourGuideService.getUserLocation(user)).thenReturn(visitedLocation);
        when(tourGuideService.getNearByAttractions(visitedLocation)).thenReturn(nearAttractionDTOS);

        mockMvc.perform(get("/getNearbyAttractions")
                .param("userName", user.getUserName()))
                .andExpect(status().isOk());
    }

    @Test
    void getRewards() throws Exception {
        InternalTestHelper.setInternalUserNumber(0);
        UUID userID = UUID.randomUUID();
        User user = new User(userID, "USER", "000", "userEmail");
        List<UserReward> userRewards = new ArrayList<>();

        when(tourGuideService.getUser(user.getUserName())).thenReturn(user);
        when(tourGuideService.getUserRewards(user)).thenReturn(userRewards);

        mockMvc.perform(get("/getRewards")
                .param("userName", user.getUserName()))
                .andExpect(status().isOk());
    }

    @Test
    void getAllCurrentLocations() throws Exception {
        InternalTestHelper.setInternalUserNumber(0);
        UUID userID = UUID.randomUUID();
        User user = new User(userID, "USER", "000", "userEmail");
        Location location = new Location(20.5, 30.54);
        VisitedLocation visitedLocation = new VisitedLocation(userID, location, new Date());

        user.addToVisitedLocations(visitedLocation);

        Map<String, Location> allCurrentLocations = new HashMap<>();
        allCurrentLocations.put(user.getUserId().toString(), user.getLastVisitedLocation().location);

        when(tourGuideService.getUser(user.getUserName())).thenReturn(user);
        when(tourGuideService.getAllCurrentLocations()).thenReturn(allCurrentLocations);

        mockMvc.perform(get("/getAllCurrentLocations"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$." + user.getUserId().toString() + ".latitude", is(20.5)));

    }
}