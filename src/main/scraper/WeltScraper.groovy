import com.gargoylesoftware.htmlunit.html.DomNode
import com.gargoylesoftware.htmlunit.html.DomNodeList
import com.gargoylesoftware.htmlunit.html.HtmlPage
import java.text.SimpleDateFormat

/**
 * @author Georg Westerholt
 */
class WeltScraper extends ScraperJob{

    String BASE_URL = "http://www.welt.de"

    WeltScraper(String name) {
        super(name)
    }

    @Override
    void run() {
        logger.info("Started execution of ${this.scraperName}")
        HtmlPage page = getPageResponse("https://www.welt.de/themen/alle-artikel-der-welt")

        DomNodeList<DomNode> links = page.querySelectorAll("span.headline A")

        List<String> articleLinks = []

        links.each {
            if(isStopped()) return
            String link = it.getAttributes().getNamedItem("href").nodeValue
            if(!link.startsWith("http")){
                link = "$BASE_URL$link"
            }

            articleLinks << link
        }

        int size = articleLinks.size()
        int done = 0

        articleLinks.each {
            if(isStopped()) return
            HtmlPage article = getPageResponse(it)

            if(article != null){
                String textResult = ""

                DomNodeList<DomNode> articleTexts = article.querySelectorAll(".c-article-text P")

                if(articleTexts.size() > 0){
                    articleTexts.each {
                        textResult+=it.getTextContent() + "\n\n"
                    }

                    //textResult.replaceAll("\n", "")

                    DomNode time = article.querySelector(".c-publish-date ")
                    if(time){
                        String dateString = time.getAttributes().getNamedItem("datetime")?.nodeValue

                        Date date = null
                        if(dateString!= null){
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                            date = sdf.parse(dateString)
                        }

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
        logger.info("Execution of ${this.scraperName} ended.")
    }
}
