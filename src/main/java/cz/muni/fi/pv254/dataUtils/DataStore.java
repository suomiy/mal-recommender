package cz.muni.fi.pv254.dataUtils;

import cz.muni.fi.pv254.data.Anime;
import cz.muni.fi.pv254.data.AnimeEntry;
import cz.muni.fi.pv254.data.User;
import cz.muni.fi.pv254.repository.AnimeRepository;
import cz.muni.fi.pv254.repository.UserRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by skylar on 23.11.15.
 */
@ApplicationScoped
public class DataStore {

    @Inject
    UserRepository userRepository;

    @Inject
    AnimeRepository animeRepository;

    private List<User> users;
    private List<Anime> animes;

    public List<Anime> findAllAnimes() {
        return animes;
    }

    public List<User> findAllUsers() {
        return users;
    }

    public User findUserByName(String name){
        return findUser(u -> u.getName() != null && u.getName().equals(name));
    }

    public User findUserById(Long Id){
        return findUser(u -> u.getId() != null && u.getId().longValue() ==  Id);
    }

    public Anime findAnimeById(Long Id){
        return findAnime(a -> a.getId() != null && a.getId().longValue() ==  Id);
    }

    public Anime findAnimeByMalId(Long malId) {
        return findAnime(a -> a.getMalId() != null && a.getMalId().longValue() ==  malId);
    }

    public User findUserByMalId(Long malId) {
        return findUser(u -> u.getMalId() != null && u.getMalId().longValue() ==  malId);
    }

    public void deleteDbUser(Long id) throws Exception {
        users = findUsers( u -> u.getId().longValue() !=  id);
        userRepository.delete(id);
    }

    public void createAnime(Anime anime) throws Exception {
        animes.add(anime);
    }


    public List<AnimeEntry> findAllAnimeEntries() {
        List<AnimeEntry>  entries = new ArrayList<>();
        users.forEach(u -> entries.addAll(u.getAnimeEntries()));
        return entries;
    }

    public List<AnimeEntry> findAllAnimeEntriesWithScore() {
        return filterUnscoredEntries(findAllAnimeEntries());
    }

    public Map<User, List<AnimeEntry>> findUsersWithEntries() {
        return users.stream().collect(Collectors.toMap(u -> u, User::getAnimeEntries));
    }

    public Map<User, List<AnimeEntry>> findUsersWithEntriesWithScore() {
        Map<User, List<AnimeEntry>> usersEntries = findUsersWithEntries();
        usersEntries.replaceAll((k, v) -> filterUnscoredEntries(v));
        return usersEntries;
    }

    public User findUser(Predicate<User> predicate){
        return users.stream().filter(predicate).findFirst().orElse(null);
    }

    public List<User> findUsers(Predicate<User> predicate){
        return users.stream().filter(predicate).collect(Collectors.toList());
    }

    public Anime findAnime(Predicate<Anime> predicate){
        return animes.stream().filter(predicate).findFirst().orElse(null);
    }

    public List<Anime> findAnimes(Predicate<Anime> predicate){
        return animes.stream().filter(predicate).collect(Collectors.toList());
    }

    public void fetchData(){
        users = userRepository.findAll();
        animes = animeRepository.findAll();
    }

    public void flush(){
        userRepository.batchUpdate(users);
        animeRepository.batchUpdate(animes);
    }

    private List<AnimeEntry> filterUnscoredEntries(List<AnimeEntry> entries){
        return entries.stream().filter(a -> a.getScore() > 0).collect(Collectors.toList());
    }
}