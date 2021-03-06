package cz.muni.fi.pv254.resource;

import cz.muni.fi.pv254.algorithms.Normalizer;
import cz.muni.fi.pv254.algorithms.SlopeOne;
import cz.muni.fi.pv254.algorithms.TextAnalyzer;
import cz.muni.fi.pv254.data.Anime;
import cz.muni.fi.pv254.data.AnimeEntry;
import cz.muni.fi.pv254.data.RemoteUser;
import cz.muni.fi.pv254.data.User;
import cz.muni.fi.pv254.data.dto.AnimeDTO;
import cz.muni.fi.pv254.data.dto.RecommendationDTO;
import cz.muni.fi.pv254.data.dto.RecommendationInputDTO;
import cz.muni.fi.pv254.data.enums.AnimeEntryStatus;
import cz.muni.fi.pv254.data.enums.Genre;
import cz.muni.fi.pv254.dataUtils.DataStore;
import cz.muni.fi.pv254.repository.RemoteUserRepository;
import cz.muni.fi.pv254.utils.Utils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by suomiy on 11/29/15.
 */
@Path("/recommend")
@ApplicationScoped
public class RecommendationResource {

    @Inject
    DataStore dataStore;

    @Inject
    Normalizer normalizer;

    @Inject
    TextAnalyzer textAnalyzer;

    @Inject
    RemoteUserRepository remoteUserRepository;

    private static final int minOneSlopeEntries = 7;
    private static final int minTfIdfEntries = 1;
    private static final int maxOneSlopeWeirdEntries = 3;
    private static final int outputSize = 20;


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public RecommendationDTO recommend(RecommendationInputDTO input, @Context HttpServletRequest request) {
        List<AnimeEntry> entries = input.getEntries();

        if(entries == null) {
            return  null;
        }
        RecommendationDTO result = new RecommendationDTO();
        logUser(request, entries);
        List<AnimeEntry> allEntries = entries.stream().filter(e -> e.getMalId() != 0).collect(Collectors.toList());
        List<AnimeEntry> slopeOneEntries = allEntries.stream()
                .filter(e -> e.getScore() != 0 && !dataStore.findAnimeByMalId(e.getMalId()).isDeleted())
                .collect(Collectors.toList());

        //Tf-idf
        if(allEntries.size() >= minTfIdfEntries){
            User user = new User();
            user.setAnimeEntries(allEntries);
            result.setTfIdfList(getTfIdfList(input.getGenres(), user));
        }else{
            result.setTfIdfListMessage("You need at least one anime in your list for Tf-Idf.");
        }

        // Slope One
        if(slopeOneEntries.size() >= minOneSlopeEntries){
            User user = new User();
            user.setAnimeEntries(slopeOneEntries);
            normalizer.normalizeUser(user);

            result.setSlopeOneList(getSlopeOneList(input.getGenres(), user));
            result.setSlopeOneWeirdList(getSlopeOneWeirdList(input.getGenres(), user));
        }else{
            String message = "Only " + slopeOneEntries.size() + " scored animes designated for One Slope algorithm are in your list. Minimum of " + minOneSlopeEntries + "  is required.";
            result.setSlopeOneListMessage(message);
            result.setSlopeOneWeirdListMessage(message);
        }

        result.setRandomList(getRandomList(input.getGenres()));



        return result;
    }

    private List<AnimeDTO> getRandomList(Map<Genre, Boolean> genres){
        return Utils.getRandomSubList(dataStore.findAnimesForTextAnalysis().stream()
                .filter(a -> filterByGenre(a, genres))
                .collect(Collectors.toList()), outputSize).stream()
                .map(e -> new AnimeDTO(e, 0D))
                .collect(Collectors.toList());
    }

    private boolean filterByGenre(Anime anime, Map<Genre, Boolean> genres) {
        boolean valid = true;
        for(Genre g :  anime.getGenres()){
            if(genres.get(g) == null || !genres.get(g)){
                valid = false;
                break;
            }
        }
        return valid;
    }

    private List<AnimeDTO> getTfIdfList(Map<Genre, Boolean> genres, User user){
        Map<Anime, Float> map = Utils.sortByValue(textAnalyzer.recommendToUser(user));
        return  map.entrySet().stream()
                .filter(e -> filterByGenre(e.getKey(), genres))
                .limit(outputSize).map(e -> new AnimeDTO(e.getKey(), (double) e.getValue()))
                .collect(Collectors.toList());
    }

    private List<AnimeDTO> getSlopeOneList(Map<Genre, Boolean> genres, User user){
        Map<Anime, Double> map = Utils.sortByValue((new SlopeOne(dataStore, false)).recommendToUser(user, minOneSlopeEntries, null));
        return map.entrySet().stream()
                .filter(e -> filterByGenre(e.getKey(), genres))
                .limit(outputSize).map(e -> new AnimeDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private List<AnimeDTO> getSlopeOneWeirdList(Map<Genre, Boolean> genres, User user){
        Map<Anime, Double> map = Utils.sortByValue((new SlopeOne(dataStore, false)).recommendToUser(user, null, maxOneSlopeWeirdEntries));
        return map.entrySet().stream()
                .filter(e -> filterByGenre(e.getKey(), genres))
                .limit(outputSize).map(e -> new AnimeDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private void logUser(HttpServletRequest request, List<AnimeEntry> entries){
        try{
            String address = request.getRemoteAddr();
            if(address != null){
                OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));
                RemoteUser remoteUser = remoteUserRepository.findByAddress(address);
                String agent = request.getHeader("user-agent");
                if(remoteUser == null){
                    remoteUser = new RemoteUser(address, agent, now, 1L);
                    remoteUser.setAnimeEntries(entries);
                    remoteUserRepository.create(remoteUser);
                }else{
                    remoteUser.setUserAgent(agent);
                    remoteUser.setVisitsCount(remoteUser.getVisitsCount() + 1);
                    remoteUser.setLastSeen(now);
                    remoteUser.setAnimeEntries(entries);
                    remoteUserRepository.update(remoteUser);
                }
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

}
