package ca.vishal.assignment1_vishal;

import android.net.Uri;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MovieViewModel extends ViewModel {
    private final MutableLiveData<List<Movie>> movieList = new MutableLiveData<>();
    private static final String API_KEY = "e5bfd832"; // Keep your API key here
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public LiveData<List<Movie>> getMovieList() {
        return movieList;
    }

    public void searchMovies(String query) {
        executorService.execute(() -> {
            try {
                // 1. Search for a list of movies
                URL searchUrl = new URL("https://www.omdbapi.com/?apikey=" + API_KEY + "&s=" + Uri.encode(query));
                String searchResponse = getResponseFromUrl(searchUrl);

                if (searchResponse != null) {
                    JSONObject searchJsonObject = new JSONObject(searchResponse);
                    if (searchJsonObject.optString("Response", "False").equals("True")) {
                        JSONArray searchResultsArray = searchJsonObject.getJSONArray("Search");
                        List<Movie> detailedMovies = new ArrayList<>();

                        // 2. For each movie found, get its full details
                        for (int i = 0; i < searchResultsArray.length(); i++) {
                            JSONObject movieStub = searchResultsArray.getJSONObject(i);
                            String imdbID = movieStub.getString("imdbID");
                            URL detailUrl = new URL("https://www.omdbapi.com/?apikey=" + API_KEY + "&i=" + imdbID);
                            String detailResponse = getResponseFromUrl(detailUrl);

                            if (detailResponse != null) {
                                JSONObject movieJson = new JSONObject(detailResponse);
                                Movie movie = parseMovieFromJson(movieJson);
                                detailedMovies.add(movie);
                            }
                        }
                        // Update the UI on the main thread
                        movieList.postValue(detailedMovies);
                    } else {
                        // Handle cases where the search returns no results
                        movieList.postValue(new ArrayList<>());
                    }
                }
            } catch (Exception e) {
                Log.e("MovieViewModel", "Error during movie search", e);
                movieList.postValue(new ArrayList<>());
            }
        });
    }

    private String getResponseFromUrl(URL url) {
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            Log.e("MovieViewModel", "Error fetching data from URL: " + url, e);
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private Movie parseMovieFromJson(JSONObject jsonObject) {
        Movie movie = new Movie();
        movie.setTitle(jsonObject.optString("Title", "N/A"));
        movie.setYear(jsonObject.optString("Year", "N/A"));
        movie.setRated(jsonObject.optString("Rated", "N/A"));
        movie.setPlot(jsonObject.optString("Plot", "N/A"));
        movie.setPoster(jsonObject.optString("Poster", "N/A"));
        movie.setImdbRating(jsonObject.optString("imdbRating", "N/A"));
        movie.setImdbID(jsonObject.optString("imdbID", "N/A"));
        return movie;
    }
}