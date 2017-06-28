import com.gargoylesoftware.htmlunit.ScriptResult
import com.gargoylesoftware.htmlunit.html.DomNode
import com.gargoylesoftware.htmlunit.html.DomNodeList
import com.gargoylesoftware.htmlunit.html.HtmlPage

import java.text.SimpleDateFormat

/**
 * Created by Max on 12.06.2017.
 */
class FocusScraper extends ScraperJob{
    String BASE_URL = "http://www.focus.de"

    FocusScraper(String name){
        super(name)
    }

    List<String> articleLinks(DomNodeList<DomNode> links){
        List<String> articlelinks = []
        links.each {
            if(isStopped()) return
            String link
            if(it.getAttributes().getNamedItem("href") != null){
                link = it.getAttributes().getNamedItem("href").nodeValue
            }
            else return
            if(!link.startsWith("http")){
                link = "$BASE_URL$link"
            }
            if(link.startsWith("http://rss")) return
            articlelinks.add(link)
        }
        return articlelinks
    }

    @Override
    void run(){
        logger.info("Started execution of ${this.scraperName}")

        HtmlPage politikPage = getPageResponse("http://www.focus.de/politik/")
        HtmlPage finanzPage = getPageResponse("http://www.focus.de/finanzen/")
        HtmlPage sportPage = getPageResponse("http://www.focus.de/sport/")

        DomNodeList<DomNode> politikLinks = politikPage.querySelectorAll(".block.grid_8 A")
        DomNodeList<DomNode> finanzLinks = finanzPage.querySelectorAll(".block.grid_8 A")
        DomNodeList<DomNode> sportLinks = sportPage.querySelectorAll(".block.grid_8 A")

        List<String> politikArticleLinks = articleLinks(politikLinks)
        List<String> finanzArticleLinks = articleLinks(finanzLinks)
        List<String> sportArticleLinks = articleLinks(sportLinks)

        scrapeLinks(politikArticleLinks, "politik")
        scrapeLinks(finanzArticleLinks, "finanzen")
        scrapeLinks(sportArticleLinks, "sport")

        saveResults()
        logger.info("Finished Execution of ${this.scraperName}")
    }

    void scrapeLinks(List<String> articleLinks, String tag){
        int size = articleLinks.size()
        int done = 0
        articleLinks.each{

            if(isStopped()) return
                if(!it.startsWith("http://www.finanzen100.de/")){
                HtmlPage article = getPageResponse(it)

                if(article != null){
                    String textResult = ""

                    DomNodeList<DomNode> articleTexts = article.querySelectorAll(".textBlock P")

                    if(articleTexts.size() > 0){
                        articleTexts.each {
                            textResult+=it.getTextContent() + "\n\n"
                        }

                        //textResult.replaceAll("\n", "")

                        DomNode time = article.querySelector(".displayDate A")
                        if(time){
                            String dateString = time.getTextContent()
                            List<String>dateStrings = dateString.split(",")

                            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy hh:mm")
                            Date date = sdf.parse(dateStrings.get(1)+" "+dateStrings.get(2))

                            addText(textResult, date,it)
                        }else{
                            logger.warn("Couldn't find date on $it")
                            addText(textResult, null, it)
                        }
                    }else{
                        logger.warn("Couldn't find article Texts on $it")
                    }
                    done++

                    double percentageD = (done * 100) / size
                    int percentage = Math.floor(percentageD)
                    if(percentage % 5 == 0){
                        if(!NewsScraperMain.log.getText().contains("$percentage% done...")){
                            logger.info("$percentage% done...")
                        }
                    }
                }
            }
        }

    }


}
