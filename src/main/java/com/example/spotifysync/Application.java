package com.example.spotifysync;

import com.example.spotifysync.schema.FrontendPlayResponse;
import com.example.spotifysync.schema.SpotifyCurrentPlaying;
import com.example.spotifysync.utils.SpotifyUtils;
import com.example.spotifysync.utils.YouTubeUtils;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/**
 * This file runs the Spring Application and is also a Spring MVC Controler for the Spotify Youtube
 * Application
 */
@Controller
@SpringBootApplication
public class Application {
  private final SpotifyUtils spotifyUtils = new SpotifyUtils();
  private final YouTubeUtils youTubeUtils = new YouTubeUtils();

  /**
   * Routes request to the index page. Currently a place holder.
   */
  @GetMapping("/")
  public String home() {
    return "index";
  }

  /**
   * Routes request to the index page. Currently a place holder.
   */
  @GetMapping("/index")
  public String index() {
    return "index";
  }

  /**
   * Error routing
   */
  @GetMapping("/error")
  public String error() {
    return "error";
  }

  /**
   * Endpoint that calls Spotify to get updated playback information for the authenticated user
   */
  @GetMapping(value = "/update", produces = "application/json")
  @ResponseBody
  public FrontendPlayResponse update(
      @CookieValue(value = "access_token", defaultValue = "") String accessToken,
      final @CookieValue(value = "refresh_token", defaultValue = "") String refreshToken,
      @RequestParam(name = "spotifyUri", required = false) final String spotifyUriPlayingOnFrontend,
      final Model model,
      final HttpServletResponse httpServletResponse
  ) {

    Map<String, String> response = new HashMap<>();
    //Check Access Token
    accessToken = spotifyUtils.verifyOrRefreshSpotifyAccessToken(accessToken, refreshToken, httpServletResponse, model);
    if (accessToken == null) {
      //error
      return new FrontendPlayResponse("Could not get access token from Spotify");
    }

    //Ask Spotify for Current Playing
    final SpotifyCurrentPlaying currentPlaying = spotifyUtils.getCurrentPlayingFromSpotify(accessToken);

    if (currentPlaying == null) {
      return new FrontendPlayResponse("Could not get access token from Spotify");
    } else if (currentPlaying.isEmpty()) {
      response.put("empty", "No data found");
      return new FrontendPlayResponse();
    }

    final String youTubeId;

    // Make API request to YouTube if no spotify URI provided from front end or if frontend Spotify
    // URI does not match up to date URI from Spotify
    if (!currentPlaying.getSpotifyUri().equals(spotifyUriPlayingOnFrontend)) {
      youTubeId = youTubeUtils.getYouTubeLinkFromSpotifyTrack(currentPlaying);
    } else {
      youTubeId = null;
    }

    //Return results
    return new FrontendPlayResponse(
        currentPlaying.getSpotifyUri(),
        youTubeId,
        currentPlaying.getProgressSeconds() + 1,
        currentPlaying.isPlaying());
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
