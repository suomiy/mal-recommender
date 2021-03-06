package cz.muni.fi.pv254.utils; /**
 * Created by skylar on 20.11.15.
 */

/**
 * Created by skylar on 20.11.15.
 */

import cz.muni.fi.pv254.data.Anime;
import cz.muni.fi.pv254.data.AnimeEntry;
import cz.muni.fi.pv254.data.User;
import cz.muni.fi.pv254.data.enums.AnimeEntryStatus;
import cz.muni.fi.pv254.data.enums.AnimeType;
import cz.muni.fi.pv254.dataUtils.DataStore;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


@ApplicationScoped
public class AnimeListParser {

    @Inject
    DataStore dataStore;

    private static final String listsFolder = "/home/ansy/Downloads/mal_data/allLists";

    public void run() {
        try{
            File[] files = new File(listsFolder).listFiles();

            if (files == null) {
                throw new IOException("Not a directory!");
            }

            for (File file : files) {
                String name = file.getName().replaceFirst("[.][^.]+$", "");
                log(getCurrentTimeStamp() + ": " + name);

                Document dom = parseXmlFile(file);
                traverseDocument(dom, name);

                logln(" - Done!");
            }

            dataStore.flush();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private Document parseXmlFile(File file) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();

        return db.parse(file.getPath());
    }


    private void traverseDocument(Document dom, String name) throws Exception {
        Element root = dom.getDocumentElement();

        NodeList myInfos = root.getElementsByTagName("myinfo");
        User user = dataStore.findUserByName(name);

        if(user == null){
            log("- not found!");
            return;
        }

        if(myInfos == null || myInfos.getLength() == 0){
            log( "- deleted.");
            try { dataStore.deleteDbUser(user.getId()); } catch (Exception e) { e.printStackTrace(); }
            return;
        }

        for(int i = 0 ; i < myInfos.getLength(); i++) {
            updateUser(user, (Element)myInfos.item(i));
        }


        NodeList animes = root.getElementsByTagName("anime");

        if(animes == null || animes.getLength() == 0){
            return;
        }

        List<AnimeEntry> usersAnimeEntries = user.getAnimeEntries();

        for(int i = 0 ; i < animes.getLength(); i++) {
            processAnime(usersAnimeEntries, (Element)animes.item(i));
        }
    }


    private void updateUser(User user, Element userElement) throws IllegalAccessException {

        String name = getTextValue(userElement, "user_name");

        if(!name.equals(user.getName())){
            log("Invalid username for user" + user.getName() + " is " + name + "!");
            throw new IllegalArgumentException(user.getName() + " vs " + name);
        }

        int id = getIntValue(userElement, "user_id");

        user.setMalId((long) id);
    }

    private void processAnime(List<AnimeEntry> usersAnimeEntries, Element animeElem) throws Exception {
        Long malId = (long) getIntValue(animeElem, "series_animedb_id");
        Anime anime = dataStore.findAnimeByMalId(malId);

        if(anime == null) {
            String title = getTextValue(animeElem, "series_title");
            String imageLink = getTextValue(animeElem, "series_image");

            Long episodes = (long) getIntValue(animeElem, "series_episodes");
            AnimeType type = AnimeType.get(getIntValue(animeElem, "series_type"));

            anime = new Anime(title, imageLink, malId, episodes, type);
            dataStore.createAnime(anime);
        }

        AnimeEntryStatus status =  AnimeEntryStatus.get(getIntValue(animeElem, "my_status"));
        Integer score =  getIntValue(animeElem, "my_score");
        usersAnimeEntries.add(new AnimeEntry(anime.getMalId(), score, status));

    }


    private String getTextValue(Element element, String tagName) {
        String textVal = null;
        NodeList nl = element.getElementsByTagName(tagName);
        if(nl != null && nl.getLength() > 0) {
            Element el = (Element)nl.item(0);
            textVal = el.getFirstChild().getNodeValue();
        }
        return textVal;
    }

    private int getIntValue(Element element, String tagName) {
        return Integer.parseInt(getTextValue(element,tagName));
    }

    private String getCurrentTimeStamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    private void log(String message){
        System.out.print(message);
    }

    private void logln(String message){
        System.out.println(message);
    }
}