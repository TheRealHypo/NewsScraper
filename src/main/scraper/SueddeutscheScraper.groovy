import com.gargoylesoftware.htmlunit.html.DomNode
import com.gargoylesoftware.htmlunit.html.DomNodeList
import com.gargoylesoftware.htmlunit.html.HtmlPage

import java.text.SimpleDateFormat

/**
 * Created by Max on 12.06.2017.
 */
class SueddeutscheScraper extends ScraperJob{
    String BASE_URL = "http://www.sueddeutsche.de/"

    SueddeutscheScraper(String name){
        super(name)
    }

    @Override
    void run(){
        logger.info("Started execution of ${this.scraperName}")

        String url = "http://www.sueddeutsche.de/"
        HtmlPage page = getPageResponse(url)

        DomNodeList<DomNode> links = page.querySelectorAll(".mainpage A")

        List<String> articleLinks = []
        links.each {
            if(isStopped()) return
            String link = it.getAttributes().getNamedItem("href").nodeValue
            if(!link.startsWith("http")){
                link = "$BASE_URL$link"
            }
            if(link.startsWith(BASE_URL))
            articleLinks << link
        }

        int size = articleLinks.size()
        int done = 0

        articleLinks.each {
            if(isStopped()) return
            HtmlPage article = getPageResponse(it)

            if(article != null){
                String textResult = ""

                DomNodeList<DomNode> articleTexts = article.querySelectorAll(".body P")

                if(articleTexts.size() > 0){
                    articleTexts.each {
                        textResult+=it.getTextContent() + "\n\n"
                    }

                    //textResult.replaceAll("\n", "")

                    DomNode time = article.querySelector(".header time")
                    if(time){
                        String dateString = time.getAttributes().getNamedItem("datetime").nodeValue

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd k:mm:ss")
                        Date date = sdf.parse(dateString)

                        addText(textResult, date, it)
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

        saveResults()
        logger.info("Finished Execution of ${this.scraperName}")
    }
}

